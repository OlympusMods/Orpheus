package com.github.olympus.orpheus.mixin;

import com.github.olympus.orpheus.client.OrpheusHooks;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineTickMixin {
    /** Lets the stream keep-alive know the game is still ticking the sound engine on time. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void orpheus$noteTick(boolean paused, CallbackInfo callback) {
        OrpheusHooks.noteSoundTick();
    }
}
