package com.github.olympus.orpheus.client.ui;

public final class ModernTheme implements Theme {
    private static final int RED = 0xFFE5484D;
    private int accent = 0xFF8B5CF6;

    @Override
    public void accent(int argb) {
        accent = argb;
    }

    @Override
    public boolean animated() {
        return true;
    }

    @Override
    public boolean shadowText() {
        return false;
    }

    @Override
    public boolean centeredTitle() {
        return false;
    }

    @Override
    public boolean slidingSegments() {
        return true;
    }

    @Override
    public int textPrimary() {
        return 0xFFECEFF4;
    }

    @Override
    public int textSecondary() {
        return 0xFF9AA3B2;
    }

    @Override
    public int textDisabled() {
        return 0xFF5C6370;
    }

    @Override
    public int titleColor() {
        return 0xFFFFFFFF;
    }

    @Override
    public void backdrop(Canvas c, int width, int height, boolean inWorld) {
        c.gradient(0, 0, width, height, 0x8C05060A, 0xC005060A);
    }

    @Override
    public void window(Canvas c, int x, int y, int w, int h) {
        Draw.shadow(c, x, y, w, h, 6, 9, 150);
        Draw.panel(c, x, y, w, h, 6, 0xEB111218, 0x2EFFFFFF);
    }

    @Override
    public void card(Canvas c, int x, int y, int w, int h) {
        Draw.panel(c, x, y, w, h, 4, 0x12FFFFFF, 0x14FFFFFF);
    }

    @Override
    public void button(Canvas c, int x, int y, int w, int h, ButtonKind kind, float hover, float press,
            float selected, boolean enabled) {
        int shrink = press > 0.5f ? 1 : 0;
        int bx = x + shrink;
        int by = y + shrink;
        int bw = w - 2 * shrink;
        int bh = h - 2 * shrink;
        float dim = enabled ? 1f : 0.45f;
        switch (kind) {
            case PRIMARY -> {
                int fill = Colors.lighten(accent, 0.18f * hover);
                if (hover > 0.02f) {
                    Draw.round(c, x - 1, y - 1, w + 2, h + 2, 4, Colors.alpha(accent, 0.28f * hover));
                }
                Draw.round(c, bx, by, bw, bh, 3, Colors.alpha(fill, dim));
            }
            case DANGER -> {
                int fill = Colors.lighten(RED, 0.18f * hover);
                Draw.round(c, bx, by, bw, bh, 3, Colors.alpha(fill, dim));
            }
            case GHOST -> Draw.round(c, bx, by, bw, bh, 3, Colors.alpha(0x30FFFFFF, hover * dim));
            case SEGMENT -> {
            }
            default -> {
                int fill = Colors.mix(0x1CFFFFFF, 0x38FFFFFF, hover);
                Draw.round(c, bx + 1, by + 1, bw - 2, bh - 2, 2, Colors.alpha(fill, dim));
                Draw.ring(c, bx, by, bw, bh, 3,
                        Colors.mix(0x22FFFFFF, Colors.alpha(accent, 0.75f), hover * dim));
            }
        }
    }

    @Override
    public int buttonText(ButtonKind kind, float hover, float selected, boolean enabled) {
        if (!enabled) {
            return textDisabled();
        }
        return switch (kind) {
            case PRIMARY, DANGER -> 0xFFFFFFFF;
            case GHOST -> Colors.mix(textSecondary(), textPrimary(), hover);
            default -> textPrimary();
        };
    }

    @Override
    public void segmentTrack(Canvas c, int x, int y, int w, int h) {
        Draw.panel(c, x, y, w, h, 4, 0x2E000000, 0x16FFFFFF);
    }

    @Override
    public void segmentThumb(Canvas c, int x, int y, int w, int h) {
        Draw.round(c, x, y, w, h, 3, Colors.alpha(accent, 0.95f));
        c.gradient(x + 1, y + 1, w - 2, Math.max(1, h / 2), 0x22FFFFFF, 0x00FFFFFF);
    }

    @Override
    public int segmentText(float selected, float hover) {
        return Colors.mix(Colors.mix(textSecondary(), textPrimary(), hover), 0xFFFFFFFF, selected);
    }

    @Override
    public int toggleWidth() {
        return 30;
    }

    @Override
    public void toggle(Canvas c, int x, int y, int h, float on, float hover, boolean enabled, String onLabel,
            String offLabel) {
        int w = toggleWidth();
        int track = Colors.mix(Colors.mix(0x3AFFFFFF, 0x55FFFFFF, hover), accent, on);
        Draw.round(c, x, y, w, h, h / 2, Colors.alpha(track, enabled ? 1f : 0.4f));
        int knob = h - 4;
        int kx = x + 2 + Math.round((w - knob - 4) * on);
        Draw.round(c, kx, y + 2, knob, knob, knob / 2, 0xFFFFFFFF);
    }

