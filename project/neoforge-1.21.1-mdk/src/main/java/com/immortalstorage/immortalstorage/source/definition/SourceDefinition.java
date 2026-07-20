package com.immortalstorage.immortalstorage.source.definition;

import com.immortalstorage.immortalstorage.block.custom.VeinKind;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/** Immutable, reload-safe source definition. Registry objects are resolved only after validation. */
public record SourceDefinition(
        ResourceLocation id,
        OutputType outputType,
        ResourceLocation outputId,
        long yuanCostPerBatch,
        long outputsPerBatch,
        int minStage,
        long defaultRate,
        long maxRate,
        String displayName,
        int coreColor,
        String modelHint,
        List<ResourceLocation> aliases,
        @Nullable VeinKind legacyKind
) {
    public SourceDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(outputType, "outputType");
        Objects.requireNonNull(outputId, "outputId");
        if (yuanCostPerBatch < 0L) throw new IllegalArgumentException("yuan_cost_per_batch must be non-negative");
        if (outputsPerBatch <= 0L) throw new IllegalArgumentException("outputs_per_batch must be positive");
        if (minStage < 0 || minStage > 10) throw new IllegalArgumentException("min_stage must be in 0..10");
        if (defaultRate < 0L) throw new IllegalArgumentException("default_rate must be non-negative");
        if (maxRate < 0L) throw new IllegalArgumentException("max_rate must be non-negative");
        if (defaultRate > maxRate) throw new IllegalArgumentException("default_rate must not exceed max_rate");
        if (coreColor < 0 || coreColor > 0xFFFFFF) throw new IllegalArgumentException("core_color must be RGB");
        displayName = displayName == null ? "" : displayName;
        modelHint = modelHint == null ? "" : modelHint;
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
        if (aliases.contains(id)) throw new IllegalArgumentException("definition cannot alias itself");
    }

    public boolean fluid() {
        return outputType == OutputType.FLUID;
    }

    public boolean free() {
        return yuanCostPerBatch == 0L;
    }

    public enum OutputType { ITEM, FLUID }
}
