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
- A **Music Options** button on the vanilla Options screen

## Supported versions

| Minecraft   | Fabric | Forge | NeoForge |
|-------------|:------:|:-----:|:--------:|
| 1.20.1      | ✅ | ✅ | n/a |
| 1.21.1      | ✅ | ✅ | ✅ |
| 26.1-26.3 | ✅ | ✅ | ✅ |

## Where the music comes from

No audio is bundled with the mod.

- **Missing game songs.** Songs added in later versions can be played on older ones. Orpheus reads the newest release's asset index and downloads the missing files from Mojang's asset servers into a resource pack, then enables the pack and reloads resources.
- **Unused tracks.** These are songs from the Minecraft albums that aren't in the game files, so there is nothing to download. Orpheus creates a resource pack folder with a README, and you add your own `.ogg` files with the expected names.

## Installing

1. Download the jar for your loader and Minecraft version from [Modrinth](https://modrinth.com/project/VRXL6jnh) or [Releases](https://github.com/OlympusMods/Orpheus/releases).
2. Put it in your `mods` folder. Fabric also needs Fabric API.
3. Launch the game and open **Options > Music Options**, or bind a key to the Orpheus menu under Controls.

## Building

Every loader and version is its own Gradle project, driven from one root build. Use the JDK each Minecraft version needs (the root `build.gradle` shows which).

```sh
# one target
./gradlew :Fabric-1.21.1:build

# everything
./gradlew build
```

Finished jars are collected into `build/dist` and named `orpheus-<loader>-<minecraft>-<version>.jar`. The mod version is set in the root `gradle.properties`.

## Project layout

```
common/
  core/        Loader- and version-independent logic: song catalog, presets,
               playback controller, weighted selection, pack downloader
  client/      Screens, keybinds and translations shared by every build
  mc1_20/      Minecraft-specific glue and mixins for 1.20.x
  mc1_21/      ... for 1.21.x
  mc26/        ... for 26.x
  resources/   Language file and icon
Fabric-*/  Forge-*/  NeoForge-*/    Thin per-loader projects
tools/         generate_tracks.py, builds the song catalog
```

Most behavior lives in `common/core`, which has no Minecraft dependency. The version folders only adapt it to each game version's sound engine, so a change to playback logic usually only touches `core`.

## Contributing

Bug reports and pull requests are welcome. For a bug, include your Minecraft version, loader and `latest.log`. Commit messages follow `type: short description` (`feat`, `fix`, `chore`, `refactor`).

## License

[GPL-3.0](LICENSE)
