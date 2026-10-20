package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.client.ui.Canvas;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

final class GuiCanvas implements Canvas {
    private static final Map<String, ItemStack> STACKS = new HashMap<>();

    private static final Object PIPELINE = pipeline();
    private static final Method BLIT_SPRITE = method("blitSprite", 6);
    private static final Method BLIT = method("blit", 10);

    private GuiGraphicsExtractor graphics;
    private Font font;
    private int width;
    private int height;

    void bind(GuiGraphicsExtractor graphics, Font font, int width, int height) {
        this.graphics = graphics;
        this.font = font;
        this.width = width;
        this.height = height;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public void fill(int x, int y, int w, int h, int argb) {
        graphics.fill(x, y, x + w, y + h, argb);
    }

    @Override
    public void gradient(int x, int y, int w, int h, int topArgb, int bottomArgb) {
        graphics.fillGradient(x, y, x + w, y + h, topArgb, bottomArgb);
    }

    @Override
    public void text(String text, int x, int y, int argb, boolean shadow) {
        graphics.text(font, text, x, y, argb, shadow);
    }

    @Override
    public int textWidth(String text) {
        return font.width(text);
    }

    @Override
    public int lineHeight() {
        return font.lineHeight;
    }

    @Override
    public void push() {
        graphics.pose().pushMatrix();
    }

    @Override
    public void pop() {
        graphics.pose().popMatrix();
    }

    @Override
    public void translate(float x, float y) {
        graphics.pose().translate(x, y);
    }

    @Override
    public void scale(float x, float y) {
        graphics.pose().scale(x, y);
    }

    @Override
    public void clip(int x, int y, int w, int h) {
        graphics.enableScissor(x, y, x + w, y + h);
    }

    @Override
    public void unclip() {
        graphics.disableScissor();
    }

    @Override
    public void sprite(String path, int x, int y, int w, int h) {
        Identifier location = Identifier.fromNamespaceAndPath("orpheus", path);
        try {
            BLIT_SPRITE.invoke(graphics, PIPELINE, location, x, y, w, h);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not draw the sprite " + path, exception);
        }
    }

    @Override
    public boolean exists(String namespace, String path) {
        return Minecraft.getInstance().getResourceManager()
                .getResource(Identifier.fromNamespaceAndPath(namespace, path)).isPresent();
    }

    @Override
    public void tiled(String namespace, String path, int x, int y, int w, int h) {
        Identifier location = Identifier.fromNamespaceAndPath(namespace, path);
        try {
            BLIT.invoke(graphics, PIPELINE, location, x, y, 0f, 0f, w, h, 32, 32);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not draw the texture " + path, exception);
        }
    }

    private static Object pipeline() {
        try {
            return Class.forName("net.minecraft.client.renderer.RenderPipelines").getField("GUI_TEXTURED").get(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not find the GUI textured pipeline", exception);
        }
    }

    private static Method method(String name, int parameters) {
        for (Method candidate : GuiGraphicsExtractor.class.getMethods()) {
            Class<?>[] types = candidate.getParameterTypes();
            if (candidate.getName().equals(name) && types.length == parameters
                    && types[0].getSimpleName().equals("RenderPipeline") && types[1] == Identifier.class) {
                return candidate;
            }
        }
        throw new IllegalStateException("GuiGraphicsExtractor." + name + " was not found");
    }
}
