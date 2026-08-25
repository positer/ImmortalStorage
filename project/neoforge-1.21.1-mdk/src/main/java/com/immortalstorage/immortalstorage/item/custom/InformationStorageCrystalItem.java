package com.immortalstorage.immortalstorage.item.custom;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import java.util.List;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Stores the latest configuration/state snapshot for each ImmortalStorage machine type. */
public final class InformationStorageCrystalItem extends Item {
    private static final String ROOT = "ImmortalStorageSnapshots";

    public InformationStorageCrystalItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<net.minecraft.network.chat.Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CompoundTag snapshots = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompound(ROOT);
        if (snapshots.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.immortalstorage.information_storage.empty"));
            return;
        }
        for (String key : snapshots.getAllKeys()) {
            Component name = Component.literal(key);
            try {
                ResourceLocation id = ResourceLocation.parse(key);
                var type = BuiltInRegistries.BLOCK_ENTITY_TYPE.get(id);
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
                return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
            }
            return InteractionResult.PASS;
        }
        ItemStack crystal = context.getItemInHand();
        if (context.getPlayer() == null) return InteractionResult.PASS;
        ResourceLocation type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(machine.getType());
        if (type == null) return InteractionResult.PASS;
        String key = type.toString();
        Component machineName = machine.getBlockState().getBlock().getName();
        CompoundTag crystalTag = crystal.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag snapshots = crystalTag.getCompound(ROOT);
        if (context.getPlayer().isShiftKeyDown()) {
            CompoundTag saved = machine.saveWithoutMetadata(context.getLevel().registryAccess());
            saved.remove("id");
            saved.remove("x");
            saved.remove("y");
            saved.remove("z");
            snapshots.put(key, saved);
            crystalTag.put(ROOT, snapshots);
            crystal.set(DataComponents.CUSTOM_DATA, CustomData.of(crystalTag));
            context.getPlayer().displayClientMessage(Component.translatable(
                    "message.immortalstorage.information_storage.saved", machineName), true);
        } else if (!snapshots.contains(key)) {
            context.getPlayer().displayClientMessage(Component.translatable(
                    "message.immortalstorage.information_storage.missing", machineName), true);
        } else {
            machine.loadWithComponents(snapshots.getCompound(key), context.getLevel().registryAccess());
            machine.setChanged();
            context.getLevel().sendBlockUpdated(machine.getBlockPos(), machine.getBlockState(),
                    machine.getBlockState(), 3);
            context.getPlayer().displayClientMessage(Component.translatable(
                    "message.immortalstorage.information_storage.pasted", machineName), true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    private static void crystalClear(ItemStack crystal, net.minecraft.world.entity.player.Player player) {
        crystal.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        player.displayClientMessage(Component.translatable(
                "message.immortalstorage.information_storage.cleared"), true);
    }

    private static boolean isSupported(BlockEntity entity) {
        String name = entity.getClass().getName();
        return name.contains("SimulatedSpiritFieldBlockEntity")
                || name.contains("SimulatedReincarnationFurnaceBlockEntity")
                || name.contains("XianqiaoInterfaceBlockEntity")
                || name.contains("MiniatureImmortalRuinBlockEntity")
                || name.contains("StabilizedMiniatureImmortalRuinBlockEntity")
                || name.contains("EntangledStabilizedMiniatureImmortalRuinBlockEntity")
                || name.contains("AdvancedStabilizedMiniatureImmortalRuinBlockEntity")
                || name.contains("AdvancedEntangledStabilizedMiniatureImmortalRuinBlockEntity")
                || name.contains("EnergyCrystalBlockEntity")
                || name.startsWith("mekanism.") || name.contains(".mekanism.");
    }
}
