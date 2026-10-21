package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.model.Track;
import com.github.olympus.orpheus.core.model.TrackKind;
import com.github.olympus.proteus.client.ui.ToastOverlay;
import net.minecraft.network.chat.Component;

public final class NowPlaying {
    private static final String[] CLASSIC_DISCS = {
        "13", "cat", "blocks", "chirp", "far", "mall", "mellohi", "stal", "strad", "ward", "11", "wait"
    };

    private NowPlaying() {
    }

    public static void show(Track track) {
        ToastOverlay.show(Component.translatable("orpheus.toast.title").getString(),
                Texts.nowPlaying(track).getString(), iconFor(track));
    }

    public static void preview() {
        ToastOverlay.show(Component.translatable("orpheus.toast.title").getString(),
                Component.translatable("orpheus.ui.toast.sample").getString(), "music_disc_cat");
    }

    private static String iconFor(Track track) {
        String name = track.kind() == TrackKind.DISC
                ? track.filePath().substring("records/".length())
                : CLASSIC_DISCS[Math.floorMod(track.id().hashCode(), CLASSIC_DISCS.length)];
        return "music_disc_" + name;
    }
}
