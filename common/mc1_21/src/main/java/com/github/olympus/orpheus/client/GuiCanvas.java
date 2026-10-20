package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.client.ui.Canvas;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class GuiCanvas implements Canvas {
    private static final Map<String, ItemStack> STACKS = new HashMap<>();

    private GuiGraphics graphics;
    private Font font;
    private int width;
    private int height;

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
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("minecraft", name));
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
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath("orpheus", path);
        RenderSystem.enableBlend();
        graphics.blitSprite(location, x, y, w, h);
        RenderSystem.disableBlend();
    }

    @Override
    public boolean exists(String namespace, String path) {
        return Minecraft.getInstance().getResourceManager()
                .getResource(ResourceLocation.fromNamespaceAndPath(namespace, path)).isPresent();
    }

    @Override
    public void tiled(String namespace, String path, int x, int y, int w, int h) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(namespace, path);
        graphics.blit(location, x, y, 0f, 0f, w, h, 32, 32);
    }
}
