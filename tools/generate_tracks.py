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


TITLES = {
    "music/game/calm1": "Minecraft",
    "music/game/calm2": "Clark",
    "music/game/calm3": "Sweden",
    "music/game/hal1": "Subwoofer Lullaby",
    "music/game/hal2": "Living Mice",
    "music/game/hal3": "Haggstrom",
    "music/game/hal4": "Danny",
    "music/game/nuance1": "Key",
    "music/game/nuance2": "Oxygène",
    "music/game/piano1": "Dry Hands",
    "music/game/piano2": "Wet Hands",
    "music/game/piano3": "Mice on Venus",
    "music/game/creative/creative1": "Biome Fest",
    "music/game/creative/creative2": "Blind Spots",
    "music/game/creative/creative3": "Haunt Muskie",
    "music/game/creative/creative4": "Aria Math",
    "music/game/creative/creative5": "Dreiton",
    "music/game/creative/creative6": "Taswell",
    "music/game/nether/nether1": "Concrete Halls",
    "music/game/nether/nether2": "Dead Voxel",
    "music/game/nether/nether3": "Warmth",
    "music/game/nether/nether4": "Ballad of the Cats",
    "music/menu/menu1": "Mutation",
    "music/menu/menu2": "Moog City 2",
    "music/menu/menu3": "Beginning 2",
    "music/menu/menu4": "Floating Trees",
    "music/game/oxygene": "Oxygène",
}


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
    if track_id in TITLES:
        return TITLES[track_id]
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
    (target / "sounds.json").write_text(json.dumps(sounds, indent=2) + "\n", encoding="utf-8")
    (target / "tracks.json").write_text(json.dumps({"tracks": catalog}, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return len(catalog)


def main():
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent
    for family, versions in FAMILIES.items():
        count = write_family(root, family, collect(versions))
        print(family, count)


if __name__ == "__main__":
    main()
