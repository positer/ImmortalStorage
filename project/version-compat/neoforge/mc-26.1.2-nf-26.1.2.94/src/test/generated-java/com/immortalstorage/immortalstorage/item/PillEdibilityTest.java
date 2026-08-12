package com.immortalstorage.immortalstorage.item;

import net.minecraft.world.food.FoodProperties;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PillEdibilityTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path MOD_ITEMS = locateMainSources().resolve("item/ModItems.java");
    private static final Path IMMORTAL_PILL = locateMainSources().resolve("item/custom/ImmortalPillItem.java");

    @Test
    void everyEdiblePillAndEdibleEmbryoCanBeConsumedAtFullHunger() {
        assertFood(ModItems.pillFood(3, 0.5f), 3);
        assertFood(ModItems.pillFood(3, 0.6f), 3);
        assertFood(ModItems.pillFood(4, 0.8f), 4);
        assertFood(ModItems.pillFood(0, 0.0f), 0);
    }

    @Test
    void breakthroughEmbryoRemainsARecipeIngredientRatherThanFood() throws IOException {
        String source = Files.readString(MOD_ITEMS);
        assertTrue(source.contains(
                "BREAKTHROUGH_PILL_EMBRYO = registerItem(\"breakthrough_pill_embryo\", BreakthroughPillEmbryoItem::new)"));
        assertTrue(!source.contains("BREAKTHROUGH_PILL_EMBRYO = registerItem(\"breakthrough_pill_embryo\", p ->"));
    }

    @Test
    void registrationsApplyTheAlwaysEdiblePropertiesToEveryRequiredItem() throws IOException {
        String source = Files.readString(MOD_ITEMS);
        for (String registration : List.of(
                "new CrudePillEmbryoItem(pillProps(p, 3, 0.5f))",
                "new CrudePillItem(pillProps(p, 3, 0.6f))",
                "new RefinedPillEmbryoItem(pillProps(p, 4, 0.8f))",
                "new RefinedPillItem(pillProps(p, 4, 0.8f))",
                "new BreakthroughPillItem(pillProps(p, 0, 0.0f))",
                "new AscensionDanItem(pillProps(p, 0, 0.0f))")) {
            assertTrue(source.contains(registration), () -> "missing edible registration: " + registration);
        }
        assertTrue(source.contains("BREAKTHROUGH_PILL_EMBRYO = registerItem(\"breakthrough_pill_embryo\", BreakthroughPillEmbryoItem::new)"));
        assertTrue(Files.readString(IMMORTAL_PILL).contains(".alwaysEdible()"),
                "immortal pill must remain consumable at full hunger");
    }

    private static void assertFood(FoodProperties food, int nutrition) {
        assertEquals(nutrition, food.nutrition());
        assertTrue(food.canAlwaysEat(), "pill food must ignore full hunger");
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage main sources from "
                + Path.of("").toAbsolutePath());
    }
}
