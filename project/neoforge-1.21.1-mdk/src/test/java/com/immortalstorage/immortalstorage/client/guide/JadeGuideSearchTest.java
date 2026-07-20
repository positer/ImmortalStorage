package com.immortalstorage.immortalstorage.client.guide;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class JadeGuideSearchTest {
    private static JadeGuideSearch.Document document(String id, String title, String body, String... keywords) {
        return new JadeGuideSearch.Document(id, title, body, List.of(keywords), true);
    }

    @Test
    void localizedSearchMatchesTitleBodyAndKeywords() {
        List<JadeGuideSearch.Document> documents = List.of(
                document("staff", "Spirit Staff", "Four practical modes", "wrench", "build"),
                document("storage", "Xianqiao Storage", "Smooth item and fluid terminal", "scroll"));

        assertEquals("staff", JadeGuideSearch.search(documents, "spirit").getFirst().chapterId());
        assertEquals("storage", JadeGuideSearch.search(documents, "fluid").getFirst().chapterId());
        assertEquals("staff", JadeGuideSearch.search(documents, "wrench").getFirst().chapterId());
    }

    @Test
    void chineseSearchMatchesContinuousText() {
        List<JadeGuideSearch.Document> documents = List.of(
                document("tribulation", "仙窍渡劫", "只能在个人仙窍维度中进行", "进阶"));

        assertEquals("tribulation", JadeGuideSearch.search(documents, "仙窍维度").getFirst().chapterId());
    }
}
