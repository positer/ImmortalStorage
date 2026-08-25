package com.immortalstorage.immortalstorage;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ZeroPointOnePointTwoAssetFidelityTest {
    private static final Path ROOT = locate("project/neoforge-1.21.1-mdk/src/main", "src/main");
    private static final Path SCRIPT = locate("tools/generate-0.1.2-textures.py", null);

    private static Path locate(String workspaceRelative, String moduleRelative) {
        for (Path cursor = Path.of("").toAbsolutePath(); cursor != null; cursor = cursor.getParent()) {
            Path workspace = cursor.resolve(workspaceRelative);
            if (Files.exists(workspace)) return workspace;
            if (moduleRelative != null) {
                Path module = cursor.resolve(moduleRelative);
                if (Files.exists(module)) return module;
            }
        }
        throw new IllegalStateException("Could not locate " + workspaceRelative);
    }

    @Test void talismanIsLockedToSixteenPixelReferenceSampling() throws Exception {
        BufferedImage image = ImageIO.read(ROOT.resolve(
                "resources/assets/immortalstorage/textures/item/immortal_master_talisman.png").toFile());
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());

        String script = Files.readString(SCRIPT);
        assertTrue(script.contains("codex-clipboard-678d0e23-d0bd-4817-8881-306f2abda4af.png"));
        assertTrue(script.contains("getchannel(\"A\").getbbox()"));
        assertTrue(script.contains("target_size = 16"));
        assertTrue(script.contains("Image.Resampling.BOX"));
        assertFalse(script.contains("immortal_master_talisman.png\", DEBRIS_RAMP"));
    }

    @Test void everyRecolorNamesItsRequiredImmutableSource() throws Exception {
        String script = Files.readString(SCRIPT);
        for (String source : new String[]{
                "ancient_debris_side.png", "ancient_debris_top.png", "netherite_scrap.png",
                "netherite_ingot.png", "netherite_axe.png", "netherite_shovel.png",
                "netherite_hoe.png", "shears.png"}) {
            assertTrue(script.contains(source), source);
        }
        for (String mode : new String[]{"explore", "wrench", "pick", "build", "teleport"}) {
            assertTrue(script.contains("spirit_staff_{name}.png"));
            BufferedImage source = ImageIO.read(ROOT.resolve(
                    "resources/assets/immortalstorage/textures/item/spirit_staff_" + mode + ".png").toFile());
            BufferedImage output = ImageIO.read(ROOT.resolve(
                    "resources/assets/immortalstorage/textures/item/immortal_artifact_" + mode + ".png").toFile());
            assertEquals(source.getWidth(), output.getWidth());
            assertEquals(source.getHeight(), output.getHeight());
            for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
                assertEquals(source.getRGB(x, y) >>> 24, output.getRGB(x, y) >>> 24,
                        mode + " alpha at " + x + "," + y);
            }
            assertOneToOnePaletteReplacement(source, output, mode);
        }
        assertFalse(script.contains("spyglass.png"));
        assertFalse(script.contains("flint_and_steel.png"));
    }

    private static void assertOneToOnePaletteReplacement(
            BufferedImage source, BufferedImage output, String name) {
        Map<Integer, Integer> replacement = new HashMap<>();
        Set<Integer> targets = new HashSet<>();
        for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
            int sourcePixel = source.getRGB(x, y);
            if ((sourcePixel >>> 24) == 0) continue;
            int outputPixel = output.getRGB(x, y);
            assertEquals(replacement.computeIfAbsent(sourcePixel, ignored -> outputPixel), outputPixel,
                    name + " must replace each source tone consistently");
            targets.add(outputPixel);
        }
        assertEquals(replacement.size(), targets.size(), name + " must not collapse source detail tones");
    }

    @Test void auraIconIsComposedOnlyFromVanillaBubbleAndDiamondChestplate() throws Exception {
        String script = Files.readString(SCRIPT);
        assertTrue(script.contains("textures/particle/bubble.png"));
        assertTrue(script.contains("textures/item/diamond_chestplate.png"));
        assertFalse(script.contains("px[x, y]"));
        assertFalse(script.contains("bubble = (188, 244, 255"));

        BufferedImage icon = ImageIO.read(ROOT.resolve(
                "resources/assets/immortalstorage/textures/mob_effect/spiritual_aura_guard.png").toFile());
        assertEquals(18, icon.getWidth());
        assertEquals(18, icon.getHeight());
    }

    @Test void auraElytraKeepsVanillaGeometryWithRequestedTranslucency() throws Exception {
        String script = Files.readString(SCRIPT);
        assertTrue(script.contains("textures/entity/elytra.png"));
        assertTrue(script.contains("mapped_rgba(elytra, ramp)"));

        BufferedImage elytra = ImageIO.read(ROOT.resolve(
                "resources/assets/immortalstorage/textures/entity/spiritual_aura_elytra.png").toFile());
        boolean foundTranslucentPixel = false;
        for (int y = 0; y < elytra.getHeight(); y++) for (int x = 0; x < elytra.getWidth(); x++) {
            int alpha = elytra.getRGB(x, y) >>> 24;
            if (alpha > 0) {
                assertTrue(alpha < 255, "every visible aura-elytra pixel must be translucent");
                foundTranslucentPixel = true;
            }
        }
        assertTrue(foundTranslucentPixel);
    }
}
