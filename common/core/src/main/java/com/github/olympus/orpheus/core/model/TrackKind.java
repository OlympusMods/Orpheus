package com.github.olympus.orpheus.core.model;

import java.util.Locale;

/** Where a song comes from. The settings screen lets the player look at one kind at a time. */
public enum TrackKind {
    /** Minecraft's background music. */
    GAME,
    /** Songs from the Minecraft albums that never made it into the game. */
    LEFTOVER,
    /** Jukebox discs. */
    DISC;

    public String translationKey() {
        return "orpheus.kind." + name().toLowerCase(Locale.ROOT);
    }

    public TrackKind next() {
        TrackKind[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
