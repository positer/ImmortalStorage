package com.immortalstorage.immortalstorage.api.storage.terminal;

import java.util.Locale;

/** Server-visible terminal query and ordering state. */
public record TerminalQuery(String text, SortOrder sortOrder, SortDirection sortDirection) {
    public static final TerminalQuery DEFAULT = new TerminalQuery("", SortOrder.AMOUNT, SortDirection.DESCENDING);
    public static final int MAX_SEARCH_LENGTH = 128;

    public TerminalQuery {
        text = sanitize(text);
        sortOrder = sortOrder == null ? SortOrder.AMOUNT : sortOrder;
        sortDirection = sortDirection == null ? SortDirection.DESCENDING : sortDirection;
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        String trimmed = value.strip();
        return trimmed.substring(0, Math.min(trimmed.length(), MAX_SEARCH_LENGTH));
    }

    public String normalizedText() {
        return text.toLowerCase(Locale.ROOT);
    }

    public enum SortOrder {
        AMOUNT,
        NAME,
        MOD_ID;

        public static SortOrder byId(int id) {
            SortOrder[] values = values();
            return id >= 0 && id < values.length ? values[id] : AMOUNT;
        }
    }

    public enum SortDirection {
        ASCENDING,
        DESCENDING;

        public static SortDirection byId(int id) {
            return id == ASCENDING.ordinal() ? ASCENDING : DESCENDING;
        }
    }
}
