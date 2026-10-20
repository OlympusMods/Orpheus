package com.github.olympus.orpheus.client.ui;

import com.github.olympus.orpheus.client.MusicDownload;
import com.github.olympus.orpheus.client.OrpheusClient;
import com.github.olympus.orpheus.client.Texts;
import com.github.olympus.orpheus.core.OrpheusRuntime;
import com.github.olympus.orpheus.core.model.MusicContext;
import com.github.olympus.orpheus.core.model.Track;
import com.github.olympus.orpheus.core.model.TrackKind;
import com.github.olympus.orpheus.core.playback.PlaybackAction;
import com.github.olympus.orpheus.core.preset.ContextSettings;
import com.github.olympus.orpheus.core.preset.Preset;
import com.github.olympus.orpheus.core.preset.PresetStore;
import com.github.olympus.orpheus.core.ui.DelayStep;
import com.github.olympus.orpheus.core.ui.TrackListing;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class OrpheusUi {
    private static final int PAD = 10;
    private static final int GAP = 6;
    private static final int ROW = 20;
    private static final int SONG_ROW = 26;
    private static final int REFRESH_TICKS = 10;
    private static final String NAME_FIELD = "preset-name";

    private enum Page {
        MAIN,
        PRESETS
    }

    private enum Tab {
        SETTINGS,
        SONGS,
        APPEARANCE
    }

    private final UiHost host;
    private final Gui gui;
    private final FadeCanvas fade = new FadeCanvas();
    private final UiPrefs prefs = UiPrefs.get();
    private final Theme modern = new ModernTheme();
    private final Theme classic = new ClassicTheme();

    private int width;
    private int height;
    private Page page = Page.MAIN;
    private Tab tab = Tab.SETTINGS;
    private MusicContext context = MusicContext.SURVIVAL;
    private TrackKind type = TrackKind.GAME;
    private float open;
    private float contentFade;
    private boolean closing;
    private boolean closeReady;
    private boolean closed;
    private long lastNanos = System.nanoTime();
    private double clock;
    private int ticks;
    private Set<String> available = new HashSet<>();
    private int missing;
    private List<Track> listing = List.of();
    private boolean listingDirty = true;
    private boolean customDirty;
    private TrackKind listingType = TrackKind.GAME;
    private List<String> presetNames = List.of();
    private String presetStatus = text("orpheus.presets.hint");

    public OrpheusUi(UiHost host) {
        this.host = host;
        this.gui = new Gui(host);
        refresh();
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public boolean vanillaBackdrop(boolean inWorld) {
        if (prefs.classic()) {
            return false;
        }
        return prefs.blur() || !inWorld;
    }

    public void tick() {
        ticks++;
        if (closeReady && !closed) {
            closed = true;
            host.close();
            return;
        }
        if (ticks % REFRESH_TICKS == 0) {
            refresh();
        }
    }

    public void requestClose() {
        closing = true;
    }

    public void mousePressed(int button) {
        gui.press(button);
    }

    public void mouseReleased(int button) {
        gui.release(button);
    }

    public void mouseScrolled(double amount) {
        gui.scrolled(amount);
    }

    public boolean keyPressed(int key, int mods) {
        if (gui.keyPressed(key, mods)) {
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (page == Page.PRESETS) {
                openMain();
            } else {
                requestClose();
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(int codepoint) {
        return gui.charTyped(codepoint);
    }

    public void render(Canvas target, int mouseX, int mouseY) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        clock += dt;

        Theme theme = prefs.classic() ? classic : modern;
        theme.accent(prefs.accentColor());
        boolean animated = theme.animated() && prefs.animations();
        float openTarget = closing ? 0f : 1f;
        open = animated ? Motion.approach(open, openTarget, dt, 14f) : openTarget;
        contentFade = animated ? Motion.approach(contentFade, 1f, dt, 12f) : 1f;
        if (closing && open <= 0.02f) {
            closeReady = true;
        }
        int shift = animated ? Math.round((1f - open) * 10f) : 0;

        fade.bind(target, open);
        theme.backdrop(fade, width, height, host.inWorld());
        fade.shift(shift);
        gui.begin(fade, theme, mouseX, mouseY - shift, dt, clock, !animated);
        gui.lock(closing);

        if (page == Page.MAIN) {
            renderMain(theme);
        } else {
            renderPresets(theme);
        }

        gui.end();
        fade.unshift();
        fade.unbind();
    }

    private void refresh() {
        OrpheusRuntime runtime = OrpheusClient.runtime();
        Set<String> now = new HashSet<>();
        int absent = 0;
        for (Track track : runtime.catalog().all()) {
            if (runtime.isAvailable(track)) {
                now.add(track.id());
            } else if (track.isDownloadable()) {
                absent++;
            }
        }
        missing = absent;
        if (!now.equals(available)) {
            available = now;
            listingDirty = true;
        }
    }

    private List<Track> listing(OrpheusRuntime runtime) {
        if (listingDirty || listingType != type) {
            List<Track> tracks = new ArrayList<>(TrackListing.sorted(runtime.catalog()));
            tracks.removeIf(track -> track.kind() != type);
            tracks.sort(Comparator.comparing(track -> !available.contains(track.id())));
            listing = tracks;
            listingType = type;
            listingDirty = false;
        }
        return listing;
    }

    private void switchTab(Tab target) {
        tab = target;
        contentFade = 0f;
    }

    private void openMain() {
        page = Page.MAIN;
        contentFade = 0f;
        gui.blur();
    }

    private void openPresets() {
        OrpheusClient.runtime().saveActive();
        page = Page.PRESETS;
        contentFade = 0f;
        presetStatus = text("orpheus.presets.hint");
        presetNames = OrpheusClient.runtime().presetNames();
        gui.setField(NAME_FIELD, "");
        gui.blur();
    }

    private static String text(String key, Object... arguments) {
        return Component.translatable(key, arguments).getString();
    }

    private int windowWidth() {
        return Math.max(260, Math.min(width - 16, 440));
    }

    private int windowHeight() {
        return Math.max(176, Math.min(height - 12, 330));
    }

    private void beginContent() {
        fade.push();
        fade.translate(0f, Math.round((1f - Motion.easeOutCubic(contentFade)) * 8f));
    }

    private void endContent() {
        fade.pop();
    }

    private int heading(String label, int x, int y) {
        gui.text(label, x + 2, y + 1, gui.theme().textSecondary());
        return y + 13;
    }

    private void renderMain(Theme theme) {
        OrpheusRuntime runtime = OrpheusClient.runtime();
        Preset preset = runtime.active();
        int pw = windowWidth();
        int ph = windowHeight();
        int px = (width - pw) / 2;
        int py = (height - ph) / 2;
        theme.window(fade, px, py, pw, ph);

        int ix = px + PAD;
        int iw = pw - 2 * PAD;
        int y = py + PAD;

        String title = text("orpheus.screen.title");
        int toggleRow = Math.min(iw, 120);
        if (theme.centeredTitle() && pw >= 340) {
            gui.centered(title, px + pw / 2, y + 5, theme.titleColor());
        } else {
            fade.push();
            fade.translate(ix, y + 2);
            fade.scale(1.25f, 1.25f);
            gui.text(title, 0, 0, theme.titleColor());
            fade.pop();
        }
        if (gui.switchRow("enabled", ix + iw - toggleRow, y, toggleRow, 18, text("orpheus.ui.enabled"),
                preset.enabled(), true, text("orpheus.screen.enabled.tooltip"), text("orpheus.state.on"),
                text("orpheus.state.off"))) {
            preset.setEnabled(!preset.enabled());
        }
        y += 26;

        y = nowPlaying(theme, runtime, ix, y, iw);

        String[] tabs = {
            text("orpheus.screen.tab.settings"), text("orpheus.screen.tab.songs"),
            text("orpheus.screen.tab.appearance")
        };
        String[] tabTips = {
            text("orpheus.screen.tab.settings.tooltip"), text("orpheus.screen.tab.songs.tooltip"),
            text("orpheus.screen.tab.appearance.tooltip")
        };
        int chosen = gui.segmented("tabs", ix, y, iw, 20, tabs, tab.ordinal(), tabTips, null);
        if (chosen >= 0) {
            switchTab(Tab.values()[chosen]);
        }
        y += 20 + GAP;

        int footerY = py + ph - PAD - ROW;
        int contentHeight = footerY - GAP - y;

        beginContent();
        float before = fade.alpha();
        fade.alpha(before * Motion.easeOutCubic(contentFade));
        switch (tab) {
            case SETTINGS -> renderSettings(runtime, preset, ix, y, iw, contentHeight);
            case SONGS -> renderSongs(runtime, preset, ix, y, iw, contentHeight);
            case APPEARANCE -> renderAppearance(ix, y, iw, contentHeight);
        }
        fade.alpha(before);
        endContent();

        int presetWidth = (iw - GAP) * 3 / 5;
        if (gui.button("preset", ix, footerY, presetWidth, ROW, text("orpheus.screen.preset", preset.name()),
                text("orpheus.screen.preset.tooltip"))) {
            openPresets();
        }
        if (gui.button("done", ix + presetWidth + GAP, footerY, iw - presetWidth - GAP, ROW,
                text("orpheus.screen.done"), ButtonKind.PRIMARY, false, true, null)) {
            requestClose();
        }
    }

    private int nowPlaying(Theme theme, OrpheusRuntime runtime, int ix, int y, int iw) {
        int h = 32;
        theme.card(fade, ix, y, iw, h);
        var current = runtime.nowPlaying();
        boolean playing = current.isPresent();
        boolean paused = runtime.controller().isPaused();
        float level = gui.anim("eq", playing && !paused ? 1f : 0.12f, 6f);
        int accent = prefs.classic() ? 0xFF55FF55 : prefs.accentColor();
        Icons.equalizer(fade, ix + 10, y + 8, 16, clock, level, accent);

        int buttonsWidth = 3 * 24 + 2 * 3;
        int textX = ix + 34;
        int textWidth = iw - 34 - buttonsWidth - 14;
        if (playing) {
            Track track = current.get();
            gui.text(gui.fit(Texts.track(track).getString(), textWidth), textX, y + (track.hasArtist() ? 7 : 12),
                    theme.textPrimary());
            if (track.hasArtist()) {
                gui.text(gui.fit(track.artist(), textWidth), textX, y + 17, theme.textSecondary());
            }
        } else {
            gui.text(gui.fit(text("orpheus.screen.nothing_playing"), textWidth), textX, y + 12,
                    theme.textSecondary());
        }

        int bx = ix + iw - 8 - buttonsWidth;
        int by = y + (h - ROW) / 2;
        if (gui.iconButton("previous", bx, by, 24, ROW, "previous", ButtonKind.NORMAL, true,
                text("orpheus.ui.previous"))) {
            runtime.perform(PlaybackAction.PREVIOUS);
        }
        if (gui.iconButton("pause", bx + 27, by, 24, ROW, paused || !playing ? "play" : "pause", ButtonKind.NORMAL,
                playing, text("orpheus.ui.pause"))) {
            runtime.perform(PlaybackAction.TOGGLE_PAUSE);
        }
        if (gui.iconButton("skip", bx + 54, by, 24, ROW, "next", ButtonKind.NORMAL, true,
                text("orpheus.ui.skip"))) {
            runtime.perform(PlaybackAction.SKIP);
        }
        return y + h + GAP;
    }

    private int situationRow(int ix, int y, int iw) {
        Theme theme = gui.theme();
        if (gui.iconButton("situation-", ix, y, 24, ROW, "left", ButtonKind.NORMAL, true,
                text("orpheus.screen.situation.previous"))) {
            context = context.previous();
            gui.setAnim("slide", -16f);
        }
        if (gui.iconButton("situation+", ix + iw - 24, y, 24, ROW, "right", ButtonKind.NORMAL, true,
                text("orpheus.screen.situation.next"))) {
            context = context.next();
            gui.setAnim("slide", 16f);
        }
        int boxX = ix + 27;
        int boxW = iw - 54;
        theme.inset(fade, boxX, y, boxW, ROW);
        float slide = gui.anim("slide", 0f, 14f);
        fade.clip(boxX + 1, y, boxW - 2, ROW);
        String label = text("orpheus.screen.situation", Texts.context(context).getString());
        gui.centered(gui.fit(label, boxW - 8), boxX + boxW / 2 + Math.round(slide), gui.textY(y, ROW),
                theme.textPrimary());
        fade.unclip();
        if (gui.over(boxX, y, boxW, ROW)) {
            gui.tooltip(text("orpheus.screen.situation.tooltip"));
        }
        return y + ROW + GAP;
    }

    private void renderSettings(OrpheusRuntime runtime, Preset preset, int ix, int cy, int iw, int ch) {
        Theme theme = gui.theme();
        ContextSettings settings = preset.settings(context);
        int y = situationRow(ix, cy, iw);
        int viewH = cy + ch - y;
        float scroll = gui.scrollBegin("settings", ix, y, iw, viewH);
        int lw = iw - 8;
        int top = y - Math.round(scroll);
        int yy = top;

        yy = heading(text("orpheus.ui.source", Texts.context(context).getString()), ix, yy);
        theme.card(fade, ix, yy, lw, 36);
        String[] sources = {text("orpheus.ui.source.orpheus"), text("orpheus.ui.source.minecraft")};
        String[] sourceTips = {text("orpheus.screen.source.tooltip"), text("orpheus.screen.source.tooltip")};
        int pick = gui.segmented("source", ix + 8, yy + 8, lw - 16, ROW, sources, settings.managed() ? 0 : 1,
                sourceTips, null);
        if (pick >= 0) {
            settings.setManaged(pick == 0);
        }
        yy += 36 + GAP;

        yy = heading(text("orpheus.ui.timing"), ix, yy);
        theme.card(fade, ix, yy, lw, 84);
        int rowY = yy + 6;
        int stepperW = Math.min(132, lw - 120);
        yy = timingRow(ix, lw, rowY, stepperW, "min", text("orpheus.ui.delay.min"),
                Texts.duration(settings.minDelaySeconds()).getString(), text("orpheus.screen.delay.min.tooltip"),
                direction -> settings.setMinDelaySeconds(
                        DelayStep.step(settings.minDelaySeconds(), direction > 0)),
                settings.minDelaySeconds() > 0, settings.minDelaySeconds() < ContextSettings.MAX_DELAY_SECONDS);
        yy = timingRow(ix, lw, yy, stepperW, "max", text("orpheus.ui.delay.max"),
                Texts.duration(settings.maxDelaySeconds()).getString(), text("orpheus.screen.delay.max.tooltip"),
                direction -> settings.setMaxDelaySeconds(
                        DelayStep.step(settings.maxDelaySeconds(), direction > 0)),
                settings.maxDelaySeconds() > 0, settings.maxDelaySeconds() < ContextSettings.MAX_DELAY_SECONDS);
        yy = timingRow(ix, lw, yy, stepperW, "repeat", text("orpheus.ui.repeat"),
                String.valueOf(settings.repeatAvoidance()), text("orpheus.screen.repeat.tooltip"),
                direction -> settings.setRepeatAvoidance(settings.repeatAvoidance() + direction),
                settings.repeatAvoidance() > 0, settings.repeatAvoidance() < ContextSettings.MAX_REPEAT_AVOIDANCE);
        yy = rowY - 6 + 84 + GAP;

        yy = heading(text("orpheus.ui.general"), ix, yy);
        theme.card(fade, ix, yy, lw, 62);
        if (gui.switchRow("toast", ix + 10, yy + 6, lw - 20, ROW, text("orpheus.ui.toast"),
                preset.nowPlayingToast(), true, text("orpheus.screen.toast.tooltip"), text("orpheus.state.on"),
                text("orpheus.state.off"))) {
            preset.setNowPlayingToast(!preset.nowPlayingToast());
        }
        if (gui.button("reset", ix + 8, yy + 6 + ROW + 6, lw - 16, ROW, text("orpheus.screen.reset"),
                text("orpheus.screen.reset.tooltip"))) {
            settings.resetTracks(context, runtime.catalog());
        }
        yy += 62 + GAP;

        gui.scrollEnd(yy - top);
    }

    private int timingRow(int ix, int lw, int y, int stepperW, String id, String caption, String value, String tip,
            IntConsumer change, boolean canDecrease, boolean canIncrease) {
        gui.text(gui.fit(caption, lw - stepperW - 28), ix + 12, gui.textY(y, 18), gui.theme().textPrimary());
        int result = gui.stepper(id, ix + lw - 10 - stepperW, y, stepperW, 18, value, tip, canDecrease, canIncrease);
        if (result != 0) {
            change.accept(result);
        }
        return y + 24;
    }

    private void renderSongs(OrpheusRuntime runtime, Preset preset, int ix, int cy, int iw, int ch) {
        Theme theme = gui.theme();
        ContextSettings settings = preset.settings(context);
        int y = situationRow(ix, cy, iw);

        TrackKind[] kinds = TrackKind.values();
        String[] labels = new String[kinds.length];
        String[] tips = new String[kinds.length];
        boolean[] enabled = new boolean[kinds.length];
        for (int i = 0; i < kinds.length; i++) {
            TrackKind kind = kinds[i];
            labels[i] = text(kind.translationKey());
            tips[i] = text("orpheus.screen.type.tooltip");
            enabled[i] = runtime.catalog().all().stream().anyMatch(track -> track.kind() == kind);
        }
        int picked = gui.segmented("type", ix, y, iw, 18, labels, type.ordinal(), tips, enabled);
        if (picked >= 0) {
            type = kinds[picked];
            gui.setAnim("songs", 0f);
        }
        y += 18 + GAP;

        if (!settings.managed()) {
            for (String line : Draw.wrap(fade, text("orpheus.screen.songs.minecraft",
                    Texts.context(context).getString()), iw - 20)) {
                gui.centered(line, ix + iw / 2, y + 6, theme.textSecondary());
                y += 11;
            }
            if (gui.button("use-orpheus", ix + iw / 2 - 110, y + 14, 220, ROW,
                    text("orpheus.screen.songs.use_orpheus"), ButtonKind.PRIMARY, false, true, null)) {
                settings.setManaged(true);
            }
            return;
        }

        y = downloadControls(runtime, ix, y, iw);

        List<Track> tracks = listing(runtime);
        int viewH = cy + ch - y;
        if (viewH < 8) {
            return;
        }
        float scroll = gui.scrollBegin("songs", ix, y, iw, viewH);
        int lw = iw - 8;
        int top = y - Math.round(scroll);
        if (tracks.isEmpty()) {
            gui.centered(text("orpheus.ui.songs.none"), ix + lw / 2, top + 12, theme.textSecondary());
        }
        for (int i = 0; i < tracks.size(); i++) {
            int ry = top + i * SONG_ROW;
            if (ry + SONG_ROW < y || ry > y + viewH) {
                continue;
            }
            songRow(runtime, preset, settings, tracks.get(i), ix, ry, lw);
        }
        gui.scrollEnd(Math.max(tracks.size() * SONG_ROW, 1));
    }

    private int downloadControls(OrpheusRuntime runtime, int ix, int y, int iw) {
        Theme theme = gui.theme();
        if (type == TrackKind.UNUSED) {
            if (gui.button("unused", ix, y, iw, ROW, text("orpheus.unused.button"),
                    text("orpheus.unused.button.tooltip"))) {
                MusicDownload.prepareUnused(runtime);
            }
            y += ROW + 3;
            Component status = MusicDownload.unusedStatus();
            if (status != null) {
                y = statusLines(status.getString(), ix, y, iw);
            }
            return y + 3;
        }
        MusicDownload.Phase phase = MusicDownload.phase();
        if (missing == 0 && phase == MusicDownload.Phase.IDLE) {
            return y;
        }
        boolean running = phase == MusicDownload.Phase.RUNNING;
        if (gui.button("download", ix, y, iw, ROW, MusicDownload.buttonText(missing).getString(),
                ButtonKind.PRIMARY, false, missing > 0 && !running, text("orpheus.download.button.tooltip"))) {
            MusicDownload.start(runtime);
        }
        y += ROW + 3;
        if (running) {
            gui.progress(ix, y, iw, 8, -1f);
            y += 11;
        }
        Component status = MusicDownload.status();
        if (status != null) {
            y = statusLines(status.getString(), ix, y, iw);
        }
        return y + 3;
    }

    private int statusLines(String status, int ix, int y, int iw) {
        for (String line : Draw.wrap(fade, status, iw - 8)) {
            gui.text(line, ix + 4, y, gui.theme().textSecondary());
            y += 11;
        }
        return y;
    }

    private void songRow(OrpheusRuntime runtime, Preset preset, ContextSettings settings, Track track, int ix,
            int y, int lw) {
        Theme theme = gui.theme();
        String id = track.id();
        boolean playable = available.contains(id);
        boolean included = settings.includes(id);
        boolean blocked = preset.isBlacklisted(id);

        int playW = 24;
        int blockW = 54;
        int chanceW = 58;
        int controls = playW + blockW + chanceW + 3 * 4 + 6;
        int titleZone = lw - controls;

        gui.rowBackground("row:" + id, ix, y, lw, SONG_ROW - 2, included && playable);
        String tip = playable
                ? text("orpheus.screen.song.toggle.tooltip", Texts.context(context).getString())
                : missingTooltip(track);
        if (gui.hit("hit:" + id, ix, y, titleZone, SONG_ROW - 2, playable, tip)) {
            settings.toggle(id);
        }

        int boxY = y + (SONG_ROW - 2 - 12) / 2;
        if (playable) {
            gui.checkbox("box:" + id, ix + 8, boxY, 12, included, true);
        } else {
            gui.text("!", ix + 12, gui.textY(y, SONG_ROW - 2), 0xFFF59E0B);
        }

        int textX = ix + 28;
        int textW = titleZone - 28 - 4;
        int titleColor = !playable ? theme.textDisabled() : blocked ? theme.textSecondary() : theme.textPrimary();
        String title = gui.fit(Texts.track(track).getString(), textW);
        String sub = blocked ? text("orpheus.ui.blocked") : track.hasArtist() ? track.artist() : null;
        if (sub == null) {
            gui.text(title, textX, gui.textY(y, SONG_ROW - 2), titleColor);
        } else {
            gui.text(title, textX, y + 3, titleColor);
            gui.text(gui.fit(sub, textW), textX, y + 13, blocked ? 0xFFE5484D : theme.textSecondary());
        }

        int x = ix + lw - 4 - playW;
        int by = y + 2;
        int bh = SONG_ROW - 6;
        if (gui.iconButton("play:" + id, x, by, playW, bh, "play", ButtonKind.NORMAL, playable,
                text("orpheus.screen.play.tooltip"))) {
            runtime.controller().playNow(track);
        }
        x -= blockW + 4;
        if (gui.button("block:" + id, x, by, blockW, bh,
                text(blocked ? "orpheus.screen.unblock" : "orpheus.screen.block"),
                blocked ? ButtonKind.DANGER : ButtonKind.NORMAL, false, true,
                text(blocked ? "orpheus.screen.unblock.tooltip" : "orpheus.screen.block.tooltip"))) {
            preset.toggleBlacklist(id);
        }
        x -= chanceW + 4;
        int change = gui.chip("chance:" + id, x, by, chanceW, bh,
                text("orpheus.screen.chance", settings.weight(id)), included && playable,
                text("orpheus.screen.chance.tooltip"));
        if (change != 0) {
            int next = settings.weight(id) + change;
            if (change > 0 && next > ContextSettings.MAX_WEIGHT) {
                next = ContextSettings.MIN_WEIGHT;
            }
            if (change < 0 && next < ContextSettings.MIN_WEIGHT) {
                next = ContextSettings.MAX_WEIGHT;
            }
            settings.setWeight(id, next);
        }
    }

    private static String missingTooltip(Track track) {
        if (track.kind() == TrackKind.UNUSED) {
            String path = track.filePath();
            return text("orpheus.screen.song.unused.tooltip", path.substring(path.lastIndexOf('/') + 1) + ".ogg");
        }
        return text("orpheus.screen.song.missing.tooltip");
    }

    private void renderAppearance(int ix, int cy, int iw, int ch) {
        Theme theme = gui.theme();
        float scroll = gui.scrollBegin("appearance", ix, cy, iw, ch);
        int lw = iw - 8;
        int top = cy - Math.round(scroll);
        int y = top;

        y = heading(text("orpheus.ui.style"), ix, y);
        theme.card(fade, ix, y, lw, 36);
        String[] styles = {text("orpheus.ui.style.modern"), text("orpheus.ui.style.classic")};
        String[] styleTips = {text("orpheus.ui.style.modern.tooltip"), text("orpheus.ui.style.classic.tooltip")};
        int picked = gui.segmented("style", ix + 8, y + 8, lw - 16, ROW, styles, prefs.classic() ? 1 : 0, styleTips,
                null);
        if (picked >= 0) {
            prefs.setClassic(picked == 1);
            contentFade = 0f;
        }
        y += 36 + GAP;

        if (prefs.classic()) {
            for (String line : Draw.wrap(fade, text("orpheus.ui.classic_note"), lw - 8)) {
                gui.text(line, ix + 4, y, theme.textSecondary());
                y += 11;
            }
            y = toastSection(ix, y + GAP, lw);
            gui.scrollEnd(y - top);
            return;
        }

        y = heading(text("orpheus.ui.accent"), ix, y);
        boolean custom = prefs.accentIndex() == UiPrefs.CUSTOM;
        int cardH = custom ? 38 + 3 * 22 + 4 : 38;
        theme.card(fade, ix, y, lw, cardH);
        int count = UiPrefs.ACCENTS.length + 2;
        int size = 20;
        int gap = Math.min(14, (lw - 24 - count * size) / (count - 1));
        for (int i = 0; i < count; i++) {
            int sx = ix + 12 + i * (size + gap);
            boolean isCustom = i == UiPrefs.CUSTOM;
            boolean isChroma = i == UiPrefs.CHROMA;
            int color = isCustom ? prefs.customColor() : isChroma ? prefs.chromaColor() : UiPrefs.ACCENTS[i];
            String key = isCustom ? "custom" : isChroma ? "chroma" : UiPrefs.ACCENT_KEYS[i];
            if (gui.swatch("accent" + i, sx, y + 9, size, color, prefs.accentIndex() == i, isCustom,
                    text("orpheus.ui.accent." + key))) {
                prefs.setAccent(i);
            }
        }
        if (custom) {
            float hue = prefs.hue();
            float saturation = prefs.saturation();
            float lightness = prefs.lightness();
            int sliderX = ix + 12;
            int sliderW = lw - 24;
            int sy = y + 44;
            float newHue = gui.slider("hue", sliderX, sy, sliderW, 8, hue,
                    t -> Colors.hsl((float) t, 0.3f + 0.7f * saturation, 0.3f + 0.4f * lightness));
            float newSaturation = gui.slider("saturation", sliderX, sy + 22, sliderW, 8, saturation,
                    t -> Colors.hsl(hue, 0.3f + 0.7f * (float) t, 0.3f + 0.4f * lightness));
            float newLightness = gui.slider("lightness", sliderX, sy + 44, sliderW, 8, lightness,
                    t -> Colors.hsl(hue, 0.3f + 0.7f * saturation, 0.3f + 0.4f * (float) t));
            if (newHue != hue || newSaturation != saturation || newLightness != lightness) {
                prefs.setCustom(newHue, newSaturation, newLightness);
                customDirty = true;
            }
        }
        if (customDirty && !gui.dragging()) {
            prefs.save();
            customDirty = false;
        }
        y += cardH + GAP;

        y = heading(text("orpheus.ui.effects"), ix, y);
        theme.card(fade, ix, y, lw, 32);
        if (gui.switchRow("animations", ix + 10, y + 6, lw - 20, ROW, text("orpheus.ui.animations"),
                prefs.animations(), true, text("orpheus.ui.animations.tooltip"), text("orpheus.state.on"),
                text("orpheus.state.off"))) {
            prefs.setAnimations(!prefs.animations());
        }
        y += 32 + GAP;
        y = toastSection(ix, y, lw);
        gui.scrollEnd(y - top);
    }

    private int toastSection(int ix, int y, int lw) {
        Theme theme = gui.theme();
        y = heading(text("orpheus.ui.toast.heading"), ix, y);
        theme.card(fade, ix, y, lw, 58);
        int boxX = ix + 12;
        int boxY = y + 9;
        theme.inset(fade, boxX, boxY, 72, 40);
        String[] corners = {"top_left", "top_right", "bottom_left", "bottom_right"};
        for (int i = 0; i < corners.length; i++) {
            int padX = boxX + 4 + (i % 2) * (72 - 8 - 24);
            int padY = boxY + 4 + (i / 2) * (40 - 8 - 10);
            if (gui.pad("corner" + i, padX, padY, 24, 10, prefs.toastCorner() == i,
                    text("orpheus.ui.toast." + corners[i]))) {
                prefs.setToastCorner(i);
                ToastOverlay.preview();
            }
        }
        int sideX = boxX + 72 + 14;
        int sideW = lw - 12 - (sideX - ix);
        int seconds = prefs.toastSeconds();
        int step = gui.stepper("toast-seconds", sideX, y + 9, sideW, 18, Texts.duration(seconds).getString(),
                text("orpheus.ui.toast.duration"), seconds > 2, seconds < 15);
        if (step != 0) {
            prefs.setToastSeconds(seconds + step);
        }
        if (gui.button("toast-preview", sideX, y + 31, sideW, 18, text("orpheus.ui.toast.preview"),
                text("orpheus.ui.toast.preview.tooltip"))) {
            ToastOverlay.preview();
        }
        return y + 58 + GAP;
    }

    private void renderPresets(Theme theme) {
        OrpheusRuntime runtime = OrpheusClient.runtime();
        int pw = windowWidth();
        int ph = windowHeight();
        int px = (width - pw) / 2;
        int py = (height - ph) / 2;
        theme.window(fade, px, py, pw, ph);

        int ix = px + PAD;
        int iw = pw - 2 * PAD;
        int y = py + PAD;

        if (gui.iconButton("presets-back", ix, y, 28, ROW, "left", ButtonKind.NORMAL, true,
                text("orpheus.presets.back"))) {
            openMain();
        }
        String title = text("orpheus.presets.title");
        if (theme.centeredTitle() && pw >= 340) {
            gui.centered(title, px + pw / 2, y + 5, theme.titleColor());
        } else {
            fade.push();
            fade.translate(ix + 36, y + 2);
            fade.scale(1.25f, 1.25f);
            gui.text(title, 0, 0, theme.titleColor());
            fade.pop();
        }
        y += ROW + GAP;

        int footerY = py + ph - PAD - ROW;
        int fieldY = footerY - ROW - GAP;
        int listBottom = fieldY - GAP - 14;

        beginContent();
        float before = fade.alpha();
        fade.alpha(before * Motion.easeOutCubic(contentFade));
        gui.text(gui.fit(presetStatus, iw - 4), ix + 2, y + 1, theme.textSecondary());
        y += 14;

        int viewH = listBottom - y + 14;
        float scroll = gui.scrollBegin("presets", ix, y, iw, viewH);
        int lw = iw - 8;
        int top = y - Math.round(scroll);
        boolean canDelete = presetNames.size() > 1;
        for (int i = 0; i < presetNames.size(); i++) {
            String name = presetNames.get(i);
            int ry = top + i * 26;
            if (ry + 26 < y || ry > y + viewH) {
                continue;
            }
            presetRow(runtime, name, ix, ry, lw, canDelete);
        }
        gui.scrollEnd(Math.max(1, presetNames.size() * 26));
        fade.alpha(before);
        endContent();

        int copyW = 62;
        int blankW = 62;
        int fieldW = iw - copyW - blankW - 2 * 4;
        String name = gui.field(NAME_FIELD, ix, fieldY, fieldW, ROW, text("orpheus.presets.name"),
                PresetStore.MAX_NAME_LENGTH);
        boolean submit = gui.submitted(NAME_FIELD);
        if (gui.button("copy", ix + fieldW + 4, fieldY, copyW, ROW, text("orpheus.presets.copy"),
                text("orpheus.presets.copy.tooltip")) || submit) {
            create(runtime, name, true);
        }
        if (gui.button("blank", ix + fieldW + copyW + 8, fieldY, blankW, ROW, text("orpheus.presets.blank"),
                text("orpheus.presets.blank.tooltip"))) {
            create(runtime, name, false);
        }
        if (gui.button("back", ix, footerY, iw, ROW, text("orpheus.presets.back"), ButtonKind.PRIMARY, false, true,
                null)) {
            openMain();
        }
    }

    private void presetRow(OrpheusRuntime runtime, String name, int ix, int y, int lw, boolean canDelete) {
        Theme theme = gui.theme();
        boolean active = name.equalsIgnoreCase(runtime.active().name());
        int deleteW = 62;
        int zone = lw - deleteW - 8;
        gui.rowBackground("preset-row:" + name, ix, y, lw, 24, active);
        if (gui.hit("preset-hit:" + name, ix, y, zone, 24, true, text("orpheus.presets.switch.tooltip"))) {
            presetStatus = runtime.switchTo(name)
                    ? text("orpheus.presets.switched", name)
                    : text("orpheus.presets.switch_failed", name);
        }
        float dot = gui.anim("dot:" + name, active ? 1f : 0f, 14f);
        int accent = prefs.classic() ? 0xFF55FF55 : prefs.accentColor();
        Draw.round(fade, ix + 10, y + 9, 6, 6, 3, Colors.mix(0x33FFFFFF, accent, dot));
        gui.text(gui.fit(name, zone - 34), ix + 24, gui.textY(y, 24),
                active ? theme.titleColor() : theme.textPrimary());
        if (gui.button("preset-delete:" + name, ix + lw - deleteW - 4, y + 2, deleteW, 20,
                text("orpheus.presets.delete"), ButtonKind.DANGER, false, canDelete,
                text("orpheus.presets.delete.tooltip"))) {
            presetStatus = runtime.deletePreset(name)
                    ? text("orpheus.presets.deleted", name)
                    : text("orpheus.presets.delete_failed", name);
            presetNames = runtime.presetNames();
        }
    }

    private void create(OrpheusRuntime runtime, String name, boolean copy) {
        boolean created = copy ? runtime.createFromActive(name) : runtime.createDefault(name);
        if (created) {
            presetStatus = text("orpheus.presets.created", runtime.active().name());
            gui.setField(NAME_FIELD, "");
        } else {
            presetStatus = text("orpheus.presets.invalid_name");
        }
        presetNames = runtime.presetNames();
    }
}
