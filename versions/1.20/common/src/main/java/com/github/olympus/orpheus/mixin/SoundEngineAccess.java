package com.github.olympus.orpheus.mixin;

import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundEngine.class)
public interface SoundEngineAccess {
    @Accessor("instanceToChannel")
    Map<SoundInstance, ChannelAccess.ChannelHandle> orpheus$channels();

    @Accessor("channelAccess")
    ChannelAccess orpheus$channelAccess();
}
