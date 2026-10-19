package com.immortalstorage.immortalstorage.block.entity;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class EnchantmentMachineContractTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test void theSpiritMachineKeepsItsRecipesConsumptionTimingAndNbtKeys() throws Exception {
        String logic = read("block/entity/SpiritCompressorLogic.java");
        for (String key : List.of("\"CompressorInventory\"", "\"CompressorOutputCount\"", "\"DecompressorInventory\"",
                "\"DecompressorOutputCounts\"", "\"CompressorFaces\"", "\"CompressorXianqiao\"", "\"CompressorOwner\""))
            assertTrue(logic.contains(key), key);
        // Materials are still spent in the tick that crafts, and nothing there can ever defer a cost.
        assertTrue(logic.contains("inventory.extractItem(i, materials.consumed()[i], false)"));
        assertTrue(logic.contains("public void settlePending() {}"));
        assertTrue(logic.contains("public void discardPending() {}"));
        assertTrue(logic.contains("public boolean inputsLocked() { return false; }"));
        assertTrue(logic.contains("SpiritCompressorRecipes.entries(level, reversed())"));
        assertTrue(logic.contains("RuinMachineKind.SPIRIT"));
        assertTrue(logic.contains("SpiritCompressorCacheEjection.eject("));
        assertTrue(logic.contains("List.of(inventory, decompressionInventory)"));
        assertFalse(logic.contains("EnchantMachine"), "no enchantment persistence leaked into the spirit path");
        assertFalse(logic.contains("EnchantmentCompressionPlan"));
        assertFalse(logic.contains("DeferredEnchantmentTransaction"));
    }

    @Test void theEnchantmentMachinePersistsBothInventoriesFacesXianqiaoOwnerAndThePendingTransaction() throws Exception {
        String logic = read("block/entity/EnchantmentMachineLogic.java");
        for (String key : List.of("\"EnchantMachineInventory\"", "\"EnchantMachineOutputCount\"",
                "\"EnchantMachineDecompressorInventory\"", "\"EnchantMachineDecompressorOutputCounts\"",
                "\"EnchantMachineFaces\"", "\"EnchantMachineXianqiao\"", "\"EnchantMachineOwner\"",
                "\"EnchantMachinePendingConsume\"", "\"EnchantMachinePendingStrip\""))
            assertTrue(logic.contains(key), key);
        assertTrue(logic.contains("transaction.afterExtract(this, slot, EnchantmentCompressionPlan.OUTPUT_SLOT)"));
        assertTrue(logic.contains("transaction.afterExtract(this, slot, EnchantmentDecompressionPlan.OUTPUT_START)"));
        assertTrue(logic.contains("if (transaction.pending()) return;"));
        assertTrue(logic.contains("if (!current) discardPending();"), "losing the structure voids the uncollected product");
        assertTrue(logic.contains("transaction.discard(activeInventory(), outputStart())"));
        String be = read("block/entity/MiniatureImmortalRuinBlockEntity.java");
        assertTrue(be.contains("enchanter.save(tag, registries)"));
        assertTrue(be.contains("enchanter.load(tag, registries)"));
    }

    /**
     * WIRING ONLY, NOT BEHAVIOUR EVIDENCE. The teardown policy itself is driven behaviourally in
     * RuinMachineTeardownPolicyTest (real policy methods, recording eject callback). The four real entry points
     * (break, wrench, polarity switch, kind change) cannot be exercised in this JVM because it has no server level
     * to hold the block entity. What is pinned below is only that each entry calls that shared policy, and that the
     * take paths never do.
     */
    @Test void everyTeardownEntryCallsTheSharedPolicyWhileTheTakePathsSettle() throws Exception {
        String logic = read("block/entity/EnchantmentMachineLogic.java");
        // Teardown: the uncollected product is voided before any cache is dropped.
        String eject = methodBody(logic, "public void ejectCaches(ServerLevel level)");
        assertTrue(eject.indexOf("discardPending()") < eject.indexOf("SpiritCompressorCacheEjection.eject("));
        assertTrue(logic.contains("if (!current) discardPending();"));
        // Take: only a real extraction pays, and production needs a completely empty output row.
        assertTrue(logic.contains("transaction.afterExtract(this, slot, EnchantmentCompressionPlan.OUTPUT_SLOT)"));
        assertTrue(logic.contains("transaction.afterExtract(this, slot, EnchantmentDecompressionPlan.OUTPUT_START)"));
        String cycle = methodBody(logic, "void runCycle()");
        assertTrue(cycle.indexOf("if (transaction.pending()) return;") < cycle.indexOf("if (hasStoredOutput(inventory)) return;"));
        String be = read("block/entity/MiniatureImmortalRuinBlockEntity.java");
        assertTrue(be.contains("public void discardUncollectedProducts() { compressor.discardPending(); enchanter.discardPending(); }"));
        // Polarity switch uses the shared policy, and so does the kind transition.
        String setBlockState = methodBody(be, "public void setBlockState(BlockState nextState)");
        assertTrue(setBlockState.contains("ejectBothCachesAfterTeardown(machine -> machine.ejectCaches(server))"));
        String track = methodBody(be, "private void trackMachineKind(ServerLevel level)");
        assertTrue(track.contains("applyKindTransition(previous, kind, machine -> machine.ejectCaches(level))"));
        assertFalse(track.contains("compressor.ejectCaches(level)"), "no kind change may eject unconditionally (F2)");
        String block = read("block/custom/MiniatureImmortalRuinBlock.java");
        String wrench = methodBody(block, "protected InteractionResult useItemOn(");
        assertTrue(wrench.indexOf("discardUncollectedProducts()") < wrench.indexOf("getUpdateTag"));
        String removal = methodBody(block, "protected void affectNeighborsAfterRemoval(");
        assertTrue(removal.indexOf("discardUncollectedProducts()") < removal.indexOf("unlinkForBreak()"));
        assertFalse(block.contains("settlePendingTransactions()"));
    }

    @Test void everyRuinCapabilityAndUiPathGoesThroughTheKindAwareAccessor() throws Exception {
        String capabilities = read("block/entity/ModBlockEntities.java");
        assertEquals(4, capabilities.split(java.util.regex.Pattern.quote("ruin.machine().handler(side)"), -1).length - 1,
                "every ruin capability lookup resolves the machine kind");
        assertFalse(capabilities.contains("ruin.compressor().handler(side)"));
        assertTrue(read("item/custom/MiniatureImmortalRuinItem.java").contains("ruin.placedBy("));
        String be = read("block/entity/MiniatureImmortalRuinBlockEntity.java");
        assertTrue(be.contains("public RuinMachine machine()"));
        assertTrue(be.contains("machine.titleKey()"));
        assertTrue(be.contains("if (machine().formed())"));
        String menu = read("menu/custom/SpiritCompressorMenu.java");
        assertTrue(menu.contains("machine.titleKey()"));
        assertTrue(menu.contains("kind == ruin.machineKind()"));
        // The gate must read the machine the slots were built from (F1); re-deriving it from the world would
        // unlock the inputs between a kind change and the core's next tick while the captured machine still bills.
        assertTrue(menu.contains("lockedInput(index)"));
        assertTrue(menu.contains("return machine != null && machine.locksSlot(slot);"));
        assertFalse(menu.contains("ruin.machine().inputEnd()"));
        assertTrue(read("block/entity/RuinMachine.java").contains("default boolean locksSlot(int slot) { return slot < inputEnd() && inputsLocked(); }"));
        assertTrue(read("client/screen/SpiritCompressorScreen.java").contains("Component.translatable(menu.titleKey())"));
    }

    @Test void theSharedScreenGeometryIsUnchangedAndInputsLockOnlyWhileATransactionIsPending() throws Exception {
        String menu = read("menu/custom/SpiritCompressorMenu.java");
        assertTrue(menu.contains("addSlot(new SlotItemHandler(handler, 0, 26, 46)"));
        assertTrue(menu.contains("98 + i % 4 * 18, 28 + i / 4 * 18"));
        assertTrue(menu.contains("8 + i % 4 * 18, 28 + i / 4 * 18"));
        assertTrue(menu.contains("134, 46"));
        assertTrue(menu.contains("private boolean lockedInput(int slot) {"));
        assertTrue(menu.contains("return machine != null && machine.locksSlot(slot);"));
        assertFalse(menu.contains("ruin.machine().inputEnd()"), "the gate must never re-derive the machine from the world (F1)");
        // Shift-click and the number-key swap replace the slot directly instead of calling extractItem,
        // so the product slots settle the deferred cost themselves as well.
        assertEquals(2, menu.split("super.onTake\\(player, stack\\); settleOutput\\(player\\);", -1).length - 1,
                "both output rows settle a container take");
        assertTrue(menu.contains("if (machine != null && !player.level().isClientSide()) machine.settlePending();"));
    }

    @Test void chainParticlesKeepTheirSharedGeometryAndTakeTheKindColour() throws Exception {
        String particles = read("client/render/SpiritCompressorParticles.java");
        assertTrue(particles.contains("level.getRandom().nextInt(160)"));
        assertTrue(particles.contains("particle.setLifetime(24)"));
        assertTrue(particles.contains("-dx / 24, -dy / 24, -dz / 24"));
        assertTrue(particles.contains("immortalstorage$setGravity(0)"));
        assertTrue(particles.contains("immortalstorage$setFriction(1)"));
        assertTrue(particles.contains("immortalstorage$setHasPhysics(false)"));
        assertTrue(particles.contains("SpiritCompressorStructure.blockId(x, y, z).equals(\"minecraft:chain\")"));
        assertTrue(particles.contains("ruin.machine().formed()"));
        assertTrue(particles.contains("RuinMachineKind kind = ruin.machineKind()"));
        assertTrue(particles.contains("kind.particleColour()"));
        assertTrue(particles.contains("kind.particleFadeColour()"));
    }

    private static String read(String relative) throws Exception {
        return Files.readString(locateProject().resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source/com/immortalstorage/immortalstorage").resolve(relative));
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) throw new AssertionError("Missing method: " + signature);
        int next = source.indexOf("\n    private ", start + signature.length());
        return next < 0 ? source.substring(start) : source.substring(start, next);
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("../version-compat/neoforge/mc-26.1.2-nf-26.1.2.94/src/test/compat-source"))) return current;
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot locate NeoForge project");
    }
}
