package com.github.olympus.orpheus.core.download;

import com.github.olympus.orpheus.core.model.Track;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Sets up the resource pack folder where the player puts songs that are not part of the game.
 *
 * <p>The mod cannot supply these songs: they are not on Mojang's servers. It only creates the
 * folder and a README that says which file name each song expects.
 */
public final class LeftoverPack {
    private static final String SONGS_PATH = "assets/orpheus/sounds/leftover";

    private LeftoverPack() {
    }

    /** Creates the folder, pack.mcmeta and README. Returns the folder the song files go into. */
    public static Path prepare(Path packDirectory, String packMetadata, Collection<Track> tracks) throws IOException {
        Path songs = packDirectory.resolve(SONGS_PATH);
        Files.createDirectories(songs);
        Path metadata = packDirectory.resolve("pack.mcmeta");
        if (!Files.exists(metadata)) {
            Files.writeString(metadata, packMetadata, StandardCharsets.UTF_8);
        }
        Files.writeString(packDirectory.resolve("README.txt"), readme(tracks), StandardCharsets.UTF_8);
        return songs;
    }

    static String readme(Collection<Track> tracks) {
        StringBuilder text = new StringBuilder();
        text.append("Orpheus leftover songs\n");
        text.append("======================\n\n");
        text.append("These songs were never added to Minecraft, so Orpheus cannot download them.\n");
        text.append("If you own them (for example from the album), convert them to .ogg (Vorbis)\n");
        text.append("and put the files into:\n\n    ").append(SONGS_PATH).append("/\n\n");
        text.append("Use exactly these file names:\n\n");
        for (Track track : tracks) {
            String path = track.filePath();
            text.append("    ").append(path.substring(path.lastIndexOf('/') + 1)).append(".ogg   ")
                    .append(track.title()).append('\n');
        }
        text.append("\nThen enable this resource pack in Options > Resource Packs.\n");
        return text.toString();
    }
}
