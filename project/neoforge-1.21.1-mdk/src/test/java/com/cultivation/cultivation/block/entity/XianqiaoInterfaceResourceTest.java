package com.cultivation.cultivation.block.entity;

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
        String recipe = resource("data/cultivation/recipe/xianqiao_interface.json");
        assertTrue(recipe.contains("\"ICI\""));
        assertTrue(recipe.contains("\"CSC\""));
        assertTrue(recipe.contains("\"cultivation:spirit_iron_block\""));
        assertTrue(recipe.contains("\"cultivation:spirit_crystal\""));
        assertTrue(recipe.contains("\"cultivation:spirit_core\""));
        assertTrue(recipe.contains("\"cultivation:xianqiao_interface\""));

        String blockstate = resource("assets/cultivation/blockstates/xianqiao_interface.json");
        for (String facing : List.of("north", "east", "south", "west")) {
            assertTrue(blockstate.contains("facing=" + facing));
        }
        String itemModel = resource("assets/cultivation/models/item/xianqiao_interface.json");
        assertTrue(itemModel.contains("cultivation:block/xianqiao_interface"));
    }

    @Test
    void originalBlockTexturesArePixelSizedAndLanguageHasNoPlaceholderCopy() throws IOException {
        for (String name : List.of("front", "side", "top")) {
            Path texture = RESOURCES.resolve(
                    "assets/cultivation/textures/block/xianqiao_interface_" + name + ".png");
            BufferedImage image = ImageIO.read(texture.toFile());
            assertNotNull(image, texture.toString());
            assertEquals(16, image.getWidth(), texture.toString());
            assertEquals(16, image.getHeight(), texture.toString());
        }

        for (String language : List.of("zh_cn", "en_us")) {
            String text = resource("assets/cultivation/lang/" + language + ".json");
            assertTrue(text.contains("\"block.cultivation.xianqiao_interface\""));
            assertTrue(text.contains("\"container.cultivation.xianqiao_interface.targets\""));
            assertTrue(text.contains("\"container.cultivation.xianqiao_interface.buffers\""));
            assertTrue(text.contains("\"message.cultivation.xianqiao_interface.not_owner\""));
            assertFalse(text.contains(
                    "\"container.cultivation.xianqiao_interface.ae2_style_pending\""));
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
