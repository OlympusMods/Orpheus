package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.model.Track;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Tells whether the audio file of a song exists in the game's current resources.
 *
 * <p>The game reloads resources inside the same resource manager object, so a reload cannot be
 * detected from the outside. Answers are therefore only remembered for a moment, which keeps
 * the screens cheap and still shows a freshly enabled resource pack within a second.
 */
public final class TrackAvailability implements Predicate<Track> {
    private final Minecraft minecraft;
    private static final long REMEMBER_NANOS = 1_000_000_000L;

    private final Map<String, Boolean> cache = new HashMap<>();
    private long checkedAt;

    public TrackAvailability(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.checkedAt = System.nanoTime() - 2 * REMEMBER_NANOS;
    }

    @Override
    public synchronized boolean test(Track track) {
        ResourceManager current = minecraft.getResourceManager();
        long now = System.nanoTime();
        if (now - checkedAt > REMEMBER_NANOS) {
            cache.clear();
            checkedAt = now;
        }
        return cache.computeIfAbsent(track.id(), id -> exists(current, track.file()));
    }

    private static boolean exists(ResourceManager resources, String file) {
        String path = "sounds/" + file + ".ogg";
        return resources.getResource(new ResourceLocation("minecraft", path)).isPresent();
    }
}
