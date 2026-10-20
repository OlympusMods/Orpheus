package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.client.ui.ToastOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class ToastHud {
    private static final GuiCanvas CANVAS = new GuiCanvas();

    private ToastHud() {
    }

    public static void render(GuiGraphics graphics) {
        if (!ToastOverlay.active()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) {
            return;
        }
        try {
            graphics.nextStratum();
            CANVAS.bind(graphics, minecraft.font, graphics.guiWidth(), graphics.guiHeight());
            ToastOverlay.render(CANVAS);
        } catch (RuntimeException exception) {
            OrpheusClient.LOGGER.warn("Orpheus: could not draw the toast", exception);
            ToastOverlay.dismiss();
        }
    }
}
