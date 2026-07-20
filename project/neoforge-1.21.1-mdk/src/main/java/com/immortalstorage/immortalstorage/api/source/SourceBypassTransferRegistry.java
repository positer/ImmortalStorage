package com.immortalstorage.immortalstorage.api.source;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Neutral registration boundary for optional storage-mod native transfers.
 * No optional-mod type may appear in this package.
 */
public final class SourceBypassTransferRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();

    @FunctionalInterface
    public interface Provider {
        SourceBypassTransferTarget find(ServerLevel level, BlockPos targetPos, Direction targetSide);
    }

    private static final Map<ResourceLocation, ProviderEntry> PROVIDERS = new LinkedHashMap<>();

    public static synchronized void register(ResourceLocation id, Provider provider) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(provider, "provider");
        ProviderEntry previous = PROVIDERS.putIfAbsent(id, new ProviderEntry(id, provider));
        if (previous != null && previous.provider != provider) {
            throw new IllegalStateException("Source bypass provider already registered: " + id);
        }
    }

    /** Returns the first native endpoint registered for the target capability. */
    public static SourceBypassTransferTarget find(ServerLevel level, BlockPos targetPos,
                                                  Direction targetSide) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(targetPos, "targetPos");
        Objects.requireNonNull(targetSide, "targetSide");
        ProviderEntry[] snapshot;
        synchronized (SourceBypassTransferRegistry.class) {
            snapshot = PROVIDERS.values().toArray(ProviderEntry[]::new);
        }
        for (ProviderEntry entry : snapshot) {
            SourceBypassTransferTarget target = entry.find(level, targetPos, targetSide);
            if (target != null) return target;
        }
        return null;
    }

    private static final class ProviderEntry {
        private final ResourceLocation id;
        private final Provider provider;
        private volatile boolean disabled;

        private ProviderEntry(ResourceLocation id, Provider provider) {
            this.id = id;
            this.provider = provider;
        }

        private SourceBypassTransferTarget find(ServerLevel level, BlockPos targetPos,
                                                Direction targetSide) {
            if (disabled) return null;
            try {
                return provider.find(level, targetPos, targetSide);
            } catch (RuntimeException | LinkageError error) {
                if (disable()) {
                    LOGGER.error("Disabling source bypass provider {} after an incompatible runtime failure",
                            id, error);
                }
                return null;
            }
        }

        private synchronized boolean disable() {
            if (disabled) return false;
            disabled = true;
            return true;
        }
    }

    private SourceBypassTransferRegistry() {}
}
