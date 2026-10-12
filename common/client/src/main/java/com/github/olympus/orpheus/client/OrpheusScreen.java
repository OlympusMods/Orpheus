package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.OrpheusRuntime;
import com.github.olympus.orpheus.core.model.MusicContext;
import com.github.olympus.orpheus.core.model.Track;
import com.github.olympus.orpheus.core.model.TrackKind;
import com.github.olympus.orpheus.core.preset.ContextSettings;
import com.github.olympus.orpheus.core.preset.Preset;
import com.github.olympus.orpheus.core.ui.DelayStep;
import com.github.olympus.orpheus.core.ui.Pager;
import com.github.olympus.orpheus.core.ui.TrackListing;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The main Orpheus screen.
 *
 * <pre>
 *   Now playing: ...                         row 0: what is playing
 *   [ Settings ] [ Songs ]                   row 1: tabs
 *   [<]  Situation: Survival  [>]            row 2: which situation is being edited
 *   ...tab content...                        Settings tab or Songs tab
 *   [ Done ]
 * </pre>
 *
 * Everything on this screen belongs to one "situation" (survival, nether, main menu, ...)
 * except the on/off switch and the preset, which apply to the whole mod.
 */
public final class OrpheusScreen extends OrpheusBaseScreen {
    private static final int TOP = 6;
    private static final int CONTENT_TOP = TOP + 3 * STEP + GAP;
    private static final int FOOTER_HEIGHT = 2 * STEP + GAP;
    private static final int ROW_HEIGHT = 22;
    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 12;
    private static final int REFRESH_TICKS = 10;

    private enum Tab {
        SETTINGS,
        SONGS
    }

    private final Screen parent;
    private Tab tab = Tab.SETTINGS;
    private MusicContext context = MusicContext.SURVIVAL;
    private TrackKind type = TrackKind.GAME;
    private int page;
    private Button nowPlaying;
    private Button downloadButton;
    private Button downloadStatus;
    private MusicDownload.Phase seenPhase = MusicDownload.phase();
    private int ticks;

