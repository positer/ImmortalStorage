package com.immortalstorage.immortalstorage.player;

import com.immortalstorage.immortalstorage.item.custom.ImmortalYuanItem;
import com.immortalstorage.immortalstorage.item.custom.TrueYuanItem;
import com.immortalstorage.immortalstorage.player.yuan.YuanItemPolicy;
import com.immortalstorage.immortalstorage.player.yuan.YuanKind;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImmortalStoragePlayerDeferredYuanTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static final long MATERIALIZATION_BATCH = 2_048L * 64L;
    private static Item trueYuanItem;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        ((MappedRegistry<Item>) BuiltInRegistries.ITEM).unfreeze(false);
        trueYuanItem = Registry.register(BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath("cultivation_deferred_test", "true_yuan"),
                new TrueYuanItem(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("cultivation_deferred_test", "true_yuan"))).stacksTo(64)));
        Registry.register(BuiltInRegistries.ITEM,
                Identifier.fromNamespaceAndPath("cultivation_deferred_test", "immortal_yuan"),
                new ImmortalYuanItem(new Item.Properties().setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath("cultivation_deferred_test", "immortal_yuan"))).stacksTo(64)));
        BuiltInRegistries.ITEM.freeze();
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.rebindItemComponents();
    }

    @Test
    void largeLegacyBalancePersistsAndConservesAcrossMultipleBoundedAdvances() {
        long original = MATERIALIZATION_BATCH * 2L + 17L;
        CompoundTag legacy = new CompoundTag();
        legacy.putInt("stage", 9);
        CompoundTag account = new CompoundTag();
        account.putInt("version", 4);
        account.putLong("legacyImmortalPending", original);
        legacy.put("yuanAccount", account);

        ImmortalStoragePlayerData first = new ImmortalStoragePlayerData();
        first.deserializeNBT(REGISTRIES, legacy);
        CompoundTag afterFirstAdvance = first.serializeNBT(REGISTRIES);
        long firstPhysical = physical(first, YuanKind.IMMORTAL);
        assertTrue(firstPhysical > 0L);
        assertTrue(afterFirstAdvance.getLongOr("deferredImmortalYuanMaterialization", 0L) > 0L);
        assertConserved(first, original, YuanKind.IMMORTAL);

        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        restored.deserializeNBT(REGISTRIES, afterFirstAdvance);
        CompoundTag afterSecondAdvance = restored.serializeNBT(REGISTRIES);
        assertTrue(physical(restored, YuanKind.IMMORTAL) > firstPhysical);
        assertTrue(afterSecondAdvance.getLongOr("deferredImmortalYuanMaterialization", 0L) > 0L);
        assertConserved(restored, original, YuanKind.IMMORTAL);

        restored.advanceDeferredYuanWork();
        CompoundTag completed = restored.serializeNBT(REGISTRIES);
        assertEquals(0L, completed.getLongOr("deferredImmortalYuanMaterialization", 0L));
        assertEquals(0L, completed.getLongOr("deferredImmortalYuanDrop", 0L));
        assertEquals(original, physical(restored, YuanKind.IMMORTAL));
    }

    @Test
    void capOverflowRemainsPersistedWhenItCannotYetBeDropped() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(1);
        data.setKongqiaoSlot(0, new ItemStack(trueYuanItem, 64));
        data.setKongqiaoSlot(1, new ItemStack(trueYuanItem, 64));

        data.advanceDeferredYuanWork();

        CompoundTag saved = data.serializeNBT(REGISTRIES);
        assertEquals(64L, physical(data, YuanKind.TRUE));
        assertEquals(64L, saved.getLongOr("deferredTrueYuanDrop", 0L));

        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        restored.deserializeNBT(REGISTRIES, saved);
        restored.advanceDeferredYuanWork();
        CompoundTag resaved = restored.serializeNBT(REGISTRIES);
        assertEquals(64L, physical(restored, YuanKind.TRUE));
        assertEquals(64L, resaved.getLongOr("deferredTrueYuanDrop", 0L),
                "a detached/offline owner must retain the full ejection liability");
    }

    private static void assertConserved(ImmortalStoragePlayerData data, long original, YuanKind kind) {
        CompoundTag saved = data.serializeNBT(REGISTRIES);
        String materializationKey = kind == YuanKind.TRUE
                ? "deferredTrueYuanMaterialization" : "deferredImmortalYuanMaterialization";
        String dropKey = kind == YuanKind.TRUE ? "deferredTrueYuanDrop" : "deferredImmortalYuanDrop";
        assertEquals(original, physical(data, kind)
                + saved.getLongOr(materializationKey, 0L)
                + saved.getLongOr(dropKey, 0L));
    }

    private static long physical(ImmortalStoragePlayerData data, YuanKind kind) {
        List<ItemStack> active = data.isStorageIsKongqiaoLegacy()
                ? data.getKongqiaoItems().subList(0, data.getKongqiaoMaxSlots())
                : data.getXianqiaoStorageItems();
        return active.stream()
                .filter(stack -> YuanItemPolicy.kindOf(stack) == kind)
                .mapToLong(ItemStack::getCount)
                .sum();
    }
}
