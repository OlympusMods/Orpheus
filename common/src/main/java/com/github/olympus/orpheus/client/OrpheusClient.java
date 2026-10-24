package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.OrpheusRuntime;
import com.github.olympus.orpheus.core.model.TrackCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OrpheusClient {
    public static final String MOD_ID = "orpheus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final String CATALOG = "/assets/orpheus/tracks.json";

    private static Path configDirectory = Path.of("config");
    private static OrpheusRuntime runtime;
    private static KeyBindings keys;

    private OrpheusClient() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void configure(Path directory) {
        configDirectory = directory;
    }

    public static Path configDirectory() {
        return configDirectory;
    }

    public static synchronized OrpheusRuntime runtime() {
        if (runtime == null) {
            runtime = createRuntime(Minecraft.getInstance());
            OrpheusRuntime.install(runtime);
        }
        return runtime;
    }

    public static synchronized KeyBindings keys() {
        if (keys == null) {
            keys = KeyFactory.create(KeyFactory.registerCategory());
        }
        return keys;
    }

    public static synchronized void installKeys(KeyBindings bindings) {
        keys = bindings;
    }

    public static Path gameDirectory() {
        return Minecraft.getInstance().gameDirectory.toPath();
    }

    /** The pack.mcmeta written next to downloaded songs; the pack format depends on the game version. */
    public static String musicPackMetadata() {
        return "{\"pack\":{\"description\":\"Songs downloaded by Orpheus\",\"min_format\":84,\"max_format\":97}}";
    }

    public static void show(Screen screen) {
        Minecraft.getInstance().setScreen(screen);
    }

    private static OrpheusRuntime createRuntime(Minecraft minecraft) {
        TrackCatalog catalog;
        try (InputStream stream = OrpheusClient.class.getResourceAsStream(CATALOG)) {
            if (stream == null) {
                throw new IllegalStateException("Missing " + CATALOG);
            }
            catalog = TrackCatalog.read(stream);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + CATALOG, exception);
        }
        TrackAvailability availability = new TrackAvailability(minecraft);
        long playable = catalog.all().stream().filter(availability).count();
        LOGGER.info("Orpheus knows {} songs, {} of them can be played right now", catalog.size(), playable);
        return new OrpheusRuntime(
                configDirectory.resolve(MOD_ID),
                catalog,
                availability,
                new MinecraftBackend(minecraft),
                message -> LOGGER.error(message));
    }
}
