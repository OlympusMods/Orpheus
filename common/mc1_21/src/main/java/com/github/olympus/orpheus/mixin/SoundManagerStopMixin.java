package com.github.olympus.orpheus.mixin;

import com.github.olympus.orpheus.client.OrpheusHooks;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public abstract class SoundManagerStopMixin {
    /**
     * Leaving a world stops every sound. Orpheus music should keep playing through that,
     * so while Orpheus manages the music everything except the music category is stopped.
     */
    @Inject(method = "stop()V", at = @At("HEAD"), cancellable = true)
    private void orpheus$stopEverythingButMusic(CallbackInfo callback) {
        if (!OrpheusHooks.isManaging()) {
            return;
        }
        SoundManager manager = (SoundManager) (Object) this;
        for (SoundSource source : SoundSource.values()) {
            if (source != SoundSource.MUSIC) {
                manager.stop(null, source);
            }
        }
        callback.cancel();
    }
}
