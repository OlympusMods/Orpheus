package com.github.olympus.orpheus.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Constructor;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Creates the key mappings for 26.1 through 26.3.
 *
 * <p>26.3 replaced GLFW with SDL: {@code InputConstants.Type.KEYSYM} became {@code KEYBOARD}, and key codes are now
 * SDL scancodes instead of GLFW key codes. The same jar has to load on both, so the key type and the default codes
 * are chosen at runtime instead of being compiled in.
 */
public final class KeyFactory {
    private static final boolean SDL = isSdl();

    private KeyFactory() {
    }

    public static KeyMapping.Category registerCategory() {
        return KeyMapping.Category.register(OrpheusClient.id("main"));
    }

    public static KeyBindings create(KeyMapping.Category category) {
        // GLFW key codes before 26.3, SDL scancodes (USB HID usage ids) from 26.3 on.
        return new KeyBindings(
                key("menu", GLFW.GLFW_KEY_O, 18, category),
                key("previous", GLFW.GLFW_KEY_LEFT_BRACKET, 47, category),
                key("skip", GLFW.GLFW_KEY_RIGHT_BRACKET, 48, category),
                key("pause", GLFW.GLFW_KEY_BACKSLASH, 49, category),
                key("blacklist", GLFW.GLFW_KEY_APOSTROPHE, 52, category));
    }

    private static boolean isSdl() {
        for (Object constant : InputConstants.Type.class.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals("KEYBOARD")) {
                return true;
            }
        }
        return false;
    }

    private static KeyMapping key(String name, int glfw, int sdl, KeyMapping.Category category) {
        String translationKey = "key.orpheus." + name;
        int code = SDL ? sdl : glfw;
        try {
            Constructor<KeyMapping> withoutType =
                    findConstructor(String.class, int.class, KeyMapping.Category.class);
            if (withoutType != null) {
                return withoutType.newInstance(translationKey, code, category);
            }
            Constructor<KeyMapping> withType = findConstructor(
                    String.class, InputConstants.Type.class, int.class, KeyMapping.Category.class);
            if (withType != null) {
                return withType.newInstance(translationKey, keyboardType(), code, category);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not create the key mapping " + translationKey, exception);
        }
        throw new IllegalStateException("This Minecraft version has no supported KeyMapping constructor");
    }

    private static Constructor<KeyMapping> findConstructor(Class<?>... parameters) {
        try {
            return KeyMapping.class.getConstructor(parameters);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }

    private static InputConstants.Type keyboardType() {
        String name = SDL ? "KEYBOARD" : "KEYSYM";
        for (InputConstants.Type type : InputConstants.Type.class.getEnumConstants()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        throw new IllegalStateException("InputConstants.Type has no " + name);
    }
}
