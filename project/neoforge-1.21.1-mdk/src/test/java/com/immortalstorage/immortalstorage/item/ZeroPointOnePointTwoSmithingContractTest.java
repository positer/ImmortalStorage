package com.immortalstorage.immortalstorage.item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ZeroPointOnePointTwoSmithingContractTest {
    private static final Path PROJECT = locateProject();

    @Test
    void vanillaAndEmbeddedSmithingKeepOnlyAncientJadeForTheTalisman() throws IOException {
        String mixin = read("src/main/java/com/immortalstorage/immortalstorage/mixin/core/SmithingMenuMixin.java");
        String embedded = read("src/main/java/com/immortalstorage/immortalstorage/menu/custom/EmbeddedSmithingBackend.java");
        assertTrue(mixin.contains("ordinal = 1"));
        assertTrue(mixin.contains("IMMORTAL_MASTER_TALISMAN"));
        assertTrue(embedded.contains("slot == BASE"));
        assertTrue(embedded.contains("IMMORTAL_MASTER_TALISMAN"));
    }

    @Test
    void transformRecipesUseTheRequestedTemplateBaseAndAddition() throws IOException {
        String talisman = read("src/main/resources/data/immortalstorage/recipe/immortal_master_talisman.json");
        String artifact = read("src/main/resources/data/immortalstorage/recipe/immortal_artifact.json");
        assertTrue(talisman.contains("\"template\": { \"item\": \"immortalstorage:spirit_core\" }"));
        assertTrue(talisman.contains("\"base\": { \"item\": \"immortalstorage:jade_guide\" }"));
        assertTrue(artifact.contains("\"base\": { \"item\": \"immortalstorage:spirit_staff\" }"));
        assertTrue(artifact.contains("\"addition\": { \"item\": \"immortalstorage:immortal_forged_alloy\" }"));
    }

    @Test
    void smithingTransformSemanticsPreserveBaseComponentsForArtifactInheritance() throws IOException {
        String recipe = read("src/main/resources/data/immortalstorage/recipe/immortal_artifact.json");
        String item = read("src/main/java/com/immortalstorage/immortalstorage/item/custom/ImmortalArtifactItem.java");
        assertTrue(recipe.contains("minecraft:smithing_transform"));
        assertTrue(item.contains("extends SpiritStaffItem"));
        assertTrue(item.contains("super(properties, true)"));
    }

    private static String read(String relative) throws IOException {
        return Files.readString(PROJECT.resolve(relative));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("src/main/java"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project");
    }
}
