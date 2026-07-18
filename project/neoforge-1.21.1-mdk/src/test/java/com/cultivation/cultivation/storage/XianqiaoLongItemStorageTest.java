package com.cultivation.cultivation.storage;

import com.cultivation.cultivation.api.storage.terminal.TerminalEntryKey;
import com.cultivation.cultivation.api.storage.terminal.TerminalStorageAction;
import com.cultivation.cultivation.player.CultivationPlayerData;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class XianqiaoLongItemStorageTest {
    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void simulateAndExecuteAgreeAboveTheIntBoundaryWithoutRevisionAmplification() {
        CultivationPlayerData data = new CultivationPlayerData();
        data.setStage(6);
        TerminalEntryKey iron = TerminalEntryKey.of(new ItemStack(Items.IRON_INGOT));
        long offered = 2L * Integer.MAX_VALUE + 17L;

        assertEquals(offered, data.insertXianqiaoItem(iron, offered, TerminalStorageAction.SIMULATE));
        assertTrue(data.getXianqiaoItemSummary().isEmpty());
        assertEquals(0L, data.getXianqiaoStorageRevision());

        assertEquals(offered, data.insertXianqiaoItem(iron, offered, TerminalStorageAction.EXECUTE));
        assertEquals(offered, data.getXianqiaoItemSummary().getFirst().amount());
        assertEquals(1L, data.getXianqiaoStorageRevision(),
                "one long transaction must publish one revision, not one revision per int chunk");

        long requested = (long) Integer.MAX_VALUE + 9L;
        assertEquals(requested, data.extractXianqiaoItem(iron, requested, TerminalStorageAction.SIMULATE));
        assertEquals(offered, data.getXianqiaoItemSummary().getFirst().amount());
        assertEquals(requested, data.extractXianqiaoItem(iron, requested, TerminalStorageAction.EXECUTE));
        assertEquals(offered - requested, data.getXianqiaoItemSummary().getFirst().amount());
        assertEquals(2L, data.getXianqiaoStorageRevision());
    }

    @Test
    void completeDataComponentsRemainSeparateAndKongqiaoCannotUseTheLongSurface() {
        CultivationPlayerData stageFive = new CultivationPlayerData();
        stageFive.setStage(5);
        TerminalEntryKey diamond = TerminalEntryKey.of(new ItemStack(Items.DIAMOND));
        assertEquals(0L, stageFive.insertXianqiaoItem(
                diamond, 1L, TerminalStorageAction.EXECUTE));

        CultivationPlayerData data = new CultivationPlayerData();
        data.setStage(6);
        ItemStack plain = new ItemStack(Items.DIAMOND);
        ItemStack named = new ItemStack(Items.DIAMOND);
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal("bound identity"));

        assertEquals(7L, data.insertXianqiaoItem(
                TerminalEntryKey.of(plain), 7L, TerminalStorageAction.EXECUTE));
        assertEquals(11L, data.insertXianqiaoItem(
                TerminalEntryKey.of(named), 11L, TerminalStorageAction.EXECUTE));
        assertEquals(2, data.getXianqiaoItemSummary().size());
        assertEquals(0L, data.extractXianqiaoItem(
                TerminalEntryKey.of(new ItemStack(Items.EMERALD)), 1L,
                TerminalStorageAction.EXECUTE));
    }
}
