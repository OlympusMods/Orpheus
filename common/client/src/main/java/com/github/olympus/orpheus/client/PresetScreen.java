package com.github.olympus.orpheus.client;

import com.github.olympus.orpheus.core.OrpheusRuntime;
import com.github.olympus.orpheus.core.preset.PresetStore;
import com.github.olympus.orpheus.core.ui.Pager;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Lists the saved presets and lets the player switch, create or delete them. */
public final class PresetScreen extends OrpheusBaseScreen {
    private static final int ROW_HEIGHT = 22;
    private static final int HEADER_HEIGHT = 34;
    private static final int FOOTER_HEIGHT = 84;
    private static final int MIN_ROWS = 3;
    private static final int MAX_ROWS = 10;
    private static final int DELETE_WIDTH = 70;
    private static final int CREATE_WIDTH = 80;

    private final Screen parent;
    private EditBox nameBox;
    private String nameText = "";
    private Component status = text("orpheus.presets.hint");
    private int page;

    public PresetScreen(Screen parent) {
        super(text("orpheus.presets.title"));
        this.parent = parent;
    }

    @Override
    public void onClose() {
        OrpheusClient.show(parent);
    }

    @Override
    protected void rebuild() {
        clearWidgets();
        OrpheusRuntime runtime = OrpheusClient.runtime();
        int left = left();
        label(left, 8, WIDTH, status, null);

        List<String> names = runtime.presetNames();
        int rows = Math.max(MIN_ROWS, Math.min(MAX_ROWS, (height - HEADER_HEIGHT - FOOTER_HEIGHT) / ROW_HEIGHT));
        page = Pager.clamp(page, names.size(), rows);
        int y = HEADER_HEIGHT;
        for (String name : Pager.slice(names, page, rows)) {
            addPresetRow(runtime, name, left, y, names.size() > 1);
            y += ROW_HEIGHT;
        }

        int pageCount = Pager.pageCount(names.size(), rows);
        int footer = height - FOOTER_HEIGHT + GAP;
        button(left, footer, HEIGHT, Component.literal("<"), null, () -> {
            page = Math.max(0, page - 1);
            rebuild();
        });
        label(left + HEIGHT + GAP, footer, WIDTH - 2 * (HEIGHT + GAP),
                text("orpheus.screen.page", page + 1, pageCount), null);
        button(left + WIDTH - HEIGHT, footer, HEIGHT, Component.literal(">"), null, () -> {
            page = Math.min(pageCount - 1, page + 1);
            rebuild();
        });

        footer += STEP;
        int fieldWidth = WIDTH - 2 * CREATE_WIDTH - 2 * GAP;
        nameBox = new EditBox(font, left, footer, fieldWidth, HEIGHT, text("orpheus.presets.name"));
        nameBox.setMaxLength(PresetStore.MAX_NAME_LENGTH);
        nameBox.setValue(nameText);
        nameBox.setResponder(value -> nameText = value);
        addRenderableWidget(nameBox);
        button(left + fieldWidth + GAP, footer, CREATE_WIDTH, text("orpheus.presets.copy"),
                text("orpheus.presets.copy.tooltip"), () -> create(runtime, true));
        button(left + fieldWidth + CREATE_WIDTH + 2 * GAP, footer, CREATE_WIDTH, text("orpheus.presets.blank"),
                text("orpheus.presets.blank.tooltip"), () -> create(runtime, false));

        footer += STEP;
        button(left, footer, WIDTH, text("orpheus.presets.back"), null, this::onClose);
    }

    private void addPresetRow(OrpheusRuntime runtime, String name, int left, int y, boolean canDelete) {
        boolean active = name.equalsIgnoreCase(runtime.active().name());
        int nameWidth = WIDTH - DELETE_WIDTH - GAP;
        button(left, y, nameWidth, Component.literal((active ? "* " : "") + name),
                text("orpheus.presets.switch.tooltip"), () -> {
                    status = runtime.switchTo(name)
                            ? text("orpheus.presets.switched", name)
                            : text("orpheus.presets.switch_failed", name);
                    rebuild();
                });
        Button delete = button(left + nameWidth + GAP, y, DELETE_WIDTH, text("orpheus.presets.delete"),
                text("orpheus.presets.delete.tooltip"), () -> {
                    status = runtime.deletePreset(name)
                            ? text("orpheus.presets.deleted", name)
                            : text("orpheus.presets.delete_failed", name);
                    rebuild();
                });
        delete.active = canDelete;
    }

    private void create(OrpheusRuntime runtime, boolean copy) {
        boolean created = copy ? runtime.createFromActive(nameText) : runtime.createDefault(nameText);
        if (created) {
            status = text("orpheus.presets.created", runtime.active().name());
            nameText = "";
        } else {
            status = text("orpheus.presets.invalid_name");
        }
        rebuild();
    }
}
