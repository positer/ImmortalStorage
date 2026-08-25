package com.immortalstorage.immortalstorage.item.custom;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.immortalstorage.immortalstorage.compat.mc2612.CompatItem;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages;
import com.immortalstorage.immortalstorage.compat.mc2612.CompatValueIo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.entity.BlockEntity;

/** 26.1.2 ValueInput/ValueOutput implementation of the information crystal. */
public final class InformationStorageCrystalItem extends CompatItem {
    private static final String ROOT = "ImmortalStorageSnapshots";

    public InformationStorageCrystalItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CompoundTag snapshots = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getCompoundOrEmpty(ROOT);
        if (snapshots.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.immortalstorage.information_storage.empty"));
            return;
        }
        for (String key : keys(snapshots)) {
            Component name = Component.literal(key);
            try {
                Identifier id = Identifier.parse(key);
                var type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id).map(holder -> holder.value()).orElse(null);
                if (type != null) {
                    var block = type.getValidBlocks().stream().findFirst().orElse(null);
                    if (block != null) name = block.getName();
                }
            } catch (RuntimeException ignored) { }
            tooltip.add(Component.translatable("tooltip.immortalstorage.information_storage.entry", name));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof BlockEntity machine)
                || !isSupported(machine)) {
            if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
                crystalClear(context.getItemInHand(), context.getPlayer());
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (context.getPlayer() == null) return InteractionResult.PASS;
        Identifier type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(machine.getType());
        if (type == null) return InteractionResult.PASS;
        String key = type.toString();
        Component machineName = machine.getBlockState().getBlock().getName();
        ItemStack crystal = context.getItemInHand();
        CompoundTag crystalTag = crystal.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag snapshots = crystalTag.getCompoundOrEmpty(ROOT);
        if (context.getPlayer().isShiftKeyDown()) {
            CompoundTag saved = CompatValueIo.serialize(machine, context.getLevel().registryAccess());
            saved.remove("id"); saved.remove("x"); saved.remove("y"); saved.remove("z");
            snapshots.put(key, saved);
            crystalTag.put(ROOT, snapshots);
            crystal.set(DataComponents.CUSTOM_DATA, CustomData.of(crystalTag));
            CompatMessages.sendSystemMessage(context.getPlayer(), Component.translatable(
                    "message.immortalstorage.information_storage.saved", machineName), true);
        } else if (!snapshots.contains(key)) {
            CompatMessages.sendSystemMessage(context.getPlayer(), Component.translatable(
                    "message.immortalstorage.information_storage.missing", machineName), true);
        } else {
            CompatValueIo.deserialize(machine, context.getLevel().registryAccess(), snapshots.getCompoundOrEmpty(key));
            machine.setChanged();
            context.getLevel().sendBlockUpdated(machine.getBlockPos(), machine.getBlockState(), machine.getBlockState(), 3);
            CompatMessages.sendSystemMessage(context.getPlayer(), Component.translatable(
                    "message.immortalstorage.information_storage.pasted", machineName), true);
        }
        return InteractionResult.SUCCESS;
    }

    private static List<String> keys(CompoundTag tag) {
        try {
            Method method;
            try { method = tag.getClass().getMethod("keySet"); }
            catch (NoSuchMethodException ignored) { method = tag.getClass().getMethod("getAllKeys"); }
            Object value = method.invoke(tag);
            if (value instanceof java.util.Set<?> set) return set.stream().map(Object::toString).toList();
            if (value instanceof Iterable<?> iterable) {
                List<String> result = new ArrayList<>();
                for (Object entry : iterable) result.add(String.valueOf(entry));
                return result;
            }
        } catch (ReflectiveOperationException ignored) { }
        return List.of();
    }

    private static void crystalClear(ItemStack crystal, net.minecraft.world.entity.player.Player player) {
        crystal.remove(DataComponents.CUSTOM_DATA);
        CompatMessages.sendSystemMessage(player, Component.translatable(
                "message.immortalstorage.information_storage.cleared"), true);
    }

    private static boolean isSupported(BlockEntity entity) {
        String name = entity.getClass().getName();
        return name.contains("SimulatedSpiritFieldBlockEntity") || name.contains("SimulatedReincarnationFurnaceBlockEntity")
                || name.contains("XianqiaoInterfaceBlockEntity") || name.contains("MiniatureImmortalRuinBlockEntity")
                || name.contains("StabilizedMiniatureImmortalRuinBlockEntity") || name.contains("EntangledStabilizedMiniatureImmortalRuinBlockEntity")
                || name.contains("AdvancedStabilizedMiniatureImmortalRuinBlockEntity")
                || name.contains("AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity")
                || name.contains("EnergyCrystalBlockEntity")
                || name.startsWith("mekanism.") || name.contains(".mekanism.");
    }
}
