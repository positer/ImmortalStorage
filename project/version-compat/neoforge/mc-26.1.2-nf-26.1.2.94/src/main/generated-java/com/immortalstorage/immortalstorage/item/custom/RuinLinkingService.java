package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity;
import com.immortalstorage.immortalstorage.block.entity.StabilizedMiniatureImmortalRuinBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Stores one same-dimension ruin endpoint in the wrench and creates opposite-state peer links. */
public final class RuinLinkingService {
    private static final String TYPE = "ruinLinkType", DIM = "ruinLinkDimension", POS = "ruinLinkPos";

    public static boolean interact(ServerPlayer player, ItemStack staff, BlockEntity target) {
        String type = target instanceof StabilizedMiniatureImmortalRuinBlockEntity ? "stabilized"
                : target instanceof MiniatureImmortalRuinBlockEntity ? "miniature" : "";
        if (type.isEmpty() || !(target.getLevel() instanceof ServerLevel level)) return false;
        CompoundTag tag = staff.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String dimension = level.dimension().identifier().toString();
        if (!type.equals(tag.getStringOr(TYPE, ""))) {
            save(tag, type, dimension, target.getBlockPos());
            staff.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable("message.immortalstorage.ruin_link.saved"), true);
            return true;
        }
        if (!dimension.equals(tag.getStringOr(DIM, ""))) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable("message.immortalstorage.ruin_link.same_dimension"), true);
            return true;
        }
        BlockPos sourcePos = BlockPos.of(tag.getLongOr(POS, 0L));
        BlockEntity source = level.getBlockEntity(sourcePos);
        if (source == null || source == target || !sameType(source, type)) {
            save(tag, type, dimension, target.getBlockPos());
            staff.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable("message.immortalstorage.ruin_link.saved"), true);
            return true;
        }
        boolean sourceReversed = reversed(source), targetReversed = reversed(target);
        if (sourceReversed == targetReversed) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable("message.immortalstorage.ruin_link.requires_opposite"), true);
            return true;
        }
        if (source instanceof StabilizedMiniatureImmortalRuinBlockEntity a
                && target instanceof StabilizedMiniatureImmortalRuinBlockEntity b) a.linkWith(b, level);
        if (source instanceof MiniatureImmortalRuinBlockEntity a
                && target instanceof MiniatureImmortalRuinBlockEntity b) {
            a.setLinkedPos(b.getBlockPos()); b.setLinkedPos(a.getBlockPos());
        }
        clear(staff);
        com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable("message.immortalstorage.ruin_link.connected"), true);
        return true;
    }

    public static boolean clear(ItemStack staff) {
        CompoundTag tag = staff.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(TYPE)) return false;
        tag.remove(TYPE); tag.remove(DIM); tag.remove(POS);
        staff.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return true;
    }

    public static BlockPos savedPos(ItemStack staff, ServerLevel level) {
        CompoundTag tag = staff.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return level.dimension().identifier().toString().equals(tag.getStringOr(DIM, "")) && tag.contains(POS)
                ? BlockPos.of(tag.getLongOr(POS, 0L)) : null;
    }

    private static void save(CompoundTag tag, String type, String dimension, BlockPos pos) {
        tag.putString(TYPE, type); tag.putString(DIM, dimension); tag.putLong(POS, pos.asLong());
    }
    private static boolean sameType(BlockEntity entity, String type) {
        return "stabilized".equals(type) ? entity instanceof StabilizedMiniatureImmortalRuinBlockEntity
                : entity instanceof MiniatureImmortalRuinBlockEntity;
    }
    private static boolean reversed(BlockEntity entity) {
        return entity instanceof StabilizedMiniatureImmortalRuinBlockEntity stable ? stable.reversed()
                : ((MiniatureImmortalRuinBlockEntity) entity).isReversed();
    }
    private RuinLinkingService() {}
}
