package com.immortalstorage.immortalstorage.client.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JadeGuideHistory {
    private final int limit;
    private final List<JadeGuideLocation> entries = new ArrayList<>();
    private int cursor = -1;

    public JadeGuideHistory(int limit) {
        if (limit < 1) throw new IllegalArgumentException("History limit must be positive");
        this.limit = limit;
    }

    public void push(JadeGuideLocation location) {
        if (cursor + 1 < entries.size()) entries.subList(cursor + 1, entries.size()).clear();
        if (cursor >= 0 && entries.get(cursor).sameEntry(location)) {
            entries.set(cursor, location);
            return;
        }
        entries.add(location);
        if (entries.size() > limit) entries.remove(0);
        cursor = entries.size() - 1;
    }

    public Optional<JadeGuideLocation> current() {
        return cursor >= 0 && cursor < entries.size() ? Optional.of(entries.get(cursor)) : Optional.empty();
    }

    public Optional<JadeGuideLocation> back() {
        if (cursor <= 0) return Optional.empty();
        cursor--;
        return current();
    }

    public Optional<JadeGuideLocation> forward() {
        if (cursor + 1 >= entries.size()) return Optional.empty();
        cursor++;
        return current();
    }

    public boolean canBack() {
        return cursor > 0;
    }

    public boolean canForward() {
        return cursor + 1 < entries.size();
    }

    public int size() {
        return entries.size();
    }
}
