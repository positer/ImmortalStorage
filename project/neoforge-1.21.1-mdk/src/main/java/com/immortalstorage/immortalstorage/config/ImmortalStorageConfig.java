package com.immortalstorage.immortalstorage.config;

import com.immortalstorage.immortalstorage.compat.CompatManager;
import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceChannelKey;
import net.neoforged.neoforge.common.ModConfigSpec;

import org.jetbrains.annotations.Nullable;

public final class ImmortalStorageConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue START_WITH_JADE_GUIDE;
    public static final ModConfigSpec.BooleanValue JADE_GUIDE_IN_VILLAGE_CHESTS;
    public static final ModConfigSpec.DoubleValue JADE_GUIDE_CHEST_CHANCE;
    public static final ModConfigSpec.DoubleValue JADE_GUIDE_ARCHAEOLOGY_CHANCE;
    public static final ModConfigSpec.DoubleValue VILLAGE_REFINED_PILL_CHANCE;
    public static final ModConfigSpec.IntValue VILLAGE_REFINED_PILL_MIN;
    public static final ModConfigSpec.IntValue VILLAGE_REFINED_PILL_MAX;
    public static final ModConfigSpec.DoubleValue NETHER_BREAKTHROUGH_CHANCE;
    public static final ModConfigSpec.IntValue NETHER_BREAKTHROUGH_MIN;
    public static final ModConfigSpec.IntValue NETHER_BREAKTHROUGH_MAX;
    public static final ModConfigSpec.DoubleValue END_CITY_ASCENSION_CHANCE;
    public static final ModConfigSpec.DoubleValue END_SHIP_IMMORTAL_CHANCE;
    public static final ModConfigSpec.DoubleValue ASCENSION_DAN_CHANCE;
    public static final ModConfigSpec.BooleanValue SOURCE_ALLOW_OTHER_PLAYER_CLAIM;
    public static final ModConfigSpec.BooleanValue SOURCE_ALLOW_OTHER_PLAYER_BREAK;
    public static final ModConfigSpec.BooleanValue SOURCE_ALLOW_MOB_BREAK;
    public static final ModConfigSpec.IntValue XIANQIAO_INTERFACE_ITEM_SLOT_LIMIT;
    public static final ModConfigSpec.IntValue XIANQIAO_INTERFACE_FLUID_SLOT_LIMIT_MB;
    public static final ModConfigSpec.IntValue SPIRIT_STAFF_BUILD_LIMIT;
    public static final ModConfigSpec.BooleanValue IMMORTAL_RUIN_SWORD_AFFECTS_OTHER_PLAYERS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> PRIMORDIAL_QI_ENTITY_BLACKLIST;
    public static final ModConfigSpec.IntValue NORMAL_PROGRESSION_MAX_STAGE;
    public static final ModConfigSpec.BooleanValue STAGE_TEN_INFINITE_IMMORTAL_YUAN;
    public static final ModConfigSpec.ConfigValue<String> TRIBULATION_TARGET_STAGE_6;
    public static final ModConfigSpec.ConfigValue<String> TRIBULATION_TARGET_STAGE_7;
    public static final ModConfigSpec.ConfigValue<String> TRIBULATION_TARGET_STAGE_8;
    public static final ModConfigSpec.ConfigValue<String> TRIBULATION_TARGET_STAGE_9;
    public static final @Nullable ConversionValues FE_CONVERSION;
    public static final @Nullable ConversionValues BOTANIA_MANA_CONVERSION;
    public static final @Nullable ConversionValues ARS_SOURCE_CONVERSION;

    static {
        BUILDER.translation(key("loot")).push("loot");
        START_WITH_JADE_GUIDE = BUILDER.translation(key("loot.startWithJadeGuide")).define("startWithJadeGuide", true);
        JADE_GUIDE_IN_VILLAGE_CHESTS = BUILDER.translation(key("loot.jadeGuideInVillageChests")).define("jadeGuideInVillageChests", true);
        JADE_GUIDE_CHEST_CHANCE = BUILDER.translation(key("loot.jadeGuideChestChance")).defineInRange("jadeGuideChestChance", 1.0, 0.0, 1.0);
        JADE_GUIDE_ARCHAEOLOGY_CHANCE = BUILDER.translation(key("loot.jadeGuideArchaeologyChance"))
                .comment("Chance for any suspicious sand or suspicious gravel archaeology loot to include Ancient Jade.")
                .defineInRange("jadeGuideArchaeologyChance", 0.05, 0.0, 1.0);
        VILLAGE_REFINED_PILL_CHANCE = BUILDER.translation(key("loot.villageRefinedPillChance")).defineInRange("villageRefinedPillChance", 0.20, 0.0, 1.0);
        VILLAGE_REFINED_PILL_MIN = BUILDER.translation(key("loot.villageRefinedPillMin")).defineInRange("villageRefinedPillMin", 3, 0, 64);
        VILLAGE_REFINED_PILL_MAX = BUILDER.translation(key("loot.villageRefinedPillMax")).defineInRange("villageRefinedPillMax", 5, 0, 64);
        NETHER_BREAKTHROUGH_CHANCE = BUILDER.translation(key("loot.netherBreakthroughChance")).defineInRange("netherBreakthroughChance", 0.20, 0.0, 1.0);
        NETHER_BREAKTHROUGH_MIN = BUILDER.translation(key("loot.netherBreakthroughMin")).defineInRange("netherBreakthroughMin", 2, 0, 16);
        NETHER_BREAKTHROUGH_MAX = BUILDER.translation(key("loot.netherBreakthroughMax")).defineInRange("netherBreakthroughMax", 3, 0, 16);
        END_CITY_ASCENSION_CHANCE = BUILDER.translation(key("loot.endCityAscensionChance")).defineInRange("endCityAscensionChance", 0.01, 0.0, 1.0);
        END_SHIP_IMMORTAL_CHANCE = BUILDER.translation(key("loot.endShipImmortalChance")).defineInRange("endShipImmortalChance", 0.50, 0.0, 1.0);
        ASCENSION_DAN_CHANCE = BUILDER.translation(key("loot.ascensionDanChance")).defineInRange("ascensionDanChance", 0.05, 0.0, 1.0);
        BUILDER.pop();
        BUILDER.translation(key("source_blocks")).push("source_blocks");
        SOURCE_ALLOW_OTHER_PLAYER_CLAIM = BUILDER
                .translation(key("source_blocks.allowOtherPlayerClaim"))
                .comment("Allow another player to claim an owned source block when their cultivation stage meets the source block requirement.")
                .define("allowOtherPlayerClaim", true);
        SOURCE_ALLOW_OTHER_PLAYER_BREAK = BUILDER
                .translation(key("source_blocks.allowOtherPlayerBreak"))
                .comment("Allow non-owner players to break owned source blocks. Server operators with permission level 2 can always break them.")
                .define("allowOtherPlayerBreak", false);
        SOURCE_ALLOW_MOB_BREAK = BUILDER
                .translation(key("source_blocks.allowMobBreak"))
                .comment("Allow mobs and non-player explosions to destroy source blocks.")
                .define("allowMobBreak", false);
        BUILDER.pop();
        BUILDER.translation(key("xianqiao_interface")).push("xianqiao_interface");
        XIANQIAO_INTERFACE_ITEM_SLOT_LIMIT = BUILDER
                .translation(key("xianqiao_interface.xianqiaoInterfaceItemSlotLimit"))
                .comment("Maximum item count held by each Xianqiao Interface cache slot.")
                .defineInRange("xianqiaoInterfaceItemSlotLimit", 128, 1, Integer.MAX_VALUE);
        XIANQIAO_INTERFACE_FLUID_SLOT_LIMIT_MB = BUILDER
                .translation(key("xianqiao_interface.xianqiaoInterfaceFluidSlotLimitMb"))
                .comment("Maximum fluid amount, in mB, held by each Xianqiao Interface cache slot.")
                .defineInRange("xianqiaoInterfaceFluidSlotLimitMb", 16_000, 1, Integer.MAX_VALUE);
        BUILDER.pop();
        BUILDER.translation(key("spirit_staff")).push("spirit_staff");
        SPIRIT_STAFF_BUILD_LIMIT = BUILDER
                .translation(key("spirit_staff.buildLimit"))
                .comment("Maximum blocks placed by one Spirit Staff build-mode action.")
                .defineInRange("buildLimit", 64, 1, 4096);
        BUILDER.pop();
        BUILDER.translation(key("immortal_ruin_sword")).push("immortal_ruin_sword");
        IMMORTAL_RUIN_SWORD_AFFECTS_OTHER_PLAYERS = BUILDER
                .translation(key("immortal_ruin_sword.affectsOtherPlayers"))
                .comment("Allow the Immortal-Ruin-Forged Spirit Sword teleport and one-second restraint to affect other players.")
                .define("affectsOtherPlayers", true);
        BUILDER.pop();
        BUILDER.translation(key("primordial_qi")).push("primordial_qi");
        PRIMORDIAL_QI_ENTITY_BLACKLIST = BUILDER
                .translation(key("primordial_qi.entityBlacklist"))
                .comment("Entity type ids that Primordial Qi cannot shrink or remove. Players are always protected.")
                .defineListAllowEmpty("entityBlacklist", java.util.List.of(
                        "minecraft:wither", "minecraft:warden"),
                        value -> value instanceof String id && net.minecraft.resources.ResourceLocation.tryParse(id) != null);
        BUILDER.pop();
        BUILDER.translation(key("progression")).push("progression");
        NORMAL_PROGRESSION_MAX_STAGE = BUILDER
                .translation(key("progression.maximumStage"))
                .comment("Highest stage reachable through normal gameplay. Debug items are not restricted by this value.")
                .defineInRange("maximumStage", 10, 1, 10);
        STAGE_TEN_INFINITE_IMMORTAL_YUAN = BUILDER
                .translation(key("progression.stageTenInfiniteImmortalYuan"))
                .comment("Enable the optional stage-10 non-consuming Long.MAX_VALUE Immortal Yuan channel. "
                        + "The default false behavior keeps storage uncapped and generates 256 every 20 ticks.")
                .define("stageTenInfiniteImmortalYuan", false);
        BUILDER.translation(key("progression.tribulation_targets")).push("tribulation_targets");
        TRIBULATION_TARGET_STAGE_6 = BUILDER.translation(key("progression.tribulation_targets.stage6To7")).define("stage6To7", "minecraft:zombie");
        TRIBULATION_TARGET_STAGE_7 = BUILDER.translation(key("progression.tribulation_targets.stage7To8")).define("stage7To8", "minecraft:wither_skeleton");
        TRIBULATION_TARGET_STAGE_8 = BUILDER.translation(key("progression.tribulation_targets.stage8To9")).define("stage8To9", "minecraft:vindicator");
        TRIBULATION_TARGET_STAGE_9 = BUILDER.translation(key("progression.tribulation_targets.stage9To10")).define("stage9To10", "minecraft:warden");
        BUILDER.pop(2);
        boolean energyCompat = CompatManager.MEKANISM_LOADED || CompatManager.FLUX_NETWORKS_LOADED;
        if (energyCompat || CompatManager.BOTANIA_LOADED || CompatManager.ARS_NOUVEAU_LOADED) {
            BUILDER.translation(key("resource_conversion")).push("resource_conversion");
            FE_CONVERSION = energyCompat
                    ? defineConversion("fe", 20_000L, 200_000L)
                    : null;
            BOTANIA_MANA_CONVERSION = CompatManager.BOTANIA_LOADED
                    ? defineConversion("botaniaMana", 10_000L, 50_000L)
                    : null;
            ARS_SOURCE_CONVERSION = CompatManager.ARS_NOUVEAU_LOADED
                    ? defineConversion("arsSource", 1_000L, 5_000L)
                    : null;
            BUILDER.pop();
        } else {
            FE_CONVERSION = null;
            BOTANIA_MANA_CONVERSION = null;
            ARS_SOURCE_CONVERSION = null;
        }
        SPEC = BUILDER.build();
    }

    public static ConversionPolicy conversionPolicy(ResourceChannelKey key) {
        ConversionValues values = key == null ? null
                : key.equals(ExternalResourceChannels.FE) ? FE_CONVERSION
                : key.equals(ExternalResourceChannels.BOTANIA_MANA) ? BOTANIA_MANA_CONVERSION
                : key.equals(ExternalResourceChannels.ARS_NOUVEAU_SOURCE) ? ARS_SOURCE_CONVERSION
                : null;
        return values == null
                ? ConversionPolicy.DISABLED
                : new ConversionPolicy(values.enabled().get(), values.resourcePerImmortalYuan().get(),
                        values.maximumConversionPerTick().get());
    }

    private static ConversionValues defineConversion(
            String name, long defaultRatio, long defaultPerTickLimit) {
        BUILDER.translation(key("resource_conversion." + name)).push(name);
        ModConfigSpec.BooleanValue enabled = BUILDER
                .translation(key("resource_conversion." + name + ".enabled"))
                .comment("Allow Immortal Yuan to cover this external resource only after stored resource is exhausted.")
                .define("enabled", true);
        ModConfigSpec.LongValue ratio = BUILDER
                .translation(key("resource_conversion." + name + ".resourcePerImmortalYuan"))
                .comment("External resource generated by one Immortal Yuan. Whole Immortal Yuan is consumed; unused output is retained in storage.")
                .defineInRange("resourcePerImmortalYuan", defaultRatio, 1L, Long.MAX_VALUE);
        ModConfigSpec.LongValue perTick = BUILDER
                .translation(key("resource_conversion." + name + ".maximumConversionPerTick"))
                .comment("Maximum resource generated by Immortal Yuan conversion for one interface in one server tick. Stored cache use is not limited.")
                .defineInRange("maximumConversionPerTick", defaultPerTickLimit, 1L, Long.MAX_VALUE);
        BUILDER.pop();
        return new ConversionValues(enabled, ratio, perTick);
    }

    public record ConversionValues(
            ModConfigSpec.BooleanValue enabled,
            ModConfigSpec.LongValue resourcePerImmortalYuan,
            ModConfigSpec.LongValue maximumConversionPerTick) {}

    public record ConversionPolicy(boolean enabled, long resourcePerImmortalYuan,
                                   long maximumConversionPerTick) {
        public static final ConversionPolicy DISABLED = new ConversionPolicy(false, 1L, 0L);
    }

    private static String key(String path) {
        return "immortalstorage.configuration." + path;
    }

    private ImmortalStorageConfig() {}
}
