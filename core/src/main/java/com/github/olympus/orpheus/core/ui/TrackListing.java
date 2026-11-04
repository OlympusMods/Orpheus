package com.github.olympus.orpheus.core.ui;

import com.github.olympus.orpheus.core.model.Track;
import com.github.olympus.orpheus.core.model.TrackCatalog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TrackListing {
    private TrackListing() {
    }

    public static List<Track> sorted(TrackCatalog catalog) {
        List<Track> tracks = new ArrayList<>(catalog.all());
        tracks.sort(Comparator.comparing(Track::title, String.CASE_INSENSITIVE_ORDER).thenComparing(Track::id));
        return tracks;
    }
}
