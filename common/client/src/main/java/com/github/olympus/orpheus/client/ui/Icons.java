package com.github.olympus.orpheus.client.ui;

public final class Icons {
    private Icons() {
    }

    public static void play(Canvas c, int x, int y, int size, int color) {
        for (int i = 0; i < size; i++) {
            int d = Math.abs(2 * i - (size - 1));
            c.fill(x, y + i, Math.max(1, Math.round((size - d) * 0.9f)), 1, color);
        }
    }

    public static void pause(Canvas c, int x, int y, int size, int color) {
        int bar = Math.max(2, size / 3);
        c.fill(x, y, bar, size, color);
        c.fill(x + size - bar, y, bar, size, color);
    }

    private static int skipWidth(int row, int size) {
        int d = Math.abs(row - (size - 1) / 2);
        return Math.max(1, Math.round(6f - d * 6f / ((size + 1) / 2f)));
    }

    public static void next(Canvas c, int x, int y, int size, int color) {
        for (int i = 0; i < size; i++) {
            c.fill(x, y + i, skipWidth(i, size), 1, color);
        }
        c.fill(x + 7, y, 2, size, color);
    }

    public static void previous(Canvas c, int x, int y, int size, int color) {
        for (int i = 0; i < size; i++) {
            int width = skipWidth(i, size);
            c.fill(x + 9 - width, y + i, width, 1, color);
        }
        c.fill(x, y, 2, size, color);
    }

    public static void check(Canvas c, int x, int y, int color) {
        int[] tops = {3, 4, 5, 4, 3, 2, 1, 0};
        for (int i = 0; i < tops.length; i++) {
            c.fill(x + i, y + tops[i], 1, 2, color);
        }
    }

    public static void cross(Canvas c, int x, int y, int color) {
        for (int i = 0; i < 7; i++) {
            c.fill(x + i, y + i, 2, 1, color);
            c.fill(x + 5 - i, y + i, 2, 1, color);
        }
    }

    public static void chevron(Canvas c, int x, int y, boolean right, int color) {
        for (int i = 0; i < 9; i++) {
            int d = Math.abs(i - 4);
            c.fill(x + (right ? 4 - d : d), y + i, 2, 1, color);
        }
    }

    public static void download(Canvas c, int x, int y, int color) {
        c.fill(x + 3, y, 3, 5, color);
        for (int r = 0; r < 5; r++) {
            c.fill(x + r, y + 4 + r, 9 - 2 * r, 1, color);
        }
    }

    public static void note(Canvas c, int x, int y, int color) {
        c.fill(x + 4, y, 2, 8, color);
        Draw.round(c, x, y + 6, 6, 5, 2, color);
        c.fill(x + 6, y, 3, 2, color);
        c.fill(x + 8, y + 2, 1, 3, color);
    }

    public static void equalizer(Canvas c, int x, int y, int height, double time, float level, int color) {
        for (int i = 0; i < 4; i++) {
            double wave = 0.5 + 0.5 * Math.sin(time * (5.0 + i * 1.7) + i * 1.3);
            int bar = Math.max(2, (int) Math.round(height * (0.2 + 0.8 * level * wave)));
            c.fill(x + i * 4, y + height - bar, 3, bar, color);
        }
    }
}
