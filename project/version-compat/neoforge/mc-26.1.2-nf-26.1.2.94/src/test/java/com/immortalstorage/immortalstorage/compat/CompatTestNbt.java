package com.immortalstorage.immortalstorage.compat;

import appeng.api.stacks.AEKey;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

/** Small target-only bridge for tests that exercise official Value I/O APIs. */
public final class CompatTestNbt {
    private CompatTestNbt() {
    }

    public static CompoundTag toTag(AEKey key) {
        TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        key.toTag(output);
        return output.buildResult();
    }

    public static ValueInput input(CompoundTag tag) {
        return TagValueInput.create(
                ProblemReporter.DISCARDING,
                CompatTestBootstrap.registryAccess(),
                tag);
    }
}
