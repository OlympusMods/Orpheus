package com.github.olympus.orpheus.core;

import com.github.olympus.orpheus.core.model.MusicContext;
import com.github.olympus.orpheus.core.model.Track;
import com.github.olympus.orpheus.core.model.TrackCatalog;
import com.github.olympus.orpheus.core.playback.PlaybackAction;
import com.github.olympus.orpheus.core.playback.PlaybackBackend;
import com.github.olympus.orpheus.core.playback.PlaybackController;
import com.github.olympus.orpheus.core.preset.Preset;
import com.github.olympus.orpheus.core.preset.PresetStore;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

public final class OrpheusRuntime {
    private static OrpheusRuntime instance;

    private final PresetStore store;
    private final TrackCatalog catalog;
    private final PlaybackController controller;
    private final Consumer<String> errors;
    private Preset active;

    public OrpheusRuntime(Path root, TrackCatalog catalog, PlaybackBackend backend, Consumer<String> errors) {
        this.store = new PresetStore(root, catalog);
        this.catalog = catalog;
        this.errors = errors;
        this.active = loadInitial();
        this.controller = new PlaybackController(backend, () -> active, () -> this.catalog, new Random());
        this.controller.onPersist(this::saveActive);
    }

    public static void install(OrpheusRuntime runtime) {
        instance = runtime;
    }

    public static Optional<OrpheusRuntime> current() {
        return Optional.ofNullable(instance);
    }

    public TrackCatalog catalog() {
        return catalog;
    }

    public Preset active() {
        return active;
    }

    public PlaybackController controller() {
        return controller;
    }

    public List<String> presetNames() {
        return store.names();
    }

    public boolean tick(MusicContext context, boolean replaceCurrent) {
        return controller.tick(context, replaceCurrent);
    }

    public void perform(PlaybackAction action) {
        switch (action) {
            case TOGGLE_PAUSE -> controller.togglePause();
            case SKIP -> controller.skip();
            case PREVIOUS -> controller.previous();
            case BLACKLIST_CURRENT -> controller.blacklistCurrent();
            case OPEN_MENU -> { }
        }
    }

    public void saveActive() {
        try {
            store.save(active);
        } catch (IOException exception) {
            errors.accept("Could not save preset " + active.name() + ": " + exception.getMessage());
        }
    }

    public boolean switchTo(String name) {
        if (!store.exists(name)) {
            return false;
        }
        try {
            saveActive();
            Preset loaded = store.load(name);
            store.setActiveName(loaded.name());
            active = loaded;
            controller.reset();
            return true;
        } catch (IOException | RuntimeException exception) {
            errors.accept("Could not load preset " + name + ": " + exception.getMessage());
            return false;
        }
    }

    public boolean createFromActive(String rawName) {
        Optional<String> name = PresetStore.normalize(rawName);
        if (name.isEmpty() || store.exists(name.get())) {
            return false;
        }
        saveActive();
        Preset copy = active.copyAs(name.get());
        try {
            store.save(copy);
            store.setActiveName(copy.name());
        } catch (IOException exception) {
            errors.accept("Could not create preset " + name.get() + ": " + exception.getMessage());
            return false;
        }
        active = copy;
        controller.reset();
        return true;
    }

    public boolean createDefault(String rawName) {
        Optional<String> name = PresetStore.normalize(rawName);
        if (name.isEmpty() || store.exists(name.get())) {
            return false;
        }
        saveActive();
        Preset fresh = Preset.defaults(name.get(), catalog);
        try {
            store.save(fresh);
            store.setActiveName(fresh.name());
        } catch (IOException exception) {
            errors.accept("Could not create preset " + name.get() + ": " + exception.getMessage());
            return false;
        }
        active = fresh;
        controller.reset();
        return true;
    }

    public boolean deletePreset(String name) {
        if (store.names().size() <= 1) {
            return false;
        }
        try {
            boolean wasActive = name.equalsIgnoreCase(active.name());
            if (!store.delete(name)) {
                return false;
            }
            if (wasActive) {
                String fallback = store.names().get(0);
                active = store.load(fallback);
                store.setActiveName(fallback);
                controller.reset();
            }
            return true;
        } catch (IOException | RuntimeException exception) {
            errors.accept("Could not delete preset " + name + ": " + exception.getMessage());
            return false;
        }
    }

    public Optional<Track> nowPlaying() {
        return controller.currentTrack();
    }

    private Preset loadInitial() {
        try {
            return store.loadActive();
        } catch (IOException exception) {
            errors.accept("Could not load presets: " + exception.getMessage());
            return Preset.defaults(PresetStore.DEFAULT_NAME, catalog);
        }
    }
}
