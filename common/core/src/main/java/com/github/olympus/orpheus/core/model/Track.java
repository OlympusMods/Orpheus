package com.github.olympus.orpheus.core.model;

import java.util.Set;

public record Track(String id, String event, String title, Set<MusicContext> contexts) {

    public Track {
        contexts = Set.copyOf(contexts);
    }

    public boolean defaultsTo(MusicContext context) {
        return contexts.contains(context);
    }
}
