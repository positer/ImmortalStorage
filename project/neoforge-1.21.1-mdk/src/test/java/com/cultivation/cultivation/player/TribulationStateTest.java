package com.cultivation.cultivation.player;

import com.cultivation.cultivation.item.custom.ImmortalYuanItem;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TribulationStateTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private static Item immortalYuan;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
        ((MappedRegistry<Item>) BuiltInRegistries.ITEM).unfreeze();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                "cultivation_tribulation_test", "immortal_yuan");
        immortalYuan = BuiltInRegistries.ITEM.getOptional(id).orElseGet(() ->
                Registry.register(BuiltInRegistries.ITEM, id,
                        new ImmortalYuanItem(new Item.Properties().stacksTo(64))));
        BuiltInRegistries.ITEM.freeze();
    }

    @Test
    void oneAttemptPersistsItsIdentityAndHasNoCountdownState() {
        CultivationPlayerData data = new CultivationPlayerData();
        data.setStage(8);
        UUID attemptId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        assertTrue(data.beginTribulation(attemptId, targetId, 9));
        CultivationPlayerData restored = new CultivationPlayerData();
        restored.deserializeNBT(REGISTRIES, data.serializeNBT(REGISTRIES));

        assertTrue(restored.isTribulationActive());
        assertEquals(attemptId, restored.getTribulationAttemptId());
        assertEquals(targetId, restored.getTribulationTargetId());
        assertEquals(9, restored.getNextStageOnSuccess());
    }

    @Test
    void onlyTheBoundAttemptTargetCanCompleteAndAdvance() {
        CultivationPlayerData data = new CultivationPlayerData();
        data.setStage(7);
        UUID attemptId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        assertTrue(data.beginTribulation(attemptId, targetId, 8));

        assertFalse(data.completeTribulation(UUID.randomUUID(), targetId, null));
        assertFalse(data.completeTribulation(attemptId, UUID.randomUUID(), null));
        assertEquals(7, data.getStage());
        assertTrue(data.completeTribulation(attemptId, targetId, null));
        assertEquals(8, data.getStage());
        assertFalse(data.isTribulationActive());
    }

    @Test
    void failureKeepsStageAndClearsTheAuthoritativeImmortalYuanStorage() {
        CultivationPlayerData data = new CultivationPlayerData();
        data.setStage(7);
        assertTrue(data.insertStack(new ItemStack(immortalYuan, 64), true).isEmpty());
        UUID attemptId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        assertTrue(data.beginTribulation(attemptId, targetId, 8));

        assertEquals(64L, data.getImmortalYuan());
        assertEquals(64L, data.failTribulation());
        assertEquals(7, data.getStage());
        assertEquals(0L, data.getImmortalYuan());
        assertFalse(data.isTribulationActive());
    }

    @Test
    void legacyWaveStateDoesNotRestoreAnUnboundAttempt() {
        CultivationPlayerData legacy = new CultivationPlayerData();
        legacy.setStage(7);
        var tag = legacy.serializeNBT(REGISTRIES);
        tag.putBoolean("tribulationActive", true);
        tag.putInt("tribulationWave", 2);
        tag.putInt("tribulationTimer", 1_200);
        tag.putInt("tribulationMobsAlive", 12);
        tag.putInt("nextStageOnSuccess", 8);

        CultivationPlayerData restored = new CultivationPlayerData();
        restored.deserializeNBT(REGISTRIES, tag);

        assertFalse(restored.isTribulationActive(),
                "old multi-wave saves have no unique target and must not become an immortal stuck attempt");
        assertEquals(7, restored.getStage());
    }

    @Test
    void aRestoredAttemptGetsABoundedTargetLoadGraceWindow() {
        CultivationPlayerData data = new CultivationPlayerData();
        data.setStage(8);
        assertTrue(data.beginTribulation(UUID.randomUUID(), UUID.randomUUID(), 9));

        for (int tick = 1; tick < 200; tick++) {
            assertFalse(data.noteTribulationTargetMissing(200));
        }
        assertTrue(data.noteTribulationTargetMissing(200));
        data.resetTribulationTargetMissing();
        assertFalse(data.noteTribulationTargetMissing(200));
    }
}
