package com.cultivation.cultivation.client.guide;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JadeGuideSession {
    public static final JadeGuideSession INSTANCE = new JadeGuideSession();

    private final JadeGuideHistory history = new JadeGuideHistory(64);
    private final List<String> recent = new ArrayList<>();
    private final Map<String, Integer> lastPageByChapter = new LinkedHashMap<>();
    private String searchQuery = "";

    private JadeGuideSession() {
    }

    public JadeGuideHistory history() {
        return history;
    }

    public void recordChapter(String chapterId, int pageIndex) {
        recent.remove(chapterId);
        recent.addFirst(chapterId);
        while (recent.size() > 5) recent.removeLast();
        lastPageByChapter.put(chapterId, Math.max(0, pageIndex));
    }

    public List<String> recent() {
        return List.copyOf(recent);
    }

    public int lastPage(String chapterId) {
        return lastPageByChapter.getOrDefault(chapterId, 0);
    }

    public String searchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery == null ? "" : searchQuery;
    }
}
