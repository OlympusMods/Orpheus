package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.client.ui.OrpheusUi;
import com.github.olympus.proteus.client.ProteusScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class OrpheusMenu {
    private OrpheusMenu() {
    }

    public static Screen create(Screen parent) {
        return new ProteusScreen(Component.translatable("orpheus.screen.title"), parent, OrpheusUi::new);
    }
}