    @Override
    public void checkbox(Canvas c, int x, int y, int size, float on, float hover, boolean enabled) {
        float dim = enabled ? 1f : 0.4f;
        int fill = Colors.mix(Colors.mix(0x22FFFFFF, 0x38FFFFFF, hover), accent, on);
        Draw.round(c, x, y, size, size, 3, Colors.alpha(fill, dim));
        if (on < 0.98f) {
            Draw.ring(c, x, y, size, size, 3, Colors.alpha(0x55FFFFFF, (1f - on) * dim));
        }
        if (on > 0.15f) {
            Icons.check(c, x + (size - 8) / 2, y + (size - 7) / 2 + 1, Colors.alpha(0xFFFFFFFF, on * dim));
        }
    }

    @Override
    public void inset(Canvas c, int x, int y, int w, int h) {
        Draw.panel(c, x, y, w, h, 3, 0x2E000000, 0x16FFFFFF);
    }

    @Override
    public void field(Canvas c, int x, int y, int w, int h, float focus, float hover) {
        Draw.panel(c, x, y, w, h, 3, 0x38000000, Colors.mix(Colors.mix(0x1CFFFFFF, 0x38FFFFFF, hover),
                Colors.alpha(accent, 0.95f), focus));
    }

    @Override
    public void row(Canvas c, int x, int y, int w, int h, float hover, boolean highlighted) {
        if (highlighted) {
            Draw.round(c, x, y, w, h, 3, Colors.alpha(accent, 0.14f));
        }
        if (hover > 0.02f) {
            Draw.round(c, x, y, w, h, 3, Colors.alpha(0x16FFFFFF, hover));
        }
    }

    @Override
    public void progress(Canvas c, int x, int y, int w, int h, float value, double time) {
        Draw.panel(c, x, y, w, h, 3, 0x38000000, 0x16FFFFFF);
        int innerX = x + 2;
        int innerW = w - 4;
        if (value >= 0f) {
            Draw.round(c, innerX, y + 2, Math.round(innerW * Math.min(1f, value)), h - 4, 2,
                    Colors.alpha(accent, 0.95f));
            return;
        }
        int band = Math.max(12, innerW / 4);
        int travel = innerW + band;
        int start = (int) ((time * 0.9 % 1.0) * travel) - band;
        int from = Math.max(0, start);
        int to = Math.min(innerW, start + band);
        if (to > from) {
            Draw.round(c, innerX + from, y + 2, to - from, h - 4, 2, Colors.alpha(accent, 0.95f));
        }
    }

    @Override
    public void scrollbar(Canvas c, int x, int y, int w, int h, int thumbY, int thumbH, float hover) {
        Draw.round(c, x, y, w, h, 1, 0x18FFFFFF);
        Draw.round(c, x, thumbY, w, thumbH, 1, Colors.alpha(accent, 0.55f + 0.4f * hover));
    }

    @Override
    public void tooltip(Canvas c, int x, int y, int w, int h) {
        Draw.shadow(c, x, y, w, h, 4, 4, 90);
        Draw.panel(c, x, y, w, h, 4, 0xF2101116, Colors.alpha(accent, 0.7f));
    }

    @Override
    public void icon(Canvas c, String name, int cx, int cy, int color) {
        switch (name) {
            case "play" -> Icons.play(c, cx - 4, cy - 4, 9, color);
            case "pause" -> Icons.pause(c, cx - 4, cy - 4, 9, color);
            case "next" -> Icons.next(c, cx - 4, cy - 4, 9, color);
            case "previous" -> Icons.previous(c, cx - 4, cy - 4, 9, color);
            case "left" -> Icons.chevron(c, cx - 3, cy - 4, false, color);
            case "right" -> Icons.chevron(c, cx - 3, cy - 4, true, color);
            case "cross" -> Icons.cross(c, cx - 4, cy - 3, color);
            case "download" -> Icons.download(c, cx - 4, cy - 4, color);
            default -> {
            }
        }
    }

    @Override
    public void swatch(Canvas c, int x, int y, int size, int color, float hover, float selected) {
        int grow = Math.round(hover);
        Draw.round(c, x - grow, y - grow, size + 2 * grow, size + 2 * grow, 4, color);
        if (selected > 0.05f) {
            Draw.ring(c, x - 3, y - 3, size + 6, size + 6, 5, Colors.alpha(0xFFFFFFFF, selected));
        }
    }
}
