package com.github.olympus.orpheus.client.ui;

public final class Motion {
    private Motion() {
    }

    public static float approach(float current, float target, float dt, float rate) {
        float next = current + (target - current) * (1f - (float) Math.exp(-rate * dt));
        return Math.abs(target - next) < 0.002f ? target : next;
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static float easeOutCubic(float t) {
        float k = 1f - clamp(t, 0f, 1f);
        return 1f - k * k * k;
    }
}
