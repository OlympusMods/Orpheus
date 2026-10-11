package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.model.Track;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Tells whether the audio file of a song exists in the game's current resources.
 *
 * <p>Answers are cached until the resources are reloaded, which hands out a new
 * resource manager, so enabling a resource pack that adds songs shows up right away.
 */
public final class TrackAvailability implements Predicate<Track> {
    private final Minecraft minecraft;
    private final Map<String, Boolean> cache = new HashMap<>();
    private ResourceManager checked;

    public TrackAvailability(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public synchronized boolean test(Track track) {
        ResourceManager current = minecraft.getResourceManager();
        if (current != checked) {
            cache.clear();
            checked = current;
        }
        return cache.computeIfAbsent(track.id(), id -> exists(current, track.file()));
    }

    private static boolean exists(ResourceManager resources, String file) {
        String path = "sounds/" + file + ".ogg";
        return resources.getResource(Identifier.fromNamespaceAndPath("minecraft", path)).isPresent();
    }
}
