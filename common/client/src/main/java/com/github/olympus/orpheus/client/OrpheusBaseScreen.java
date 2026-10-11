package com.github.olympus.orpheus.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Shared layout constants and widget helpers for the Orpheus screens.
 *
 * <p>Both screens are rebuilt from scratch whenever something changes, so every helper
 * only creates widgets; {@link #rebuild()} decides what the screen looks like.
 */
abstract class OrpheusBaseScreen extends Screen {
    /** Width of the column of widgets in the middle of the screen. */
    protected static final int WIDTH = 320;
    protected static final int HEIGHT = 20;
    protected static final int GAP = 4;
    /** Distance between two rows of widgets. */
    protected static final int STEP = HEIGHT + GAP;

    protected OrpheusBaseScreen(Component title) {
        super(title);
    }

    /** Clears the screen and creates all widgets again from the current state. */
    protected abstract void rebuild();

    @Override
    protected void init() {
        rebuild();
    }

    protected int left() {
        return (width - WIDTH) / 2;
    }

    protected Button button(int x, int y, int buttonWidth, Component text, Component tooltip, Runnable action) {
        Button.Builder builder = Button.builder(text, pressed -> action.run()).bounds(x, y, buttonWidth, HEIGHT);
        if (tooltip != null) {
            builder.tooltip(Tooltip.create(tooltip));
        }
        Button button = builder.build();
        addRenderableWidget(button);
        return button;
    }

    /** A button that does nothing, used to print text. */
    protected Button label(int x, int y, int labelWidth, Component text, Component tooltip) {
        Button label = button(x, y, labelWidth, text, tooltip, () -> { });
        label.active = false;
        return label;
    }

    /** A "-" button, a text in the middle and a "+" button. The screen is rebuilt after each click. */
    protected void stepper(
            int x, int y, int stepperWidth, Component text, Component tooltip, Runnable decrease, Runnable increase) {
        button(x, y, HEIGHT, Component.literal("-"), null, () -> {
            decrease.run();
            rebuild();
        });
        label(x + HEIGHT + GAP, y, stepperWidth - 2 * (HEIGHT + GAP), text, tooltip);
        button(x + stepperWidth - HEIGHT, y, HEIGHT, Component.literal("+"), null, () -> {
            increase.run();
            rebuild();
        });
    }

    protected static Component text(String key, Object... arguments) {
        return Component.translatable(key, arguments);
    }
}
