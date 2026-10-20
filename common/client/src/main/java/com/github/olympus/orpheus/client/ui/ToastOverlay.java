package com.github.olympus.orpheus.client.ui;

import com.github.olympus.orpheus.client.Texts;
import com.github.olympus.orpheus.core.model.Track;
import com.github.olympus.orpheus.core.model.TrackKind;
import java.util.List;
import net.minecraft.network.chat.Component;

public final class ToastOverlay {
    private static final float ENTER = 0.42f;
    private static final float EXIT = 0.3f;
    private static final int MARGIN = 8;
    private static final int PAD = 8;
    private static final int ICON = 24;
    private static final int MIN_WIDTH = 120;
    private static final int MAX_WIDTH = 320;
    private static final int MAX_LINES = 4;
    private static final String[] CLASSIC_DISCS = {
        "13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal", "strad", "ward", "11", "wait"
    };

    private static final Theme MODERN = new ModernTheme();
    private static final Theme CLASSIC = new ClassicTheme();
    private static final FadeCanvas FADE = new FadeCanvas();

    private static String title = "";
    private static String body = "";
    private static String icon = "";
    private static boolean active;
    private static double age;
    private static float width;
    private static float height;
    private static long last;

    private ToastOverlay() {
    }

    public static synchronized void show(String newTitle, String newBody, String newIcon) {
        title = newTitle;
        body = newBody;
        icon = newIcon;
        if (!active) {
            width = 0f;
            height = 0f;
            age = 0;
            last = System.nanoTime();
        } else {
            age = Math.min(age, ENTER);
        }
        active = true;
    }

    public static void showTrack(Track track) {
        show(Component.translatable("orpheus.toast.title").getString(), Texts.nowPlaying(track).getString(),
                iconFor(track));
    }

    public static void preview() {
        show(Component.translatable("orpheus.toast.title").getString(),
                Component.translatable("orpheus.ui.toast.sample").getString(), "music_disc_cat");
    }

    public static synchronized boolean active() {
        return active;
    }

    public static synchronized void dismiss() {
        active = false;
    }

    private static String iconFor(Track track) {
        String name = track.kind() == TrackKind.DISC
                ? track.filePath().substring("records/".length())
                : CLASSIC_DISCS[Math.floorMod(track.id().hashCode(), CLASSIC_DISCS.length)];
        return "music_disc_" + name;
    }

    public static synchronized void render(Canvas target) {
        if (!active) {
            return;
        }
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
        last = now;
        age += dt;

        UiPrefs prefs = UiPrefs.get();
        Theme theme = prefs.classic() ? CLASSIC : MODERN;
        theme.accent(prefs.accentColor());
        boolean animated = theme.animated() && prefs.animations();
        float hold = prefs.toastSeconds();
        float enter = animated ? Motion.clamp((float) age / ENTER, 0f, 1f) : 1f;
        float exit = animated ? Motion.clamp((float) (age - hold) / EXIT, 0f, 1f) : age >= hold ? 1f : 0f;
        if (exit >= 1f) {
            active = false;
            return;
        }

        boolean hasIcon = !icon.isEmpty() && target.hasItem(icon);
        int textX = PAD + (hasIcon ? ICON + PAD : 0);
        int maxWidth = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, target.width() - 2 * MARGIN));
        int textMax = maxWidth - textX - PAD;
        List<String> lines = Draw.wrap(target, body, textMax);
        if (lines.size() > MAX_LINES) {
            lines = lines.subList(0, MAX_LINES);
            lines.set(MAX_LINES - 1, Draw.fit(target, lines.get(MAX_LINES - 1) + "...", textMax));
        }
        int textWidth = target.textWidth(title);
        for (String line : lines) {
            textWidth = Math.max(textWidth, target.textWidth(line));
        }
        int lineHeight = target.lineHeight() + 1;
        float wantedWidth = Math.max(MIN_WIDTH, Math.min(maxWidth, textX + textWidth + PAD));
        float wantedHeight = PAD + lineHeight + 2 + lines.size() * lineHeight + PAD - 1;
        if (hasIcon) {
            wantedHeight = Math.max(ICON + 2 * PAD, wantedHeight);
        }
        if (width == 0f) {
            width = wantedWidth;
            height = wantedHeight;
        } else if (animated) {
            width = Motion.approach(width, wantedWidth, dt, 14f);
            height = Motion.approach(height, wantedHeight, dt, 14f);
        } else {
            width = wantedWidth;
            height = wantedHeight;
        }
        int w = Math.round(width);
        int h = Math.round(height);

        int corner = prefs.toastCorner();
        boolean right = corner == 1 || corner == 3;
        boolean bottom = corner == 2 || corner == 3;
        int x = right ? target.width() - MARGIN - w : MARGIN;
        int y = bottom ? target.height() - MARGIN - h : MARGIN;

        float eased = animated ? easeOutBack(enter) : 1f;
        float leave = exit * exit * exit;
        float shown = eased * (1f - leave);
        float slide = (1f - shown) * (w + MARGIN);
        int dx = Math.round(right ? slide : -slide);
        float alpha = animated ? Motion.clamp(Math.min(enter * 1.8f, 1f - exit), 0f, 1f) : 1f;

        FADE.bind(target, alpha);
        FADE.push();
        FADE.translate(dx, 0f);
        theme.toast(FADE, x, y, w, h);
        if (hasIcon) {
            int boxY = y + (h - ICON) / 2;
            theme.inset(FADE, x + PAD, boxY, ICON, ICON);
            FADE.push();
            FADE.translate(x + PAD + 2, boxY + 2);
            FADE.scale(1.25f, 1.25f);
            FADE.item(icon, 0, 0);
            FADE.pop();
        }
        boolean shadow = theme.shadowText();
        int top = y + (h - (lineHeight + 2 + lines.size() * lineHeight - 1)) / 2;
        FADE.text(Draw.fit(FADE, title, w - textX - PAD), x + textX, top,
                prefs.classic() ? 0xFFFFFF55 : theme.textSecondary(), shadow);
        for (int i = 0; i < lines.size(); i++) {
            FADE.text(lines.get(i), x + textX, top + lineHeight + 2 + i * lineHeight, theme.titleColor(), shadow);
        }
        FADE.pop();
        FADE.unbind();
    }

    private static float easeOutBack(float t) {
        float k = 1.4f;
        float u = t - 1f;
        return 1f + (k + 1f) * u * u * u + k * u * u;
    }
}
