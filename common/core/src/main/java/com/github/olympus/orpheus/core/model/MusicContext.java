package com.github.olympus.orpheus.core.model;

import java.util.Locale;
import java.util.Optional;

public enum MusicContext {
    MAIN_MENU("Main Menu", 1, 30),
    SURVIVAL("Survival", 600, 1200),
    CREATIVE("Creative", 60, 180),
    UNDERWATER("Underwater", 600, 1200),
    NETHER("Nether", 600, 1200),
    END("The End", 300, 1200),
    BOSS("Ender Dragon", 0, 0),
    CREDITS("Credits", 0, 0);

    public static final String VANILLA_NAMESPACE = "minecraft";

    private final String label;
    private final int defaultMinSeconds;
    private final int defaultMaxSeconds;

    MusicContext(String label, int defaultMinSeconds, int defaultMaxSeconds) {
        this.label = label;
        this.defaultMinSeconds = defaultMinSeconds;
        this.defaultMaxSeconds = defaultMaxSeconds;
    }

    /** English fallback name. The screens use {@link #translationKey()} so the name can be translated. */
    public String label() {
        return label;
    }

    public String translationKey() {
        return "orpheus.context." + name().toLowerCase(Locale.ROOT);
    }

    public int defaultMinSeconds() {
        return defaultMinSeconds;
    }

    public int defaultMaxSeconds() {
        return defaultMaxSeconds;
    }

    public MusicContext next() {
        MusicContext[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public MusicContext previous() {
        MusicContext[] values = values();
        return values[(ordinal() + values.length - 1) % values.length];
    }

    public static Optional<MusicContext> fromEvent(String namespace, String path) {
        if (!VANILLA_NAMESPACE.equals(namespace) || path == null) {
            return Optional.empty();
        }
        switch (path) {
            case "music.menu":
                return Optional.of(MAIN_MENU);
            case "music.creative":
                return Optional.of(CREATIVE);
            case "music.under_water":
                return Optional.of(UNDERWATER);
            case "music.end":
                return Optional.of(END);
            case "music.dragon":
                return Optional.of(BOSS);
            case "music.credits":
                return Optional.of(CREDITS);
            case "music.game":
                return Optional.of(SURVIVAL);
            default:
                break;
        }
        if (path.startsWith("music.nether.")) {
            return Optional.of(NETHER);
        }
        if (path.startsWith("music.overworld.")) {
            return Optional.of(SURVIVAL);
        }
        return Optional.empty();
    }
}
