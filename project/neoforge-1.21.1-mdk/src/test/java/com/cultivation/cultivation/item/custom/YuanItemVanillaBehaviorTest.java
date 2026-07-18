package com.cultivation.cultivation.item.custom;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class YuanItemVanillaBehaviorTest {
    private static final Path MAIN_SOURCES = locateMainSources();
    private static final Set<String> FORBIDDEN_LIFECYCLE_OVERRIDES = Set.of(
            "inventoryTick", "onDestroyed", "onEntityItemUpdate");

    @Test
    void yuanItemsKeepVanillaPickupTossInventoryAndEntityBehavior() {
        assertVanillaLifecycle(TrueYuanItem.class);
        assertVanillaLifecycle(ImmortalYuanItem.class);
    }

    @Test
    void bothRegistrationsUseTheNormalSixtyFourItemStackLimit() throws IOException {
        String source = Files.readString(MAIN_SOURCES.resolve("item/ModItems.java"));

        assertTrue(source.contains("new TrueYuanItem(ModBlocks.TRUE_YUAN_LIGHT.get()"));
        assertTrue(source.contains("new ImmortalYuanItem(ModBlocks.IMMORTAL_YUAN_LIGHT.get()"));
        assertTrue(source.contains("new Item.Properties().stacksTo(64)"));
        assertFalse(source.contains("new TrueYuanItem(p.stacksTo(1))"));
        assertFalse(source.contains("new ImmortalYuanItem(p.stacksTo(1))"));
    }

    private static void assertVanillaLifecycle(Class<?> itemClass) {
        for (Method method : itemClass.getDeclaredMethods()) {
            assertFalse(FORBIDDEN_LIFECYCLE_OVERRIDES.contains(method.getName()),
                    () -> itemClass.getSimpleName() + " must not override " + method.getName());
        }
        assertFalse(java.util.Arrays.stream(itemClass.getDeclaredClasses())
                        .anyMatch(nested -> nested.getSimpleName().equals("Events")),
                () -> itemClass.getSimpleName() + " must not register pickup/toss cancellation events");
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
