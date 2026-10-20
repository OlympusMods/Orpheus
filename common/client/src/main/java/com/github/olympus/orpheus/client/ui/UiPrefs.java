package com.github.olympus.orpheus.client.ui;

import com.github.olympus.orpheus.client.OrpheusClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UiPrefs {
    public static final int[] ACCENTS = {
        0xFF8B5CF6, 0xFF3B82F6, 0xFF14B8A6, 0xFF22C55E, 0xFFF59E0B, 0xFFF43F5E
    };
    public static final String[] ACCENT_KEYS = {"violet", "blue", "teal", "green", "amber", "rose"};

    private static final String FILE = "ui.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static UiPrefs instance;

    public static final int CUSTOM = ACCENTS.length;

    private boolean classic;
    private int accent;
    private float hue = 0.72f;
    private float saturation = 0.75f;
    private float lightness = 0.5f;
    private boolean blur = true;
    private boolean animations = true;

    private UiPrefs() {
    }

    public static synchronized UiPrefs get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public boolean classic() {
        return classic;
    }

    public void setClassic(boolean value) {
        classic = value;
        save();
    }

    public int accentIndex() {
        return accent;
    }

    public int accentColor() {
        return accent == CUSTOM ? customColor() : ACCENTS[accent];
    }

    public int customColor() {
        return Colors.hsl(hue, 0.3f + 0.7f * saturation, 0.3f + 0.4f * lightness);
    }

    public float hue() {
        return hue;
    }

    public float saturation() {
        return saturation;
    }

    public float lightness() {
        return lightness;
    }

    public void setCustom(float hue, float saturation, float lightness) {
        this.hue = hue;
        this.saturation = saturation;
        this.lightness = lightness;
    }

    public void setAccent(int index) {
        accent = Math.max(0, Math.min(CUSTOM, index));
        save();
    }

    public boolean blur() {
        return blur;
    }

    public void setBlur(boolean value) {
        blur = value;
        save();
    }

    public boolean animations() {
        return animations;
    }

    public void setAnimations(boolean value) {
        animations = value;
        save();
    }

    private static Path file() {
        return OrpheusClient.configDirectory().resolve(OrpheusClient.MOD_ID).resolve(FILE);
    }

    private static UiPrefs load() {
        UiPrefs prefs = new UiPrefs();
        Path path = file();
        if (!Files.isRegularFile(path)) {
            return prefs;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            prefs.classic = object.has("style") && "classic".equals(object.get("style").getAsString());
            if (object.has("accent")) {
                prefs.accent = Math.max(0, Math.min(CUSTOM, object.get("accent").getAsInt()));
            }
            if (object.has("hue")) {
                prefs.hue = object.get("hue").getAsFloat();
                prefs.saturation = object.get("saturation").getAsFloat();
                prefs.lightness = object.get("lightness").getAsFloat();
            }
            if (object.has("blur")) {
                prefs.blur = object.get("blur").getAsBoolean();
            }
            if (object.has("animations")) {
                prefs.animations = object.get("animations").getAsBoolean();
            }
        } catch (IOException | RuntimeException exception) {
            OrpheusClient.LOGGER.warn("Could not read {}: {}", path, exception.getMessage());
        }
        return prefs;
    }

    public void save() {
        Path path = file();
        JsonObject object = new JsonObject();
        object.addProperty("style", classic ? "classic" : "modern");
        object.addProperty("accent", accent);
        object.addProperty("hue", hue);
        object.addProperty("saturation", saturation);
        object.addProperty("lightness", lightness);
        object.addProperty("blur", blur);
        object.addProperty("animations", animations);
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(object, writer);
            }
        } catch (IOException exception) {
            OrpheusClient.LOGGER.warn("Could not save {}: {}", path, exception.getMessage());
        }
    }
}
