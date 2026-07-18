package com.cultivation.cultivation.item;

import com.cultivation.cultivation.item.custom.SpiritSwordTempering;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SpiritSwordTemperingContractTest {
    @Test
    void damageBonusAndDecayUseExactOnePercentAndFloorHalfRules() {
        assertEquals(5.0F, SpiritSwordTempering.bonusDamage(10.0F, 50L));
        assertEquals(0.0F, SpiritSwordTempering.bonusDamage(10.0F, 0L));
    }

    @Test
    void allFourFurnacePathsUseTheSharedComponentPreservingTemperingService() throws Exception {
        Path root = locateMain();
        String engine = Files.readString(root.resolve("menu/custom/ImmortalFurnaceEngine.java"));
        String placed = Files.readString(root.resolve("block/entity/ImmortalFurnaceBlockEntity.java"));
        String embedded = Files.readString(root.resolve("menu/custom/EmbeddedImmortalFurnaceBackend.java"));
        String vanilla = Files.readString(root.resolve("mixin/core/AbstractFurnaceSpiritSwordTemperingMixin.java"));
        assertTrue(engine.contains("plan.cycleInput()"));
        assertTrue(placed.contains("SpiritSwordTempering.temper(stack)"));
        assertTrue(embedded.contains("SpiritSwordTempering.temper(stack)"));
        assertTrue(vanilla.contains("items.set(0, SpiritSwordTempering.temper(input))"));

        Path recipes = locateResources().resolve("data/cultivation/recipe");
        for (String name : new String[]{"spirit_sword_tempering_smelting.json",
                "spirit_sword_tempering_blasting.json",
                "spirit_sword_tempering_immortal_furnace.json"}) {
            String json = Files.readString(recipes.resolve(name));
            assertTrue(json.contains("\"experience\": 0.0"));
        }
    }

    private static Path locateMain() { return locate("src/main/java/com/cultivation/cultivation"); }
    private static Path locateResources() { return locate("src/main/resources"); }
    private static Path locate(String relative) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relative);
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException(relative + " not found");
    }
}
