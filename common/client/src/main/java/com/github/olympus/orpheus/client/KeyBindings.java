package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.OrpheusRuntime;
import com.github.olympus.orpheus.core.model.Track;
import com.github.olympus.orpheus.core.playback.PlaybackController;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class KeyBindings {
    private final KeyMapping menu;
    private final KeyMapping previous;
    private final KeyMapping skip;
    private final KeyMapping pause;
    private final KeyMapping blacklist;

    public KeyBindings(
            KeyMapping menu, KeyMapping previous, KeyMapping skip, KeyMapping pause, KeyMapping blacklist) {
        this.menu = menu;
        this.previous = previous;
        this.skip = skip;
        this.pause = pause;
        this.blacklist = blacklist;
    }

    public List<KeyMapping> all() {
        return List.of(menu, previous, skip, pause, blacklist);
    }

    public void poll(OrpheusRuntime runtime, Minecraft minecraft) {
        PlaybackController controller = runtime.controller();
        while (menu.consumeClick()) {
            OrpheusClient.show(new OrpheusScreen(null));
        }
        while (previous.consumeClick()) {
            controller.previous();
            Notifier.announce(minecraft, describe("Previous", controller.currentTrack()));
        }
        while (skip.consumeClick()) {
            controller.skip();
            Notifier.announce(minecraft, "Skipped");
        }
        while (pause.consumeClick()) {
            Optional<Track> current = controller.currentTrack();
            if (current.isEmpty()) {
                Notifier.announce(minecraft, "Nothing is playing");
                continue;
            }
            controller.togglePause();
            Notifier.announce(minecraft, describe(controller.isPaused() ? "Paused" : "Resumed", current));
        }
        while (blacklist.consumeClick()) {
            Optional<Track> current = controller.currentTrack();
            if (controller.blacklistCurrent()) {
                Notifier.announce(minecraft, describe("Disabled", current));
            } else {
                Notifier.announce(minecraft, "Nothing is playing");
            }
        }
    }

    private static String describe(String prefix, Optional<Track> track) {
        return track.map(value -> prefix + ": " + value.title()).orElse(prefix);
    }
}
