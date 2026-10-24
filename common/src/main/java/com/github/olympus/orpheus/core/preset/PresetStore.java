package com.github.olympus.orpheus.core.preset;

import com.github.olympus.orpheus.core.model.TrackCatalog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class PresetStore {
    public static final String DEFAULT_NAME = "Default";
    public static final int MAX_NAME_LENGTH = 24;

    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9 _-]+");
    private static final String PRESET_DIRECTORY = "presets";
    private static final String STATE_FILE = "state.json";
    private static final String EXTENSION = ".json";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path root;
    private final TrackCatalog catalog;

    public PresetStore(Path root, TrackCatalog catalog) {
        this.root = root;
        this.catalog = catalog;
    }

    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String name = raw.trim().replaceAll("\\s+", " ");
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH || !VALID_NAME.matcher(name).matches()) {
            return Optional.empty();
        }
        return Optional.of(name);
    }

    public static String slug(String name) {
        return name.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public List<String> names() {
        List<String> names = new ArrayList<>();
        Path directory = root.resolve(PRESET_DIRECTORY);
        if (!Files.isDirectory(directory)) {
            return names;
        }
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(path -> path.getFileName().toString().endsWith(EXTENSION))
                    .sorted()
                    .forEach(path -> names.add(readName(path)));
        } catch (IOException ignored) {
            return names;
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public boolean exists(String name) {
        return Files.isRegularFile(fileFor(name));
    }

    public Preset load(String name) throws IOException {
        Path path = fileFor(name);
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            return PresetCodec.decode(object, name, catalog);
        }
    }

    public void save(Preset preset) throws IOException {
        write(fileFor(preset.name()), PresetCodec.encode(preset));
    }

    public boolean delete(String name) throws IOException {
        return Files.deleteIfExists(fileFor(name));
    }

    public Optional<String> activeName() {
        Path path = root.resolve(STATE_FILE);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            if (object.has("active")) {
                return Optional.of(object.get("active").getAsString());
            }
        } catch (IOException | RuntimeException ignored) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public void setActiveName(String name) throws IOException {
        JsonObject object = new JsonObject();
        object.addProperty("active", name);
        write(root.resolve(STATE_FILE), object);
    }

    public Preset loadActive() throws IOException {
        Optional<String> active = activeName();
        if (active.isPresent() && exists(active.get())) {
            try {
                return load(active.get());
            } catch (IOException | RuntimeException ignored) {
                return createDefault();
            }
        }
        return createDefault();
    }

    private Preset createDefault() throws IOException {
        Preset preset = Preset.defaults(DEFAULT_NAME, catalog);
        save(preset);
        setActiveName(preset.name());
        return preset;
    }

    private Path fileFor(String name) {
        return root.resolve(PRESET_DIRECTORY).resolve(slug(name) + EXTENSION);
    }

    private String readName(Path path) {
        String fallback = path.getFileName().toString();
        fallback = fallback.substring(0, fallback.length() - EXTENSION.length());
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            if (object.has("name")) {
                return object.get("name").getAsString();
            }
        } catch (IOException | RuntimeException ignored) {
            return fallback;
        }
        return fallback;
    }

    private void write(Path target, JsonObject object) throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            gson.toJson(object, writer);
        }
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
