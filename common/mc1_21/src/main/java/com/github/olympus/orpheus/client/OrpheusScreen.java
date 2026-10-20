package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.client.ui.OrpheusUi;
import com.github.olympus.orpheus.client.ui.UiHost;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public final class OrpheusScreen extends Screen implements UiHost {
    private final Screen parent;
    private final OrpheusUi ui = new OrpheusUi(this);
    private final GuiCanvas canvas = new GuiCanvas();

    public OrpheusScreen(Screen parent) {
        super(Component.translatable("orpheus.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ui.resize(width, height);
    }

    @Override
    public void tick() {
        ui.tick();
    }

    @Override
    public void onClose() {
        ui.requestClose();
    }

    @Override
    public void removed() {
        OrpheusClient.runtime().saveActive();
    }

    @Override
    public void close() {
        OrpheusClient.runtime().saveActive();
        OrpheusClient.show(parent);
    }

    @Override
    public void playClick() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean inWorld() {
        return minecraft != null && minecraft.level != null;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (ui.vanillaBackdrop(inWorld())) {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        canvas.bind(graphics, font, width, height);
        ui.render(canvas, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ui.mousePressed(button);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        ui.mouseReleased(button);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        ui.mouseScrolled(scrollY);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        return ui.keyPressed(key, modifiers) || super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return ui.charTyped(character);
    }
}
