package com.github.olympus.orpheus.client.ui;

public final class Colors {
    private Colors() {
    }

    public static int alpha(int argb, float factor) {
        int a = Math.round(((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, factor)));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    public static int withAlpha(int rgb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    public static int mix(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = channel(from, to, t, 24);
        int r = channel(from, to, t, 16);
        int g = channel(from, to, t, 8);
        int b = channel(from, to, t, 0);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int lighten(int argb, float t) {
        return (argb & 0xFF000000) | (mix(argb | 0xFF000000, 0xFFFFFFFF, t) & 0xFFFFFF);
    }

    public static int darken(int argb, float t) {
        return (argb & 0xFF000000) | (mix(argb | 0xFF000000, 0xFF000000, t) & 0xFFFFFF);
    }

    public static int hsl(float hue, float saturation, float lightness) {
        float chroma = (1f - Math.abs(2f * lightness - 1f)) * saturation;
        float h = (hue - (float) Math.floor(hue)) * 6f;
        float x = chroma * (1f - Math.abs(h % 2f - 1f));
        float r = 0f;
        float g = 0f;
        float b = 0f;
        switch ((int) h) {
            case 0 -> { r = chroma; g = x; }
            case 1 -> { r = x; g = chroma; }
            case 2 -> { g = chroma; b = x; }
            case 3 -> { g = x; b = chroma; }
            case 4 -> { r = x; b = chroma; }
            default -> { r = chroma; b = x; }
        }
        float m = lightness - chroma / 2f;
        return 0xFF000000 | (Math.round((r + m) * 255f) << 16) | (Math.round((g + m) * 255f) << 8)
                | Math.round((b + m) * 255f);
    }

    private static int channel(int from, int to, float t, int shift) {
        int a = (from >>> shift) & 0xFF;
        int b = (to >>> shift) & 0xFF;
        return Math.round(a + (b - a) * t);
    }
}
