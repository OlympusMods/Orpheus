package com.github.olympus.orpheus.mixin;

import com.github.olympus.orpheus.client.OrpheusClient;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Temporary diagnostics: logs who stops a music instance, or whether its channel stopped by itself.
 * Remove together with its entry in orpheus.mixins.json once the join-world cut-off is understood.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineDebugMixin {
    @Shadow
    @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Unique
    private final Set<SoundInstance> orpheus$reported = new HashSet<>();

    @Inject(method = "stop(Lnet/minecraft/client/resources/sounds/SoundInstance;)V", at = @At("HEAD"))
    private void orpheus$dbgStopInstance(SoundInstance instance, CallbackInfo callback) {
        if (instance.getSource() == SoundSource.MUSIC) {
            orpheus$log("stop(instance) " + instance.getLocation(), true);
        }
    }

    @Inject(method = "stopAll", at = @At("HEAD"))
    private void orpheus$dbgStopAll(CallbackInfo callback) {
        orpheus$log("stopAll", true);
    }

    @Inject(
            method = "stop(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/sounds/SoundSource;)V",
            at = @At("HEAD"))
    private void orpheus$dbgStopSource(ResourceLocation location, SoundSource source, CallbackInfo callback) {
        if (source == null || source == SoundSource.MUSIC) {
            orpheus$log("stop(" + location + ", " + source + ")", true);
        }
    }

    @Inject(method = "destroy", at = @At("HEAD"))
    private void orpheus$dbgDestroy(CallbackInfo callback) {
        orpheus$log("destroy/reload", true);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void orpheus$dbgTick(boolean paused, CallbackInfo callback) {
        for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : instanceToChannel.entrySet()) {
            SoundInstance instance = entry.getKey();
            if (instance.getSource() == SoundSource.MUSIC
                    && entry.getValue().isStopped()
                    && orpheus$reported.add(instance)) {
                orpheus$log("music channel stopped by itself: " + instance.getLocation(), false);
            }
        }
        if (orpheus$reported.size() > 32) {
            orpheus$reported.retainAll(instanceToChannel.keySet());
        }
    }

    @Unique
    private static void orpheus$log(String what, boolean withStack) {
        String thread = Thread.currentThread().getName();
        if (withStack) {
            OrpheusClient.LOGGER.info("Orpheus[dbg] {} (thread {})", what, thread, new Throwable());
        } else {
            OrpheusClient.LOGGER.info("Orpheus[dbg] {} (thread {})", what, thread);
        }
    }
}
