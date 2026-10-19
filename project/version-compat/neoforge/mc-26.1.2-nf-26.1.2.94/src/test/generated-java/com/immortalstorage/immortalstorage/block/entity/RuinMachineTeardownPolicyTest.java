package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.compressor.EnchantmentTestSupport;
import com.immortalstorage.immortalstorage.compressor.RuinMachineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BEHAVIOUR EVIDENCE for the teardown policy: every case below drives the real shared policy method with a recording
 * eject callback. The four real ENTRY POINTS (break, wrench, polarity switch, kind change) cannot be exercised in this
 * JVM because it has no server level to hold the block entity, so their wiring to this policy stays a source-level
 * assertion in EnchantmentMachineContractTest, labelled there as wiring-only -- that assertion is NOT behaviour evidence
 * and no reader should mistake one for the other.
 */
final class RuinMachineTeardownPolicyTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll static void bootstrap() { Bootstrap.bootStrap(); }

    private static MiniatureImmortalRuinBlockEntity ruin() {
        return new MiniatureImmortalRuinBlockEntity(BlockPos.ZERO, ModBlocks.MINIATURE_IMMORTAL_RUIN.get().defaultBlockState());
    }

    /** Leaves one uncollected enchantment product, with its material still in the input slot. */
    private static void arm(MiniatureImmortalRuinBlockEntity ruin) {
        ruin.enchanter().inventory.setStackInSlot(0, EnchantmentTestSupport.enchanted(new ItemStack(Items.DIAMOND_SWORD, 2),
                EnchantmentTestSupport.enchantment("minecraft:sharpness"), 5));
        ruin.enchanter().runCycle();
        assertFalse(ruin.enchanter().inputsLocked(), "RULING 4: the material row is never locked");
        assertFalse(ruin.enchanter().inventory.getStackInSlot(12).isEmpty());
    }

    @Test void aStructureLossVoidsAnUncollectedEnchantmentProductAndKeepsTheMaterial() {
        var ruin = ruin();
        arm(ruin);
        List<RuinMachine> ejected = new ArrayList<>();

        ruin.applyKindTransition(RuinMachineKind.ENCHANTMENT, null, ejected::add);

        assertTrue(ejected.isEmpty(), "a lost shell keeps its caches inside the block");
        assertTrue(ruin.enchanter().inventory.getStackInSlot(12).isEmpty(), "the uncollected product is voided instead");
        assertEquals(2, ruin.enchanter().inventory.getStackInSlot(0).getCount(), "and the material stays put");
    }

    @Test void aStructureLossEjectsNothingAndLeavesTheSpiritInventoryInsideTheBlock() {
        var ruin = ruin();
        ruin.compressor().inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 4));
        List<RuinMachine> ejected = new ArrayList<>();

        ruin.applyKindTransition(RuinMachineKind.SPIRIT, null, ejected::add);

        assertTrue(ejected.isEmpty(), "0.1.6 keeps the spirit inventory in the block when the structure comes apart");
        assertEquals(4, ruin.compressor().inventory.getStackInSlot(0).getCount());
    }

    @Test void aFamilySwapVoidsTheProductBeforeTheCachesAreEjected() {
        var ruin = ruin();
        ruin.compressor().inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 4));
        arm(ruin);
        List<RuinMachine> ejected = new ArrayList<>();
        List<String> productState = new ArrayList<>();

        ruin.applyKindTransition(RuinMachineKind.SPIRIT, RuinMachineKind.ENCHANTMENT, machine -> {
            ejected.add(machine);
            productState.add(machine == ruin.enchanter()
                    ? (ruin.enchanter().inventory.getStackInSlot(12).isEmpty() ? "empty" : "present") : "other family");
        });

        assertEquals(2, ejected.size(), "a deliberate family swap hands over both caches");
        assertSame(ruin.compressor(), ejected.get(0));
        assertSame(ruin.enchanter(), ejected.get(1));
        assertEquals(List.of("other family", "empty"), productState,
                "the product is already voided when the enchantment caches leave");
        assertEquals(2, ruin.enchanter().inventory.getStackInSlot(0).getCount());
        assertEquals(4, ruin.compressor().inventory.getStackInSlot(0).getCount());
    }

    @Test void aShellFormingAroundTheCoreEjectsNothing() {
        var ruin = ruin();
        ruin.compressor().inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 4));
        List<RuinMachine> ejected = new ArrayList<>();

        ruin.applyKindTransition(null, RuinMachineKind.ENCHANTMENT, ejected::add);

        assertTrue(ejected.isEmpty());
        assertEquals(4, ruin.compressor().inventory.getStackInSlot(0).getCount());
    }

    @Test void anUnchangedKindDoesNothing() {
        var ruin = ruin();
        ruin.compressor().inventory.setStackInSlot(0, new ItemStack(Items.DIAMOND, 4));
        List<RuinMachine> ejected = new ArrayList<>();

        ruin.applyKindTransition(RuinMachineKind.SPIRIT, RuinMachineKind.SPIRIT, ejected::add);

        assertTrue(ejected.isEmpty());
        assertEquals(4, ruin.compressor().inventory.getStackInSlot(0).getCount());
    }

    @Test void aPolaritySwitchVoidsTheProductAndEjectsBothCaches() {
        var ruin = ruin();
        arm(ruin);
        List<RuinMachine> ejected = new ArrayList<>();

        ruin.ejectBothCachesAfterTeardown(ejected::add);

        assertEquals(2, ejected.size());
        assertSame(ruin.compressor(), ejected.get(0));
        assertSame(ruin.enchanter(), ejected.get(1));
        assertTrue(ruin.enchanter().inventory.getStackInSlot(12).isEmpty());
        assertEquals(2, ruin.enchanter().inventory.getStackInSlot(0).getCount());
    }
}
