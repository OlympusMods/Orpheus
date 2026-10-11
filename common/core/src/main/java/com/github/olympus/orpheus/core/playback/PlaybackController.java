package com.github.olympus.orpheus.core.playback;

import com.github.olympus.orpheus.core.model.MusicContext;
import com.github.olympus.orpheus.core.model.Track;
import com.github.olympus.orpheus.core.model.TrackCatalog;
import com.github.olympus.orpheus.core.preset.ContextSettings;
import com.github.olympus.orpheus.core.preset.Preset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

public final class PlaybackController {
    private static final int TICKS_PER_SECOND = 20;
    private static final int INITIAL_DELAY_TICKS = 100;
    private static final int HISTORY_LIMIT = 64;

    private final PlaybackBackend backend;
    private final Supplier<Preset> presets;
    private final Supplier<TrackCatalog> catalog;
    private final TrackSelector selector;
    private final Random random;
    private final List<String> history = new ArrayList<>();

    private Track current;
    private MusicContext lastContext;
    private int delayTicks = INITIAL_DELAY_TICKS;
    private boolean paused;
    private Runnable persistence = () -> { };

    public PlaybackController(
            PlaybackBackend backend,
            Supplier<Preset> presets,
            Supplier<TrackCatalog> catalog,
            Random random) {
        this.backend = backend;
        this.presets = presets;
        this.catalog = catalog;
        this.random = random;
        this.selector = new TrackSelector(random);
    }

    public void onPersist(Runnable persistence) {
        this.persistence = persistence;
    }

    public boolean tick(MusicContext context, boolean replaceCurrent) {
        Preset preset = presets.get();
        ContextSettings settings = preset.settings(context);
        if (!preset.enabled() || !settings.managed()) {
            release();
            return false;
        }
        if (paused) {
            backend.hold();
            lastContext = context;
            return true;
        }
        if (current != null && !backend.isPlaying()) {
            current = null;
            delayTicks = randomDelay(settings);
        }
        if (current != null && replaceCurrent && switchesSituation(context)) {
            backend.stop();
            current = null;
            delayTicks = random.nextInt(settings.minDelaySeconds() * TICKS_PER_SECOND / 2 + 1);
        }
        lastContext = context;
        if (current != null) {
            return true;
        }
        delayTicks = Math.min(delayTicks, settings.maxDelaySeconds() * TICKS_PER_SECOND);
        if (delayTicks-- > 0) {
            return true;
        }
        Optional<Track> next = selector.select(preset, context, catalog.get(), history);
        if (next.isPresent()) {
            start(next.get());
        } else {
            delayTicks = randomDelay(settings);
        }
        return true;
    }

    public void idle() {
        release();
    }

    public void skip() {
        if (current != null) {
            backend.stop();
            current = null;
        }
        paused = false;
        delayTicks = 0;
    }

    public void previous() {
        int index = current != null ? history.size() - 2 : history.size() - 1;
        if (index < 0) {
            return;
        }
        Optional<Track> target = catalog.get().find(history.get(index));
        if (target.isEmpty()) {
            return;
        }
        history.subList(index, history.size()).clear();
        if (current != null) {
            backend.stop();
            current = null;
        }
        paused = false;
        start(target.get());
    }

    public void playNow(Track track) {
        if (current != null) {
            backend.stop();
            current = null;
        }
        paused = false;
        start(track);
    }

    public boolean blacklistCurrent() {
        if (current == null) {
            return false;
        }
        presets.get().blacklist(current.id());
        persistence.run();
        skip();
        return true;
    }

    public void togglePause() {
        if (current == null) {
            return;
        }
        if (paused) {
            backend.resume();
            paused = false;
        } else {
            backend.pause();
            paused = true;
        }
    }

    public Optional<Track> currentTrack() {
        return Optional.ofNullable(current);
    }

    public boolean isPaused() {
        return paused;
    }

    public void reset() {
        if (current != null) {
            backend.stop();
        }
        current = null;
        paused = false;
        lastContext = null;
        delayTicks = INITIAL_DELAY_TICKS;
    }

    /**
     * A song is cut short when the situation changes (for example into the Nether), except
     * when the main menu is involved. Leaving or entering a world must not interrupt the music.
     */
    private boolean switchesSituation(MusicContext context) {
        return lastContext != null
                && lastContext != context
                && lastContext != MusicContext.MAIN_MENU
                && context != MusicContext.MAIN_MENU;
    }

    private void start(Track track) {
        backend.play(track);
        current = track;
        history.add(track.id());
        if (history.size() > HISTORY_LIMIT) {
            history.subList(0, history.size() - HISTORY_LIMIT).clear();
        }
    }

    private void release() {
        if (current != null) {
            backend.stop();
            current = null;
        }
        paused = false;
        lastContext = null;
    }

    private int randomDelay(ContextSettings settings) {
        int min = settings.minDelaySeconds() * TICKS_PER_SECOND;
        int max = settings.maxDelaySeconds() * TICKS_PER_SECOND;
        return min + random.nextInt(Math.max(0, max - min) + 1);
    }
}
