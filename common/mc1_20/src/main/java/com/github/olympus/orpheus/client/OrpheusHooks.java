package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.OrpheusRuntime;
import com.github.olympus.orpheus.core.model.MusicContext;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;

public final class OrpheusHooks {
    private static boolean failed;
    private static boolean managing;

    private OrpheusHooks() {
    }

    /**
     * Runs instead of the vanilla music manager tick. Returns true when Orpheus handled the
     * music this tick, which tells the caller to skip the vanilla logic completely.
     */
    public static boolean tick(Minecraft minecraft) {
        if (failed) {
            managing = false;
            return false;
        }
        try {
            OrpheusRuntime runtime = OrpheusClient.runtime();
            OrpheusClient.keys().poll(runtime, minecraft);
            Music music = minecraft.getSituationalMusic();
            MusicContext context = contextOf(music).orElse(fallbackContext(minecraft));
            boolean replaceCurrent = music != null && music.replaceCurrentMusic();
            managing = runtime.tick(context, replaceCurrent);
            return managing;
        } catch (RuntimeException exception) {
            failed = true;
            managing = false;
            OrpheusClient.LOGGER.error("Orpheus disabled itself after an error", exception);
            return false;
        }
    }

    /** True while Orpheus is the one playing music, so sound-stopping code should spare it. */
    public static boolean isManaging() {
        return managing && !failed;
    }

    /** Situations vanilla has no music event for still belong to Orpheus, never to vanilla. */
    private static MusicContext fallbackContext(Minecraft minecraft) {
        return minecraft.level == null ? MusicContext.MAIN_MENU : MusicContext.SURVIVAL;
    }

    private static Optional<MusicContext> contextOf(Music music) {
        if (music == null) {
            return Optional.empty();
        }
        ResourceLocation key = BuiltInRegistries.SOUND_EVENT.getKey(music.getEvent().value());
        if (key == null) {
            return Optional.empty();
        }
        return MusicContext.fromEvent(key.getNamespace(), key.getPath());
    }
}
