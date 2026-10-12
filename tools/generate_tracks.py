import json
import re
import sys
import urllib.request
from pathlib import Path

SOURCE = "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/{version}/assets/minecraft/sounds.json"

# Every release whose music we merge into one catalog, oldest to newest.
HISTORY = [
    "1.8", "1.9", "1.10", "1.11", "1.12", "1.13", "1.14", "1.15", "1.16.1", "1.16.5", "1.17.1",
    "1.18.2", "1.19.2", "1.19.4", "1.20.1", "1.20.4", "1.20.6", "1.21", "1.21.1", "1.21.3",
    "1.21.4", "1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.9", "1.21.10", "1.21.11",
    "26.1", "26.2", "26.3",
]

# Every loader family gets the full catalog. The version is the one whose bundled
# files decide which file name a song is played from natively.
FAMILIES = {
    "mc1_20": "1.20.1",
    "mc1_21": "1.21.1",
    "mc26": "26.1",
}

# Mojang renamed many song files in 1.20.3. The old name and the new name are the
# same song, so the new name is the canonical id and the old one stays as an alias.
ALIASES = {
    "music/game/calm1": "music/game/minecraft",
    "music/game/calm2": "music/game/clark",
    "music/game/calm3": "music/game/sweden",
    "music/game/hal1": "music/game/subwoofer_lullaby",
    "music/game/hal2": "music/game/living_mice",
    "music/game/hal3": "music/game/haggstrom",
    "music/game/hal4": "music/game/danny",
    "music/game/nuance1": "music/game/key",
    "music/game/nuance2": "music/game/oxygene",
    "music/game/piano1": "music/game/dry_hands",
    "music/game/piano2": "music/game/wet_hands",
    "music/game/piano3": "music/game/mice_on_venus",
    "music/game/creative/creative1": "music/game/creative/biome_fest",
    "music/game/creative/creative2": "music/game/creative/blind_spots",
    "music/game/creative/creative3": "music/game/creative/haunt_muskie",
    "music/game/creative/creative4": "music/game/creative/aria_math",
    "music/game/creative/creative5": "music/game/creative/dreiton",
    "music/game/creative/creative6": "music/game/creative/taswell",
    "music/game/nether/nether1": "music/game/nether/concrete_halls",
    "music/game/nether/nether2": "music/game/nether/dead_voxel",
    "music/game/nether/nether3": "music/game/nether/warmth",
    "music/game/nether/nether4": "music/game/nether/ballad_of_the_cats",
    "music/menu/menu1": "music/menu/mutation",
    "music/menu/menu2": "music/menu/moog_city_2",
    "music/menu/menu3": "music/menu/beginning_2",
    "music/menu/menu4": "music/menu/floating_trees",
    "music/game/end/end": "music/game/end/the_end",
    # The credits song is called "Alpha"; its file was renamed in 1.20.3 as well.
    "music/game/end/credits": "music/game/end/alpha",
}

EVENT_CONTEXTS = [
    (re.compile(r"^music\.menu$"), "MAIN_MENU"),
    (re.compile(r"^music\.creative$"), "CREATIVE"),
    (re.compile(r"^music\.game$"), "SURVIVAL"),
    (re.compile(r"^music\.overworld\..+$"), "SURVIVAL"),
    (re.compile(r"^music\.under_water$"), "UNDERWATER"),
    (re.compile(r"^music\.nether(\..+)?$"), "NETHER"),
    (re.compile(r"^music\.end$"), "END"),
    (re.compile(r"^music\.dragon$"), "BOSS"),
    (re.compile(r"^music\.credits$"), "CREDITS"),
]

NESTED = {"music.creative": ["music.game"]}


# Songs from C418's Minecraft Volume Alpha and Volume Beta that were never added to the game.
# Their audio is not in Mojang's files, so the player supplies it; only the names are listed here.
# Edit this list to add or remove entries, then run the script again.
LEFTOVERS = {
    "beginning": "Beginning",
    "chris": "Chris",
    "death": "Death",
    "dog": "Dog",
    "door": "Door",
    "droopy_likes_ricochet": "Droopy Likes Ricochet",
    "droopy_likes_your_face": "Droopy Likes Your Face",
    "equinoxe": "Équinoxe",
    "excursions": "Excursions",
    "flake": "Flake",
    "ki": "Ki",
    "kyoto": "Kyoto",
    "moog_city": "Moog City",
}

