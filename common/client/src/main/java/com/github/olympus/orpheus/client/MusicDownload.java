package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.OrpheusRuntime;
import com.github.olympus.orpheus.core.download.LeftoverPack;
import com.github.olympus.orpheus.core.download.MusicPackInstaller;
import com.github.olympus.orpheus.core.download.UrlHttpSource;
import com.github.olympus.orpheus.core.model.Track;
import com.github.olympus.orpheus.core.model.TrackKind;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * Downloads the songs this game version does not ship, on a background thread.
 *
 * <p>The files go into the resource pack folder "Orpheus Music". The player still has to enable
 * that pack once; after the next resource reload the songs show up as playable.
 */
public final class MusicDownload {
    public enum Phase {
        IDLE,
        RUNNING,
        DONE,
        FAILED
    }

    public static final String PACK_NAME = "Orpheus Music";
    public static final String LEFTOVER_PACK_NAME = "Orpheus Leftovers";

    private static volatile Phase phase = Phase.IDLE;
    private static volatile int done;
    private static volatile int total;
    private static volatile MusicPackInstaller.Report report;
    private static volatile Component leftoverStatus;

    private MusicDownload() {
    }

    public static Phase phase() {
        return phase;
    }

    /** The downloadable songs and discs whose audio is not in the game right now. */
    public static List<Track> missing(OrpheusRuntime runtime) {
        return runtime.catalog().all().stream()
                .filter(track -> track.isDownloadable() && !runtime.isAvailable(track))
                .toList();
    }

    public static synchronized void start(OrpheusRuntime runtime) {
        List<Track> missing = missing(runtime);
        if (phase == Phase.RUNNING || missing.isEmpty()) {
            return;
        }
        phase = Phase.RUNNING;
        done = 0;
        total = missing.size();
        report = null;
        Path pack = OrpheusClient.gameDirectory().resolve("resourcepacks").resolve(PACK_NAME);
        Thread thread = new Thread(() -> download(missing, pack), "Orpheus music download");
        thread.setDaemon(true);
        thread.start();
    }

    private static void download(List<Track> missing, Path pack) {
        MusicPackInstaller installer = new MusicPackInstaller(
                new UrlHttpSource(),
                pack,
                OrpheusClient.musicPackMetadata(),
                message -> OrpheusClient.LOGGER.warn(message));
        try {
            report = installer.install(missing, (finished, all) -> done = finished);
            phase = Phase.DONE;
        } catch (IOException | RuntimeException exception) {
            OrpheusClient.LOGGER.error("Could not download songs", exception);
            phase = Phase.FAILED;
        }
    }

    /** Creates the folder where the player puts songs that are not part of the game. */
    public static void prepareLeftovers(OrpheusRuntime runtime) {
        List<Track> leftovers = runtime.catalog().all().stream()
                .filter(track -> track.kind() == TrackKind.LEFTOVER)
                .toList();
        Path pack = OrpheusClient.gameDirectory().resolve("resourcepacks").resolve(LEFTOVER_PACK_NAME);
        try {
            LeftoverPack.prepare(pack, OrpheusClient.musicPackMetadata(), leftovers);
            leftoverStatus = Component.translatable("orpheus.leftover.ready", "resourcepacks/" + LEFTOVER_PACK_NAME);
        } catch (IOException exception) {
            OrpheusClient.LOGGER.error("Could not create the leftovers folder", exception);
            leftoverStatus = Component.translatable("orpheus.leftover.failed");
        }
    }

    /** One line about the leftovers folder, or null before it was set up. */
    public static Component leftoverStatus() {
        return leftoverStatus;
    }

    /** Text of the button that starts the download. */
    public static Component buttonText(int missingCount) {
        if (phase == Phase.RUNNING) {
            return Component.translatable("orpheus.download.running", done, total);
        }
        return Component.translatable("orpheus.download.button", missingCount);
    }

    /** One line describing how the last download went, or null when none has run. */
    public static Component status() {
        MusicPackInstaller.Report result = report;
        switch (phase) {
            case RUNNING:
                return Component.translatable("orpheus.download.progress");
            case FAILED:
                return Component.translatable("orpheus.download.failed");
            case DONE:
                if (result == null) {
                    return null;
                }
                if (result.hasProblems()) {
                    return Component.translatable(
                            "orpheus.download.partial", result.downloaded(), result.notFound() + result.failed());
                }
                return Component.translatable(
                        result.downloaded() > 0 ? "orpheus.download.done" : "orpheus.download.already",
                        PACK_NAME);
            default:
                return null;
        }
    }
}
