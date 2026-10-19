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
    private static boolean managing;
    private static String lastStop = "";
    private static boolean lastStopSpared;
    private static long lastStopNanos;

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
        if (minecraft.level != null && minecraft.player == null) {
            // Joining a world: the player does not exist yet, so the situation would wrongly read as
            // the main menu. Keep whatever Orpheus was doing and decide nothing until it is known.
            return managing;
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

    /** Remembers who asked the sound system to stop everything, to explain songs that end unexpectedly. */
    public static void noteStop(boolean spared) {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        StringBuilder text = new StringBuilder();
        for (int index = 1; index < Math.min(stack.length, 10); index++) {
            text.append("\n    at ").append(stack[index]);
        }
        lastStop = text.toString();
        lastStopSpared = spared;
        lastStopNanos = System.nanoTime();
    }

    /** How long ago the sound system was last asked to stop everything; very large if never. */
    public static long nanosSinceLastStop() {
        return lastStopNanos == 0 ? Long.MAX_VALUE : System.nanoTime() - lastStopNanos;
    }

    public static String describeLastStop() {
        if (lastStopNanos == 0) {
            return "SoundManager.stop() was never called.";
        }
        double seconds = (System.nanoTime() - lastStopNanos) / 1_000_000_000.0;
        return String.format(
                "SoundManager.stop() was last called %.1f s ago (music %s) from:%s",
                seconds, lastStopSpared ? "spared" : "NOT spared", lastStop);
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
        Identifier key = BuiltInRegistries.SOUND_EVENT.getKey(music.sound().value());
        if (key == null) {
            return Optional.empty();
        }
        return MusicContext.fromEvent(key.getNamespace(), key.getPath());
    }
}
