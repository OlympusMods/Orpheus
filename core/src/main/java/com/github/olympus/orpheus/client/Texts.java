package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.model.MusicContext;
import com.github.olympus.orpheus.core.model.Track;
import net.minecraft.network.chat.Component;

/** Builds every piece of text the mod shows, so all of it goes through the language files. */
public final class Texts {
    private Texts() {
    }

    public static Component context(MusicContext context) {
        return Component.translatable(context.translationKey());
    }

    /**
     * Song names are proper names, so they default to the catalog title. A language file can
     * still translate one by adding the key orpheus.track.&lt;id with dots instead of slashes&gt;.
     */
    public static Component track(Track track) {
        return Component.translatableWithFallback("orpheus.track." + track.id().replace('/', '.'), track.title());
    }

    /** "Song - Artist", or only the song when the artist is not known. */
    public static Component nowPlaying(Track track) {
        if (track.hasArtist()) {
            return Component.translatable("orpheus.toast.description", track(track), track.artist());
        }
        return track(track);
    }

    /** 45 becomes "45s", 90 becomes "1m 30s"; the units come from the language file. */
    public static Component duration(int seconds) {
        if (seconds < 60) {
            return Component.translatable("orpheus.time.seconds", seconds);
        }
        int minutes = seconds / 60;
        int rest = seconds % 60;
        if (rest == 0) {
            return Component.translatable("orpheus.time.minutes", minutes);
        }
        return Component.translatable("orpheus.time.minutes_seconds", minutes, rest);
    }

    public static Component onOff(boolean value) {
        return Component.translatable(value ? "orpheus.state.on" : "orpheus.state.off");
    }
}
