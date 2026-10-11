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

    public static Component onOff(boolean value) {
        return Component.translatable(value ? "orpheus.state.on" : "orpheus.state.off");
    }
}
