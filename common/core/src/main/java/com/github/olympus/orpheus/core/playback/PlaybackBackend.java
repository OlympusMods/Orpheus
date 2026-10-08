package com.github.olympus.orpheus.core.playback;

import com.github.olympus.orpheus.core.model.Track;

public interface PlaybackBackend {
    boolean isPlaying();

    void play(Track track);

    void stop();

    void pause();

    void resume();
}
