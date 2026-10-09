package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.OrpheusRuntime;
import com.github.olympus.orpheus.core.model.MusicContext;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.Music;

public final class OrpheusHooks {
    private static boolean failed;

    private OrpheusHooks() {
    }

    public static boolean tick(Minecraft minecraft) {
        if (failed) {
            return false;
        }
        try {
            OrpheusRuntime runtime = OrpheusClient.runtime();
            OrpheusClient.keys().poll(runtime, minecraft);
            Music music = minecraft.getSituationalMusic();
            Optional<MusicContext> context = contextOf(music);
            if (context.isEmpty()) {
                runtime.controller().idle();
                return false;
            }
            return runtime.tick(context.get(), music.replaceCurrentMusic());
        } catch (RuntimeException exception) {
            failed = true;
            OrpheusClient.LOGGER.error("Orpheus disabled itself after an error", exception);
            return false;
        }
    }

    private static Optional<MusicContext> contextOf(Music music) {
        if (music == null) {
            return Optional.empty();
        }
        Identifier key = BuiltInRegistries.SOUND_EVENT.getKey(music.sound().value());
        if (key == null) {
            return Optional.empty();
        }
        return MusicContext.fromEvent(key.getNamespace(), key.getPath());
    }
}
