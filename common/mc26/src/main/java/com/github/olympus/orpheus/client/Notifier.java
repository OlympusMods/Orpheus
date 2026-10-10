package com.github.olympus.orpheus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class Notifier {
    private Notifier() {
    }

    public static void announce(Minecraft minecraft, String text) {
        if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.literal(text));
        }
    }
}
