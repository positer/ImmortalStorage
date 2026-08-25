package com.immortalstorage.immortalstorage.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImmortalArtifactAdaptiveToolContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void miningModeSelectsFromRealToolResponsesAndKeepsForcedShearing() throws IOException {
        Path source = locateProject().resolve(
                "src/main/java/com/immortalstorage/immortalstorage/item/custom/SpiritStaffItem.java");
        String text = Files.readString(source);
        assertTrue(text.contains("effectiveMiningScore"));
        assertTrue(text.contains("tool.getDestroySpeed(state)"));
        assertTrue(text.contains("tool.isCorrectToolForDrops(state)"));
        assertTrue(text.contains("Items.NETHERITE_AXE"));
        assertTrue(text.contains("Items.NETHERITE_SHOVEL"));
        assertTrue(text.contains("Items.NETHERITE_HOE"));
        assertTrue(text.contains("Items.SHEARS"));
        assertTrue(text.contains("shearable.onSheared"));
        assertTrue(!text.contains("shearable.isShearable"));
        assertTrue(text.contains("ItemAbility.get(\"pickaxe_dig\")"));
        assertTrue(text.contains("ItemAbility.get(\"axe_dig\")"));
        assertTrue(text.contains("ItemAbility.get(\"shovel_dig\")"));
        assertTrue(text.contains("ItemAbility.get(\"hoe_dig\")"));
        assertTrue(text.contains("ItemAbility.get(\"shears_dig\")"));
        assertTrue(!text.contains("hoe_till"));
        assertTrue(!text.contains("shovel_flatten"));
        assertTrue(!text.contains("shovel_douse"));
        assertTrue(!text.contains("axe_strip"));
        assertTrue(!text.contains("axe_scrape"));
        assertTrue(!text.contains("axe_wax_off"));

        String client = Files.readString(locateProject().resolve(
                "src/main/java/com/immortalstorage/immortalstorage/client/ClientSetup.java"));
        assertTrue(client.contains("EntityHitResult entityHit"));
        assertTrue(client.contains("entityHit.getEntity() instanceof net.neoforged.neoforge.common.IShearable"));
        assertTrue(client.contains("return 4.0F"));
        assertTrue(client.contains("\"immortal_artifact_shovel\""));
        assertTrue(client.contains(".standalone("));
        assertTrue(text.contains("speed + (tool.isCorrectToolForDrops(state) ? 0.001F : 0.0F)"));
    }

    @Test
    void artifactWrenchAloneAddsFiveBlocksOfEntityReach() throws IOException {
        String item = Files.readString(locateProject().resolve(
                "src/main/java/com/immortalstorage/immortalstorage/item/custom/SpiritStaffItem.java"));
        assertTrue(item.contains("Attributes.ENTITY_INTERACTION_RANGE"));
        assertTrue(item.contains("Attributes.BLOCK_INTERACTION_RANGE"));
        assertTrue(item.contains("ARTIFACT_WRENCH_ENTITY_REACH_ID, 5.0D"));
        assertTrue(item.contains("ARTIFACT_WRENCH_BLOCK_REACH_ID, 5.0D"));
        assertTrue(item.contains("stack.getItem() instanceof ImmortalArtifactItem && getMode(stack) == MODE_WRENCH"));
        assertTrue(item.contains("getDefaultAttributeModifiers(ItemStack stack)"));
        assertTrue(item.contains("ItemAttributeModifiers.builder()"));
        assertTrue(item.contains("EquipmentSlotGroup.HAND"));
        assertTrue(item.contains("canInteractWithBlock("));
        assertTrue(item.contains("context.getClickedPos(), 1.0D"));
        assertTrue(!item.contains("player.distanceToSqr(context.getClickedPos().getCenter()) > 64.0D"));
        assertTrue(!item.contains("tickArtifactWrenchReach"));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project");
    }
}
