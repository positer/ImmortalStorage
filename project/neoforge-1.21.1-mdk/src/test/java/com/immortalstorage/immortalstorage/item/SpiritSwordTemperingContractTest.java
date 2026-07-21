package com.immortalstorage.immortalstorage.item;

import com.immortalstorage.immortalstorage.item.custom.SpiritSwordTempering;
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

        Path recipes = locateResources().resolve("data/immortalstorage/recipe");
        for (String name : new String[]{"spirit_sword_tempering_smelting.json",
                "spirit_sword_tempering_blasting.json",
                "spirit_sword_tempering_immortal_furnace.json"}) {
            String json = Files.readString(recipes.resolve(name));
            assertTrue(json.contains("\"experience\": 0.0"));
            assertTrue(json.contains("\"tag\": \"immortalstorage:spirit_swords\""));
        }
        String tag = Files.readString(locateResources().resolve(
                "data/immortalstorage/tags/item/spirit_swords.json"));
        assertTrue(tag.contains("immortalstorage:spirit_sword"));
        assertTrue(tag.contains("immortalstorage:immortal_ruin_forged_spirit_sword"));
    }

    private static Path locateMain() { return locate("src/main/java/com/immortalstorage/immortalstorage"); }
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
