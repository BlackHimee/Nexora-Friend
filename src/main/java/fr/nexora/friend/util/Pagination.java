package fr.nexora.friend.util;

import java.util.List;

public final class Pagination {

    private Pagination() {
    }

    public static <T> List<T> slice(List<T> items, int page, int pageSize) {
        if (items.isEmpty() || pageSize <= 0) {
            return List.of();
        }
        int from = page * pageSize;
        if (from >= items.size() || from < 0) {
            return List.of();
        }
        int to = Math.min(from + pageSize, items.size());
        return items.subList(from, to);
    }

    public static int pageCount(int totalItems, int pageSize) {
        if (totalItems <= 0 || pageSize <= 0) {
            return 1;
        }
        return Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
    }
}
