package com.github.olympus.orpheus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class KeyFactory {
    private KeyFactory() {
    }

    public static KeyMapping.Category registerCategory() {
        return KeyMapping.Category.register(OrpheusClient.id("main"));
    }

    public static KeyBindings create(KeyMapping.Category category) {
        return new KeyBindings(
                key("menu", GLFW.GLFW_KEY_O, category),
                key("previous", GLFW.GLFW_KEY_LEFT_BRACKET, category),
                key("skip", GLFW.GLFW_KEY_RIGHT_BRACKET, category),
                key("pause", GLFW.GLFW_KEY_BACKSLASH, category),
                key("blacklist", GLFW.GLFW_KEY_APOSTROPHE, category));
    }

    private static KeyMapping key(String name, int code, KeyMapping.Category category) {
        return new KeyMapping("key.orpheus." + name, InputConstants.Type.KEYSYM, code, category);
    }
}
