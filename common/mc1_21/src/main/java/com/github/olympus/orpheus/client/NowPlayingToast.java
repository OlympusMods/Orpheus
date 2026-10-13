package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.model.Track;
import com.github.olympus.orpheus.core.model.TrackKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The "Now Playing" pop-up shown when a song starts: a music disc, "Now Playing" and "Song - Artist".
 *
 * <p>Discs show their own item. Every other song gets one of the classic discs, always the same
 * one for the same song.
 */
public final class NowPlayingToast implements Toast {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("minecraft", "toast/advancement");
    private static final long DISPLAY_MILLIS = 5000L;
    private static final int MIN_WIDTH = 160;
    private static final int MAX_WIDTH = 300;
    private static final int HEIGHT = 32;
    private static final int TEXT_X = 30;
    private static final int TEXT_PADDING = 8;
    private static final String[] CLASSIC_DISCS = {
        "13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal", "strad", "ward", "11", "wait"
    };

    private final Component title = Component.translatable("orpheus.toast.title");
    private final String description;
    private final ItemStack icon;
    private final int width;

    private NowPlayingToast(String description, ItemStack icon, int width) {
        this.description = description;
        this.icon = icon;
        this.width = width;
    }

    public static void show(Minecraft minecraft, Track track) {
        String description = Texts.nowPlaying(track).getString();
        // Grow the toast to fit "Song - Artist" instead of cutting the artist off.
        int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, TEXT_X + minecraft.font.width(description) + TEXT_PADDING));
        minecraft.getToasts().addToast(new NowPlayingToast(description, iconFor(track), width));
    }

    private static ItemStack iconFor(Track track) {
        String name = track.kind() == TrackKind.DISC
                ? track.filePath().substring("records/".length())
                : CLASSIC_DISCS[Math.floorMod(track.id().hashCode(), CLASSIC_DISCS.length)];
        try {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("minecraft", "music_disc_" + name));
            return new ItemStack(item == Items.AIR ? Items.MUSIC_DISC_CAT : item);
        } catch (RuntimeException exception) {
            // Item data may not be available yet (main menu); the toast then has no icon.
            return ItemStack.EMPTY;
        }
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toasts, long visibleMillis) {
        graphics.blitSprite(BACKGROUND, 0, 0, width, HEIGHT);
        Font font = toasts.getMinecraft().font;
        if (!icon.isEmpty()) {
            graphics.renderFakeItem(icon, 8, 8);
        }
        graphics.drawString(font, title, TEXT_X, 7, 0xFFFFFF00, false);
        graphics.drawString(font, font.plainSubstrByWidth(description, width - TEXT_X - TEXT_PADDING), TEXT_X, 18, 0xFFFFFFFF, false);
        return visibleMillis < DISPLAY_MILLIS ? Visibility.SHOW : Visibility.HIDE;
    }
}
