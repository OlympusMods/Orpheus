package com.github.olympus.orpheus.core.playback;

import com.github.olympus.orpheus.core.model.MusicContext;
import com.github.olympus.orpheus.core.model.Track;
import com.github.olympus.orpheus.core.model.TrackCatalog;
import com.github.olympus.orpheus.core.preset.ContextSettings;
import com.github.olympus.orpheus.core.preset.Preset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public final class TrackSelector {
    private final Random random;

    public TrackSelector(Random random) {
        this.random = random;
    }

    public List<Track> candidates(Preset preset, MusicContext context, TrackCatalog catalog) {
        ContextSettings settings = preset.settings(context);
        List<Track> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : settings.weights().entrySet()) {
            if (entry.getValue() <= 0 || preset.isBlacklisted(entry.getKey())) {
                continue;
            }
            catalog.find(entry.getKey()).ifPresent(result::add);
        }
        return result;
    }

    public Optional<Track> select(
            Preset preset, MusicContext context, TrackCatalog catalog, Collection<String> recent) {
        ContextSettings settings = preset.settings(context);
        List<Track> candidates = candidates(preset, context, catalog);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        int avoid = Math.min(settings.repeatAvoidance(), candidates.size() - 1);
        Set<String> excluded = lastEntries(recent, avoid);
        List<Track> pool = new ArrayList<>();
        int total = 0;
        for (Track track : candidates) {
            if (excluded.contains(track.id())) {
                continue;
            }
            pool.add(track);
            total += settings.weight(track.id());
        }
        if (pool.isEmpty()) {
            pool = candidates;
            total = 0;
            for (Track track : pool) {
                total += settings.weight(track.id());
            }
        }
        int roll = random.nextInt(total);
        for (Track track : pool) {
            roll -= settings.weight(track.id());
            if (roll < 0) {
                return Optional.of(track);
            }
        }
        return Optional.of(pool.get(pool.size() - 1));
    }

    private static Set<String> lastEntries(Collection<String> recent, int count) {
        Set<String> result = new HashSet<>();
        if (count <= 0) {
            return result;
        }
        List<String> list = new ArrayList<>(recent);
        for (int index = Math.max(0, list.size() - count); index < list.size(); index++) {
            result.add(list.get(index));
        }
        return result;
    }
}
