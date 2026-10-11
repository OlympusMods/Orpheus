package com.github.olympus.orpheus.core.model;

import java.util.List;
import java.util.Set;

/**
 * One song.
 *
 * @param id       canonical id, the newest name Mojang gave the file (e.g. music/game/minecraft)
 * @param event    sound event name registered under the orpheus namespace
 * @param title    display name of the song
 * @param file     sound file the game plays this song from (differs from the id on versions
 *                 that still ship the old file name, e.g. music/game/calm1 on 1.20.1)
 * @param contexts situations this song is part of by default
 * @param aliases  older ids of the same song, kept so saved presets keep working
 */
public record Track(
        String id, String event, String title, String file, Set<MusicContext> contexts, List<String> aliases) {

    public Track {
        contexts = Set.copyOf(contexts);
        aliases = List.copyOf(aliases);
    }

    public boolean defaultsTo(MusicContext context) {
        return contexts.contains(context);
    }
}
