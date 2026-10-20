package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.client.ui.Canvas;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class GuiCanvas implements Canvas {
    private static final String SPRITES = "textures/gui/sprites/";
    private static final Map<String, Sprite> SPRITE_CACHE = new HashMap<>();
    private static final Map<String, ItemStack> STACKS = new HashMap<>();

    private GuiGraphics graphics;
    private Font font;
    private int width;
    private int height;

    private record Sprite(ResourceLocation texture, int width, int height, int left, int top, int right, int bottom) {
    }

    void bind(GuiGraphics graphics, Font font, int width, int height) {
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
        graphics.drawString(font, text, x, y, argb, shadow);
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
        graphics.pose().pushPose();
    }

    @Override
    public void pop() {
        graphics.pose().popPose();
    }

    @Override
    public void translate(float x, float y) {
        graphics.pose().translate(x, y, 0f);
    }

    @Override
    public void scale(float x, float y) {
        graphics.pose().scale(x, y, 1f);
    }

    @Override
    public void clip(int x, int y, int w, int h) {
        graphics.enableScissor(x, y, x + w, y + h);
    }

    @Override
    public void unclip() {
        graphics.disableScissor();
    }

    private static ItemStack stack(String name) {
        ItemStack stack = STACKS.get(name);
        if (stack == null) {
            try {
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation("minecraft", name));
                stack = new ItemStack(item == Items.AIR ? Items.MUSIC_DISC_CAT : item);
            } catch (RuntimeException exception) {
                return null;
            }
            STACKS.put(name, stack);
        }
        return stack;
    }

    @Override
    public boolean hasItem(String name) {
        return stack(name) != null;
    }

    @Override
    public void item(String name, int x, int y) {
        ItemStack stack = stack(name);
        if (stack != null) {
            graphics.renderFakeItem(stack, x, y);
        }
    }

    @Override
    public void sprite(String path, int x, int y, int w, int h) {
        Sprite sprite = SPRITE_CACHE.computeIfAbsent(path, GuiCanvas::load);
        if (sprite == null) {
            return;
        }
        RenderSystem.enableBlend();
        ResourceLocation texture = sprite.texture();
        int tw = sprite.width();
        int th = sprite.height();
        int l = Math.min(sprite.left(), w / 2);
        int t = Math.min(sprite.top(), h / 2);
        int r = Math.min(sprite.right(), w / 2);
        int b = Math.min(sprite.bottom(), h / 2);
        if (l + t + r + b == 0) {
            graphics.blit(texture, x, y, w, h, 0f, 0f, tw, th, tw, th);
        } else {
            int mw = w - l - r;
            int mh = h - t - b;
            int sw = tw - sprite.left() - sprite.right();
            int sh = th - sprite.top() - sprite.bottom();
            graphics.blit(texture, x, y, l, t, 0f, 0f, l, t, tw, th);
            graphics.blit(texture, x + w - r, y, r, t, tw - r, 0f, r, t, tw, th);
            graphics.blit(texture, x, y + h - b, l, b, 0f, th - b, l, b, tw, th);
            graphics.blit(texture, x + w - r, y + h - b, r, b, tw - r, th - b, r, b, tw, th);
            graphics.blit(texture, x + l, y, mw, t, sprite.left(), 0f, sw, t, tw, th);
            graphics.blit(texture, x + l, y + h - b, mw, b, sprite.left(), th - b, sw, b, tw, th);
            graphics.blit(texture, x, y + t, l, mh, 0f, sprite.top(), l, sh, tw, th);
            graphics.blit(texture, x + w - r, y + t, r, mh, tw - r, sprite.top(), r, sh, tw, th);
            graphics.blit(texture, x + l, y + t, mw, mh, sprite.left(), sprite.top(), sw, sh, tw, th);
        }
        RenderSystem.disableBlend();
    }

    private static Sprite load(String path) {
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        ResourceLocation texture = new ResourceLocation("orpheus", SPRITES + path + ".png");
        try {
            Resource image = resources.getResource(texture).orElse(null);
            if (image == null) {
                return null;
            }
            int w;
            int h;
            try (InputStream stream = image.open(); NativeImage decoded = NativeImage.read(stream)) {
                w = decoded.getWidth();
                h = decoded.getHeight();
            }
            int[] border = new int[4];
            Resource meta = resources.getResource(new ResourceLocation("orpheus", SPRITES + path + ".png.mcmeta"))
                    .orElse(null);
            if (meta != null) {
                try (Reader reader = meta.openAsReader()) {
                    JsonObject scaling = JsonParser.parseReader(reader).getAsJsonObject()
                            .getAsJsonObject("gui").getAsJsonObject("scaling");
                    if ("nine_slice".equals(scaling.get("type").getAsString())) {
                        JsonElement value = scaling.get("border");
                        if (value.isJsonObject()) {
                            JsonObject sides = value.getAsJsonObject();
                            border = new int[] {sides.get("left").getAsInt(), sides.get("top").getAsInt(),
                                sides.get("right").getAsInt(), sides.get("bottom").getAsInt()};
                        } else {
                            int all = value.getAsInt();
                            border = new int[] {all, all, all, all};
                        }
                    }
                } catch (RuntimeException exception) {
                    border = new int[4];
                }
            }
            return new Sprite(texture, w, h, border[0], border[1], border[2], border[3]);
        } catch (IOException exception) {
            return null;
        }
    }

    @Override
    public boolean exists(String namespace, String path) {
        return Minecraft.getInstance().getResourceManager()
                .getResource(new ResourceLocation(namespace, path)).isPresent();
    }

    @Override
    public void tiled(String namespace, String path, int x, int y, int w, int h) {
        graphics.blit(new ResourceLocation(namespace, path), x, y, 0f, 0f, w, h, 32, 32);
    }
}
