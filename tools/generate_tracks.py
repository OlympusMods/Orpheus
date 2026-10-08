import json
import re
import sys
import urllib.request
from pathlib import Path

SOURCE = "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/{version}/assets/minecraft/sounds.json"

FAMILIES = {
    "mc1_20": ["1.20.1"],
    "mc1_21": ["1.21.1"],
    "mc26": ["26.1", "26.2", "26.3"],
}

EVENT_CONTEXTS = [
    (re.compile(r"^music\.menu$"), "MAIN_MENU"),
    (re.compile(r"^music\.creative$"), "CREATIVE"),
    (re.compile(r"^music\.game$"), "SURVIVAL"),
    (re.compile(r"^music\.overworld\..+$"), "SURVIVAL"),
    (re.compile(r"^music\.under_water$"), "UNDERWATER"),
    (re.compile(r"^music\.nether\..+$"), "NETHER"),
    (re.compile(r"^music\.end$"), "END"),
    (re.compile(r"^music\.dragon$"), "BOSS"),
    (re.compile(r"^music\.credits$"), "CREDITS"),
]

NESTED = {"music.creative": ["music.game"]}


def fetch(version):
    with urllib.request.urlopen(SOURCE.format(version=version), timeout=60) as response:
        return json.load(response)


def context_of(event):
    for pattern, context in EVENT_CONTEXTS:
        if pattern.match(event):
            return context
    return None


def sound_entries(definition):
    for entry in definition.get("sounds", []):
        if isinstance(entry, str):
            yield {"name": entry}
        elif entry.get("type", "file") == "file":
            yield dict(entry)


def humanize(track_id):
    leaf = track_id.rsplit("/", 1)[-1]
    leaf = re.sub(r"(\D)(\d)", r"\1 \2", leaf)
    return " ".join(part.capitalize() for part in leaf.split("_"))


def collect(versions):
    tracks = {}
    for version in versions:
        sounds = fetch(version)
        for event, definition in sounds.items():
            if not event.startswith("music."):
                continue
            if event.startswith("music_disc"):
                continue
            context = context_of(event)
            if context is None:
                continue
            contexts = {context}
            for owner, nested in NESTED.items():
                if event in nested:
                    contexts.add(context_of(owner))
            for entry in sound_entries(definition):
                track_id = entry["name"]
                track = tracks.setdefault(track_id, {"attributes": {}, "contexts": set()})
                track["contexts"].update(contexts)
                for key, value in entry.items():
                    if key not in ("name", "weight", "stream", "type"):
                        track["attributes"].setdefault(key, value)
    return tracks


def event_name(track_id):
    return track_id.replace("/", ".")


def write_family(root, family, tracks):
    target = root / "common" / "generated" / family / "assets" / "orpheus"
    target.mkdir(parents=True, exist_ok=True)
    sounds = {}
    catalog = []
    for track_id in sorted(tracks):
        track = tracks[track_id]
        sound = {"name": "minecraft:" + track_id, "stream": True}
        sound.update(track["attributes"])
        sounds[event_name(track_id)] = {"sounds": [sound]}
        catalog.append(
            {
                "id": track_id,
                "event": event_name(track_id),
                "title": humanize(track_id),
                "contexts": sorted(track["contexts"]),
            }
        )
    (target / "sounds.json").write_text(json.dumps(sounds, indent=2) + "\n")
    (target / "tracks.json").write_text(json.dumps({"tracks": catalog}, indent=2) + "\n")
    return len(catalog)


def main():
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent
    for family, versions in FAMILIES.items():
        count = write_family(root, family, collect(versions))
        print(family, count)


if __name__ == "__main__":
    main()
