package com.github.olympus.orpheus.core.download;

import com.github.olympus.orpheus.core.model.Track;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Downloads songs the game does not ship into a resource pack folder.
 *
 * <p>Nothing is bundled with the mod: the files come straight from Mojang's asset servers,
 * the same ones the launcher uses. The newest release's asset index lists every song, so
 * songs added in later versions can be played on older ones too.
 */
public final class MusicPackInstaller {
    public static final String VERSION_MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";
    public static final String ASSET_HOST = "https://resources.download.minecraft.net/";

    private static final String SOUND_KEY_PREFIX = "minecraft/sounds/";
    private static final String SOUND_PATH = "assets/minecraft/sounds/";

    /** Called after every song with how many were handled so far and how many there are. */
    public interface ProgressListener {
        void update(int done, int total);
    }

    /**
     * @param downloaded     songs fetched this time
     * @param alreadyPresent songs that were already in the pack folder
     * @param notFound       songs the asset index does not list
     * @param failed         songs that could not be downloaded or did not pass the checksum
     */
    public record Report(int downloaded, int alreadyPresent, int notFound, int failed) {
        public boolean hasProblems() {
            return notFound > 0 || failed > 0;
        }
    }

    private final HttpSource http;
    private final Path packDirectory;
    private final String packMetadata;
    private final Consumer<String> log;

    public MusicPackInstaller(HttpSource http, Path packDirectory, String packMetadata, Consumer<String> log) {
        this.http = http;
        this.packDirectory = packDirectory.toAbsolutePath().normalize();
        this.packMetadata = packMetadata;
        this.log = log;
    }

    public Report install(Collection<Track> tracks, ProgressListener listener) throws IOException {
        JsonObject manifest = fetchJson(VERSION_MANIFEST);
        JsonObject release = fetchAssetObjects(manifest, "release");
        // Songs that only exist in the newest snapshot are looked up there, once, if needed.
        JsonObject snapshot = null;
        boolean snapshotTried = false;
        int total = tracks.size();
        int downloaded = 0;
        int present = 0;
        int notFound = 0;
        int failed = 0;
        int done = 0;
        for (Track track : tracks) {
            String key = SOUND_KEY_PREFIX + track.id() + ".ogg";
            JsonElement entry = release.get(key);
            if (entry == null && !snapshotTried) {
                snapshotTried = true;
                snapshot = fetchSnapshotObjects(manifest);
            }
            if (entry == null && snapshot != null) {
                entry = snapshot.get(key);
            }
            if (entry == null || !entry.isJsonObject()) {
                notFound++;
                log.accept("Song not listed by Mojang: " + track.id());
            } else {
                try {
                    if (installOne(track, entry.getAsJsonObject())) {
                        downloaded++;
                    } else {
                        present++;
                    }
                } catch (IOException | RuntimeException exception) {
                    failed++;
                    log.accept("Could not download " + track.id() + ": " + exception.getMessage());
                }
            }
            listener.update(++done, total);
        }
        if (downloaded + present > 0) {
            Files.createDirectories(packDirectory);
            Files.writeString(packDirectory.resolve("pack.mcmeta"), packMetadata, StandardCharsets.UTF_8);
        }
        return new Report(downloaded, present, notFound, failed);
    }

    /** Returns true when the song was downloaded, false when it was already there. */
    private boolean installOne(Track track, JsonObject entry) throws IOException {
        String hash = entry.get("hash").getAsString().toLowerCase(Locale.ROOT);
        long size = entry.get("size").getAsLong();
        Path target = packDirectory.resolve(SOUND_PATH + track.id() + ".ogg").normalize();
        if (!target.startsWith(packDirectory)) {
            throw new IOException("Refusing to write outside the pack folder: " + track.id());
        }
        if (Files.isRegularFile(target) && Files.size(target) == size) {
            return false;
        }
        byte[] data = http.get(ASSET_HOST + hash.substring(0, 2) + "/" + hash);
        if (!sha1(data).equals(hash)) {
            throw new IOException("Checksum mismatch");
        }
        Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(temporary, data);
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    /** The newest snapshot's songs, or null when there is none or it cannot be read. */
    private JsonObject fetchSnapshotObjects(JsonObject manifest) {
        JsonObject latest = manifest.getAsJsonObject("latest");
        if (!latest.has("snapshot") || latest.get("snapshot").getAsString().equals(latest.get("release").getAsString())) {
            return null;
        }
        try {
            return fetchAssetObjects(manifest, "snapshot");
        } catch (IOException exception) {
            log.accept("Could not read the latest snapshot: " + exception.getMessage());
            return null;
        }
    }

    /** The asset index entries of the newest version of the given channel ("release" or "snapshot"). */
    private JsonObject fetchAssetObjects(JsonObject manifest, String channel) throws IOException {
        String latest = manifest.getAsJsonObject("latest").get(channel).getAsString();
        String versionUrl = null;
        for (JsonElement version : manifest.getAsJsonArray("versions")) {
            JsonObject object = version.getAsJsonObject();
            if (latest.equals(object.get("id").getAsString())) {
                versionUrl = object.get("url").getAsString();
                break;
            }
        }
        if (versionUrl == null) {
            throw new IOException("Version " + latest + " is missing from the version manifest");
        }
        JsonObject version = fetchJson(versionUrl);
        String indexUrl = version.getAsJsonObject("assetIndex").get("url").getAsString();
        return fetchJson(indexUrl).getAsJsonObject("objects");
    }

    private JsonObject fetchJson(String url) throws IOException {
        requireTrusted(url);
        try {
            return JsonParser.parseString(new String(http.get(url), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException exception) {
            throw new IOException("Unexpected response from " + url, exception);
        }
    }

    /** Only talks to Mojang's own hosts over https. */
    static void requireTrusted(String url) throws IOException {
        URI uri = URI.create(url);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean trustedHost = host.equals("mojang.com") || host.endsWith(".mojang.com")
                || host.equals("minecraft.net") || host.endsWith(".minecraft.net");
        if (!"https".equals(uri.getScheme()) || !trustedHost) {
            throw new IOException("Refusing to download from " + url);
        }
    }

    private static String sha1(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
