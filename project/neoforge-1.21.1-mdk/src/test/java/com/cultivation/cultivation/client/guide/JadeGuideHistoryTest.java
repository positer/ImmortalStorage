package com.cultivation.cultivation.client.guide;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JadeGuideHistoryTest {
    @Test
    void newNavigationCutsForwardTailAndRestoresSearchState() {
        JadeGuideHistory history = new JadeGuideHistory(8);
        JadeGuideLocation home = JadeGuideLocation.home();
        JadeGuideLocation storage = JadeGuideLocation.chapter("storage.terminal", 1, "fluid");
        JadeGuideLocation staff = JadeGuideLocation.chapter("tools.staff", 0, "");

        history.push(home);
        history.push(storage);
        assertEquals(home, history.back().orElseThrow());
        assertEquals(storage, history.forward().orElseThrow());
        assertEquals("fluid", history.current().orElseThrow().searchQuery());

        history.back();
        history.push(staff);
        assertTrue(history.forward().isEmpty());
        assertEquals(staff, history.current().orElseThrow());
    }

    @Test
    void sameChapterReplacesPageInsteadOfDuplicatingHistory() {
        JadeGuideHistory history = new JadeGuideHistory(8);
        history.push(JadeGuideLocation.chapter("tools.staff", 0, ""));
        history.push(JadeGuideLocation.chapter("tools.staff", 2, ""));

        assertEquals(1, history.size());
        assertEquals(2, history.current().orElseThrow().pageIndex());
    }

    @Test
    void typedLocationsPreserveHomeCategorySearchAndChapter() {
        JadeGuideLocation home = JadeGuideLocation.home();
        JadeGuideLocation category = JadeGuideLocation.category("storage");
        JadeGuideLocation search = JadeGuideLocation.search("fluid", 2);
        JadeGuideLocation chapter = JadeGuideLocation.chapter("storage.terminal", 1, "fluid");

        assertEquals(JadeGuideLocation.Kind.HOME, home.kind());
        assertEquals(JadeGuideLocation.Kind.CATEGORY, category.kind());
        assertEquals("storage", category.targetId());
        assertEquals(JadeGuideLocation.Kind.SEARCH, search.kind());
        assertEquals("fluid", search.searchQuery());
        assertEquals(2, search.pageIndex());
        assertEquals(JadeGuideLocation.Kind.CHAPTER, chapter.kind());
        assertEquals("storage.terminal", chapter.targetId());
    }

    @Test
    void openingSearchResultThenGoingBackRestoresSearchLocation() {
        JadeGuideHistory history = new JadeGuideHistory(8);
        JadeGuideLocation search = JadeGuideLocation.search("fluid", 1);
        JadeGuideLocation result = JadeGuideLocation.chapter("storage.terminal", 0, "fluid");

        history.push(JadeGuideLocation.home());
        history.push(search);
        history.push(result);

        assertEquals(search, history.back().orElseThrow());
        assertEquals(result, history.forward().orElseThrow());
    }
}
