package com.immortalstorage.immortalstorage.item.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent, stack-bound undo log for the Immortal Artifact build remover. */
public final class ImmortalArtifactRestorationLog {
    private static final String ROOT = "immortalArtifactRestoration";
    private static final String OWNER = "owner";
    private static final String DIMENSION = "dimension";
    private static final String ENTRIES = "entries";
    private static final int MAX_ENTRIES = 4096;

    private ImmortalArtifactRestorationLog() {
    }

    public static void replace(ItemStack artifact, ServerPlayer owner, List<Entry> entries) {
        if (!(artifact.getItem() instanceof ImmortalArtifactItem) || entries == null || entries.isEmpty()) return;
        CompoundTag itemData = artifact.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag root = new CompoundTag();
        root.putString(OWNER, owner.getUUID().toString());
        root.putString(DIMENSION, owner.level().dimension().identifier().toString());
        ListTag list = new ListTag();
        int start = Math.max(0, entries.size() - MAX_ENTRIES);
        for (int i = start; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            CompoundTag tag = new CompoundTag();
            tag.putLong("pos", entry.pos().asLong());
            tag.put("state", NbtUtils.writeBlockState(entry.state()));
            list.add(tag);
        }
        root.put(ENTRIES, list);
        itemData.put(ROOT, root);
        artifact.set(DataComponents.CUSTOM_DATA, CustomData.of(itemData));
    }

    public static int restore(ServerPlayer player, ItemStack artifact) {
        if (!(artifact.getItem() instanceof ImmortalArtifactItem)) return 0;
        CompoundTag itemData = artifact.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!itemData.contains(ROOT)) return 0;
        CompoundTag root = itemData.getCompoundOrEmpty(ROOT);
        if (!player.getUUID().toString().equals(root.getStringOr(OWNER, ""))) return 0;
        Identifier dimensionId = Identifier.tryParse(root.getStringOr(DIMENSION, ""));
        if (dimensionId == null || !player.level().dimension().identifier().equals(dimensionId)) return 0;

        ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();
        ListTag entries = root.getListOrEmpty(ENTRIES);
        ListTag remaining = new ListTag();
        int restored = 0;
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag tag = entries.getCompoundOrEmpty(i);
            BlockPos pos = BlockPos.of(tag.getLongOr("pos", 0L));
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).isAir()
                    || !level.mayInteract(player, pos) || !com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayer.canInteractWithBlock(player, pos, 1.0D)) {
                remaining.add(tag.copy());
                continue;
            }
            BlockState state = NbtUtils.readBlockState(
                    level.registryAccess().lookupOrThrow(Registries.BLOCK), tag.getCompoundOrEmpty("state"));
            if (state.isAir() || !level.setBlock(pos, state, 3)) {
                remaining.add(tag.copy());
                continue;
            }
            restored++;
        }
        if (remaining.isEmpty()) itemData.remove(ROOT);
        else {
            root.put(ENTRIES, remaining);
            itemData.put(ROOT, root);
        }
        artifact.set(DataComponents.CUSTOM_DATA, CustomData.of(itemData));
        if (restored > 0) player.getCooldowns().addCooldown(artifact, 4);
        return restored;
    }

    public record Entry(BlockPos pos, BlockState state) {
        public Entry {
            pos = pos.immutable();
        }
    }
}
