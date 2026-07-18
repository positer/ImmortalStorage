package com.cultivation.cultivation.menu.custom;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression contracts for bounded, server-authoritative terminal container clicks. */
final class XianqiaoContainerClickBoundaryTest {
    private static final Path MAIN = locateMainSources();

    @Test
    void selfHandledVisualSlotPressConsumesItsMatchingRelease() throws IOException {
        String screen = Files.readString(MAIN.resolve(Path.of(
                "client", "screen", "AbstractTerminalScreen.java")));
        String clicked = methodBody(screen, "public boolean mouseClicked(");
        String released = methodBody(screen, "public boolean mouseReleased(");

        assertTrue(screen.contains("private int handledVisualSlotButton = -1;"));
        assertTrue(clicked.contains("handledVisualSlotButton = button"),
                "every screen-owned visual-slot press must remember the matching release button");
        assertTrue(released.contains("button == this.handledVisualSlotButton"));
        assertTrue(released.indexOf("return true;") < released.indexOf("super.mouseReleased"),
                "a matching release must never fall through to vanilla's second click path");
    }

    @Test
    void realSlotsDelegateToVanillaQuickCraftWhileOnlyDirectoryProxiesAreSelfHandled() throws IOException {
        String screen = Files.readString(MAIN.resolve(Path.of(
                "client", "screen", "AbstractTerminalScreen.java")));
        String clicked = methodBody(screen, "public boolean mouseClicked(");
        String dragged = methodBody(screen, "public boolean mouseDragged(");
        String released = methodBody(screen, "public boolean mouseReleased(");

        int proxyGuard = clicked.indexOf("isSelfHandledStorageSlot(visualSlot)");
        int vanillaFallback = clicked.indexOf("super.mouseClicked");
        assertTrue(proxyGuard >= 0 && proxyGuard < vanillaFallback,
                "only aggregate directory cells may bypass vanilla mouse-down handling");
        assertFalse(clicked.contains("this.slotClicked(visualSlot"),
                "crafting, furnace and player slots must enter vanilla's QUICK_CRAFT state machine");
        assertTrue(dragged.contains("super.mouseDragged"),
                "vanilla must collect each real slot crossed by the cursor");
        assertTrue(released.contains("super.mouseReleased"),
                "vanilla must emit QUICK_CRAFT start/add/end packets on release");

        String menu = Files.readString(MAIN.resolve(Path.of(
                "menu", "custom", "XianqiaoStorageMenu.java")));
        String menuClicked = methodBody(menu, "public void clicked(");
        assertTrue(menuClicked.contains("slotId >= 0 && slotId < VISIBLE_STORAGE_SLOTS"));
        assertTrue(menuClicked.contains("super.clicked(slotId, button, clickType, actor)"),
                "every real server slot must accept vanilla QUICK_CRAFT clicks");
    }

    @Test
    void largeProxyDirectoryNeverParticipatesInClientPlayerQuickMovePrediction() throws IOException {
        String menu = Files.readString(MAIN.resolve(Path.of(
                "menu", "custom", "XianqiaoStorageMenu.java")));
        String quickMove = methodBody(menu, "public ItemStack quickMoveStack(");

        assertTrue(XianqiaoStorageMenu.BUFFERED_STORAGE_SLOTS > 128,
                "the regression requires more proxies than vanilla's changed-slot map accepts");
        assertTrue(quickMove.contains("actor.level().isClientSide() && slotIndex >= PLAYER_START"));
        int serverOnlyGuard = quickMove.indexOf("actor.level().isClientSide() && slotIndex >= PLAYER_START");
        assertTrue(serverOnlyGuard < quickMove.indexOf("TerminalMenuSupport.insertXianqiao"),
                "client prediction must stop before storage insertion");
        assertTrue(serverOnlyGuard < quickMove.indexOf("rebuildCatalog()"),
                "client prediction must stop before rebuilding every proxy stack");
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

    private static String methodBody(String source, String signature) {
        int name = source.indexOf(signature);
        if (name < 0) return "";
        int opening = source.indexOf('{', name);
        if (opening < 0) return "";
        int depth = 0;
        for (int index = opening; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') depth++;
            if (current == '}' && --depth == 0) return source.substring(opening, index + 1);
        }
        return "";
    }
}
