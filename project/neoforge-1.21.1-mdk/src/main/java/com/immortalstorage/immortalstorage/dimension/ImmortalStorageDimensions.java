package com.immortalstorage.immortalstorage.dimension;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class ImmortalStorageDimensions {
    public static final String XIANQIAO_REALM_PREFIX = "xianqiao_realm/";
    public static final ResourceLocation XIANQIAO_REALM_TEMPLATE_LOCATION =
            ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "xianqiao_realm");
    public static final ResourceKey<Level> XIANQIAO_REALM_LEVEL =
            ResourceKey.create(Registries.DIMENSION, XIANQIAO_REALM_TEMPLATE_LOCATION);
    public static final ResourceKey<LevelStem> XIANQIAO_REALM_STEM =
            ResourceKey.create(Registries.LEVEL_STEM, XIANQIAO_REALM_TEMPLATE_LOCATION);
    public static final ResourceKey<DimensionType> XIANQIAO_REALM_TYPE =
            ResourceKey.create(Registries.DIMENSION_TYPE, XIANQIAO_REALM_TEMPLATE_LOCATION);
    public static final ResourceKey<Biome> XIANQIAO_REALM_BIOME =
            ResourceKey.create(Registries.BIOME, XIANQIAO_REALM_TEMPLATE_LOCATION);

    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATOR_CODECS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, ImmortalStorageMod.MODID);
    public static final Supplier<MapCodec<? extends ChunkGenerator>> XIANQIAO_REALM_CODEC =
            CHUNK_GENERATOR_CODECS.register("xianqiao_realm", () -> XianqiaoRealmChunkGenerator.CODEC);

    public static void register() {
        // Registries register themselves via the FML bus (see ImmortalStorageMod constructor).
        // The template DimensionType and Biome are loaded from datapack JSON.
        // Player-owned realm levels are registered dynamically at runtime.
    }

    public static ResourceLocation personalRealmLocation(UUID playerId) {
        String stableId = playerId.toString().replace("-", "").toLowerCase(Locale.ROOT);
        return ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, XIANQIAO_REALM_PREFIX + stableId);
    }

    public static ResourceKey<Level> personalRealmKey(UUID playerId) {
        return ResourceKey.create(Registries.DIMENSION, personalRealmLocation(playerId));
    }

    public static boolean isXianqiaoRealm(ResourceKey<Level> key) {
        if (key == null) {
            return false;
        }
        ResourceLocation location = key.location();
        return ImmortalStorageMod.MODID.equals(location.getNamespace())
                && location.getPath().startsWith(XIANQIAO_REALM_PREFIX);
    }

    public static boolean isPersonalRealmFor(ResourceKey<Level> key, UUID playerId) {
        return key != null && playerId != null && key.equals(personalRealmKey(playerId));
    }

    public static Optional<UUID> personalRealmOwner(ResourceKey<Level> key) {
        if (key == null) {
            return Optional.empty();
        }
        ResourceLocation location = key.location();
        if (!ImmortalStorageMod.MODID.equals(location.getNamespace())) {
            return Optional.empty();
        }
        String path = location.getPath();
        if (!path.startsWith(XIANQIAO_REALM_PREFIX)) {
            return Optional.empty();
        }
        String raw = path.substring(XIANQIAO_REALM_PREFIX.length());
        if (raw.length() != 32) {
            return Optional.empty();
        }
        try {
            String dashed = raw.substring(0, 8) + "-"
                    + raw.substring(8, 12) + "-"
                    + raw.substring(12, 16) + "-"
                    + raw.substring(16, 20) + "-"
                    + raw.substring(20);
            return Optional.of(UUID.fromString(dashed));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @SubscribeEvent
    public static void onServerAboutToStart(net.neoforged.neoforge.event.server.ServerAboutToStartEvent e) {
        // No-op: player-owned levels are created lazily by RealmHelper when the owner enters.
    }
    private ImmortalStorageDimensions() {}
}
