package com.github.olympus.orpheus.client.ui;

public final class ClassicTheme implements Theme {
    private static final String BACKGROUND_NAMESPACE = "orpheus";
    private static final String BACKGROUND_PATH = "textures/gui/classic/background.png";

    @Override
    public void accent(int argb) {
    }

    @Override
    public boolean animated() {
        return false;
    }

    @Override
    public boolean shadowText() {
        return true;
    }

    @Override
    public boolean centeredTitle() {
        return true;
    }

    @Override
    public boolean slidingSegments() {
        return false;
    }

    @Override
    public int textPrimary() {
        return 0xFFE0E0E0;
    }

    @Override
    public int textSecondary() {
        return 0xFFA0A0A0;
    }

    @Override
    public int textDisabled() {
        return 0xFF707070;
    }

    @Override
    public int titleColor() {
        return 0xFFFFFFFF;
    }

    @Override
    public void backdrop(Canvas c, int width, int height, boolean inWorld) {
        if (inWorld) {
            c.gradient(0, 0, width, height, 0xC0101010, 0xD0101010);
            return;
        }
        if (c.exists(BACKGROUND_NAMESPACE, BACKGROUND_PATH)) {
            c.tiled(BACKGROUND_NAMESPACE, BACKGROUND_PATH, 0, 0, width, height);
            return;
        }
        c.tiled("minecraft", "textures/block/dirt.png", 0, 0, width, height);
        c.fill(0, 0, width, height, 0xB0000000);
    }

    @Override
    public void window(Canvas c, int x, int y, int w, int h) {
    }

    @Override
    public void card(Canvas c, int x, int y, int w, int h) {
        c.sprite("classic/panel", x, y, w, h);
    }

    @Override
    public void button(Canvas c, int x, int y, int w, int h, ButtonKind kind, float hover, float press,
            float selected, boolean enabled) {
        String name = !enabled ? "button_disabled"
                : selected > 0.5f ? "button_selected"
                : hover > 0.5f ? "button_hover"
                : "button";
        c.sprite("classic/" + name, x, y, w, h);
    }

    @Override
    public int buttonText(ButtonKind kind, float hover, float selected, boolean enabled) {
        if (!enabled) {
            return 0xFFA0A0A0;
        }
        if (kind == ButtonKind.DANGER) {
            return hover > 0.5f ? 0xFFFFAAAA : 0xFFFF7777;
        }
        return hover > 0.5f || selected > 0.5f ? 0xFFFFFFA0 : 0xFFE0E0E0;
    }

    @Override
    public void segmentTrack(Canvas c, int x, int y, int w, int h) {
    }

    @Override
    public void segmentThumb(Canvas c, int x, int y, int w, int h) {
    }

    @Override
    public int segmentText(float selected, float hover) {
        return 0xFFE0E0E0;
    }

    @Override
    public int toggleWidth() {
        return 34;
    }

    @Override
    public void toggle(Canvas c, int x, int y, int h, float on, float hover, boolean enabled, String onLabel,
            String offLabel) {
        int w = toggleWidth();
        button(c, x, y, w, h, ButtonKind.NORMAL, hover, 0f, 0f, enabled);
        boolean value = on > 0.5f;
        String label = value ? onLabel : offLabel;
        int color = value ? 0xFF55FF55 : 0xFFFF5555;
        c.text(label, x + (w - c.textWidth(label)) / 2, y + (h - c.lineHeight()) / 2 + 1, color, true);
    }

    @Override
    public void checkbox(Canvas c, int x, int y, int size, float on, float hover, boolean enabled) {
        boolean lit = hover > 0.5f && enabled;
        String name = on > 0.5f
                ? lit ? "checkbox_checked_hover" : "checkbox_checked"
                : lit ? "checkbox_hover" : "checkbox";
        c.sprite("classic/" + name, x, y, size, size);
    }

    @Override
    public void inset(Canvas c, int x, int y, int w, int h) {
        c.sprite("classic/inset", x, y, w, h);
    }

    @Override
    public void field(Canvas c, int x, int y, int w, int h, float focus, float hover) {
        c.sprite(focus > 0.5f ? "classic/field_focused" : "classic/field", x, y, w, h);
    }

    @Override
    public void row(Canvas c, int x, int y, int w, int h, float hover, boolean highlighted) {
        if (highlighted) {
            c.sprite("classic/row_selected", x, y, w, h);
        } else if (hover > 0.5f) {
            c.sprite("classic/row_hover", x, y, w, h);
        }
    }

    @Override
    public void progress(Canvas c, int x, int y, int w, int h, float value, double time) {
        c.sprite("classic/progress", x, y, w, h);
        int innerW = w - 4;
        if (value >= 0f) {
            c.sprite("classic/progress_fill", x + 2, y + 2, Math.round(innerW * Math.min(1f, value)), h - 4);
            return;
        }
        int band = Math.max(12, innerW / 4);
        int start = (int) ((time * 0.9 % 1.0) * (innerW + band)) - band;
        int from = Math.max(0, start);
        int to = Math.min(innerW, start + band);
        if (to > from) {
            c.sprite("classic/progress_fill", x + 2 + from, y + 2, to - from, h - 4);
        }
    }

    @Override
    public void scrollbar(Canvas c, int x, int y, int w, int h, int thumbY, int thumbH, float hover) {
        c.sprite("classic/scroller_track", x, y, w, h);
        c.sprite("classic/scroller", x, thumbY, w, thumbH);
    }

    @Override
    public void tooltip(Canvas c, int x, int y, int w, int h) {
        c.sprite("classic/tooltip", x, y, w, h);
    }

    @Override
    public void icon(Canvas c, String name, int cx, int cy, int color) {
        int w;
        int h;
        switch (name) {
            case "left", "right" -> {
                w = 6;
                h = 9;
            }
            case "cross" -> {
                w = 8;
                h = 7;
            }
            case "play", "pause", "next", "previous", "download" -> {
                w = 9;
                h = 9;
            }
            default -> {
                return;
            }
        }
        c.sprite("classic/icon/" + name, cx - w / 2, cy - h / 2, w, h);
    }

    @Override
    public void swatch(Canvas c, int x, int y, int size, int color, float hover, float selected) {
        c.fill(x, y, size, size, selected > 0.5f ? 0xFFFFFFFF : 0xFF000000);
        c.fill(x + 1, y + 1, size - 2, size - 2, color);
    }
}
