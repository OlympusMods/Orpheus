package com.github.olympus.orpheus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class KeyFactory {
    private static final String CATEGORY = "key.categories.orpheus";

    private KeyFactory() {
    }

    public static KeyBindings create() {
        return new KeyBindings(
                key("menu", GLFW.GLFW_KEY_O),
                key("previous", GLFW.GLFW_KEY_LEFT_BRACKET),
                key("skip", GLFW.GLFW_KEY_RIGHT_BRACKET),
                key("pause", GLFW.GLFW_KEY_BACKSLASH),
                key("blacklist", GLFW.GLFW_KEY_APOSTROPHE));
    }

    private static KeyMapping key(String name, int code) {
        return new KeyMapping("key.orpheus." + name, InputConstants.Type.KEYSYM, code, CATEGORY);
    }
}
