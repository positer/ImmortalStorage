package com.cultivation.cultivation.client.guide;

public record JadeGuideLocation(Kind kind, String targetId, int pageIndex, String searchQuery) {
    public JadeGuideLocation {
        if (kind == null) throw new IllegalArgumentException("Location kind is required");
        targetId = targetId == null ? "" : targetId;
        if ((kind == Kind.CATEGORY || kind == Kind.CHAPTER) && targetId.isBlank()) {
            throw new IllegalArgumentException(kind + " location requires a target id");
        }
        pageIndex = Math.max(0, pageIndex);
        searchQuery = searchQuery == null ? "" : searchQuery;
    }

    public static JadeGuideLocation home() {
        return new JadeGuideLocation(Kind.HOME, "", 0, "");
    }

    public static JadeGuideLocation category(String categoryId) {
        return new JadeGuideLocation(Kind.CATEGORY, categoryId, 0, "");
    }

    public static JadeGuideLocation search(String query, int pageIndex) {
        return new JadeGuideLocation(Kind.SEARCH, "", pageIndex, query);
    }

    public static JadeGuideLocation chapter(String chapterId, int pageIndex, String searchQuery) {
        return new JadeGuideLocation(Kind.CHAPTER, chapterId, pageIndex, searchQuery);
    }

    public boolean sameEntry(JadeGuideLocation other) {
        if (other == null || kind != other.kind) return false;
        return switch (kind) {
            case HOME, SEARCH -> true;
            case CATEGORY, CHAPTER -> targetId.equals(other.targetId);
        };
    }

    public enum Kind {
        HOME,
        CATEGORY,
        SEARCH,
        CHAPTER
    }
}
