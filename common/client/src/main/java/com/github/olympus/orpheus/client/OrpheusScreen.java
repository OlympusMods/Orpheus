package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.OrpheusRuntime;
import com.github.olympus.orpheus.core.model.MusicContext;
import com.github.olympus.orpheus.core.model.Track;
import com.github.olympus.orpheus.core.preset.ContextSettings;
import com.github.olympus.orpheus.core.preset.Preset;
import com.github.olympus.orpheus.core.ui.DelayStep;
import com.github.olympus.orpheus.core.ui.Pager;
import com.github.olympus.orpheus.core.ui.TrackListing;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class OrpheusScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 20;
    private static final int GAP = 4;
    private static final int ROW_HEIGHT = 22;
    private static final int HEADER_HEIGHT = 134;
    private static final int FOOTER_HEIGHT = 56;
    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 8;

    private final Screen parent;
    private MusicContext context = MusicContext.SURVIVAL;
    private int page;

    public OrpheusScreen(Screen parent) {
        super(Component.literal("Orpheus"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        build();
    }

    @Override
    public void onClose() {
        OrpheusClient.runtime().saveActive();
        OrpheusClient.show(parent);
    }

    private void build() {
        clearWidgets();
        OrpheusRuntime runtime = OrpheusClient.runtime();
        Preset preset = runtime.active();
        ContextSettings settings = preset.settings(context);
        int left = (width - WIDTH) / 2;
        int half = (WIDTH - GAP) / 2;
        int y = 8;

        label(left, y, WIDTH, "Orpheus - " + preset.name());
        y += HEIGHT + GAP;

        button(left, y, half, "Orpheus: " + onOff(preset.enabled()), () -> {
            preset.setEnabled(!preset.enabled());
            build();
        });
        button(left + half + GAP, y, half, "Presets", () -> {
            runtime.saveActive();
            OrpheusClient.show(new PresetScreen(this));
        });
        y += HEIGHT + GAP;

        button(left, y, HEIGHT, "<", () -> {
            context = context.previous();
            page = 0;
            build();
        });
        button(left + HEIGHT + GAP, y, WIDTH - 2 * (HEIGHT + GAP), context.label() + ": " + (settings.managed() ? "Orpheus" : "Vanilla"), () -> {
            settings.setManaged(!settings.managed());
            build();
        });
        button(left + WIDTH - HEIGHT, y, HEIGHT, ">", () -> {
            context = context.next();
            page = 0;
            build();
        });
        y += HEIGHT + GAP;

        stepper(left, y, half, "Min " + DelayStep.format(settings.minDelaySeconds()),
                () -> settings.setMinDelaySeconds(DelayStep.step(settings.minDelaySeconds(), false)),
                () -> settings.setMinDelaySeconds(DelayStep.step(settings.minDelaySeconds(), true)));
        stepper(left + half + GAP, y, half, "Max " + DelayStep.format(settings.maxDelaySeconds()),
                () -> settings.setMaxDelaySeconds(DelayStep.step(settings.maxDelaySeconds(), false)),
                () -> settings.setMaxDelaySeconds(DelayStep.step(settings.maxDelaySeconds(), true)));
        y += HEIGHT + GAP;

        stepper(left, y, half, "No repeat: " + settings.repeatAvoidance(),
                () -> settings.setRepeatAvoidance(settings.repeatAvoidance() - 1),
                () -> settings.setRepeatAvoidance(settings.repeatAvoidance() + 1));
        button(left + half + GAP, y, half, "Reset list", () -> {
            settings.resetTracks(context, runtime.catalog());
            build();
        });

        int rows = Math.max(MIN_ROWS, Math.min(MAX_ROWS, (height - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT));
        List<Track> tracks = TrackListing.sorted(runtime.selectable());
        page = Pager.clamp(page, tracks.size(), rows);
        y = HEADER_HEIGHT;
        for (Track track : Pager.slice(tracks, page, rows)) {
            trackRow(runtime, preset, settings, track, left, y);
            y += ROW_HEIGHT;
        }

        int footer = height - FOOTER_HEIGHT + GAP;
        int pageCount = Pager.pageCount(tracks.size(), rows);
        button(left, footer, HEIGHT, "<", () -> {
            page = Math.max(0, page - 1);
            build();
        });
        label(left + HEIGHT + GAP, footer, WIDTH - 2 * (HEIGHT + GAP), "Page " + (page + 1) + " / " + pageCount);
        button(left + WIDTH - HEIGHT, footer, HEIGHT, ">", () -> {
            page = Math.min(pageCount - 1, page + 1);
            build();
        });
        button(left, footer + HEIGHT + GAP, WIDTH, "Done", this::onClose);
    }

    private void trackRow(
            OrpheusRuntime runtime, Preset preset, ContextSettings settings, Track track, int left, int y) {
        boolean included = settings.includes(track.id());
        boolean banned = preset.isBlacklisted(track.id());
        int x = left;
        int titleWidth = 148;
        String title = (included ? "[x] " : "[ ] ") + track.title();
        button(x, y, titleWidth, banned ? title + " (off)" : title, () -> {
            settings.toggle(track.id());
            build();
        });
        x += titleWidth + GAP;
        Button weight = button(x, y, 46, "W " + settings.weight(track.id()), () -> {
            int next = settings.weight(track.id()) + 1;
            settings.setWeight(track.id(), next > ContextSettings.MAX_WEIGHT ? ContextSettings.MIN_WEIGHT : next);
            build();
        });
        weight.active = included;
        x += 46 + GAP;
        button(x, y, 48, banned ? "Allow" : "Ban", () -> {
            preset.toggleBlacklist(track.id());
            build();
        });
        x += 48 + GAP;
        button(x, y, WIDTH - (x - left), "Play", () -> runtime.controller().playNow(track));
    }

    private void stepper(int x, int y, int width, String text, Runnable decrease, Runnable increase) {
        button(x, y, HEIGHT, "-", () -> {
            decrease.run();
            build();
        });
        label(x + HEIGHT + GAP, y, width - 2 * (HEIGHT + GAP), text);
        button(x + width - HEIGHT, y, HEIGHT, "+", () -> {
            increase.run();
            build();
        });
    }

    private Button button(int x, int y, int width, String text, Runnable action) {
        Button button = Button.builder(Component.literal(text), pressed -> action.run())
                .bounds(x, y, width, HEIGHT)
                .build();
        addRenderableWidget(button);
        return button;
    }

    private void label(int x, int y, int width, String text) {
        Button label = button(x, y, width, text, () -> { });
        label.active = false;
    }

    private static String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }
}
