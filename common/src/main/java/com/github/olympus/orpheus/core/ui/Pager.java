package com.github.olympus.orpheus.core.ui;

import java.util.List;

public final class Pager {
    private Pager() {
    }

    public static int pageCount(int items, int pageSize) {
        return Math.max(1, (items + pageSize - 1) / pageSize);
    }

    public static int clamp(int page, int items, int pageSize) {
        return Math.max(0, Math.min(page, pageCount(items, pageSize) - 1));
    }

    public static <T> List<T> slice(List<T> items, int page, int pageSize) {
        int from = Math.min(items.size(), clamp(page, items.size(), pageSize) * pageSize);
        int to = Math.min(items.size(), from + pageSize);
        return items.subList(from, to);
    }
}
