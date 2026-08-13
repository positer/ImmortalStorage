package com.immortalstorage.immortalstorage.source;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression boundary for the cache-backed, configuration-only source-vein UI. */
final class SourceVeinArchitectureContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final Path MAIN = locateMainSources();
    private static final Path RESOURCES = locateMainResources();

    @Test
    void sourceScreenUsesTheSpecificLocalizedBlockNameWithoutOverflowingTheVanillaHeader() throws IOException {
        String entity = source("block", "entity", "SourceVeinBlockEntity.java");
        String screen = source("client", "screen", "SourceVeinScreen.java");
        String zhCn = Files.readString(RESOURCES.resolve(Path.of(
                "assets", "immortalstorage", "lang", "zh_cn.json")));
        String enUs = Files.readString(RESOURCES.resolve(Path.of(
                "assets", "immortalstorage", "lang", "en_us.json")));

        assertTrue(methodBody(entity, "public Component getDisplayName()")
                        .contains("getBlockState().getBlock().getName()"),
                "the menu provider title must be the opened source block's localized name");
        assertTrue(screen.contains("TITLE_MAX_WIDTH = 160"),
                "the title must stay inside the 176px vanilla panel with 8px padding on both sides");
        assertTrue(methodBody(screen, "private Component fittedTitle()")
                        .contains("plainSubstrByWidth"),
                "long localized names must be width-limited instead of overflowing at any GUI scale");
        assertTrue(methodBody(screen, "private void renderTitleTooltip")
                        .contains("this.title"),
                "the complete source name must remain available when the fitted title is truncated");
        assertTrue(zhCn.contains("\"block.immortalstorage.water_vein\": \"水源方块\""));
        assertTrue(enUs.contains("\"block.immortalstorage.enchanted_golden_apple_vein\": "
                + "\"Enchanted Golden Apple Source Vein\""));
    }

    @Test
    void sourceMenuAndScreenExposeSixSideConfigurationAndExactThroughputInput() throws IOException {
        String menu = source("menu", "custom", "SourceVeinMenu.java");
        String screen = source("client", "screen", "SourceVeinScreen.java");

        assertFalse(menu.contains("addSlot("), "the source menu must not register source or player item slots");
        assertTrue(screen.contains("EditBox"), "throughput must be directly editable from the source screen");
        assertFalse(screen.contains("AdjustSourceFlux"));
        assertTrue(screen.contains("SetSourceFluxLimit"));
        assertTrue(menu.contains("getFluxLimit()"), "the authoritative rate must synchronize back to the editor");
        assertFalse(screen.contains("VanillaGuiPainter.slot("));
        assertFalse(screen.contains("VanillaGuiPainter.slots("));
        assertTrue(screen.contains("Direction.UP, Direction.NORTH, Direction.DOWN"));
        assertTrue(screen.contains("Direction.WEST, Direction.SOUTH, Direction.EAST"));
        assertTrue(menu.contains("isSideFaulted"), "per-face fault state must synchronize to the client menu");
        assertTrue(menu.contains("getSideUncertainInFlight"),
                "the client must see the exact uncertain transfer amount for each face");
        assertTrue(screen.contains("side_tooltip_fault"),
                "the affected face tooltip must visibly explain its frozen uncertain transaction");
    }

    @Test
    void everyExtractionPathUsesThePersistentCacheInsteadOfChargingOnDemand() throws IOException {
        String entity = source("block", "entity", "SourceVeinBlockEntity.java");

        assertTrue(entity.contains("SourceVeinBuffer buffer"));
        assertTrue(entity.contains("buffer.save(tag)"));
        assertTrue(entity.contains("buffer.load(tag)"));
        assertTrue(methodBody(entity, "private long extractWithinFlux")
                .contains("buffer.extract("));
        assertTrue(methodBody(entity, "private final class SourceItemHandler")
                .contains("extractWithinFlux("));
        assertFalse(methodBody(entity, "private final class SourceItemHandler")
                .contains("chargeOwnerForOutput"));
        assertTrue(methodBody(entity, "private final class SourceFluidHandler")
                .contains("extractWithinFlux("));
        assertFalse(methodBody(entity, "private final class SourceFluidHandler")
                .contains("chargeOwnerForOutput"));
    }

    @Test
    void standardCapabilitiesArePublishedOnEveryFaceIndependentlyOfActiveOutputModes() throws IOException {
        String registrations = source("block", "entity", "ModBlockEntities.java");
        String itemRegistration = registrationFor(
                registrations, "Capabilities.Item.BLOCK, SOURCE_VEIN.get()");
        String fluidRegistration = registrationFor(
                registrations, "Capabilities.Fluid.BLOCK, SOURCE_VEIN.get()");

        assertTrue(itemRegistration.contains("be.getItemHandler(side)"));
        assertFalse(itemRegistration.contains("canExtractFrom"),
                "DISABLED controls active output and must not hide the item capability");
        assertTrue(fluidRegistration.contains("be.getFluidHandler(side)"));
        assertFalse(fluidRegistration.contains("canExtractFrom"),
                "DISABLED controls active output and must not hide the fluid capability");
    }

    @Test
    void shiftExtractionAndOfficialMilkFluidAreWiredAtTheRuntimeBoundary() throws IOException {
        String block = source("block", "custom", "SourceVeinBlock.java");
        String entity = source("block", "entity", "SourceVeinBlockEntity.java");
        String definitions = source("source", "definition", "SourceDefinitions.java");
        String mod = source("ImmortalStorageMod.java");

        assertTrue(methodBody(block, "public InteractionResult useWithoutItem")
                .contains("takeManualBatch"));
        assertTrue(methodBody(block, "public InteractionResult useWithoutItem")
                .contains("isShiftKeyDown"));
        assertTrue(mod.contains("NeoForgeMod.enableMilkFluid()"));
        assertTrue(definitions.contains("case MILK -> \"milk\""),
                "the data-driven builtin milk source must resolve NeoForge's official milk fluid id");
        assertTrue(methodBody(entity, "public Fluid sampleFluid()")
                        .contains("BuiltInRegistries.FLUID.get(definition.outputId()).map(net.minecraft.core.Holder.Reference::value).orElse(null)"),
                "runtime source output must resolve the validated fluid definition through the official registry");
        assertTrue(methodBody(entity, "public ItemStack filledVanillaContainer()")
                        .contains("NeoForgeMod.MILK.value()"),
                "vanilla bucket interaction must compare against NeoForge's official milk fluid holder");
        assertTrue(entity.contains("case NETHER_STAR -> Items.NETHER_STAR"));
    }

    @Test
    void bucketWithdrawalIsSimulatedBeforeCommitAndRollsBackAnyPartialCommit() throws IOException {
        String block = source("block", "custom", "SourceVeinBlock.java");
        String interaction = methodBody(block, "protected InteractionResult useItemOn");

        int simulate = interaction.indexOf("IFluidHandler.FluidAction.SIMULATE");
        int execute = interaction.indexOf("IFluidHandler.FluidAction.EXECUTE");
        assertTrue(simulate >= 0, "a bucket must first simulate a complete 1000 mB withdrawal");
        assertTrue(execute > simulate, "the real withdrawal must happen only after simulation succeeds");
        assertTrue(interaction.contains("rollbackFluidExtraction"),
                "an unexpected partial execute must restore prepaid cache and tick budget");
    }

    @Test
    void everySourceDropCarriesItsPersistentBlockEntityState() throws IOException {
        String block = source("block", "custom", "SourceVeinBlock.java");
        String staff = source("item", "custom", "SpiritStaffItem.java");
        String drops = methodBody(block, "protected List<ItemStack> getDrops");

        assertTrue(drops.contains("LootContextParams.BLOCK_ENTITY"));
        assertTrue(drops.contains("new ItemStack(this)"),
                "an explosion-decayed or otherwise empty vanilla loot result must not swallow prepaid cache");
        assertTrue(drops.contains("source.saveToItem"),
                "vanilla saveToItem writes owner/cache/flux/sides into BLOCK_ENTITY_DATA");
        String preciseHarvest = methodBody(
                staff, "private static InteractionResult preciseHarvest(");
        assertTrue(preciseHarvest.contains("player.gameMode.destroyBlock(pos)"),
                "spirit-staff precise collection must use the vanilla break path that invokes this drop override");
        assertFalse(preciseHarvest.contains("Block.getDrops("),
                "the staff must not duplicate source-drop construction outside the block's vanilla loot path");
        assertFalse(preciseHarvest.contains("setBlock("),
                "the staff must not bypass normal destruction by replacing the source directly");
    }

    @Test
    void loadedRealmSourcesShareOneVirtualDirectoryWithTerminalAndOfficialStorageViews() throws IOException {
        String entity = source("block", "entity", "SourceVeinBlockEntity.java");
        String index = source("network", "storage", "SourceVeinStorageIndex.java");
        String longItems = source("network", "storage", "PersonalStorageLongItemStorage.java");
        String fluids = source("network", "storage", "PersonalStorageFluidHandler.java");
        String menu = source("menu", "custom", "XianqiaoStorageMenu.java");

        assertTrue(methodBody(entity, "public void onLoad()")
                .contains("SourceVeinStorageIndex.register(this)"));
        assertTrue(methodBody(entity, "public void setRemoved()")
                .contains("SourceVeinStorageIndex.unregister(this)"));
        assertTrue(methodBody(entity, "public boolean isVisibleInXianqiaoStorage")
                .contains("ImmortalStorageDimensions.isPersonalRealmFor"));
        assertTrue(methodBody(entity, "public long extractForXianqiaoStorage")
                .contains("extractWithinFlux"));
        assertTrue(index.contains("prototype.copyWithCount(1)"),
                "source amounts must remain virtual instead of allocating huge ItemStack lists");
        assertTrue(index.contains("Long.MAX_VALUE"),
                "the native directory must preserve the authoritative long source amount");
        assertTrue(methodBody(entity, "private void refillCacheForAutomation")
                        .contains("buffer.fillToCapacityWithoutCharge()"),
                "free sources must materialize the compatibility maximum in their real persistent cache");
        assertTrue(longItems.contains("SourceVeinStorageIndex.itemSnapshot"));
        assertTrue(longItems.contains("SourceVeinStorageIndex.extractItem"));
        assertTrue(fluids.contains("SourceVeinStorageIndex.fluidSnapshot"));
        assertTrue(fluids.contains("SourceVeinStorageIndex.extractFluid"));
        assertTrue(methodBody(menu, "private boolean rebuildCatalog()")
                .contains("itemStorage.snapshot()"));
    }

    @Test
    void sourceItemsUseTheSharedDynamicOutputRuleWithoutChangingTheirBaseModel() throws IOException {
        String setup = source("client", "ClientSetup.java");
        String renderer = source("client", "render", "SourceVeinItemRenderer.java");
        String decorator = source("client", "render", "SourceVeinOutputDecorator.java");
        String blockItem = source("item", "SourceVeinBlockItem.java");

        assertTrue(setup.contains("RegisterSpecialModelRendererEvent"));
        assertTrue(setup.contains("source_vein"));
        assertTrue(setup.contains("SourceVeinItemRenderer.Unbaked.MAP_CODEC"));
        assertTrue(renderer.contains("implements SpecialModelRenderer<ItemStack>"));
        assertTrue(renderer.contains("implements SpecialModelRenderer<ItemStack>"));
            assertTrue(renderer.contains("SourceVeinDisplayRenderer.renderForItem(")
                    || renderer.contains("SourceVeinDisplayRenderer.submitForItem(")
                    || renderer.contains("SourceVeinDisplayRenderer.submit("),
                    "source items must share the world dynamic-output rule renderer");
        assertFalse(renderer.contains("GUI_ITEM_SCALE")
                        || renderer.contains("ITEM_PREVIEW_SCALE"),
                "the source vein item's established preview size must remain unchanged");
        assertFalse(renderer.contains("FluidUtil.getFilledBucket(new FluidStack(fluid, 1_000))"),
                "fluid sources must render their dynamic fluid material instead of a bucket proxy");
        assertTrue(renderer.contains("SpecialModelGeometry.submitBlockBase"),
                "the source item must retain its normal base model");
        String displayRenderer = source("client", "render", "SourceVeinDisplayRenderer.java");
        assertTrue(displayRenderer.contains("IClientFluidTypeExtensions")
                        || displayRenderer.contains("FluidModel"));
        assertTrue(displayRenderer.contains("BlockItem"));
        assertTrue(displayRenderer.contains("renderItem(")
                        || displayRenderer.contains("renderOutputItem(")
                        || displayRenderer.contains("submitItem(")
                        || displayRenderer.contains("submitNestedItem"));
        assertFalse(renderer.contains("base.getTransforms().getTransform(context).apply"),
                "ItemRenderer already applies the custom model transform before entering the BEWLR");
        assertFalse(renderer.contains("poseStack.translate(-0.5F, -0.5F, -0.5F)"),
                "ItemRenderer already centers the custom model before entering the BEWLR");
        assertTrue(setup.contains("RegisterItemDecorationsEvent"));
        assertTrue(setup.contains("SourceVeinOutputDecorator.INSTANCE"),
                "inventory, creative, JEI and EMI slots must share the standard decoration path");
        assertFalse(decorator.contains("enableScissor"),
                "decorators must inherit caller clipping because scissor ignores PoseStack translations");
        assertFalse(decorator.contains("disableScissor"),
                "decorators must not pop a caller-owned scissor stack");

    }

    private static String source(String... relative) throws IOException {
        Path path = MAIN;
        for (String segment : relative) path = path.resolve(segment);
        return Files.readString(path);
    }

    private static Path locateMainSources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of(
                    "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94", "src", "test", "compat-source", "com", "immortalstorage", "immortalstorage"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage main sources from "
                + Path.of("").toAbsolutePath());
    }

    private static Path locateMainResources() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve(Path.of("src", "main", "resources"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage main resources from "
                + Path.of("").toAbsolutePath());
    }

    private static String methodBody(String source, String signature) {
        int name = source.indexOf(signature);
        if (name < 0) return "";
        int opening = source.indexOf('{', name);
        if (opening < 0) return "";
        int depth = 0;
        for (int index = opening; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') depth++;
            if (current == '}' && --depth == 0) return source.substring(opening, index + 1);
        }
        return "";
    }

    private static String registrationFor(String source, String capabilityAndType) {
        int start = source.indexOf(capabilityAndType);
        if (start < 0) return "";
        int end = source.indexOf(";", start);
        return end < 0 ? source.substring(start) : source.substring(start, end + 1);
    }
}
