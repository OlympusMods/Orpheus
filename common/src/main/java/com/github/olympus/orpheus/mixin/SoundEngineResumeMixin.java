package com.github.olympus.orpheus.mixin;

import com.github.olympus.orpheus.client.OrpheusHooks;
import com.mojang.blaze3d.audio.Channel;
import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineResumeMixin {
    @Shadow
    private boolean loaded;

    @Shadow
    @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Inject(method = "resume", at = @At("HEAD"), cancellable = true)
    private void orpheus$keepHeldMusicPaused(CallbackInfo callback) {
        SoundInstance held = OrpheusHooks.heldMusic();
        if (held == null) {
            return;
        }
        if (this.loaded) {
            for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : this.instanceToChannel.entrySet()) {
                if (entry.getKey() != held) {
                    entry.getValue().execute(Channel::unpause);
                }
            }
        }
        callback.cancel();
    }
}