TITLES = {
    "records/creator_music_box": "Creator (Music Box)",
    "music/game/oxygene": "Oxygène",
    "music/game/end/the_end": "The End",
}

SMALL_WORDS = {"a", "an", "and", "in", "of", "on", "the", "to"}


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
    words = leaf.split("_")
    result = []
    for index, word in enumerate(words):
        if index > 0 and word in SMALL_WORDS:
            result.append(word)
        else:
            result.append(word.capitalize())
    return " ".join(result)


_sounds_cache = {}


def sounds_of(version):
    if version not in _sounds_cache:
        _sounds_cache[version] = fetch(version)
    return _sounds_cache[version]


def collect(versions):
    """Returns {file id: {"attributes", "contexts", "kind"}} merged over all versions."""
    tracks = {}
    for version in versions:
        for event, definition in sounds_of(version).items():
            if event.startswith("music_disc."):
                kind, contexts = "DISC", set()
            elif event.startswith("music."):
                context = context_of(event)
                if context is None:
                    continue
                kind, contexts = "GAME", {context}
                for owner, nested in NESTED.items():
                    if event in nested:
                        contexts.add(context_of(owner))
            else:
                continue
            for entry in sound_entries(definition):
                track = tracks.setdefault(entry["name"], {"attributes": {}, "contexts": set(), "kind": kind})
                track["contexts"].update(contexts)
                for key, value in entry.items():
                    if key not in ("name", "weight", "stream", "type"):
                        track["attributes"].setdefault(key, value)
    return tracks


def merge_aliases(files):
    """Folds every renamed song into its canonical id and remembers the old names."""
    songs = {}
    for file_id, data in files.items():
        canonical = ALIASES.get(file_id, file_id)
        song = songs.setdefault(
            canonical, {"attributes": {}, "contexts": set(), "aliases": set(), "kind": data["kind"]})
        song["contexts"].update(data["contexts"])
        for key, value in data["attributes"].items():
            song["attributes"].setdefault(key, value)
        if canonical != file_id:
            song["aliases"].add(file_id)
    return songs


def native_files(version):
    names = set()
    for event, definition in sounds_of(version).items():
        if event.startswith("music.") or event.startswith("music_disc."):
            for entry in sound_entries(definition):
                names.add(entry["name"])
    return names


def event_name(track_id):
    return track_id.replace("/", ".")


def leftover_songs():
    songs = {}
    for slug, title in LEFTOVERS.items():
        songs["leftover/" + slug] = {
            "attributes": {},
            "contexts": set(),
            "aliases": set(),
            "kind": "LEFTOVER",
            "file": "orpheus:leftover/" + slug,
            "title": title,
        }
    return songs


def write_family(root, family, native_version, songs):
    target = root / "common" / "generated" / family / "assets" / "orpheus"
    target.mkdir(parents=True, exist_ok=True)
    native = native_files(native_version)
    sounds = {}
    catalog = []
    for track_id in sorted(songs):
        song = songs[track_id]
        # Play the file the game really ships; fall back to the newest name, which is
        # what the downloaded music pack provides.
        file_id = song.get("file") or next(
            (name for name in [track_id] + sorted(song["aliases"]) if name in native), track_id)
        sound = {"name": file_id if ":" in file_id else "minecraft:" + file_id, "stream": True}
        sound.update(song["attributes"])
        sounds[event_name(track_id)] = {"sounds": [sound]}
        entry = {
            "id": track_id,
            "event": event_name(track_id),
            "title": song.get("title") or humanize(track_id),
            "file": file_id,
            "kind": song["kind"],
            "contexts": sorted(song["contexts"]),
        }
        if song["aliases"]:
            entry["aliases"] = sorted(song["aliases"])
        catalog.append(entry)
    (target / "sounds.json").write_text(json.dumps(sounds, indent=2) + "\n", encoding="utf-8")
    (target / "tracks.json").write_text(
        json.dumps({"tracks": catalog}, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    return len(catalog)


def main():
    root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent.parent
    songs = merge_aliases(collect(HISTORY))
    songs.update(leftover_songs())
    for family, native_version in FAMILIES.items():
        print(family, write_family(root, family, native_version, songs))


if __name__ == "__main__":
    main()
