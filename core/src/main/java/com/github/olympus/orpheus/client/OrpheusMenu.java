package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.client.ui.OrpheusUi;
import com.github.olympus.proteus.client.ProteusScreen;
import com.github.olympus.proteus.client.ui.Hub;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class OrpheusMenu {
    private OrpheusMenu() {
    }

    public static void register() {
        Hub.register(OrpheusClient.MOD_ID, () -> Component.translatable("orpheus.screen.title").getString(),
                "jukebox", OrpheusUi::new);
    }

    public static Screen create(Screen parent) {
        return ProteusScreen.open(parent, OrpheusClient.MOD_ID);
    }
}
