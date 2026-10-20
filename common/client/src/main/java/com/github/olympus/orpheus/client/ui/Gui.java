package com.github.olympus.orpheus.client.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleToIntFunction;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class Gui {
    private static final float TOOLTIP_DELAY = 0.35f;
    private static final float REPEAT_DELAY = 0.4f;
    private static final float REPEAT_INTERVAL = 0.07f;

    private final UiHost host;
    private final Map<String, Float> values = new HashMap<>();
    private final Map<String, Field> fields = new HashMap<>();
    private final Map<String, Scroll> scrolls = new HashMap<>();
    private final Deque<int[]> clips = new ArrayDeque<>();

    private Canvas canvas;
    private Theme theme;
    private boolean snap;
    private float dt;
    private double time;
    private int mouseX;
    private int mouseY;
    private boolean pressed;
    private boolean rightPressed;
    private boolean mouseDown;
    private boolean consumed;
    private boolean pendingPress;
    private boolean pendingRight;
    private boolean liveDown;
    private float wheel;
    private float pendingWheel;
    private String active;
    private String focused;
    private String tip;
    private String shownTip;
    private float tipTime;
    private boolean locked;
    private double repeatAt;
    private int dragOffset;
    private String scrollId;
    private int scrollX;
    private int scrollY;
    private int scrollW;
    private int scrollH;

    private static final class Field {
        String text = "";
        int max = 64;
        int cursor;
        boolean all;
        boolean submitted;
    }

    private static final class Scroll {
        float target;
        float current;
        float content;
        float view;
    }

    public Gui(UiHost host) {
        this.host = host;
    }

    public void press(int button) {
        if (button == 0) {
            pendingPress = true;
            liveDown = true;
        } else if (button == 1) {
            pendingRight = true;
        }
    }

    public void release(int button) {
        if (button == 0) {
            liveDown = false;
        }
    }

    public void scrolled(double amount) {
        pendingWheel += (float) amount;
    }

    public void begin(Canvas canvas, Theme theme, int mouseX, int mouseY, float dt, double time, boolean snap) {
        this.canvas = canvas;
        this.theme = theme;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.dt = dt;
        this.time = time;
        this.snap = snap;
        pressed = pendingPress;
        rightPressed = pendingRight;
        pendingPress = false;
        pendingRight = false;
        mouseDown = liveDown;
        wheel = pendingWheel;
        pendingWheel = 0f;
        consumed = false;
        if (!mouseDown) {
            active = null;
        }
        tip = null;
        clips.clear();
    }

    public void lock(boolean value) {
        locked = value;
    }

    public void end() {
        if ((pressed || rightPressed) && !consumed) {
            focused = null;
        }
        if (tip != null && tip.equals(shownTip)) {
            tipTime += dt;
        } else {
            shownTip = tip;
            tipTime = 0f;
        }
        if (tip != null && (snap || tipTime > TOOLTIP_DELAY)) {
            drawTooltip(tip);
        }
    }

    public Canvas canvas() {
        return canvas;
    }

    public Theme theme() {
        return theme;
    }

    public double time() {
        return time;
    }

    public float anim(String key, float target, float rate) {
        float current = values.getOrDefault(key, target);
        float next = snap ? target : Motion.approach(current, target, dt, rate);
        values.put(key, next);
        return next;
    }

    public void setAnim(String key, float value) {
        values.put(key, value);
    }

    public boolean over(int x, int y, int w, int h) {
        if (locked || !inside(mouseX, mouseY, x, y, w, h)) {
            return false;
        }
        int[] clip = clips.peek();
        return clip == null || inside(mouseX, mouseY, clip[0], clip[1], clip[2], clip[3]);
    }

    private boolean hot(String id, int x, int y, int w, int h) {
        return over(x, y, w, h) && (active == null || active.equals(id));
    }

    private static boolean inside(int px, int py, int x, int y, int w, int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    public void tooltip(String text) {
        if (text != null) {
            tip = text;
        }
    }

    private boolean take() {
        if (consumed) {
            return false;
        }
        consumed = true;
        host.playClick();
        return true;
    }

    public void text(String text, int x, int y, int color) {
        canvas.text(text, x, y, color, theme.shadowText());
    }

    public void centered(String text, int centerX, int y, int color) {
        canvas.text(text, centerX - canvas.textWidth(text) / 2, y, color, theme.shadowText());
    }

    public void right(String text, int rightX, int y, int color) {
        canvas.text(text, rightX - canvas.textWidth(text), y, color, theme.shadowText());
    }

    public String fit(String text, int maxWidth) {
        return Draw.fit(canvas, text, maxWidth);
    }

    public int textY(int y, int h) {
        return y + (h - canvas.lineHeight()) / 2 + 1;
    }

    public boolean button(String id, int x, int y, int w, int h, String label, String tip) {
        return button(id, x, y, w, h, label, ButtonKind.NORMAL, false, true, tip);
    }

    public boolean button(String id, int x, int y, int w, int h, String label, ButtonKind kind, boolean selected,
            boolean enabled, String tip) {
        boolean hot = hot(id, x, y, w, h);
        float hover = anim(id + "#h", hot && enabled ? 1f : 0f, 16f);
        float press = anim(id + "#p", hot && enabled && mouseDown ? 1f : 0f, 30f);
        float chosen = anim(id + "#s", selected ? 1f : 0f, 14f);
        theme.button(canvas, x, y, w, h, kind, hover, press, chosen, enabled);
        int color = theme.buttonText(kind, hover, chosen, enabled);
        String shown = fit(label, w - 6);
        canvas.text(shown, x + (w - canvas.textWidth(shown)) / 2, textY(y, h), color, theme.shadowText());
        if (hot) {
            tooltip(tip);
        }
        return hot && enabled && pressed && take();
    }

    public boolean iconButton(String id, int x, int y, int w, int h, String icon, ButtonKind kind, boolean enabled,
            String tip) {
        boolean hot = hot(id, x, y, w, h);
        float hover = anim(id + "#h", hot && enabled ? 1f : 0f, 16f);
        float press = anim(id + "#p", hot && enabled && mouseDown ? 1f : 0f, 30f);
        theme.button(canvas, x, y, w, h, kind, hover, press, 0f, enabled);
        int color = theme.buttonText(kind, hover, 0f, enabled);
        int shift = press > 0.5f ? 1 : 0;
        theme.icon(canvas, icon, x + w / 2 + shift, y + h / 2 + shift, color);
        if (hot) {
            tooltip(tip);
        }
        return hot && enabled && pressed && take();
    }

    public int repeatButton(String id, int x, int y, int w, int h, String label, boolean enabled, String tip) {
        boolean hot = hot(id, x, y, w, h);
        float hover = anim(id + "#h", hot && enabled ? 1f : 0f, 16f);
        float press = anim(id + "#p", hot && enabled && mouseDown ? 1f : 0f, 30f);
        theme.button(canvas, x, y, w, h, ButtonKind.NORMAL, hover, press, 0f, enabled);
        int color = theme.buttonText(ButtonKind.NORMAL, hover, 0f, enabled);
        canvas.text(label, x + (w - canvas.textWidth(label)) / 2, textY(y, h), color, theme.shadowText());
        if (hot) {
            tooltip(tip);
        }
        if (!enabled) {
            return 0;
        }
        if (hot && pressed && take()) {
            if (mouseDown) {
                active = id;
            }
            repeatAt = time + REPEAT_DELAY;
            return 1;
        }
        if (active != null && active.equals(id) && mouseDown && over(x, y, w, h) && time >= repeatAt) {
            repeatAt = time + REPEAT_INTERVAL;
            return 1;
        }
        return 0;
    }

    public int stepper(String id, int x, int y, int w, int h, String value, String tip, boolean canDecrease,
            boolean canIncrease) {
        int result = 0;
        if (repeatButton(id + "-", x, y, h, h, "-", canDecrease, tip) > 0) {
            result = -1;
        }
        int fieldX = x + h + 3;
        int fieldW = w - 2 * h - 6;
        theme.inset(canvas, fieldX, y, fieldW, h);
        centered(fit(value, fieldW - 6), fieldX + fieldW / 2, textY(y, h), theme.textPrimary());
        if (over(fieldX, y, fieldW, h)) {
            tooltip(tip);
        }
        if (repeatButton(id + "+", x + w - h, y, h, h, "+", canIncrease, tip) > 0) {
            result = 1;
        }
        return result;
    }

    public int segmented(String id, int x, int y, int w, int h, String[] labels, int selected, String[] tips,
            boolean[] enabled) {
        int count = labels.length;
        boolean sliding = theme.slidingSegments();
        int pad = sliding ? 2 : 0;
        int gap = sliding ? 0 : 2;
        float inner = w - 2 * pad - gap * (count - 1);
        float each = inner / count;
        int result = -1;
        float thumb = anim(id + "#t", selected, 16f);
        if (sliding) {
            theme.segmentTrack(canvas, x, y, w, h);
            int left = x + pad + Math.round(thumb * each);
            int right = x + pad + Math.round(thumb * each + each);
            theme.segmentThumb(canvas, left, y + pad, right - left, h - 2 * pad);
        }
        for (int i = 0; i < count; i++) {
            int sx = x + pad + Math.round(i * (each + gap));
            int ex = i == count - 1 ? x + w - pad : x + pad + Math.round(i * (each + gap) + each);
            int segW = ex - sx;
            boolean on = enabled == null || enabled[i];
            String key = id + i;
            boolean hot = hot(key, sx, y, segW, h);
            float hover = anim(key + "#h", hot && on ? 1f : 0f, 16f);
            float chosen = Motion.clamp(1f - Math.abs(thumb - i), 0f, 1f);
            if (sliding) {
                int color = on ? theme.segmentText(chosen, hover) : theme.textDisabled();
                String shown = fit(labels[i], segW - 6);
                canvas.text(shown, sx + (segW - canvas.textWidth(shown)) / 2, textY(y, h), color, false);
            } else {
                float press = anim(key + "#p", hot && on && mouseDown ? 1f : 0f, 30f);
                theme.button(canvas, sx, y, segW, h, ButtonKind.SEGMENT, hover, press, i == selected ? 1f : 0f, on);
                int color = theme.buttonText(ButtonKind.SEGMENT, hover, i == selected ? 1f : 0f, on);
                String shown = fit(labels[i], segW - 6);
                canvas.text(shown, sx + (segW - canvas.textWidth(shown)) / 2, textY(y, h), color,
                        theme.shadowText());
            }
            if (hot && tips != null) {
                tooltip(tips[i]);
            }
            if (hot && on && pressed && i != selected && take()) {
                result = i;
            }
        }
        return result;
    }

    public float slider(String id, int x, int y, int w, int h, float value, DoubleToIntFunction colorAt) {
        boolean hot = hot(id, x, y, w, h + 4);
        boolean dragging = id.equals(active);
        for (int i = 0; i < w; i += 2) {
            canvas.fill(x + i, y, Math.min(2, w - i), h, colorAt.applyAsInt(i / (double) (w - 1)));
        }
        Draw.ring(canvas, x - 1, y - 1, w + 2, h + 2, 2, 0x33FFFFFF);
        if (hot && pressed && !consumed) {
            consumed = true;
            active = id;
            dragging = true;
        }
        if (dragging && mouseDown) {
            value = Motion.clamp((mouseX - x) / (float) (w - 1), 0f, 1f);
        }
        float grow = anim(id + "#k", hot || dragging ? 1f : 0f, 16f);
        int knobX = x + Math.round(value * (w - 1));
        int extra = Math.round(grow);
        Draw.round(canvas, knobX - 3, y - 3 - extra, 7, h + 6 + 2 * extra, 3, 0xFFFFFFFF);
        Draw.round(canvas, knobX - 2, y - 2 - extra, 5, h + 4 + 2 * extra, 2,
                colorAt.applyAsInt(value) | 0xFF000000);
        return value;
    }

    private void drawWheel(int x, int y, int size, float hover, float selected) {
        double radius = size / 2.0 + hover;
        double center = size / 2.0 - 0.5;
        int reach = (int) Math.ceil(radius + 4);
        for (int py = -reach; py <= size + reach; py++) {
            for (int px = -reach; px <= size + reach; px++) {
                double dx = px - center;
                double dy = py - center;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance <= radius + 0.5) {
                    double angle = Math.atan2(dy, dx) / (2 * Math.PI);
                    float saturation = (float) Math.min(1.0, distance / radius);
                    int color = Colors.hsl((float) angle, 1f, 1f - 0.5f * saturation);
                    float cover = (float) Math.min(1.0, radius + 0.5 - distance);
                    canvas.fill(x + px, y + py, 1, 1, Colors.alpha(color, cover));
                } else if (selected > 0.05f && distance >= radius + 2 && distance <= radius + 3.5) {
                    float cover = (float) Math.min(1.0, Math.min(distance - radius - 1.5, radius + 4 - distance));
                    canvas.fill(x + px, y + py, 1, 1, Colors.alpha(0xFFFFFFFF, selected * cover));
                }
            }
        }
    }

    public boolean dragging() {
        return active != null;
    }

    public boolean switchRow(String id, int x, int y, int w, int h, String label, boolean value, boolean enabled,
            String tip, String onLabel, String offLabel) {
        boolean hot = hot(id, x, y, w, h);
        float hover = anim(id + "#h", hot && enabled ? 1f : 0f, 16f);
        float on = anim(id + "#o", value ? 1f : 0f, 16f);
        int toggleW = theme.toggleWidth();
        canvas.text(fit(label, w - toggleW - 8), x, textY(y, h), enabled ? theme.textPrimary() : theme.textDisabled(),
                theme.shadowText());
        int toggleH = Math.min(h, theme.slidingSegments() ? 16 : 20);
        theme.toggle(canvas, x + w - toggleW, y + (h - toggleH) / 2, toggleH, on, hover, enabled, onLabel, offLabel);
        if (hot) {
            tooltip(tip);
        }
        return hot && enabled && pressed && take();
    }

    public void checkbox(String id, int x, int y, int size, boolean value, boolean enabled) {
        float hover = anim(id + "#h", over(x, y, size, size) && enabled ? 1f : 0f, 16f);
        float on = anim(id + "#o", value ? 1f : 0f, 18f);
        theme.checkbox(canvas, x, y, size, on, hover, enabled);
    }

    public void rowBackground(String id, int x, int y, int w, int h, boolean highlighted) {
        float hover = anim(id + "#r", over(x, y, w, h) ? 1f : 0f, 18f);
        theme.row(canvas, x, y, w, h, hover, highlighted);
    }

    public boolean hit(String id, int x, int y, int w, int h, boolean enabled, String tip) {
        boolean hot = hot(id, x, y, w, h);
        if (hot) {
            tooltip(tip);
        }
        return hot && enabled && pressed && take();
    }

    public int chip(String id, int x, int y, int w, int h, String label, boolean enabled, String tip) {
        boolean hot = hot(id, x, y, w, h);
        float hover = anim(id + "#h", hot && enabled ? 1f : 0f, 16f);
        float press = anim(id + "#p", hot && enabled && mouseDown ? 1f : 0f, 30f);
        theme.button(canvas, x, y, w, h, ButtonKind.NORMAL, hover, press, 0f, enabled);
        int color = theme.buttonText(ButtonKind.NORMAL, hover, 0f, enabled);
        canvas.text(label, x + (w - canvas.textWidth(label)) / 2, textY(y, h), color, theme.shadowText());
        if (hot) {
            tooltip(tip);
        }
        if (!hot || !enabled) {
            return 0;
        }
        if (wheel != 0f) {
            int direction = wheel > 0f ? 1 : -1;
            wheel = 0f;
            return direction;
        }
        if (rightPressed && !consumed) {
            consumed = true;
            host.playClick();
            return -1;
        }
        return pressed && take() ? 1 : 0;
    }

    public boolean swatch(String id, int x, int y, int size, int color, boolean selected, boolean rainbow,
            String tip) {
        boolean hot = hot(id, x, y, size, size);
        float hover = anim(id + "#h", hot ? 1f : 0f, 16f);
        float chosen = anim(id + "#s", selected ? 1f : 0f, 14f);
        if (rainbow) {
            drawWheel(x, y, size, hover, chosen);
        } else {
            theme.swatch(canvas, x, y, size, color, hover, chosen);
        }
        if (hot) {
            tooltip(tip);
        }
        return hot && pressed && take();
    }

    public void progress(int x, int y, int w, int h, float value) {
        theme.progress(canvas, x, y, w, h, value, time);
    }

    public String field(String id, int x, int y, int w, int h, String hint, int maxLength) {
        Field field = fields.computeIfAbsent(id, key -> new Field());
        field.max = maxLength;
        boolean hot = hot(id, x, y, w, h);
        boolean focus = id.equals(focused);
        float hover = anim(id + "#h", hot ? 1f : 0f, 16f);
        float glow = anim(id + "#f", focus ? 1f : 0f, 16f);
        theme.field(canvas, x, y, w, h, glow, hover);
        if (hot && pressed) {
            consumed = true;
            focused = id;
            focus = true;
            field.all = false;
            field.cursor = indexAt(field.text, mouseX - (x + 4));
        }
        int textX = x + 4;
        int available = w - 8;
        int start = 0;
        while (start < field.cursor && canvas.textWidth(field.text.substring(start, field.cursor)) > available - 2) {
            start++;
        }
        canvas.clip(x + 2, y, w - 4, h);
        int ty = textY(y, h);
        if (field.text.isEmpty() && !focus) {
            canvas.text(hint, textX, ty, theme.textDisabled(), false);
        } else {
            String shown = field.text.substring(start);
            if (field.all && focus) {
                canvas.fill(textX - 1, y + 3, canvas.textWidth(shown) + 2, h - 6, 0x663B82F6);
            }
            canvas.text(shown, textX, ty, theme.textPrimary(), theme.shadowText());
            if (focus && !field.all && (int) (time * 2) % 2 == 0) {
                int caret = textX + canvas.textWidth(field.text.substring(start, field.cursor));
                canvas.fill(caret, y + 3, 1, h - 6, theme.textPrimary());
            }
        }
        canvas.unclip();
        field.cursor = Math.min(field.cursor, field.text.length());
        return field.text;
    }

    private int indexAt(String text, int offset) {
        int best = 0;
        for (int i = 1; i <= text.length(); i++) {
            if (canvas.textWidth(text.substring(0, i)) - canvas.textWidth(text.substring(i - 1, i)) / 2 > offset) {
                return best;
            }
            best = i;
        }
        return best;
    }

    public void setField(String id, String value) {
        Field field = fields.computeIfAbsent(id, key -> new Field());
        field.text = value;
        field.cursor = value.length();
        field.all = false;
    }

    public boolean submitted(String id) {
        Field field = fields.get(id);
        if (field == null || !field.submitted) {
            return false;
        }
        field.submitted = false;
        return true;
    }

    public boolean typing() {
        return focused != null;
    }

    public void blur() {
        focused = null;
    }

    public boolean keyPressed(int key, int mods) {
        if (focused == null) {
            return false;
        }
        Field field = fields.get(focused);
        if (field == null) {
            return false;
        }
        boolean control = (mods & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
        if (control) {
            switch (key) {
                case GLFW.GLFW_KEY_A -> {
                    field.all = !field.text.isEmpty();
                    return true;
                }
                case GLFW.GLFW_KEY_C -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(field.text);
                    return true;
                }
                case GLFW.GLFW_KEY_X -> {
                    Minecraft.getInstance().keyboardHandler.setClipboard(field.text);
                    field.text = "";
                    field.cursor = 0;
                    field.all = false;
                    return true;
                }
                case GLFW.GLFW_KEY_V -> {
                    insert(field, Minecraft.getInstance().keyboardHandler.getClipboard());
                    return true;
                }
                default -> {
                    return false;
                }
            }
        }
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> focused = null;
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> field.submitted = true;
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (field.all) {
                    field.text = "";
                    field.cursor = 0;
                    field.all = false;
                } else if (field.cursor > 0) {
                    field.text = field.text.substring(0, field.cursor - 1) + field.text.substring(field.cursor);
                    field.cursor--;
                }
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (field.all) {
                    field.text = "";
                    field.cursor = 0;
                    field.all = false;
                } else if (field.cursor < field.text.length()) {
                    field.text = field.text.substring(0, field.cursor) + field.text.substring(field.cursor + 1);
                }
            }
            case GLFW.GLFW_KEY_LEFT -> {
                field.all = false;
                field.cursor = Math.max(0, field.cursor - 1);
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                field.all = false;
                field.cursor = Math.min(field.text.length(), field.cursor + 1);
            }
            case GLFW.GLFW_KEY_HOME -> {
                field.all = false;
                field.cursor = 0;
            }
            case GLFW.GLFW_KEY_END -> {
                field.all = false;
                field.cursor = field.text.length();
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    public boolean charTyped(int codepoint) {
        if (focused == null || Character.isISOControl(codepoint)) {
            return false;
        }
        Field field = fields.get(focused);
        if (field == null) {
            return false;
        }
        insert(field, new String(Character.toChars(codepoint)));
        return true;
    }

    private void insert(Field field, String text) {
        String clean = text.replaceAll("\\p{Cntrl}", "");
        if (field.all) {
            field.text = "";
            field.cursor = 0;
            field.all = false;
        }
        String next = field.text.substring(0, field.cursor) + clean + field.text.substring(field.cursor);
        if (next.length() > field.max) {
            return;
        }
        field.text = next;
        field.cursor += clean.length();
    }

    public float scrollBegin(String id, int x, int y, int w, int h) {
        Scroll scroll = scrolls.computeIfAbsent(id, key -> new Scroll());
        scroll.view = h;
        float max = Math.max(0f, scroll.content - h);
        if (over(x, y, w, h) && wheel != 0f) {
            scroll.target -= wheel * 24f;
            wheel = 0f;
        }
        String barId = "sb:" + id;
        if (barId.equals(active) && mouseDown && scroll.content > h) {
            int thumbH = thumbHeight(scroll);
            float ratio = (mouseY - dragOffset - y) / (float) Math.max(1, h - thumbH);
            scroll.target = Motion.clamp(ratio, 0f, 1f) * max;
            scroll.current = scroll.target;
        }
        scroll.target = Motion.clamp(scroll.target, 0f, max);
        scroll.current = snap ? scroll.target : Motion.approach(scroll.current, scroll.target, dt, 18f);
        scroll.current = Motion.clamp(scroll.current, 0f, max);
        int[] parent = clips.peek();
        int cx = x;
        int cy = y;
        int cw = w;
        int ch = h;
        if (parent != null) {
            cx = Math.max(x, parent[0]);
            cy = Math.max(y, parent[1]);
            cw = Math.min(x + w, parent[0] + parent[2]) - cx;
            ch = Math.min(y + h, parent[1] + parent[3]) - cy;
        }
        clips.push(new int[] {cx, cy, Math.max(0, cw), Math.max(0, ch)});
        canvas.clip(x, y, w, h);
        scrollId = id;
        scrollX = x;
        scrollY = y;
        scrollW = w;
        scrollH = h;
        return scroll.current;
    }

    public void scrollEnd(int contentHeight) {
        Scroll scroll = scrolls.get(scrollId);
        scroll.content = contentHeight;
        canvas.unclip();
        clips.pop();
        if (contentHeight <= scrollH) {
            return;
        }
        int thumbH = thumbHeight(scroll);
        float max = contentHeight - scrollH;
        int thumbY = scrollY + Math.round((scrollH - thumbH) * (scroll.current / max));
        int trackX = scrollX + scrollW - 4;
        String barId = "sb:" + scrollId;
        boolean hot = (active == null || barId.equals(active)) && over(trackX - 2, scrollY, 6, scrollH);
        float hover = anim(barId + "#h", hot || barId.equals(active) ? 1f : 0f, 16f);
        theme.scrollbar(canvas, trackX, scrollY, 3, scrollH, thumbY, thumbH, hover);
        if (hot && pressed && !consumed) {
            consumed = true;
            active = barId;
            if (mouseY >= thumbY && mouseY < thumbY + thumbH) {
                dragOffset = mouseY - thumbY;
            } else {
                dragOffset = thumbH / 2;
                float ratio = (mouseY - dragOffset - scrollY) / (float) Math.max(1, scrollH - thumbH);
                scroll.target = Motion.clamp(ratio, 0f, 1f) * max;
            }
        }
    }

    private int thumbHeight(Scroll scroll) {
        return Math.max(16, Math.round(scroll.view * scroll.view / Math.max(1f, scroll.content)));
    }

    private void drawTooltip(String text) {
        int maxWidth = Math.min(220, canvas.width() - 24);
        List<String> lines = Draw.wrap(canvas, text, maxWidth);
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, canvas.textWidth(line));
        }
        int lineHeight = canvas.lineHeight() + 1;
        int w = width + 10;
        int h = lines.size() * lineHeight + 8;
        int x = mouseX + 10;
        int y = mouseY + 12;
        if (x + w > canvas.width() - 4) {
            x = Math.max(4, mouseX - w - 8);
        }
        if (y + h > canvas.height() - 4) {
            y = Math.max(4, canvas.height() - 4 - h);
        }
        float fade = snap ? 1f : Motion.clamp((tipTime - TOOLTIP_DELAY) / 0.12f, 0f, 1f);
        float previous = 1f;
        if (canvas instanceof FadeCanvas fadeCanvas) {
            previous = fadeCanvas.alpha();
            fadeCanvas.alpha(previous * fade);
        }
        theme.tooltip(canvas, x, y, w, h);
        for (int i = 0; i < lines.size(); i++) {
            canvas.text(lines.get(i), x + 5, y + 5 + i * lineHeight, theme.textPrimary(), theme.shadowText());
        }
        if (canvas instanceof FadeCanvas fadeCanvas) {
            fadeCanvas.alpha(previous);
        }
    }
}
