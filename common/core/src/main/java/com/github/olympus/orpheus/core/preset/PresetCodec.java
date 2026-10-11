package com.github.olympus.orpheus.core.preset;

import com.github.olympus.orpheus.core.model.MusicContext;
import com.github.olympus.orpheus.core.model.TrackCatalog;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;

public final class PresetCodec {
    private PresetCodec() {
    }

    public static JsonObject encode(Preset preset) {
        JsonObject root = new JsonObject();
        root.addProperty("name", preset.name());
        root.addProperty("enabled", preset.enabled());
        JsonArray blacklist = new JsonArray();
        for (String id : preset.blacklisted()) {
            blacklist.add(id);
        }
        root.add("blacklist", blacklist);
        JsonObject contexts = new JsonObject();
        for (MusicContext context : MusicContext.values()) {
            ContextSettings settings = preset.settings(context);
            JsonObject object = new JsonObject();
            object.addProperty("managed", settings.managed());
            object.addProperty("minDelaySeconds", settings.minDelaySeconds());
            object.addProperty("maxDelaySeconds", settings.maxDelaySeconds());
            object.addProperty("repeatAvoidance", settings.repeatAvoidance());
            JsonObject tracks = new JsonObject();
            for (Map.Entry<String, Integer> entry : settings.weights().entrySet()) {
                tracks.addProperty(entry.getKey(), entry.getValue());
            }
            object.add("tracks", tracks);
            contexts.add(context.name(), object);
        }
        root.add("contexts", contexts);
        return root;
    }

    public static Preset decode(JsonObject root, String fallbackName, TrackCatalog catalog) {
        String name = fallbackName;
        if (root.has("name") && root.get("name").isJsonPrimitive()) {
            name = root.get("name").getAsString();
        }
        Preset preset = Preset.defaults(name, catalog);
        if (root.has("enabled") && root.get("enabled").isJsonPrimitive()) {
            preset.setEnabled(root.get("enabled").getAsBoolean());
        }
        if (root.has("blacklist") && root.get("blacklist").isJsonArray()) {
            for (JsonElement element : root.getAsJsonArray("blacklist")) {
                preset.blacklist(catalog.canonicalId(element.getAsString()));
            }
        }
        if (root.has("contexts") && root.get("contexts").isJsonObject()) {
            JsonObject contexts = root.getAsJsonObject("contexts");
            for (MusicContext context : MusicContext.values()) {
                if (contexts.has(context.name()) && contexts.get(context.name()).isJsonObject()) {
                    decodeContext(contexts.getAsJsonObject(context.name()), preset.settings(context), catalog);
                }
            }
        }
        return preset;
    }

    private static void decodeContext(JsonObject object, ContextSettings settings, TrackCatalog catalog) {
        if (object.has("managed")) {
            settings.setManaged(object.get("managed").getAsBoolean());
        }
        if (object.has("maxDelaySeconds")) {
            settings.setMaxDelaySeconds(object.get("maxDelaySeconds").getAsInt());
        }
        if (object.has("minDelaySeconds")) {
            settings.setMinDelaySeconds(object.get("minDelaySeconds").getAsInt());
        }
        if (object.has("repeatAvoidance")) {
            settings.setRepeatAvoidance(object.get("repeatAvoidance").getAsInt());
        }
        if (object.has("tracks") && object.get("tracks").isJsonObject()) {
            for (String id : settings.weights().keySet().toArray(new String[0])) {
                settings.exclude(id);
            }
            for (Map.Entry<String, JsonElement> entry : object.getAsJsonObject("tracks").entrySet()) {
                String id = catalog.canonicalId(entry.getKey());
                int weight = entry.getValue().getAsInt();
                settings.setWeight(id, Math.max(weight, settings.weight(id)));
            }
        }
    }
}
