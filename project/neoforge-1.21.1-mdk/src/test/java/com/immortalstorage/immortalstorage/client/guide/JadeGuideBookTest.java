package com.immortalstorage.immortalstorage.client.guide;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JadeGuideBookTest {
    @Test
    void catalogHasFiveStableCategoriesAndUniqueChapterIds() {
        JadeGuideBook book = JadeGuideBook.defaultBook();

        assertEquals(List.of("progression", "storage", "resources", "tools", "realm_compat"),
                book.categories().stream().map(JadeGuideCategory::id).toList());
        assertEquals(book.chapters().size(), book.chaptersById().size());
        assertTrue(book.chaptersById().containsKey("progression.overview"));
        assertTrue(book.chaptersById().containsKey("storage.terminal"));
        assertTrue(book.chaptersById().containsKey("realm_compat.tribulation"));
    }

    @Test
    void lockedChaptersRemainDiscoverableWithAReason() {
        JadeGuideBook book = JadeGuideBook.defaultBook();
        JadeGuideChapter terminal = book.chaptersById().get("storage.terminal");

        assertFalse(terminal.isUnlocked(2));
        assertEquals("guide.immortalstorage.jade.lock.stage", terminal.lockSummaryKey());
        assertTrue(terminal.isUnlocked(6));
    }
}
