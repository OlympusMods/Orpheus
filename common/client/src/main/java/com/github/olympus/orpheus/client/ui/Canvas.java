package com.github.olympus.orpheus.client.ui;

public interface Canvas {
    int width();

    int height();

    void fill(int x, int y, int w, int h, int argb);

    void gradient(int x, int y, int w, int h, int topArgb, int bottomArgb);

    void text(String text, int x, int y, int argb, boolean shadow);

    int textWidth(String text);

    int lineHeight();

    void push();

    void pop();

    void translate(float x, float y);

    void scale(float x, float y);

    void clip(int x, int y, int w, int h);

    void unclip();

    boolean hasItem(String name);

    void item(String name, int x, int y);

    void sprite(String path, int x, int y, int w, int h);

    boolean exists(String namespace, String path);

    void tiled(String namespace, String path, int x, int y, int w, int h);
}
