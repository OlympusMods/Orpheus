package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.model.Track;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Announces the song that just started.
 *
 * <p>On 26.1 this prints "Now playing: Song - Artist" above the hotbar. The real pop-up with the
 * disc icon exists for 1.20.1 and 1.21.1 and still has to be ported to the newer toast API.
 */
public final class NowPlayingToast {
    private NowPlayingToast() {
    }

    public static void show(Minecraft minecraft, Track track) {
        Notifier.announce(minecraft, Component.translatable("orpheus.toast.overlay", Texts.nowPlaying(track)));
    }
}
