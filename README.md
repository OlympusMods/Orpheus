# Orpheus

A client-side Minecraft mod that gives you control over the game's music: which songs play, how often, and how long the game waits between them, set separately for each situation.

Runs on Fabric, Forge and NeoForge. Servers don't need it.

## Features

- Separate settings and song lists for the main menu, survival, creative, underwater, the Nether, the End, the Ender Dragon fight and the credits
- Per-song weighting (1-10) and a "don't repeat the last N songs" rule
- Minimum and maximum pause between songs
- Music discs can be added to the rotation as normal background music
- Keybinds for previous, skip, pause/resume, block current track and opening the menu
- "Now Playing" toast with the song and artist
- Presets for saving, copying and switching whole setups
- A custom menu with blur, animations and accent colors, or the old Minecraft look, set on the Proteus card at the top of the menu
- A **Music Options** button on the vanilla Options screen

## Supported versions

| Minecraft | Branch | Fabric | Forge | NeoForge |
|-----------|--------|:------:|:-----:|:--------:|
| 1.20-1.20.2 | [`1.20`](https://github.com/OlympusMods/Orpheus/tree/1.20) | ✅ | ✅ | n/a |
| 1.20.3-1.20.4 | [`1.20.3`](https://github.com/OlympusMods/Orpheus/tree/1.20.3) | ✅ | ✅ | ✅ |
| 1.20.5-1.20.6 | [`1.20.5`](https://github.com/OlympusMods/Orpheus/tree/1.20.5) | ✅ | ✅ | ✅ |
| 1.21-1.21.1 | [`1.21`](https://github.com/OlympusMods/Orpheus/tree/1.21) | ✅ | ✅ | ✅ |
| 1.21.2-1.21.3 | [`1.21.2`](https://github.com/OlympusMods/Orpheus/tree/1.21.2) | ✅ | ✅ (1.21.3) | ✅ (1.21.3) |
| 1.21.4-1.21.5 | [`1.21.4`](https://github.com/OlympusMods/Orpheus/tree/1.21.4) | ✅ | ✅ | ✅ |
| 1.21.6-1.21.8 | [`1.21.6`](https://github.com/OlympusMods/Orpheus/tree/1.21.6) | ✅ | ✅ | ✅ |
| 1.21.9-1.21.10 | [`1.21.9`](https://github.com/OlympusMods/Orpheus/tree/1.21.9) | ✅ | ✅ | ✅ |
| 1.21.11 | [`1.21.11`](https://github.com/OlympusMods/Orpheus/tree/1.21.11) | ✅ | ✅ | ✅ |
| 26.1-26.1.2 | [`26.1`](https://github.com/OlympusMods/Orpheus/tree/26.1) | ✅ | ✅ | ✅ |
| 26.2-26.3 | [`main`](https://github.com/OlympusMods/Orpheus/tree/main) | ✅ | ✅ | ✅ |

Each range of Minecraft versions that one jar per loader can cover has its own branch, named after the first version of the range. `main` is always the newest one. Fabric needs Fabric API. NeoForge has no stable release for 1.20.2 or 1.21.2 and Forge has none for 1.21.2, so those versions are covered by Fabric (and Forge for 1.20.2) only.

## Where the music comes from

No audio is bundled with the mod.

- **Missing game songs.** Songs added in later versions can be played on older ones. Orpheus reads the newest release's asset index and downloads the missing files from Mojang's asset servers into a resource pack, then enables the pack and reloads resources.
- **Unused tracks.** These are songs from the Minecraft albums that aren't in the game files, so there is nothing to download. Orpheus creates a resource pack folder with a README, and you add your own `.ogg` files with the expected names.

## Installing

1. Download the jar for your loader and Minecraft version from [Modrinth](https://modrinth.com/project/VRXL6jnh) or [Releases](https://github.com/OlympusMods/Orpheus/releases).
2. Put it in your `mods` folder together with [Proteus](https://github.com/OlympusMods/Proteus), the UI library Orpheus draws its menu and toast with. Fabric also needs Fabric API.
3. Launch the game and open **Options > Music Options**, or bind a key to the Orpheus menu under Controls.

## Texture packs

The Classic style is drawn by Proteus from normal GUI sprites, so a resource pack can restyle all of it. The shared sprites live in `assets/proteus/textures/gui/sprites/classic/`, and the five music icons in the same folder structure under `assets/orpheus/`. Buttons and other stretchy parts are nine-slice sprites: their `.png.mcmeta` is the same kind vanilla uses for its own buttons, so keep it next to the image and update `width`, `height` and `border` if you change the size.

| Sprite | Size | Notes |
|--------|------|-------|
| `button`, `button_hover`, `button_disabled`, `button_selected` | 20x20 | nine-slice, border 3. `button_selected` is the active tab or option |
| `checkbox`, `checkbox_hover`, `checkbox_checked`, `checkbox_checked_hover` | 12x12 | drawn as is |
| `field`, `field_focused` | 8x8 | nine-slice, border 2, the preset name box |
| `inset` | 8x8 | nine-slice, border 2, the value boxes next to the arrow buttons |
| `panel` | 8x8 | nine-slice, border 2, translucent group background |
| `row_hover` | 4x4 | nine-slice, border 1 |
| `row_selected` | 8x8 | nine-slice, border 2 |
| `tooltip` | 8x8 | nine-slice, border 3, also the Now Playing toast |
| `scroller` | 3x3 | nine-slice, border 1, the scrollbar handle |
| `scroller_track` | 3x3 | stretched |
| `progress` | 8x8 | nine-slice, border 2 |
| `progress_fill` | 3x3 | nine-slice, border 1 |
| `orpheus:classic/icon/play`, `pause`, `next`, `previous`, `download` | 9x9 | drawn at that size. These five belong to Orpheus, so they live in `assets/orpheus/` and not in Proteus |
| `icon/left`, `icon/right` | 6x9 | drawn at that size |
| `icon/cross` | 8x7 | drawn at that size |

The background is `assets/orpheus/textures/gui/classic/background.png`, tiled and drawn as is. Without it, Orpheus uses vanilla dirt darkened. Text colors are not part of the pack.

## Building

Each branch is one root Gradle build that drives a project per loader: `fabric`, `forge` and `neoforge`. They all compile the shared code in `common`. Every loader project is a Gradle build of its own, so it can also be opened and built on its own. Use the JDK each loader needs (`settings.gradle` lists them; Fabric needs Java 25 to run Gradle itself).

```sh
# one loader
./gradlew :fabric:build

# every loader on this branch
./gradlew build
```

Finished jars are collected into `build/dist` and named `orpheus-<loader>-<minecraft versions>-<version>.jar`. The mod version is set in the root `gradle.properties`.

## Publishing

Releases go to Modrinth and CurseForge from the same build. Put `modrinthToken` and `curseforgeToken` in `~/.gradle/gradle.properties` (or set `MODRINTH_TOKEN` and `CURSEFORGE_TOKEN`), and add a `## <version>` entry to `CHANGELOG.md`, which becomes the changelog of every upload.

```sh
# print what would be uploaded, send nothing
./gradlew publishMods -PdryRun=true

# one site, or both
./gradlew publishModrinth
./gradlew publishCurseforge
./gradlew publishMods

# one loader
./gradlew :neoforge:publishModrinth
```

Each branch is published from its own checkout. Without any token the tasks only do a dry run. The Minecraft versions each jar is uploaded for come from `publish_minecraft_versions` in each loader's `gradle.properties`.

## Project layout

This branch holds Minecraft 1.21.2 to 1.21.3. The other ranges are on the branches listed above.

```
common/src/main/java        Shared code
common/src/main/resources   Language file, textures and icon
fabric/  forge/  neoforge/  Thin per-loader projects
publish.gradle              Modrinth and CurseForge setup shared by every loader
tools/sync.sh               Copies a folder from this branch to every other branch
tools/generate_tracks.py           Builds the song catalog and sounds.json
```

`common` holds loader- and version-independent logic in `core` (song catalog, presets, playback), the screens in `client/ui`, and the glue for this Minecraft version in `client` and `mixin`. Every branch keeps the same paths, so a fix is made once and cherry-picked to the other branches. A folder that is the same everywhere can also be copied over in one go, for example `tools/sync.sh common/src/main/java/com/github/olympus/orpheus/core`, which commits it to every other branch.

When a Minecraft update breaks the code, cut a new branch from the newest one, fix what the compiler reports, and narrow the version range on the branch it came from. A branch covers the range where one jar per loader still loads and its mixin targets still exist.

## Contributing

Bug reports and pull requests are welcome. Fixes go to `main` first and are cherry-picked to the older branches, which share the same paths. For a bug, include your Minecraft version, loader and `latest.log`. Commit messages follow `type: short description` (`feat`, `fix`, `chore`, `refactor`).

## License

[GPL-3.0](LICENSE)
