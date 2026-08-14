package com.immortalstorage.immortalstorage.worldshard;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Discovers ores that are generated inside structure templates rather than as
 * biome placed features.  The vanilla placed-feature scanner in
 * {@link WorldShardOreScanner} cannot see these because a block placed by a
 * structure template (for example allthemodium inside an ancient city, or a
 * mod's custom ore embedded in its own structure) is not an {@code OreFeature}.
 *
 * <p>The scanner enumerates every datapack {@code structures/**.nbt} template
 * and reads its {@code palettes} directly from NBT, so it runs on both the
 * initial reload and a later {@code /reload} without needing a live
 * {@code StructureTemplateManager}.  A block counts as an ore when it is in
 * the {@code c:ores} common tag, matching the convention JEI and most ore
 * mods use for world generation.
 *
 * <p>Structure templates carry no dimension tag of their own, so dimension
 * assignment follows the template path (the same convention the vanilla
 * structure files already encode: {@code end_city/*}, {@code bastion/*},
 * {@code nether_*}).  Every other template — including modded structures that
 * extend the ancient city — belongs to the Overworld.
 */
public final class WorldShardStructureOreScanner {
    private static final Logger LOG = LoggerFactory.getLogger(ImmortalStorageMod.MODID + ".worldshard.ore.structure");
    private static final long WEIGHT_PER_BLOCK = WorldShardOreScanner.WEIGHT_SCALE;

    private WorldShardStructureOreScanner() {
    }

    /**
     * Scans all structure templates and returns the {@code c:ores} blocks found
     * in templates assigned to {@code mode}, weighted by occurrence count.
     */
    public static Map<Item, Long> scanStructureOres(
            ResourceManager resourceManager, HolderGetter<Block> blockGetter, ResourceLocation mode) {
        Map<Item, Long> weights = new HashMap<>();
        Map<ResourceLocation, Resource> templates = listTemplates(resourceManager);
        for (Map.Entry<ResourceLocation, Resource> entry : templates.entrySet()) {
            if (!mode.equals(modeFor(entry.getKey()))) continue;
            scanTemplate(entry.getKey(), entry.getValue(), blockGetter, weights);
        }
        LOG.info("Structure ore scan for mode {} found {} distinct ores across {} templates",
                mode, weights.size(), templates.size());
        return Map.copyOf(weights);
    }

    static Map<ResourceLocation, Resource> listTemplates(ResourceManager resourceManager) {
        Map<ResourceLocation, Resource> templates = new HashMap<>();
        resourceManager.listResources("structures",
                path -> path.getPath().endsWith(".nbt")).forEach(templates::put);
        return templates;
    }

    private static void scanTemplate(ResourceLocation templateId, Resource resource,
                                     HolderGetter<Block> blockGetter, Map<Item, Long> weights) {
        try (InputStream stream = resource.open()) {
            CompoundTag root = NbtIo.readCompressed(stream, new NbtAccounter(Long.MAX_VALUE, 512));
            ListTag palettes = root.getList("palettes", ListTag.TAG_LIST);
            if (palettes.isEmpty()) {
                // Older/single-palette templates keep the flat "blocks" list.
                scanBlocks(root.getList("blocks", CompoundTag.TAG_COMPOUND), blockGetter, weights, templateId);
                return;
            }
            for (int paletteIndex = 0; paletteIndex < palettes.size(); paletteIndex++) {
                ListTag palette = palettes.getList(paletteIndex);
                for (int stateIndex = 0; stateIndex < palette.size(); stateIndex++) {
                    CompoundTag stateTag = palette.getCompound(stateIndex);
                    BlockState state = NbtUtils.readBlockState(blockGetter, stateTag);
                    countOre(state, weights);
                }
            }
        } catch (Exception error) {
            LOG.warn("Could not scan structure template {} for ores: {}", templateId, error.getMessage());
        }
    }

    private static void scanBlocks(ListTag blocks, HolderGetter<Block> blockGetter,
                                   Map<Item, Long> weights, ResourceLocation templateId) {
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag entry = blocks.getCompound(i);
            if (!entry.contains("state", CompoundTag.TAG_COMPOUND)) continue;
            try {
                BlockState state = NbtUtils.readBlockState(blockGetter, entry.getCompound("state"));
                countOre(state, weights);
            } catch (RuntimeException error) {
                LOG.warn("Could not decode block state in structure template {}: {}", templateId, error.getMessage());
            }
        }
    }

    private static void countOre(BlockState state, Map<Item, Long> weights) {
        if (state == null || state.isAir()) return;
        if (!state.is(Tags.Blocks.ORES)) return;
        Item item = state.getBlock().asItem();
        if (item == Items.AIR) return;
        weights.merge(item, WEIGHT_PER_BLOCK, WorldShardStructureOreScanner::saturatingAdd);
    }

    /** Assigns a template to the dimension its path encodes (see class javadoc). */
    static ResourceLocation modeFor(ResourceLocation templateId) {
        String path = templateId.getPath();
        if (path.startsWith("structures/end_") || path.contains("/end_") || path.contains("end_city")) {
            return WorldShardMinerModes.END;
        }
        if (path.contains("nether") || path.contains("bastion") || path.contains("fortress")) {
            return WorldShardMinerModes.NETHER;
        }
        return WorldShardMinerModes.OVERWORLD;
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }
}
