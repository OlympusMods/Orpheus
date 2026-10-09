package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.model.Track;
import com.github.olympus.orpheus.core.playback.PlaybackBackend;
import com.github.olympus.orpheus.mixin.SoundEngineAccess;
import com.github.olympus.orpheus.mixin.SoundManagerAccess;
import com.mojang.blaze3d.audio.Channel;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundEvent;

public final class MinecraftBackend implements PlaybackBackend {
    private final Minecraft minecraft;
    private SoundInstance instance;
    private boolean paused;

    public MinecraftBackend(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public boolean isPlaying() {
        return instance != null && minecraft.getSoundManager().isActive(instance);
    }

    @Override
    public void play(Track track) {
        stop();
        SoundEvent event = SoundEvent.createVariableRangeEvent(OrpheusClient.id(track.event()));
        instance = SimpleSoundInstance.forMusic(event);
        paused = false;
        minecraft.getSoundManager().play(instance);
    }

    @Override
    public void stop() {
        if (instance != null) {
            minecraft.getSoundManager().stop(instance);
            instance = null;
        }
        paused = false;
    }

    @Override
    public void pause() {
        paused = true;
        applyToChannel(Channel::pause);
    }

    @Override
    public void resume() {
        paused = false;
        applyToChannel(Channel::unpause);
    }

    @Override
    public void hold() {
        if (paused) {
            applyToChannel(Channel::pause);
        }
    }

    private void applyToChannel(Consumer<Channel> action) {
        if (instance == null) {
            return;
        }
        SoundManager manager = minecraft.getSoundManager();
        SoundEngineAccess engine = (SoundEngineAccess) ((SoundManagerAccess) manager).orpheus$engine();
        ChannelAccess.ChannelHandle handle = engine.orpheus$channels().get(instance);
        if (handle != null) {
            handle.execute(action);
        }
    }
}
