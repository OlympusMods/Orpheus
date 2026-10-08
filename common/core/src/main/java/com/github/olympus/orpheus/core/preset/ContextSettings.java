package com.github.olympus.orpheus.core.preset;

import com.github.olympus.orpheus.core.model.MusicContext;
import com.github.olympus.orpheus.core.model.Track;
import com.github.olympus.orpheus.core.model.TrackCatalog;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ContextSettings {
    public static final int MIN_WEIGHT = 1;
    public static final int MAX_WEIGHT = 10;
    public static final int DEFAULT_WEIGHT = 5;
    public static final int MAX_DELAY_SECONDS = 3600;
    public static final int MAX_REPEAT_AVOIDANCE = 20;
    public static final int DEFAULT_REPEAT_AVOIDANCE = 3;

    private boolean managed = true;
    private int minDelaySeconds;
    private int maxDelaySeconds;
    private int repeatAvoidance = DEFAULT_REPEAT_AVOIDANCE;
    private final Map<String, Integer> weights = new LinkedHashMap<>();

    private ContextSettings() {
    }

    public static ContextSettings defaults(MusicContext context, TrackCatalog catalog) {
        ContextSettings settings = new ContextSettings();
        settings.minDelaySeconds = context.defaultMinSeconds();
        settings.maxDelaySeconds = context.defaultMaxSeconds();
        settings.resetTracks(context, catalog);
        return settings;
    }

    public void resetTracks(MusicContext context, TrackCatalog catalog) {
        weights.clear();
        for (Track track : catalog.defaultsFor(context)) {
            weights.put(track.id(), DEFAULT_WEIGHT);
        }
    }

    public boolean managed() {
        return managed;
    }

    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    public int minDelaySeconds() {
        return minDelaySeconds;
    }

    public int maxDelaySeconds() {
        return maxDelaySeconds;
    }

    public void setMinDelaySeconds(int seconds) {
        minDelaySeconds = clamp(seconds, 0, MAX_DELAY_SECONDS);
        if (maxDelaySeconds < minDelaySeconds) {
            maxDelaySeconds = minDelaySeconds;
        }
    }

    public void setMaxDelaySeconds(int seconds) {
        maxDelaySeconds = clamp(seconds, 0, MAX_DELAY_SECONDS);
        if (minDelaySeconds > maxDelaySeconds) {
            minDelaySeconds = maxDelaySeconds;
        }
    }

    public int repeatAvoidance() {
        return repeatAvoidance;
    }

    public void setRepeatAvoidance(int count) {
        repeatAvoidance = clamp(count, 0, MAX_REPEAT_AVOIDANCE);
    }

    public boolean includes(String trackId) {
        return weights.containsKey(trackId);
    }

    public void include(String trackId) {
        weights.putIfAbsent(trackId, DEFAULT_WEIGHT);
    }

    public void exclude(String trackId) {
        weights.remove(trackId);
    }

    public boolean toggle(String trackId) {
        if (weights.containsKey(trackId)) {
            weights.remove(trackId);
            return false;
        }
        weights.put(trackId, DEFAULT_WEIGHT);
        return true;
    }

    public int weight(String trackId) {
        return weights.getOrDefault(trackId, 0);
    }

    public void setWeight(String trackId, int weight) {
        weights.put(trackId, clamp(weight, MIN_WEIGHT, MAX_WEIGHT));
    }

    public Map<String, Integer> weights() {
        return Collections.unmodifiableMap(weights);
    }

    public ContextSettings copy() {
        ContextSettings copy = new ContextSettings();
        copy.managed = managed;
        copy.minDelaySeconds = minDelaySeconds;
        copy.maxDelaySeconds = maxDelaySeconds;
        copy.repeatAvoidance = repeatAvoidance;
        copy.weights.putAll(weights);
        return copy;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
