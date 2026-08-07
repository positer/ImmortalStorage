package com.immortalstorage.immortalstorage.block.entity;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceResourceTest {
    private static final Path PROJECT = locateProject();
    private static final Path RESOURCES = PROJECT.resolve("src/main/resources");

    @Test
    void shapedRecipeAndFacingAssetsMatchTheGoalContract() throws IOException {
        String recipe = resource("data/immortalstorage/recipe/xianqiao_interface.json");
        assertTrue(recipe.contains("\"ICI\""));
        assertTrue(recipe.contains("\"CSC\""));
        assertTrue(recipe.contains("\"immortalstorage:spirit_iron_block\""));
        assertTrue(recipe.contains("\"immortalstorage:spirit_crystal\""));
        assertTrue(recipe.contains("\"immortalstorage:spirit_core\""));
        assertTrue(recipe.contains("\"immortalstorage:xianqiao_interface\""));

        String blockstate = resource("assets/immortalstorage/blockstates/xianqiao_interface.json");
        for (String facing : List.of("north", "east", "south", "west")) {
            assertTrue(blockstate.contains("facing=" + facing));
        }
        String itemModel = resource("assets/immortalstorage/models/item/xianqiao_interface.json");
        assertTrue(itemModel.contains("immortalstorage:block/xianqiao_interface"));
    }

    @Test
    void originalBlockTexturesArePixelSizedAndLanguageHasNoPlaceholderCopy() throws IOException {
        for (String name : List.of("panel", "side", "top")) {
            Path texture = RESOURCES.resolve(
                    "assets/immortalstorage/textures/block/xianqiao_interface_" + name + ".png");
            BufferedImage image = ImageIO.read(texture.toFile());
            assertNotNull(image, texture.toString());
            assertEquals(16, image.getWidth(), texture.toString());
            assertEquals(16, image.getHeight(), texture.toString());
        }

        for (String language : List.of("zh_cn", "en_us")) {
            String text = resource("assets/immortalstorage/lang/" + language + ".json");
            assertTrue(text.contains("\"block.immortalstorage.xianqiao_interface\""));
            assertTrue(text.contains("\"container.immortalstorage.xianqiao_interface.targets\""));
            assertTrue(text.contains("\"container.immortalstorage.xianqiao_interface.buffers\""));
            assertTrue(text.contains("\"message.immortalstorage.xianqiao_interface.not_owner\""));
            assertFalse(text.contains(
                    "\"container.immortalstorage.xianqiao_interface.ae2_style_pending\""));
            assertFalse(text.contains("待完成"));
        }
    }

    private static String resource(String relative) throws IOException {
        return Files.readString(RESOURCES.resolve(relative));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("build.gradle"))
                    && Files.isDirectory(current.resolve("src/main/resources"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project root");
    }
}
