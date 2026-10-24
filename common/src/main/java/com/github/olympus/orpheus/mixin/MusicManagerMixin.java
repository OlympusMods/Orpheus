package com.github.olympus.orpheus.mixin;

import com.github.olympus.orpheus.client.OrpheusHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private SoundInstance currentMusic;

    @Shadow
    public abstract void stopPlaying();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void orpheus$tick(CallbackInfo callback) {
        if (OrpheusHooks.tick(this.minecraft)) {
            if (this.currentMusic != null) {
                this.stopPlaying();
            }
            callback.cancel();
        }
    }
}
