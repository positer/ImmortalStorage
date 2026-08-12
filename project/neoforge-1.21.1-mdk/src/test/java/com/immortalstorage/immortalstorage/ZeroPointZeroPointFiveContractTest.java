package com.immortalstorage.immortalstorage;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class ZeroPointZeroPointFiveContractTest {
    private static final Path ROOT = locateProject();
    private static String read(String relative) throws Exception {
        return Files.readString(ROOT.resolve("src/main").resolve(relative));
    }

    @Test void swordAndTemperingContract() throws Exception {
        String tempering = read("java/com/immortalstorage/immortalstorage/item/custom/SpiritSwordTempering.java");
        String sword = read("java/com/immortalstorage/immortalstorage/item/custom/OneQiReturningOriginSwordItem.java");
        assertTrue(tempering.contains("MAX_POINTS = 5_000L"));
        assertTrue(sword.contains("elapsed == 5") && sword.contains("elapsed == 25"));
        assertTrue(sword.contains("points - 10L") && sword.contains("0.50F"));
        assertTrue(sword.contains("getEntitiesOfClass") && sword.contains("clip(start, end)"));
        assertTrue(sword.contains("shouldCauseReequipAnimation"));
        assertTrue(sword.contains("slotChanged || oldStack.getItem() != newStack.getItem()"));
        assertFalse(sword.contains("sendParticles"));
    }

    @Test void oneQiSwordIsInContinuousTemperingAndBeamIsWorldRendered() throws Exception {
        String swords = read("resources/data/immortalstorage/tags/item/spirit_swords.json");
        String beam = read("java/com/immortalstorage/immortalstorage/client/render/OneQiBeamRenderer.java");
        String menu = read("java/com/immortalstorage/immortalstorage/menu/custom/SimulatedReincarnationFurnaceMenu.java");
        assertTrue(swords.contains("immortalstorage:one_qi_returning_origin_sword"));
        assertTrue(beam.contains("minecraft.level.players()"));
        assertTrue(beam.contains("handPosition") && beam.contains("RenderType.lightning()"));
        assertTrue(beam.contains("buffers.endBatch(RenderType.lightning())"));
        assertTrue(beam.contains("OneQiHeldItemMuzzle.current()"));
        assertTrue(beam.contains("muzzle.subtract(end.subtract(muzzle).normalize().scale(4.0D))"));
        assertTrue(beam.contains("phase == 1 ? 0.004D : 0.016D"));
        assertTrue(beam.contains("0.040D") && beam.contains("0.013D"));
        String muzzle = read("java/com/immortalstorage/immortalstorage/client/render/OneQiHeldItemMuzzle.java");
        assertTrue(muzzle.contains("new Vector4f(0.5F, 0.5F, 0.5F, 1.0F)"));
        assertTrue(beam.contains("renderDistance().get()") && !beam.contains("pick("));
        assertTrue(menu.contains("blockEntity == null")
                && menu.contains("blockEntity.dataAccess()"),
                "the client menu must use the block entity's synchronized switch data");
    }

    @Test void recolorSourcesAndTexturesExist() {
        Path texture = ROOT.resolve("src/main/resources/assets/immortalstorage/textures");
        assertTrue(Files.exists(texture.resolve("item/one_qi_returning_origin_sword.png")));
        assertTrue(Files.exists(texture.resolve("item/soul_catcher_base.png")));
        assertTrue(Files.exists(texture.resolve("item/soul_catcher.png")));
        for (String face : new String[]{"bottom", "top", "side_off", "side_on", "front_off", "front_on"}) {
            assertTrue(Files.exists(texture.resolve("block/simulated_reincarnation_furnace_" + face + ".png")), face);
        }
        assertTrue(Files.exists(texture.resolve("gui/container/simulated_reincarnation_furnace.png")));
    }

    @Test void oneQiSwordIsSixteenPixelHardTransparentVanillaStyle() throws Exception {
        Path path = ROOT.resolve("src/main/resources/assets/immortalstorage/textures/item/one_qi_returning_origin_sword.png");
        BufferedImage image = ImageIO.read(path.toFile());
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        int transparent = 0;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                assertTrue(alpha == 0 || alpha == 255, "texture must not contain partial alpha at " + x + "," + y);
                if (alpha == 0) transparent++;
            }
        }
        assertTrue(transparent > 0, "vanilla-style sword texture must retain transparent pixels");
    }

    @Test void furnaceRegistrationModelAndWorkingStateAreComplete() throws Exception {
        String creative = read("java/com/immortalstorage/immortalstorage/item/ModCreativeTabs.java");
        String block = read("java/com/immortalstorage/immortalstorage/block/custom/SimulatedReincarnationFurnaceBlock.java");
        String entity = read("java/com/immortalstorage/immortalstorage/block/entity/SimulatedReincarnationFurnaceBlockEntity.java");
        String model = read("resources/assets/immortalstorage/models/block/simulated_reincarnation_furnace.json");
        String workingModel = read("resources/assets/immortalstorage/models/block/simulated_reincarnation_furnace_on.json");
        String states = read("resources/assets/immortalstorage/blockstates/simulated_reincarnation_furnace.json");
        assertTrue(creative.contains("output.accept(ModBlocks.SIMULATED_REINCARNATION_FURNACE.get())"));
        assertTrue(block.contains("BlockStateProperties.LIT") && block.contains("HorizontalDirectionalBlock.FACING"));
        assertTrue(entity.contains("updateWorkingState") && entity.contains("setValue(SimulatedReincarnationFurnaceBlock.LIT"));
        assertTrue(model.contains("minecraft:block/template_vault") && model.contains("minecraft:cutout"));
        assertTrue(workingModel.contains("front_on") && workingModel.contains("side_on")
                && workingModel.contains("minecraft:cutout"));
        assertTrue(states.contains("lit=false") && states.contains("lit=true"));
    }

    @Test void furnaceOutputCacheIsExactlyFourByThree() throws Exception {
        String entity = read("java/com/immortalstorage/immortalstorage/block/entity/SimulatedReincarnationFurnaceBlockEntity.java");
        String menu = read("java/com/immortalstorage/immortalstorage/menu/custom/SimulatedReincarnationFurnaceMenu.java");
        assertTrue(entity.contains("OUTPUT_COUNT = 12"));
        assertTrue(menu.contains("row<3") && menu.contains("col<4"));
        assertTrue(menu.contains("3 + row*4 + col"));
        assertTrue(menu.contains("mayPlace(ItemStack stack) { return false; }"));
    }

    @Test void furnacePreviewAndAutomaticOwnerDeliveryMatchContract() throws Exception {
        String entity = read("java/com/immortalstorage/immortalstorage/block/entity/SimulatedReincarnationFurnaceBlockEntity.java");
        String renderer = read("java/com/immortalstorage/immortalstorage/client/render/SimulatedReincarnationFurnaceRenderer.java");
        String screen = read("java/com/immortalstorage/immortalstorage/client/screen/SimulatedReincarnationFurnaceScreen.java");
        assertTrue(entity.contains("killer.giveExperiencePoints(xp")
                && entity.contains("effectiveXianqiaoOwner"));
        assertTrue(renderer.contains("SOURCE_SLOT") && renderer.contains("Axis.YP.rotationDegrees"));
        assertTrue(screen.contains("FacePreviewButton") && screen.contains("adjacentBlockPreview"));
    }

    @Test void creativeSoulCatcherReplacesHeldStackWithCapturedData() throws Exception {
        String catcher = read("java/com/immortalstorage/immortalstorage/item/custom/SoulCatcherItem.java");
        assertTrue(catcher.contains("ItemStack captured = stack.copy()"));
        assertTrue(catcher.contains("player.setItemInHand(hand, captured)"));
        assertTrue(catcher.contains("inventoryMenu.broadcastChanges()"));
    }

    @Test void furnaceIsPeacefulAndNonWorldSpawning() throws Exception {
        String furnace = read("java/com/immortalstorage/immortalstorage/block/entity/SimulatedReincarnationFurnaceBlockEntity.java");
        assertTrue(furnace.contains("LootContextParamSets.ENTITY"));
        assertTrue(furnace.contains("getLootTable(specimen.getLootTable())"));
        assertFalse(furnace.contains("addFreshEntity(specimen)"));
        assertFalse(furnace.contains("getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity"));
    }

    @Test void recipesAndBedrockDropMatchSpecification() throws Exception {
        String swordRecipe = read("resources/data/immortalstorage/recipe/immortal_ruin_forged_spirit_sword.json");
        String bedrockLoot = read("resources/data/immortalstorage/loot_table/blocks/nurturing_crystal_bedrock.json");
        assertTrue(swordRecipe.contains("\"template\": { \"item\": \"immortalstorage:spirit_core\" }"));
        assertTrue(swordRecipe.contains("\"addition\": { \"item\": \"immortalstorage:miniature_immortal_ruin\" }"));
        assertTrue(bedrockLoot.contains("inactive_nurturing_crystal_bedrock"));
    }

    @Test void advancementTreeContainsEveryRequestedNode() throws Exception {
        Path root = ROOT.resolve("src/main/resources/data/immortalstorage/advancement");
        assertEquals(17L, Files.list(root).filter(p -> p.toString().endsWith(".json")).count());
        String all = String.join("\n", Files.list(root).map(path -> {
            try { return Files.readString(path); } catch (Exception e) { throw new RuntimeException(e); }
        }).toList());
        String chinese = read("resources/assets/immortalstorage/lang/zh_cn.json");
        String english = read("resources/assets/immortalstorage/lang/en_us.json");
        for (String title : new String[]{"启灵", "升仙，我命由我不由天！", "代价是……？", "？！这么快！？",
                "俱往矣……", "？！科技！？", "互联", "人造古迹", "我们掌握了什么？", "万物在我手中",
                "世界原始的气息……", "回归原始", "你要干什么！", "你干了什么……", "这是什么power！？"}) {
            assertTrue(chinese.contains(title), title);
        }
        assertTrue(all.contains("advancement.immortalstorage.awakening.title"));
        assertFalse(all.contains("\"title\":\"启灵\""));
        assertTrue(english.contains("Awakening") && english.contains("Return to Origin"));
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("src/main/java/com/immortalstorage/immortalstorage"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage project");
    }
}
