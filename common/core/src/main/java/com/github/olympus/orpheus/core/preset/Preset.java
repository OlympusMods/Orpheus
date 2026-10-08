package com.github.olympus.orpheus.core.preset;

import com.github.olympus.orpheus.core.model.MusicContext;
import com.github.olympus.orpheus.core.model.TrackCatalog;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class Preset {
    private final String name;
    private boolean enabled = true;
    private final Map<MusicContext, ContextSettings> contexts = new EnumMap<>(MusicContext.class);
    private final Set<String> blacklist = new LinkedHashSet<>();

    private Preset(String name) {
        this.name = name;
    }

    public static Preset defaults(String name, TrackCatalog catalog) {
        Preset preset = new Preset(name);
        for (MusicContext context : MusicContext.values()) {
            preset.contexts.put(context, ContextSettings.defaults(context, catalog));
        }
        return preset;
    }

    public String name() {
        return name;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ContextSettings settings(MusicContext context) {
        return contexts.get(context);
    }

    public void replaceSettings(MusicContext context, ContextSettings settings) {
        contexts.put(context, settings);
    }

    public boolean isBlacklisted(String trackId) {
        return blacklist.contains(trackId);
    }

    public void blacklist(String trackId) {
        blacklist.add(trackId);
    }

    public void unblacklist(String trackId) {
        blacklist.remove(trackId);
    }

    public boolean toggleBlacklist(String trackId) {
        if (blacklist.remove(trackId)) {
            return false;
        }
        blacklist.add(trackId);
        return true;
    }

    public Set<String> blacklisted() {
        return Collections.unmodifiableSet(blacklist);
    }

    public Preset copyAs(String newName) {
        Preset copy = new Preset(newName);
        copy.enabled = enabled;
        for (Map.Entry<MusicContext, ContextSettings> entry : contexts.entrySet()) {
            copy.contexts.put(entry.getKey(), entry.getValue().copy());
        }
        copy.blacklist.addAll(blacklist);
        return copy;
    }
}
