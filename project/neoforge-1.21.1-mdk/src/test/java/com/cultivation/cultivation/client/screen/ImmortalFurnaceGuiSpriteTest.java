package com.cultivation.cultivation.client.screen;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImmortalFurnaceGuiSpriteTest {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int SHADOW = 0xFF8B8B8B;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int CYAN_MID = 0xFF6FD9E0;
    private static final int CYAN_BRIGHT = 0xFFEAFFFF;
    private static final int CYAN_DARK = 0xFF2A7E8B;

    /** Exact geometry of the Minecraft 1.21.1 furnace lit-progress sprite. */
    private static final String[] EXPECTED_GEOMETRY = {
            ".D.........D..",
            ".MD...D...DM#.",
            "..M...M...M.#.",
            ".DB#..BD..BD..",
            ".MB#...M..BM..",
            ".BW#..DM#.WB#.",
            "DBM#..BM#.MBD.",
            "MWD#.DWB#.DBM#",
            "BW#..MBD#..WB#",
            "WWM..MW##.MWB#",
            "DWB#.BW#..BWD#",
            ".WW#.BWB..WW#.",
            "MWM#.MWW#.MWM.",
            ".###..###..###"
    };

    @Test
    void cyanWhiteSpritePreservesEveryVanillaPixelPosition() throws IOException {
        Path sprite = locateProjectRoot().resolve(
                "src/main/resources/assets/cultivation/textures/gui/sprites/container/immortal_furnace/lit_progress.png");
        assertTrue(Files.isRegularFile(sprite), "missing generated cyan-white furnace sprite");

        BufferedImage image = ImageIO.read(sprite.toFile());
        assertEquals(14, image.getWidth());
        assertEquals(14, image.getHeight());

        Map<Integer, Character> symbols = Map.of(
                PANEL, '.',
                SHADOW, '#',
                WHITE, 'W',
                CYAN_MID, 'M',
                CYAN_BRIGHT, 'B',
                CYAN_DARK, 'D');
        for (int y = 0; y < image.getHeight(); y++) {
            StringBuilder actual = new StringBuilder(14);
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                assertEquals(0xFF, argb >>> 24, "alpha changed at " + x + "," + y);
                Character symbol = symbols.get(argb);
                assertFalse(symbol == null,
                        "unexpected palette colour 0x" + Integer.toHexString(argb)
                                + " at " + x + "," + y);
                actual.append(symbol);
            }
            assertEquals(EXPECTED_GEOMETRY[y], actual.toString(), "pixel row " + y);
        }
    }

    @Test
    void generatorPinsTheVanillaSourceAndOnlyRecoloursFireBands() throws IOException {
        String generator = Files.readString(locateProjectRoot()
                .resolve("tools/generate_immortal_furnace_gui_sprite.py"));

        assertTrue(generator.contains("32f69838e8fbf0b980ec3f8b205d0ddb5f477fcf6202feddbd1b6a0b4524b6eb"));
        assertTrue(generator.contains("assets/minecraft/textures/gui/sprites/container/furnace/lit_progress.png"));
        assertTrue(generator.contains("SOURCE_FIRE_PALETTE"));
    }

    private static Path locateProjectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("src/main/resources/assets/cultivation"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate NeoForge project root from "
                + Path.of("").toAbsolutePath());
    }
}
