package com.github.olympus.orpheus.mixin;

import com.github.olympus.orpheus.client.ToastHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public abstract class ToastOverlayMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void orpheus$renderToast(GuiGraphics graphics, CallbackInfo callback) {
        ToastHud.render(graphics);
    }
}
