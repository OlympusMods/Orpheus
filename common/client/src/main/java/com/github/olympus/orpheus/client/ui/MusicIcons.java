package com.github.olympus.orpheus.client.ui;

import com.github.olympus.proteus.client.ui.Canvas;
import com.github.olympus.proteus.client.ui.Draw;
import com.github.olympus.proteus.client.ui.Icons;

/** The icons only Orpheus needs. Proteus draws them through its icon registry and owns everything else. */
public final class MusicIcons {
    public static final String PLAY = "orpheus:play";
    public static final String PAUSE = "orpheus:pause";
    public static final String NEXT = "orpheus:next";
    public static final String PREVIOUS = "orpheus:previous";
    public static final String DOWNLOAD = "orpheus:download";

    private static boolean registered;

    private MusicIcons() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        Icons.register(PLAY, 9, 9, (c, cx, cy, color) -> play(c, cx - 4, cy - 4, 9, color));
        Icons.register(PAUSE, 9, 9, (c, cx, cy, color) -> pause(c, cx - 4, cy - 4, 9, color));
        Icons.register(NEXT, 9, 9, (c, cx, cy, color) -> next(c, cx - 4, cy - 4, 9, color));
        Icons.register(PREVIOUS, 9, 9, (c, cx, cy, color) -> previous(c, cx - 4, cy - 4, 9, color));
        Icons.register(DOWNLOAD, 9, 9, (c, cx, cy, color) -> download(c, cx - 4, cy - 4, color));
    }

    static void play(Canvas c, int x, int y, int size, int color) {
        for (int i = 0; i < size; i++) {
            int d = Math.abs(2 * i - (size - 1));
            c.fill(x, y + i, Math.max(1, Math.round((size - d) * 0.9f)), 1, color);
        }
    }

    static void pause(Canvas c, int x, int y, int size, int color) {
        int bar = Math.max(2, size / 3);
        c.fill(x, y, bar, size, color);
        c.fill(x + size - bar, y, bar, size, color);
    }

    private static int skipWidth(int row, int size) {
        int d = Math.abs(row - (size - 1) / 2);
        return Math.max(1, Math.round(6f - d * 6f / ((size + 1) / 2f)));
    }

    static void next(Canvas c, int x, int y, int size, int color) {
        for (int i = 0; i < size; i++) {
            c.fill(x, y + i, skipWidth(i, size), 1, color);
        }
        c.fill(x + 7, y, 2, size, color);
    }

    static void previous(Canvas c, int x, int y, int size, int color) {
        for (int i = 0; i < size; i++) {
            int width = skipWidth(i, size);
            c.fill(x + 9 - width, y + i, width, 1, color);
        }
        c.fill(x, y, 2, size, color);
    }

    static void download(Canvas c, int x, int y, int color) {
        c.fill(x + 3, y, 3, 5, color);
        for (int r = 0; r < 5; r++) {
            c.fill(x + r, y + 4 + r, 9 - 2 * r, 1, color);
        }
    }

    static void note(Canvas c, int x, int y, int color) {
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
