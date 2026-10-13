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
    private static final long CUT_OFF_WINDOW_NANOS = 3_000_000_000L;

    private String playingEvent = "";
    private long startedAt;
    private boolean endReported;
    private boolean cutOff;

    public MinecraftBackend(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public boolean endedAbruptly() {
        return cutOff;
    }

    @Override
    public boolean isPlaying() {
        if (instance == null) {
            return false;
        }
        boolean active = minecraft.getSoundManager().isActive(instance);
        if (!active && !endReported) {
            endReported = true;
            // A song that ends right after the sound system was told to stop everything was cut off.
            cutOff = OrpheusHooks.nanosSinceLastStop() < CUT_OFF_WINDOW_NANOS;
            double seconds = (System.nanoTime() - startedAt) / 1_000_000_000.0;
            OrpheusClient.LOGGER.info(
                    "Orpheus: {} is no longer active after {} s (screen: {}, world loaded: {}). {}",
                    playingEvent,
                    String.format("%.1f", seconds),
                    minecraft.screen == null ? "none" : minecraft.screen.getClass().getSimpleName(),
                    minecraft.level != null,
                    OrpheusHooks.describeLastStop());
        }
        return active;
    }

    @Override
    public void play(Track track) {
        stop();
        SoundEvent event = SoundEvent.createVariableRangeEvent(OrpheusClient.id(track.event()));
        instance = SimpleSoundInstance.forMusic(event);
        paused = false;
        playingEvent = track.event();
        startedAt = System.nanoTime();
        endReported = false;
        cutOff = false;
        minecraft.getSoundManager().play(instance);
        if (OrpheusClient.runtime().active().nowPlayingToast()) {
            try {
                NowPlayingToast.show(minecraft, track);
                OrpheusClient.LOGGER.info("Orpheus: showing the Now Playing toast for {}", track.event());
            } catch (RuntimeException exception) {
                // A broken toast must never silence the music.
                OrpheusClient.LOGGER.warn("Orpheus: could not show the Now Playing toast", exception);
            }
        }
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
