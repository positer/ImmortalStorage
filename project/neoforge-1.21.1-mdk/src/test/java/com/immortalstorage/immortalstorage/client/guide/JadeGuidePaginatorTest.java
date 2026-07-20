package com.immortalstorage.immortalstorage.client.guide;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JadeGuidePaginatorTest {
    @Test
    void paginationUsesMeasuredGlyphWidthAndPreservesAllWords() {
        JadeGuideTextMeasurer measurer = value -> value.codePoints()
                .map(codePoint -> codePoint > 127 ? 2 : 1)
                .sum();
        JadeGuidePaginator paginator = new JadeGuidePaginator(measurer, 8, 2);

        List<JadeGuidePage> pages = paginator.paginate("title", List.of("alpha beta gamma", "中文测试内容"));

        assertTrue(pages.size() >= 2);
        String joined = pages.stream().flatMap(page -> page.lines().stream()).reduce("", String::concat);
        assertEquals("alphabetagamma中文测试内容", joined.replace(" ", ""));
        assertTrue(pages.stream().flatMap(page -> page.lines().stream()).allMatch(line -> measurer.width(line) <= 8));
    }
}
