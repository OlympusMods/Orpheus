package com.github.olympus.orpheus.mixin;

import com.github.olympus.orpheus.client.OrpheusClient;
import com.github.olympus.orpheus.client.OrpheusScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds a "Music Options" button to the bottom right corner of the vanilla Options screen. */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"), require = 0)
    private void orpheus$addMusicOptionsButton(CallbackInfo callback) {
        Screen self = this;
        Button button = Button.builder(
                        Component.translatable("orpheus.options.button"),
                        pressed -> OrpheusClient.show(new OrpheusScreen(self)))
                .bounds(this.width - 110, this.height - 30, 100, 20)
                .tooltip(Tooltip.create(Component.translatable("orpheus.options.button.tooltip")))
                .build();
        this.addRenderableWidget(button);
    }
}
