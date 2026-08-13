package com.immortalstorage.immortalstorage.compat.mc2612;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Regression contracts for client slot validation and recipe-book synchronization. */
final class EmbeddedFurnaceRecipeBookContractTest {
    @Test
    void embeddedFurnaceNeverCastsTheClientRecipeViewToRecipeManager() throws IOException {
        String source = Files.readString(generatedSource(
                "menu", "custom", "EmbeddedImmortalFurnaceBackend.java"));
        int methodStart = source.indexOf("boolean isRecipeInput(Player player, ItemStack stack)");
        int methodEnd = source.indexOf("public boolean summonSpiritSword", methodStart);
        String method = source.substring(methodStart, methodEnd);

        assertTrue(method.contains("player.level().isClientSide()"));
        assertTrue(method.contains("return true"));
        assertFalse(method.contains("RecipeManager"));
        assertFalse(method.contains("findRecipe(player, stack)")
                        && method.indexOf("findRecipe(player, stack)")
                        < method.indexOf("player.level().isClientSide()"),
                "the client-side guard must run before the server-only recipe scan");
    }

    @Test
    void immortalFurnaceUsesARegisteredVanillaRecipeBookCategory() throws IOException {
        String source = Files.readString(overrideSource(
                "recipe", "ImmortalFurnaceRecipe.java"));
        assertTrue(source.contains("RecipeBookCategories.FURNACE_MISC"));
        assertFalse(source.contains("new net.minecraft.world.item.crafting.RecipeBookCategory()"));
        assertFalse(source.contains("new RecipeBookCategory()"));
    }

    private static Path generatedSource(String... parts) {
        return sourceRoot("generated-java", parts);
    }

    private static Path overrideSource(String... parts) {
        return sourceRoot("java", parts);
    }

    private static Path sourceRoot(String sourceSet, String... parts) {
        Path relative = Path.of("project", "version-compat", "neoforge",
                "mc-26.1.2-nf-26.1.2.94", "src", "main", sourceSet,
                "com", "immortalstorage", "immortalstorage");
        for (String part : parts) relative = relative.resolve(part);
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(relative);
            if (Files.isRegularFile(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate 26.1.2 source: " + relative);
    }
}
