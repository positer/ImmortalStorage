package com.cultivation.cultivation.player;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class CultivationPlayerRealmTimeScaleTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    @Test
    void everyStageUsesItsGoalRange() {
        CultivationPlayerData data = new CultivationPlayerData();

        data.setStage(7);
        data.setTimeScale(0.0F);
        assertEquals(500, data.getRealmTimeRatePermille());
        data.setTimeScale(99.0F);
        assertEquals(4_000, data.getRealmTimeRatePermille());

        data.setStage(8);
        data.setTimeScale(0.0F);
        assertEquals(200, data.getRealmTimeRatePermille());
        data.setTimeScale(99.0F);
        assertEquals(8_000, data.getRealmTimeRatePermille());

        data.setStage(9);
        data.setTimeScale(0.0F);
        assertEquals(100, data.getRealmTimeRatePermille());
        data.setTimeScale(99.0F);
        assertEquals(16_000, data.getRealmTimeRatePermille());

        data.setStage(10);
        data.setTimeScale(0.0F);
        assertEquals(0, data.getRealmTimeRatePermille());
        data.setTimeScale(99.0F);
        assertEquals(32_000, data.getRealmTimeRatePermille());
    }

    @Test
    void unsafeHistoricalValueIsClampedAndWrittenBack() {
        CompoundTag legacy = new CompoundTag();
        legacy.putInt("stage", 10);
        legacy.putInt("realmTimeRatePermille", Integer.MAX_VALUE);

        CultivationPlayerData restored = new CultivationPlayerData();
        restored.deserializeNBT(REGISTRIES, legacy);

        assertEquals(32_000, restored.getRealmTimeRatePermille());
        assertEquals(32_000, restored.serializeNBT(REGISTRIES)
                .getInt("realmTimeRatePermille"));
    }

    @Test
    void oldSaveWithoutRateDefaultsToNormalInsteadOfFreezing() {
        CompoundTag legacy = new CompoundTag();
        legacy.putInt("stage", 10);

        CultivationPlayerData restored = new CultivationPlayerData();
        restored.deserializeNBT(REGISTRIES, legacy);

        assertEquals(1_000, restored.getRealmTimeRatePermille());
    }
}
