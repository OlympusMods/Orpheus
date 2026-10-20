package com.github.olympus.orpheus.client.ui;

public interface Theme {
    void accent(int argb);

    boolean animated();

    boolean shadowText();

    boolean centeredTitle();

    boolean slidingSegments();

    int textPrimary();

    int textSecondary();

    int textDisabled();

    int titleColor();

    void backdrop(Canvas c, int width, int height, boolean inWorld);

    void window(Canvas c, int x, int y, int w, int h);

    void card(Canvas c, int x, int y, int w, int h);

    void button(Canvas c, int x, int y, int w, int h, ButtonKind kind, float hover, float press, float selected,
            boolean enabled);

    int buttonText(ButtonKind kind, float hover, float selected, boolean enabled);

    void segmentTrack(Canvas c, int x, int y, int w, int h);

    void segmentThumb(Canvas c, int x, int y, int w, int h);

    int segmentText(float selected, float hover);

    int toggleWidth();

    void toggle(Canvas c, int x, int y, int h, float on, float hover, boolean enabled, String onLabel,
            String offLabel);

    void checkbox(Canvas c, int x, int y, int size, float on, float hover, boolean enabled);

    void inset(Canvas c, int x, int y, int w, int h);

    void field(Canvas c, int x, int y, int w, int h, float focus, float hover);

    void row(Canvas c, int x, int y, int w, int h, float hover, boolean highlighted);

    void progress(Canvas c, int x, int y, int w, int h, float value, double time);

    void scrollbar(Canvas c, int x, int y, int w, int h, int thumbY, int thumbH, float hover);

    void tooltip(Canvas c, int x, int y, int w, int h);

    void icon(Canvas c, String name, int centerX, int centerY, int color);

    void toast(Canvas c, int x, int y, int w, int h);

    void swatch(Canvas c, int x, int y, int size, int color, float hover, float selected);
}
