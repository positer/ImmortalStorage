package com.immortalstorage.immortalstorage.compat.mc2612;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

/** Regression contracts for the 26.1.2-only datapack codec migration. */
final class TargetRecipeDataContractTest {
    private static final Path TARGET_ROOT = Path.of(
            "project", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94");
    private static final Path CANONICAL_ROOT = Path.of("project", "neoforge-1.21.1-mdk");

    @Test
    void targetRecipesUseStringIngredientsAndDirectEnchantments() throws IOException {
        Path workspace = locateWorkspace();
        Path targetRecipes = workspace.resolve(TARGET_ROOT).resolve(Path.of(
                "src", "main", "resources", "data", "immortalstorage", "recipe"));
        Path canonicalRecipes = workspace.resolve(CANONICAL_ROOT).resolve(Path.of(
                "src", "main", "resources", "data", "immortalstorage", "recipe"));
        long targetCount = Files.list(targetRecipes).filter(Files::isRegularFile).count();
        long canonicalCount = Files.list(canonicalRecipes).filter(Files::isRegularFile).count();
        assertEquals(canonicalCount, targetCount, "the target lane must carry every canonical recipe");

        try (var files = Files.list(targetRecipes)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                try {
                    assertNoLegacyIngredientObject(JsonParser.parseString(Files.readString(file)), file);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }

        for (String name : new String[]{"spirit_staff.json", "spirit_sword.json"}) {
            JsonObject result = json(targetRecipes.resolve(name)).getAsJsonObject("result");
            JsonObject enchantments = result.getAsJsonObject("components")
                    .getAsJsonObject("minecraft:enchantments");
            assertTrue(enchantments.has("immortalstorage:spirit_repair"));
            assertFalse(enchantments.has("levels"), "26.1 ItemEnchantments is not wrapped in levels");
        }
    }

    @Test
    void targetLootModifiersAreIndividualFilesWithItemStackIds() throws IOException {
        Path workspace = locateWorkspace();
        Path targetLoot = workspace.resolve(TARGET_ROOT).resolve(Path.of(
                "src", "main", "resources", "data", "immortalstorage", "loot_modifiers"));
        Path canonicalLoot = workspace.resolve(CANONICAL_ROOT).resolve(Path.of(
                "src", "main", "resources", "data", "immortalstorage", "loot_modifiers"));
        try (var files = Files.list(targetLoot)) {
            assertEquals(Files.list(canonicalLoot).filter(Files::isRegularFile).count(),
                    files.filter(Files::isRegularFile).count());
        }
        try (var files = Files.list(targetLoot)) {
            files.filter(Files::isRegularFile).forEach(file -> {
                try {
                    JsonObject modifier = json(file);
                    assertTrue(modifier.has("type"), file.toString());
                    if (modifier.has("item")) {
                        JsonObject item = modifier.getAsJsonObject("item");
                        assertTrue(item.has("id"), "26.1 ItemStack codec requires item.id");
                        assertTrue(item.has("components"), "26.1 ItemStack codec requires item.components");
                        assertTrue(item.get("components").isJsonObject(), "item.components must be an object");
                        assertFalse(item.has("item"));
                    }
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }
        assertFalse(Files.exists(workspace.resolve(TARGET_ROOT).resolve(Path.of(
                "src", "main", "resources", "data", "neoforge", "loot_modifiers",
                "global_loot_modifiers.json"))),
                "26.1 must not receive the 1.21.1 global loot-modifier index");
    }

    private static void assertNoLegacyIngredientObject(JsonElement element, Path file) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.size() == 1 && (object.has("item") || object.has("tag"))) {
                throw new AssertionError("legacy ingredient object remains in " + file + ": " + object);
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                assertNoLegacyIngredientObject(entry.getValue(), file);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                assertNoLegacyIngredientObject(child, file);
            }
        }
    }

    private static JsonObject json(Path file) throws IOException {
        assertTrue(Files.isRegularFile(file), "missing target resource: " + file);
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    private static Path locateWorkspace() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve(TARGET_ROOT))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate workspace for target datapack contracts");
    }
}
