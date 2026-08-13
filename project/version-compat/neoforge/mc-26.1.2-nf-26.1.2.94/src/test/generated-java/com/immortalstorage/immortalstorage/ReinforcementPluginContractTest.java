package com.immortalstorage.immortalstorage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Release boundary for the 0.0.12 reinforcement-plugin family. */
final class ReinforcementPluginContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path PROJECT = locateProject();
    private static final Path MAIN = PROJECT.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source/com/immortalstorage/immortalstorage");
    private static final Path RESOURCES = PROJECT.resolve("src/main/resources");

    @Test
    void threeTiersAndUpgradeOnlyInstallationUseOneSharedContract() throws IOException {
        String items = source("item/ModItems.java");
        String host = source("block/entity/ReinforcementPluginHost.java");
        String events = source("event/CommonEvents.java");

        assertTrue(items.contains("new ReinforcementPluginItem(p, 4)"));
        assertTrue(items.contains("new ReinforcementPluginItem(p, 16)"));
        assertTrue(items.contains("new ReinforcementPluginItem(p, 256)"));
        assertTrue(host.contains("offered <= multiplier(current)"));
        assertTrue(host.contains("player.getInventory().add(current.copy())"));
        assertTrue(host.contains("player.drop(current.copy(), false)"));
        assertTrue(events.contains("tryInstallReinforcement"));
    }

    @Test
    void multipliersNeverScaleFuelDurationAndStopBlockedRuinLoopsEarly() throws IOException {
        String engine = source("menu/custom/ImmortalFurnaceEngine.java");
        String crystal = source("block/entity/EnergyCrystalBlockEntity.java");
        String field = source("block/entity/SimulatedSpiritFieldBlockEntity.java");
        String reincarnation = source("block/entity/SimulatedReincarnationFurnaceBlockEntity.java");
        String fieldMenu = source("menu/custom/SimulatedSpiritFieldMenu.java");
        String reincarnationMenu = source("menu/custom/SimulatedReincarnationFurnaceMenu.java");
        String embeddedFurnace = source("menu/custom/EmbeddedImmortalFurnaceBackend.java");
        String stable = source("block/entity/StabilizedMiniatureImmortalRuinBlockEntity.java");
        String advanced = source("block/entity/AdvancedStabilizedMiniatureImmortalRuinBlockEntity.java");

        assertTrue(engine.contains("litTime--"));
        assertTrue(engine.contains("progress[channel] + processingMultiplier"));
        assertTrue(embeddedFurnace.contains("this::isRecallReserved, reinforcementMultiplier()"));
        assertTrue(field.contains("items.get(SEED_SLOT).getCount()) * reinforcementMultiplier()"));
        assertTrue(reincarnation.contains("getStackInSlot(SOURCE_SLOT).getCount())\n                * reinforcementMultiplier()"));
        assertTrue(field.contains("PLUGIN_SLOT = TOOL_SLOT"));
        assertTrue(reincarnation.contains("PLUGIN_SLOT = WEAPON_SLOT"));
        assertTrue(!fieldMenu.contains("new PluginSlot"));
        assertTrue(!reincarnationMenu.contains("95, 62"));
        assertTrue(crystal.contains("configuredOutput(crystal.kind), crystal.reinforcementMultiplier()"));
        assertTrue(crystal.contains("!hasReinforcementPlugin() && chargedQuartzOutput(stack) != null"));
        assertTrue(crystal.contains("!ReinforcementPluginHost.isPlugin(stack)"));
        assertTrue(stable.contains("if (!eject(serverLevel)) break"));
        assertTrue(advanced.contains(")) break;"));
    }

    @Test
    void smithingInputsMatchTheSpecifiedOrder() throws IOException {
        assertRecipe("dimensional_peeking_order", "minecraft:echo_shard",
                "minecraft:netherite_upgrade_smithing_template", "immortalstorage:spirit_crystal");
        assertRecipe("dimensional_parallel_talisman", "immortalstorage:spirit_core",
                "immortalstorage:dimensional_peeking_order", "immortalstorage:nurturing_crystal");
        assertRecipe("great_thousand_world_parallel_edict", "immortalstorage:spirit_core",
                "immortalstorage:dimensional_parallel_talisman", "immortalstorage:miniature_immortal_ruin");
    }

    @Test
    void topTierKeepsThePixelTemplateAndAddsPaleSpiralBands() throws IOException {
        Path texture = RESOURCES.resolve("assets/immortalstorage/textures/item/great_thousand_world_parallel_edict.png");
        BufferedImage image = ImageIO.read(texture.toFile());
        assertEquals(32, image.getWidth());
        assertEquals(32, image.getHeight());
        int pale = 0;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int argb = image.getRGB(x, y);
            int red = argb >>> 16 & 255, green = argb >>> 8 & 255, blue = argb & 255;
            if ((argb >>> 24) != 0 && red >= 180 && green >= 150 && blue >= 220) pale++;
        }
        assertTrue(pale >= 20, "the two front spiral bands must remain visible at native resolution");
    }

    @Test
    void bilingualBuiltInHandbookDocumentsTheMachineSpecificRules() {
        for (String locale : new String[]{"zh_cn", "en_us"}) {
            Path entry = RESOURCES.resolve("assets/immortalstorage/patchouli_books/jade_guide/" + locale
                    + "/entries/automation/reinforcement_plugins.json");
            assertTrue(Files.isRegularFile(entry), locale);
        }
    }

    private static void assertRecipe(String name, String template, String base, String addition) throws IOException {
        JsonObject json = JsonParser.parseString(Files.readString(RESOURCES.resolve(
                "data/immortalstorage/recipe/" + name + ".json"))).getAsJsonObject();
        assertEquals("minecraft:smithing_transform", json.get("type").getAsString());
        assertEquals(template, json.getAsJsonObject("template").get("item").getAsString());
        assertEquals(base, json.getAsJsonObject("base").get("item").getAsString());
        assertEquals(addition, json.getAsJsonObject("addition").get("item").getAsString());
    }

    private static String source(String relative) throws IOException {
        return Files.readString(MAIN.resolve(relative));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))
                    && Files.isDirectory(current.resolve("src/main/resources"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage project");
    }
}
