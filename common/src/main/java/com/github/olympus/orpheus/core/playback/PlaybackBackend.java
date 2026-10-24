package com.github.olympus.orpheus.core.playback;

import com.github.olympus.orpheus.core.model.Track;

public interface PlaybackBackend {
    boolean isPlaying();

    void play(Track track);

    void stop();

    /** Starts the same song again after it was cut off, without announcing it a second time. */
    default void replay(Track track) {
        play(track);
    }

    void pause();

    void resume();

    void hold();

    /**
     * True when the song that just stopped did not end by itself but was cut off by the game,
     * for example while joining a world.
     */
    default boolean endedAbruptly() {
        return false;
    }
}
