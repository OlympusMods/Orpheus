package com.github.olympus.orpheus.client.ui;

import java.util.ArrayList;
import java.util.List;

public final class Draw {
    private static final int[][] INSETS = new int[9][];

    private Draw() {
    }

    private static int[] insets(int radius) {
        if (radius < INSETS.length && INSETS[radius] != null) {
            return INSETS[radius];
        }
        int[] rows = new int[radius];
        for (int i = 0; i < radius; i++) {
            double dy = radius - i - 0.5;
            double dx = Math.sqrt(radius * (double) radius - dy * dy);
            rows[i] = radius - (int) Math.round(dx);
        }
        if (radius < INSETS.length) {
            INSETS[radius] = rows;
        }
        return rows;
    }

    public static void round(Canvas c, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int r = Math.min(radius, Math.min(w, h) / 2);
        if (r <= 0) {
            c.fill(x, y, w, h, color);
            return;
        }
        if (r < 3) {
            int[] inset = insets(r);
            for (int i = 0; i < r; i++) {
                c.fill(x + inset[i], y + i, w - 2 * inset[i], 1, color);
                c.fill(x + inset[i], y + h - 1 - i, w - 2 * inset[i], 1, color);
            }
            c.fill(x, y + r, w, h - 2 * r, color);
            return;
        }
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            double edge = r - Math.sqrt(r * (double) r - dy * dy);
            int start = (int) Math.ceil(edge - 0.001);
            double cover = start - edge;
            c.fill(x + start, y + i, w - 2 * start, 1, color);
            c.fill(x + start, y + h - 1 - i, w - 2 * start, 1, color);
            if (cover > 0.1 && start > 0) {
                int partial = Colors.alpha(color, (float) cover);
                c.fill(x + start - 1, y + i, 1, 1, partial);
                c.fill(x + w - start, y + i, 1, 1, partial);
                c.fill(x + start - 1, y + h - 1 - i, 1, 1, partial);
                c.fill(x + w - start, y + h - 1 - i, 1, 1, partial);
            }
        }
        c.fill(x, y + r, w, h - 2 * r, color);
    }

    public static void ring(Canvas c, int x, int y, int w, int h, int radius, int color) {
        if (w <= 1 || h <= 1) {
            return;
        }
        int r = Math.min(radius, Math.min(w, h) / 2);
        if (r <= 0) {
            c.fill(x, y, w, 1, color);
            c.fill(x, y + h - 1, w, 1, color);
            c.fill(x, y + 1, 1, h - 2, color);
            c.fill(x + w - 1, y + 1, 1, h - 2, color);
            return;
        }
        int[] outer = insets(r);
        int[] inner = r > 1 ? insets(r - 1) : new int[0];
        for (int i = 0; i < r; i++) {
            int top = y + i;
            int bottom = y + h - 1 - i;
            if (i == 0) {
                c.fill(x + outer[0], top, w - 2 * outer[0], 1, color);
                c.fill(x + outer[0], bottom, w - 2 * outer[0], 1, color);
                continue;
            }
            int innerInset = 1 + (i - 1 < inner.length ? inner[i - 1] : 0);
            int span = innerInset - outer[i];
            if (span > 0) {
                c.fill(x + outer[i], top, span, 1, color);
                c.fill(x + w - innerInset, top, span, 1, color);
                c.fill(x + outer[i], bottom, span, 1, color);
                c.fill(x + w - innerInset, bottom, span, 1, color);
            }
        }
        c.fill(x, y + r, 1, h - 2 * r, color);
        c.fill(x + w - 1, y + r, 1, h - 2 * r, color);
    }

    public static void panel(Canvas c, int x, int y, int w, int h, int radius, int fill, int border) {
        round(c, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fill);
        ring(c, x, y, w, h, radius, border);
    }

    public static void shadow(Canvas c, int x, int y, int w, int h, int radius, int spread, int maxAlpha) {
        int total = spread * (spread + 1) / 2;
        for (int k = spread; k >= 1; k--) {
            int alpha = Math.max(1, Math.round(maxAlpha * (spread + 1 - k) / (float) total));
            round(c, x - k, y - k + 3, w + 2 * k, h + 2 * k, radius + k, alpha << 24);
        }
    }

    public static String fit(Canvas c, String text, int maxWidth) {
        if (c.textWidth(text) <= maxWidth) {
            return text;
        }
        String dots = "...";
        int end = text.length();
        while (end > 0 && c.textWidth(text.substring(0, end)) + c.textWidth(dots) > maxWidth) {
            end--;
        }
        return text.substring(0, end).stripTrailing() + dots;
    }

    public static List<String> wrap(Canvas c, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && c.textWidth(candidate) > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }
}
