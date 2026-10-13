package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.model.Track;
import com.github.olympus.orpheus.core.model.TrackKind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The "Now Playing" pop-up shown when a song starts: a music disc, "Now Playing" and "Song - Artist".
 *
 * <p>Discs show their own item. Every other song gets one of the classic discs, always the same
 * one for the same song.
 */
public final class NowPlayingToast implements Toast {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("minecraft", "toast/advancement");
    private static final long DISPLAY_MILLIS = 5000L;
    private static final int WIDTH = 160;
    private static final int HEIGHT = 32;
    private static final int TEXT_X = 30;
    private static final int TEXT_WIDTH = WIDTH - TEXT_X - 6;
    private static final String[] CLASSIC_DISCS = {
        "13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal", "strad", "ward", "11", "wait"
    };

    private final Component title = Component.translatable("orpheus.toast.title");
    private final String description;
    private final ItemStack icon;
    private Visibility wantedVisibility = Visibility.SHOW;

    private NowPlayingToast(String description, ItemStack icon) {
        this.description = description;
        this.icon = icon;
    }

    public static void show(Minecraft minecraft, Track track) {
        minecraft.getToastManager().addToast(new NowPlayingToast(Texts.nowPlaying(track).getString(), iconFor(track)));
    }

    private static ItemStack iconFor(Track track) {
        String name = track.kind() == TrackKind.DISC
                ? track.filePath().substring("records/".length())
                : CLASSIC_DISCS[Math.floorMod(track.id().hashCode(), CLASSIC_DISCS.length)];
        return new ItemStack(BuiltInRegistries.ITEM
                .getOptional(Identifier.fromNamespaceAndPath("minecraft", "music_disc_" + name))
                .orElse(Items.MUSIC_DISC_CAT));
    }

    @Override
    public Visibility getWantedVisibility() {
        return wantedVisibility;
    }

    @Override
    public void update(ToastManager manager, long visibleMillis) {
        boolean show = visibleMillis < DISPLAY_MILLIS * manager.getNotificationDisplayTimeMultiplier();
        wantedVisibility = show ? Visibility.SHOW : Visibility.HIDE;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, Font font, long visibleMillis) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, 0, 0, WIDTH, HEIGHT);
        graphics.fakeItem(icon, 8, 8);
        graphics.text(font, title, TEXT_X, 7, 0xFFFFFF00, false);
        graphics.text(font, font.plainSubstrByWidth(description, TEXT_WIDTH), TEXT_X, 18, 0xFFFFFFFF, false);
    }
}
