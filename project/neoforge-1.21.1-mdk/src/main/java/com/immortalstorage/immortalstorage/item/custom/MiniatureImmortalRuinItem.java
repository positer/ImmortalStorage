package com.immortalstorage.immortalstorage.item.custom;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Places the unbreakable field anchor represented by this one-stack item. */
public final class MiniatureImmortalRuinItem extends Item {
    public MiniatureImmortalRuinItem(Properties properties) { super(properties); }

    @Override
    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.immortalstorage.immortalstorage.client.render.WorldBlockItemRenderer.INSTANCE;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.immortalstorage.miniature_immortal_ruin.range"));
        tooltip.add(Component.translatable("tooltip.immortalstorage.miniature_immortal_ruin.holder"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (!level.getBlockState(pos).canBeReplaced()) return InteractionResult.FAIL;
        var customData = context.getItemInHand().get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        net.minecraft.nbt.CompoundTag saved = customData == null ? new net.minecraft.nbt.CompoundTag() : customData.copyTag();
        if (!level.isClientSide && level.setBlock(pos, ModBlocks.MINIATURE_IMMORTAL_RUIN.get().defaultBlockState()
                .setValue(com.immortalstorage.immortalstorage.block.custom.MiniatureImmortalRuinBlock.REVERSED, saved.getBoolean("Reversed")), 3)
                && context.getPlayer() != null) {
            if (level.getBlockEntity(pos) instanceof com.immortalstorage.immortalstorage.block.entity.MiniatureImmortalRuinBlockEntity ruin) {
                ruin.restoreFromItem(saved);
                ruin.compressor().placedBy(com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.id(context.getPlayer()));
                level.sendBlockUpdated(pos, ruin.getBlockState(), ruin.getBlockState(), 3);
            }
            if (!context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
