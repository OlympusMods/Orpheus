package com.github.olympus.orpheus.client.ui;

public final class FadeCanvas implements Canvas {
    private Canvas target;
    private float alpha = 1f;
    private int shift;

    public void bind(Canvas target, float alpha) {
        this.target = target;
        this.alpha = alpha;
        this.shift = 0;
    }

    public void shift(int dy) {
        shift = dy;
        target.push();
        target.translate(0f, dy);
    }

    public void unshift() {
        target.pop();
        shift = 0;
    }

    public void unbind() {
        target = null;
    }

    public float alpha() {
        return alpha;
    }

    public void alpha(float value) {
        alpha = value;
    }

    @Override
    public int width() {
        return target.width();
    }

    @Override
    public int height() {
        return target.height();
    }

    @Override
    public void fill(int x, int y, int w, int h, int argb) {
        int color = Colors.alpha(argb, alpha);
        if ((color >>> 24) == 0 || w <= 0 || h <= 0) {
            return;
        }
        target.fill(x, y, w, h, color);
    }

    @Override
    public void gradient(int x, int y, int w, int h, int topArgb, int bottomArgb) {
        int top = Colors.alpha(topArgb, alpha);
        int bottom = Colors.alpha(bottomArgb, alpha);
        if (((top | bottom) >>> 24) == 0 || w <= 0 || h <= 0) {
            return;
        }
        target.gradient(x, y, w, h, top, bottom);
    }

    @Override
    public void text(String text, int x, int y, int argb, boolean shadow) {
        int color = Colors.alpha(argb, alpha);
        if ((color >>> 24) < 4 || text.isEmpty()) {
            return;
        }
        target.text(text, x, y, color, shadow);
    }

    @Override
    public int textWidth(String text) {
        return target.textWidth(text);
    }

    @Override
    public int lineHeight() {
        return target.lineHeight();
    }

    @Override
    public void push() {
        target.push();
    }

    @Override
    public void pop() {
        target.pop();
    }

    @Override
    public void translate(float x, float y) {
        target.translate(x, y);
    }

    @Override
    public void scale(float x, float y) {
        target.scale(x, y);
    }

    @Override
    public void clip(int x, int y, int w, int h) {
        target.clip(x, y + shift, w, h);
    }

    @Override
    public void unclip() {
        target.unclip();
    }

    @Override
    public void sprite(String path, int x, int y, int w, int h) {
        if (alpha > 0.45f && w > 0 && h > 0) {
            target.sprite(path, x, y, w, h);
        }
    }

    @Override
    public boolean exists(String namespace, String path) {
        return target.exists(namespace, path);
    }

    @Override
    public void tiled(String namespace, String path, int x, int y, int w, int h) {
        if (alpha <= 0.01f) {
            return;
        }
        target.tiled(namespace, path, x, y, w, h);
    }
}
