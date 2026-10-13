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
    private static NowPlayingToast active;

    private String description;
    private ItemStack icon;
    private int width;
    private long shownAt = System.currentTimeMillis();
    private boolean finished;

    private NowPlayingToast(String description, ItemStack icon, int width) {
        this.description = description;
        this.icon = icon;
        this.width = width;
    }

    public static void show(Minecraft minecraft, Track track) {
        String description = Texts.nowPlaying(track).getString();
        // Grow the toast to fit "Song - Artist" instead of cutting the artist off.
        int width = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, TEXT_X + minecraft.font.width(description) + TEXT_PADDING));
        ItemStack icon = iconFor(track);
        NowPlayingToast existing = active;
        if (existing != null && existing.isLive()) {
            existing.replaceWith(description, icon, width);
            return;
        }
        NowPlayingToast toast = new NowPlayingToast(description, icon, width);
        active = toast;
        minecraft.getToasts().addToast(toast);
    }

    /** A toast that is still on screen (or about to be) is reused, so skipping quickly never stacks pop-ups. */
    private boolean isLive() {
        return !finished && System.currentTimeMillis() - shownAt < DISPLAY_MILLIS + 2000L;
    }

    private void replaceWith(String description, ItemStack icon, int width) {
        this.description = description;
        this.icon = icon;
        this.width = width;
        this.shownAt = System.currentTimeMillis();
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
        boolean show = System.currentTimeMillis() - shownAt < DISPLAY_MILLIS;
        finished = !show;
        return show ? Visibility.SHOW : Visibility.HIDE;
    }
}
