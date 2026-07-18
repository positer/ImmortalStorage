package com.cultivation.cultivation.block.entity;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoInterfaceArchitectureContractTest {
    private static final Path MAIN = locateMainSources();

    @Test
    void blockUsesAHorizontalFrontAndRejectsInvalidPlacementBeforeMutation() throws IOException {
        String block = source("block", "custom", "XianqiaoInterfaceBlock.java");

        assertTrue(block.contains("extends HorizontalDirectionalBlock"));
        assertTrue(block.contains("b.add(FACING)"));
        assertTrue(methodBody(block, "getStateForPlacement(")
                .contains("getHorizontalDirection().getOpposite()"));
        String placement = methodBody(block, "getStateForPlacement(");
        assertTrue(placement.contains("getStage() < 6"));
        assertTrue(placement.contains("canPlaceStackFor"),
                "a bound item held by another player must return null before any block is placed");
        assertFalse(placement.contains("setBlock"));
        assertFalse(block.contains("CultivationDimensions"),
                "the owner-bound interface is intentionally valid outside the personal dimension");
    }

    @Test
    void openingValidatesExistingOwnershipAndNeverClaims() throws IOException {
        String block = source("block", "custom", "XianqiaoInterfaceBlock.java");
        String entity = source("block", "entity", "XianqiaoInterfaceBlockEntity.java");
        String open = methodBody(block, "public InteractionResult useWithoutItem");

        assertTrue(open.contains("canUse"));
        assertTrue(open.contains("openMenu"));
        assertFalse(open.contains("tryBindOwner"));
        assertFalse(methodBody(entity, "createMenu(")
                .contains("tryBindOwner"));
    }

    @Test
    void serverTickVisitsAllNineCachesAndAllSixFacesUseDirectionAwareFailClosedHandlers() throws IOException {
        String entity = source("block", "entity", "XianqiaoInterfaceBlockEntity.java");
        String registrations = source("block", "entity", "ModBlockEntities.java");

        assertTrue(methodBody(entity, "public void serverTick()")
                .contains("replenishAllSlots(TerminalStorageAction.EXECUTE)"));
        assertTrue(entity.contains("PersonalStorageApi.resolveXianqiao"));
        assertTrue(registrations.contains("Capabilities.ItemHandler.BLOCK, XIANQIAO_INTERFACE.get()"));
        assertTrue(registrations.contains("Capabilities.FluidHandler.BLOCK, XIANQIAO_INTERFACE.get()"));
        assertTrue(registrations.contains("(be, side) -> be.getItemHandler(side)"),
                "the queried face must select its own item direction policy");
        assertTrue(registrations.contains("(be, side) -> be.getFluidHandler(side)"),
                "the queried face must select its own fluid direction policy");
        assertTrue(methodBody(entity, "getItemHandler(").contains("side"));
        assertTrue(methodBody(entity, "getFluidHandler(").contains("side"));
    }

    @Test
    void sideModesAndConfigurationRevisionAreOwnedAndPersistedByEachBlockEntity() throws IOException {
        String entity = source("block", "entity", "XianqiaoInterfaceBlockEntity.java");
        String save = methodBody(entity, "protected void saveAdditional(");
        String load = methodBody(entity, "protected void loadAdditional(");

        assertTrue(entity.contains("PUSH") && entity.contains("PULL") && entity.contains("DISABLED"));
        assertTrue(entity.contains("getSideMode(") && entity.contains("setSideMode("));
        assertTrue(entity.contains("getConfigRevision("));
        assertTrue(save.contains("SIDE_MODES_TAG"));
        assertTrue(save.contains("CONFIG_REVISION_TAG"));
        assertTrue(load.contains("SIDE_MODES_TAG"));
        assertTrue(load.contains("CONFIG_REVISION_TAG"));
        assertFalse(entity.contains("static final") && entity.contains("SIDE_MODES ="),
                "side modes are per placed block, never a shared global array");
    }

    @Test
    void interfaceConfigurationPayloadsRequireTheOpenOwnedMenuAndARevision() throws IOException {
        String payloads = source("network", "ModPayloads.java");
        String network = source("network", "ModNetwork.java");

        assertTrue(payloads.contains("SetXianqiaoInterfaceSideMode"));
        assertTrue(payloads.contains("SetXianqiaoInterfaceTargetAmount"));
        assertTrue(payloads.contains("SetXianqiaoInterfaceItemTarget"));
        assertTrue(payloads.contains("SetXianqiaoInterfaceFluidTarget"));
        assertTrue(payloads.contains("configRevision"));
        assertTrue(network.contains("playToServer(ModPayloads.SetXianqiaoInterfaceSideMode.TYPE"));
        assertTrue(network.contains("playToServer(ModPayloads.SetXianqiaoInterfaceTargetAmount.TYPE"));
        assertTrue(network.contains("playToServer(ModPayloads.SetXianqiaoInterfaceItemTarget.TYPE"));
        assertTrue(network.contains("playToServer(ModPayloads.SetXianqiaoInterfaceFluidTarget.TYPE"));

        String sideHandler = methodBody(network, "handleSetXianqiaoInterfaceSideMode(");
        String amountHandler = methodBody(network, "handleSetXianqiaoInterfaceTargetAmount(");
        for (String handler : java.util.List.of(sideHandler, amountHandler)) {
            assertTrue(handler.contains("containerId"));
            assertTrue(handler.contains("XianqiaoInterfaceMenu"));
            assertTrue(handler.contains("getBlockPos"));
            assertTrue(handler.contains("distanceToSqr"));
            assertTrue(handler.contains("stillValid"));
            assertTrue(handler.contains("configRevision"));
        }
        assertTrue(sideHandler.contains("setSideMode"));
        assertTrue(amountHandler.contains("setTargetAmount"));
        assertTrue(sideHandler.indexOf("getConfigRevision") < sideHandler.indexOf("setSideMode"),
                "a stale side-mode request must be rejected before mutation");
        assertTrue(amountHandler.indexOf("getConfigRevision") < amountHandler.indexOf("setTargetAmount"),
                "a stale target-amount request must be rejected before mutation");
    }

    @Test
    void removalReturnsBuffersThenBoundsWorldDropsAndPreservesExactCarrierRemainders() throws IOException {
        String block = source("block", "custom", "XianqiaoInterfaceBlock.java");
        String entity = source("block", "entity", "XianqiaoInterfaceBlockEntity.java");
        String inventory = source("block", "entity", "XianqiaoInterfaceInventory.java");
        String removal = methodBody(block, "protected void onRemove");

        assertTrue(removal.contains("releaseBuffersForRemoval"));
        assertTrue(methodBody(entity, "public void releaseBuffersForRemoval")
                .contains("prepareBuffersForRemoval"));
        assertTrue(methodBody(entity, "public void releaseBuffersForRemoval")
                .contains("Block.popResource"));
        assertTrue(methodBody(entity, "List<ItemStack> prepareBuffersForRemoval()")
                .contains("settleItemBuffersForRemoval"));
        assertTrue(inventory.contains("MAX_MATERIALIZED_REMOVAL_STACKS"));
        assertTrue(methodBody(inventory, "settleItemBuffersForRemoval")
                .contains("requiredStacks > remainingStackBudget"));
        String drops = methodBody(block, "protected List<ItemStack> getDrops");
        assertTrue(drops.contains("saveToItem"));
        assertTrue(drops.indexOf("releaseBuffersForRemoval") < drops.indexOf("super.getDrops"),
                "real buffers must settle before any loot function can copy block-entity data");
        assertTrue(methodBody(entity, "public void removeComponentsFromTag")
                .contains("tag.remove(BUFFERS_TAG)"));
        assertTrue(methodBody(entity, "public void removeComponentsFromTag")
                .contains("!preserveRetainedItemBuffersInDrop"));
        assertTrue(entity.contains("ReleaseState.RELEASING"));
        assertTrue(methodBody(entity, "List<ItemStack> prepareBuffersForRemoval()")
                .contains("releaseState = ReleaseState.OPEN"),
                "a failed multi-slot settlement must remain retryable");
    }

    @Test
    void bufferQuickMoveExtractsBeforeMutatingPlayerSlotsAndCompensatesRemainders() throws IOException {
        String menu = source("menu", "custom", "XianqiaoInterfaceMenu.java");
        String quickMove = methodBody(menu, "public ItemStack quickMoveStack");
        assertTrue(quickMove.indexOf("backend.extractItem") < quickMove.indexOf("moveItemStackTo"));
        assertTrue(quickMove.contains("simulatedPlayerCapacity"));
        assertTrue(quickMove.contains("restoreOrReturn"));
        assertFalse(quickMove.contains("throw new IllegalStateException"));
    }

    @Test
    void productionSliceHasNoNumenOrNativeOptionalModDependency() throws IOException {
        String block = source("block", "custom", "XianqiaoInterfaceBlock.java");
        String entity = source("block", "entity", "XianqiaoInterfaceBlockEntity.java");
        String combined = (block + entity).toLowerCase();

        assertFalse(combined.contains("numen"));
        assertFalse(combined.contains("appeng."));
        assertFalse(combined.contains("refinedstorage"));
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
                    "src", "main", "java", "com", "cultivation", "cultivation"));
            if (Files.isDirectory(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate Cultivation main sources");
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
}
