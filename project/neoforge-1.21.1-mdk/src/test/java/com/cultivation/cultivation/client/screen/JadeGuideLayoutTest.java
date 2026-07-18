package com.cultivation.cultivation.client.screen;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JadeGuideLayoutTest {
    @Test
    void screenUsesCenteredTwoPageHierarchyAndSharedCoordinates() throws Exception {
        String source = Files.readString(locateMainSources().resolve(
                "client/screen/JadeGuideScreen.java"));

        assertFalse(source.contains("extends BookViewScreen"));
        assertFalse(source.contains("new BookAccess(List.of"));
        assertTrue(source.contains("static final int BOOK_WIDTH = 356"));
        assertTrue(source.contains("static final int BOOK_HEIGHT = 224"));
        assertTrue(source.contains("private int bookTop()"));
        assertTrue(source.contains("return (height - BOOK_HEIGHT) / 2"));
        assertTrue(source.contains("renderBookChrome("));
        assertTrue(source.contains("RIGHT_PAGE_X"));
        assertTrue(source.contains("renderUnlockProgress("));
        assertTrue(source.contains("renderChapterPage(graphics, left + LEFT_PAGE_X"));
        assertTrue(source.contains("renderChapterPage(graphics, left + RIGHT_PAGE_X"));
        assertTrue(source.contains("JadeGuideBook.defaultBook()"));
        assertTrue(source.contains("setInitialFocus(searchBox)"));
        assertTrue(source.contains("getNarrationMessage()"));
        assertTrue(source.indexOf("renderBackground(graphics") < source.indexOf("renderPageContent(graphics"));
        assertTrue(source.indexOf("renderPageContent(graphics") < source.indexOf("renderWidgets(graphics"));
        assertTrue(source.contains("drawWrappedText("));
        assertTrue(source.contains("renderRecipePage("));
        assertTrue(source.contains("VanillaGuiPainter.slot("));
        assertTrue(source.contains("graphics.renderItem(stack"));
        assertTrue(source.contains("graphics.renderTooltip(font, hit.stack()"));
        assertTrue(source.contains("if (searchBox.isFocused())"));
        assertTrue(source.contains("restoreLocation("));
        assertTrue(source.contains("setSearchValue(session.searchQuery())"));
        assertTrue(source.contains("int y = top + CONTENT_Y"));
        assertTrue(source.contains("int recentY = top + 184"));
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "src", "main", "java", "com", "cultivation", "cultivation"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate Cultivation main sources from "
                + Path.of("").toAbsolutePath());
    }
}
