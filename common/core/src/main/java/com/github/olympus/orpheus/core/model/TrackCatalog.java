package com.github.olympus.orpheus.core.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public final class TrackCatalog {
    private final Map<String, Track> tracks;
    private final Map<String, String> aliases;

    private TrackCatalog(Map<String, Track> tracks, Map<String, String> aliases) {
        this.tracks = tracks;
        this.aliases = aliases;
    }

    public static TrackCatalog of(Collection<Track> tracks) {
        Map<String, Track> map = new LinkedHashMap<>();
        Map<String, String> aliases = new LinkedHashMap<>();
        for (Track track : tracks) {
            map.put(track.id(), track);
            for (String alias : track.aliases()) {
                aliases.put(alias, track.id());
            }
        }
        return new TrackCatalog(Collections.unmodifiableMap(map), Collections.unmodifiableMap(aliases));
    }

    public static TrackCatalog empty() {
        return of(List.of());
    }

    public static TrackCatalog read(InputStream stream) throws IOException {
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray array = root.getAsJsonArray("tracks");
            List<Track> list = new ArrayList<>();
            for (JsonElement element : array) {
                JsonObject object = element.getAsJsonObject();
                EnumSet<MusicContext> contexts = EnumSet.noneOf(MusicContext.class);
                for (JsonElement context : object.getAsJsonArray("contexts")) {
                    try {
                        contexts.add(MusicContext.valueOf(context.getAsString()));
                    } catch (IllegalArgumentException ignored) {
                        continue;
                    }
                }
                String id = object.get("id").getAsString();
                List<String> aliases = new ArrayList<>();
                if (object.has("aliases")) {
                    for (JsonElement alias : object.getAsJsonArray("aliases")) {
                        aliases.add(alias.getAsString());
                    }
                }
                list.add(
                        new Track(
                                id,
                                object.get("event").getAsString(),
                                object.get("title").getAsString(),
                                object.has("file") ? object.get("file").getAsString() : id,
                                contexts,
                                aliases));
            }
            return of(list);
        }
    }

    public Collection<Track> all() {
        return tracks.values();
    }

    public Optional<Track> find(String id) {
        return Optional.ofNullable(tracks.get(canonicalId(id)));
    }

    public boolean contains(String id) {
        return tracks.containsKey(canonicalId(id));
    }

    /** Maps an old song id to the id the catalog uses today; unknown ids come back unchanged. */
    public String canonicalId(String id) {
        return aliases.getOrDefault(id, id);
    }

    public int size() {
        return tracks.size();
    }

    public List<Track> defaultsFor(MusicContext context) {
        List<Track> result = new ArrayList<>();
        for (Track track : tracks.values()) {
            if (track.defaultsTo(context)) {
                result.add(track);
            }
        }
        return result;
    }

    public TrackCatalog filter(Predicate<Track> predicate) {
        List<Track> kept = new ArrayList<>();
        for (Track track : tracks.values()) {
            if (predicate.test(track)) {
                kept.add(track);
            }
        }
        return of(kept);
    }
}
