package com.github.olympus.orpheus.core.ui;

public final class DelayStep {
    private DelayStep() {
    }

    public static int step(int current, boolean increase) {
        int size = sizeFor(current, increase);
        int next = increase ? current + size : current - size;
        return Math.max(0, next);
    }

    private static int sizeFor(int current, boolean increase) {
        if (current < 60 || (!increase && current == 60)) {
            return 5;
        }
        if (current < 600 || (!increase && current == 600)) {
            return 30;
        }
        return 60;
    }

    public static String format(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        int minutes = seconds / 60;
        int rest = seconds % 60;
        return rest == 0 ? minutes + "m" : minutes + "m " + rest + "s";
    }
}