    public OrpheusScreen(Screen parent) {
        super(text("orpheus.screen.title"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        OrpheusClient.runtime().saveActive();
        OrpheusClient.show(parent);
    }

    /** Keeps the "now playing" line up to date while the screen is open. */
    public void tick() {
        if (++ticks % REFRESH_TICKS != 0) {
            return;
        }
        OrpheusRuntime runtime = OrpheusClient.runtime();
        if (nowPlaying != null) {
            nowPlaying.setMessage(nowPlayingText(runtime));
        }
        if (seenPhase != MusicDownload.phase()) {
            seenPhase = MusicDownload.phase();
            rebuild();
        } else if (downloadButton != null) {
            downloadButton.setMessage(MusicDownload.buttonText(MusicDownload.missing(runtime).size()));
        }
    }

    @Override
    protected void rebuild() {
        clearWidgets();
        downloadButton = null;
        downloadStatus = null;
        OrpheusRuntime runtime = OrpheusClient.runtime();
        int left = left();

        nowPlaying = label(left, TOP, WIDTH, nowPlayingText(runtime), null);
        addTabs(left, TOP + STEP);
        addSituationRow(left, TOP + 2 * STEP);

        if (tab == Tab.SETTINGS) {
            addSettingsTab(runtime, left, CONTENT_TOP);
        } else {
            addSongsTab(runtime, left, CONTENT_TOP);
        }
        button(left, height - STEP - GAP, WIDTH, text("orpheus.screen.done"), null, this::onClose);
    }

    private Component nowPlayingText(OrpheusRuntime runtime) {
        return runtime.nowPlaying()
                .map(track -> text("orpheus.screen.now_playing", Texts.track(track)))
                .orElseGet(() -> text("orpheus.screen.nothing_playing"));
    }

    private void addTabs(int left, int y) {
        int half = (WIDTH - GAP) / 2;
        Button settings = button(left, y, half, text("orpheus.screen.tab.settings"),
                text("orpheus.screen.tab.settings.tooltip"), () -> switchTab(Tab.SETTINGS));
        settings.active = tab != Tab.SETTINGS;
        Button songs = button(left + half + GAP, y, half, text("orpheus.screen.tab.songs"),
                text("orpheus.screen.tab.songs.tooltip"), () -> switchTab(Tab.SONGS));
        songs.active = tab != Tab.SONGS;
    }

    private void switchTab(Tab target) {
        tab = target;
        page = 0;
        rebuild();
    }

    private void addSituationRow(int left, int y) {
        button(left, y, HEIGHT, Component.literal("<"), text("orpheus.screen.situation.previous"), () -> {
            context = context.previous();
            page = 0;
            rebuild();
        });
        label(left + HEIGHT + GAP, y, WIDTH - 2 * (HEIGHT + GAP),
                text("orpheus.screen.situation", Texts.context(context)), text("orpheus.screen.situation.tooltip"));
        button(left + WIDTH - HEIGHT, y, HEIGHT, Component.literal(">"), text("orpheus.screen.situation.next"), () -> {
            context = context.next();
            page = 0;
            rebuild();
        });
    }

    // ---------------------------------------------------------------- settings tab

    private void addSettingsTab(OrpheusRuntime runtime, int left, int top) {
        Preset preset = runtime.active();
        ContextSettings settings = preset.settings(context);
        int half = (WIDTH - GAP) / 2;
        int y = top;

        button(left, y, half, text("orpheus.screen.enabled", Texts.onOff(preset.enabled())),
                text("orpheus.screen.enabled.tooltip"), () -> {
                    preset.setEnabled(!preset.enabled());
                    rebuild();
                });
        button(left + half + GAP, y, half, text("orpheus.screen.preset", preset.name()),
                text("orpheus.screen.preset.tooltip"), () -> {
                    runtime.saveActive();
                    OrpheusClient.show(new PresetScreen(this));
                });
        y += STEP;

        String sourceKey = settings.managed() ? "orpheus.screen.source.orpheus" : "orpheus.screen.source.minecraft";
        button(left, y, WIDTH, text(sourceKey, Texts.context(context)), text("orpheus.screen.source.tooltip"), () -> {
            settings.setManaged(!settings.managed());
            rebuild();
        });
        y += STEP;

        stepper(left, y, WIDTH, text("orpheus.screen.delay.min", Texts.duration(settings.minDelaySeconds())),
                text("orpheus.screen.delay.min.tooltip"),
                () -> settings.setMinDelaySeconds(DelayStep.step(settings.minDelaySeconds(), false)),
                () -> settings.setMinDelaySeconds(DelayStep.step(settings.minDelaySeconds(), true)));
        y += STEP;

        stepper(left, y, WIDTH, text("orpheus.screen.delay.max", Texts.duration(settings.maxDelaySeconds())),
                text("orpheus.screen.delay.max.tooltip"),
                () -> settings.setMaxDelaySeconds(DelayStep.step(settings.maxDelaySeconds(), false)),
                () -> settings.setMaxDelaySeconds(DelayStep.step(settings.maxDelaySeconds(), true)));
        y += STEP;

        stepper(left, y, WIDTH, text("orpheus.screen.repeat", settings.repeatAvoidance()),
                text("orpheus.screen.repeat.tooltip"),
                () -> settings.setRepeatAvoidance(settings.repeatAvoidance() - 1),
                () -> settings.setRepeatAvoidance(settings.repeatAvoidance() + 1));
        y += STEP;

        button(left, y, WIDTH, text("orpheus.screen.reset", Texts.context(context)),
                text("orpheus.screen.reset.tooltip"), () -> {
                    settings.resetTracks(context, runtime.catalog());
                    rebuild();
                });
    }

    // ------------------------------------------------------------------- songs tab

    private void addSongsTab(OrpheusRuntime runtime, int left, int top) {
        Preset preset = runtime.active();
        ContextSettings settings = preset.settings(context);

        if (!settings.managed()) {
            label(left, top, WIDTH, text("orpheus.screen.songs.minecraft", Texts.context(context)), null);
            button(left, top + STEP, WIDTH, text("orpheus.screen.songs.use_orpheus"), null, () -> {
                settings.setManaged(true);
                rebuild();
            });
            return;
        }

        int listTop = addDownloadControls(runtime, left, top);
        List<Track> tracks = new ArrayList<>(TrackListing.sorted(runtime.catalog()));
        tracks.removeIf(track -> track.kind() != type);
        // Songs that can be played come first, missing ones go to the end.
        tracks.sort(Comparator.comparing(track -> !runtime.isAvailable(track)));

        // The type selector sits in its own row above the page controls.
        int rows = Math.max(
                MIN_ROWS, Math.min(MAX_ROWS, (height - listTop - FOOTER_HEIGHT - STEP) / ROW_HEIGHT));
        page = Pager.clamp(page, tracks.size(), rows);
        int y = listTop;
        for (Track track : Pager.slice(tracks, page, rows)) {
            addSongRow(runtime, preset, settings, track, left, y);
            y += ROW_HEIGHT;
        }

        int pageCount = Pager.pageCount(tracks.size(), rows);
        int footer = height - FOOTER_HEIGHT;
        button(left, footer - STEP, WIDTH, text("orpheus.screen.type", text(type.translationKey())),
                text("orpheus.screen.type.tooltip"), () -> {
                    type = nextTypeWithSongs(runtime);
                    page = 0;
                    rebuild();
                });
        button(left, footer, HEIGHT, Component.literal("<"), null, () -> {
            page = Math.max(0, page - 1);
            rebuild();
        });
        label(left + HEIGHT + GAP, footer, WIDTH - 2 * (HEIGHT + GAP),
                text("orpheus.screen.page", page + 1, pageCount), null);
        button(left + WIDTH - HEIGHT, footer, HEIGHT, Component.literal(">"), null, () -> {
            page = Math.min(pageCount - 1, page + 1);
            rebuild();
        });
    }

    /** The next song type in the cycle, skipping types the catalog has no songs for. */
    private TrackKind nextTypeWithSongs(OrpheusRuntime runtime) {
        TrackKind candidate = type.next();
        while (candidate != type) {
            TrackKind kind = candidate;
            if (runtime.catalog().all().stream().anyMatch(track -> track.kind() == kind)) {
                return candidate;
            }
            candidate = candidate.next();
        }
        return type;
    }

    /**
     * Songs the game does not ship can be downloaded from Mojang's servers. Adds the button
     * (and, after it was used, a line saying how it went) and returns where the song list starts.
     */
    private int addDownloadControls(OrpheusRuntime runtime, int left, int top) {
        int missing = MusicDownload.missing(runtime).size();
        MusicDownload.Phase phase = MusicDownload.phase();
        if (missing == 0 && phase == MusicDownload.Phase.IDLE) {
            return top;
        }
        int y = top;
        downloadButton = button(left, y, WIDTH, MusicDownload.buttonText(missing),
                text("orpheus.download.button.tooltip"), () -> {
                    MusicDownload.start(runtime);
                    rebuild();
                });
        downloadButton.active = missing > 0 && phase != MusicDownload.Phase.RUNNING;
        y += STEP;
        Component status = MusicDownload.status();
        if (status != null) {
            downloadStatus = label(left, y, WIDTH, status, null);
            y += STEP;
        }
        return y;
    }

    /** One song: [x] title | chance | block | play. */
    private void addSongRow(
            OrpheusRuntime runtime, Preset preset, ContextSettings settings, Track track, int left, int y) {
        boolean available = runtime.isAvailable(track);
        boolean included = settings.includes(track.id());
        boolean blocked = preset.isBlacklisted(track.id());
        int x = left;

        int titleWidth = 168;
        String mark = !available ? "[!] " : included ? "[x] " : "[ ] ";
        Component title = Component.literal(mark).append(Texts.track(track));
        Component titleTip = available
                ? text("orpheus.screen.song.toggle.tooltip", Texts.context(context))
                : text("orpheus.screen.song.missing.tooltip");
        Button titleButton = button(x, y, titleWidth, title, titleTip, () -> {
            settings.toggle(track.id());
            rebuild();
        });
        titleButton.active = available;
        x += titleWidth + GAP;

        int chanceWidth = 48;
        Button chance = button(x, y, chanceWidth, text("orpheus.screen.chance", settings.weight(track.id())),
                text("orpheus.screen.chance.tooltip"), () -> {
                    int next = settings.weight(track.id()) + 1;
                    settings.setWeight(
                            track.id(), next > ContextSettings.MAX_WEIGHT ? ContextSettings.MIN_WEIGHT : next);
                    rebuild();
                });
        chance.active = included;
        x += chanceWidth + GAP;

        int blockWidth = 52;
        button(x, y, blockWidth, text(blocked ? "orpheus.screen.unblock" : "orpheus.screen.block"),
                text(blocked ? "orpheus.screen.unblock.tooltip" : "orpheus.screen.block.tooltip"), () -> {
                    preset.toggleBlacklist(track.id());
                    rebuild();
                });
        x += blockWidth + GAP;

        Button play = button(x, y, WIDTH - (x - left), text("orpheus.screen.play"),
                text("orpheus.screen.play.tooltip"), () -> runtime.controller().playNow(track));
        play.active = available;
    }
}
