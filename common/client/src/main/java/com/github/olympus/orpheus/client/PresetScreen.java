package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.OrpheusRuntime;
import com.github.olympus.orpheus.core.preset.PresetStore;
import com.github.olympus.orpheus.core.ui.Pager;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PresetScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int HEIGHT = 20;
    private static final int GAP = 4;
    private static final int ROW_HEIGHT = 22;
    private static final int HEADER_HEIGHT = 34;
    private static final int FOOTER_HEIGHT = 84;
    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 8;

    private final Screen parent;
    private EditBox nameBox;
    private String nameText = "";
    private String status = "Switch, create or delete presets";
    private int page;

    public PresetScreen(Screen parent) {
        super(Component.literal("Orpheus Presets"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        build();
    }

    @Override
    public void onClose() {
        OrpheusClient.show(parent);
    }

    private void build() {
        clearWidgets();
        OrpheusRuntime runtime = OrpheusClient.runtime();
        int left = (width - WIDTH) / 2;
        int y = 8;
        label(left, y, WIDTH, status);

        List<String> names = runtime.presetNames();
        int rows = Math.max(MIN_ROWS, Math.min(MAX_ROWS, (height - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT));
        page = Pager.clamp(page, names.size(), rows);
        y = HEADER_HEIGHT;
        for (String name : Pager.slice(names, page, rows)) {
            boolean active = name.equalsIgnoreCase(runtime.active().name());
            int nameWidth = WIDTH - 70 - GAP;
            button(left, y, nameWidth, (active ? "* " : "") + name, () -> {
                if (runtime.switchTo(name)) {
                    status = "Switched to " + name;
                } else {
                    status = "Could not load " + name;
                }
                build();
            });
            Button delete = button(left + nameWidth + GAP, y, 70, "Delete", () -> {
                status = runtime.deletePreset(name) ? "Deleted " + name : "Could not delete " + name;
                build();
            });
            delete.active = names.size() > 1;
            y += ROW_HEIGHT;
        }

        int pageCount = Pager.pageCount(names.size(), rows);
        int footer = height - FOOTER_HEIGHT + GAP;
        button(left, footer, HEIGHT, "<", () -> {
            page = Math.max(0, page - 1);
            build();
        });
        label(left + HEIGHT + GAP, footer, WIDTH - 2 * (HEIGHT + GAP), "Page " + (page + 1) + " / " + pageCount);
        button(left + WIDTH - HEIGHT, footer, HEIGHT, ">", () -> {
            page = Math.min(pageCount - 1, page + 1);
            build();
        });

        footer += HEIGHT + GAP;
        int fieldWidth = WIDTH - 2 * 80 - 2 * GAP;
        nameBox = new EditBox(font, left, footer, fieldWidth, HEIGHT, Component.literal("Preset name"));
        nameBox.setMaxLength(PresetStore.MAX_NAME_LENGTH);
        nameBox.setValue(nameText);
        nameBox.setResponder(value -> nameText = value);
        addRenderableWidget(nameBox);
        button(left + fieldWidth + GAP, footer, 80, "Copy", () -> create(runtime, true));
        button(left + fieldWidth + 80 + 2 * GAP, footer, 80, "Blank", () -> create(runtime, false));

        footer += HEIGHT + GAP;
        button(left, footer, WIDTH, "Back", this::onClose);
    }

    private void create(OrpheusRuntime runtime, boolean copy) {
        boolean created = copy ? runtime.createFromActive(nameText) : runtime.createDefault(nameText);
        if (created) {
            status = "Created " + runtime.active().name();
            nameText = "";
        } else {
            status = "Invalid or duplicate name";
        }
        build();
    }

    private Button button(int x, int y, int width, String text, Runnable action) {
        Button button = Button.builder(Component.literal(text), pressed -> action.run())
                .bounds(x, y, width, HEIGHT)
                .build();
        addRenderableWidget(button);
        return button;
    }

    private void label(int x, int y, int width, String text) {
        Button label = button(x, y, width, text, () -> { });
        label.active = false;
    }
}
