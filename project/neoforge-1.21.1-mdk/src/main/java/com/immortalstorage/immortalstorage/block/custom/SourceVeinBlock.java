package com.immortalstorage.immortalstorage.block.custom;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity;
import com.immortalstorage.immortalstorage.item.ModDataComponents;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinitions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SourceVeinBlock extends BaseEntityBlock {
    private final VeinKind kind;
    private final boolean genericDefinitionCarrier;
    public SourceVeinBlock(VeinKind kind) {
        this(kind, false);
    }

    public SourceVeinBlock(VeinKind kind, boolean genericDefinitionCarrier) {
        super(idProps(kind.name().toLowerCase(), BlockBehaviour.Properties.of().strength(1.0f).lightLevel(s -> 4).requiresCorrectToolForDrops().noOcclusion()));
        this.kind = kind;
        this.genericDefinitionCarrier = genericDefinitionCarrier;
        this.registerDefaultState(this.stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
    }

    private static BlockBehaviour.Properties idProps(String name, BlockBehaviour.Properties p) {
        return p;
    }
    public VeinKind getKind() { return kind; }
    public boolean isGenericDefinitionCarrier() { return genericDefinitionCarrier; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(HorizontalDirectionalBlock.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (genericDefinitionCarrier) {
            var definitionId = ctx.getItemInHand().get(ModDataComponents.SOURCE_DEFINITION_ID.get());
            if (definitionId == null || SourceDefinitions.find(definitionId).isEmpty()) return null;
        }
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && genericDefinitionCarrier
                && level.getBlockEntity(pos) instanceof SourceVeinBlockEntity source) {
            source.setSourceDefinitionId(stack.get(ModDataComponents.SOURCE_DEFINITION_ID.get()));
        }
        if (!level.isClientSide && level instanceof net.minecraft.server.level.ServerLevel serverLevel
                && level.getBlockEntity(pos) instanceof SourceVeinBlockEntity source) {
            com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions
                    .personalRealmOwner(serverLevel.dimension()).ifPresent(source::setOwner);
        }
    }

    @Override
    protected MapCodec<? extends SourceVeinBlock> codec() {
        return simpleCodec(p -> new SourceVeinBlock(kind, genericDefinitionCarrier));
    }

    @Override
    public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return new SourceVeinBlockEntity(p, s, kind);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState s, BlockEntityType<T> t) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof SourceVeinBlockEntity v) v.serverTick();
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SourceVeinBlockEntity source) || !source.fluidSource()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ItemStack filled = source.filledVanillaContainer();
        if (filled.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(Items.BUCKET) && !player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                SourceVeinBlockEntity.ClaimResult claim = source.claimFor(player);
                if (claim == SourceVeinBlockEntity.ClaimResult.DENIED) {
                    player.displayClientMessage(Component.translatable("message.immortalstorage.source_vein.claim.denied"), true);
                    return ItemInteractionResult.CONSUME;
                }
                if (claim == SourceVeinBlockEntity.ClaimResult.STAGE_TOO_LOW) {
                    player.displayClientMessage(Component.translatable("message.immortalstorage.source_vein.claim.stage", kind.minStage), true);
                    return ItemInteractionResult.CONSUME;
                }
                IFluidHandler handler = source.getFluidHandler();
                if (handler == null) return ItemInteractionResult.CONSUME;
                FluidStack simulated = handler.drain(1000, IFluidHandler.FluidAction.SIMULATE);
                if (simulated.getAmount() != 1000 || simulated.getFluid() != source.sampleFluid()) {
                    return ItemInteractionResult.CONSUME;
                }
                FluidStack extracted = handler.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                if (extracted.getAmount() != 1000 || extracted.getFluid() != source.sampleFluid()) {
                    source.rollbackFluidExtraction(extracted.getAmount());
                    return ItemInteractionResult.CONSUME;
                }
                replaceHeldContainer(player, hand, stack, filled);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isShiftKeyDown() && ItemStack.isSameItemSameComponents(stack, filled)) {
            if (!level.isClientSide) {
                replaceHeldContainer(player, hand, stack, new ItemStack(Items.BUCKET));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static void replaceHeldContainer(Player player, InteractionHand hand, ItemStack held, ItemStack replacement) {
        if (player.getAbilities().instabuild) {
            if (!player.getInventory().contains(replacement)) {
                player.getInventory().add(replacement);
            }
            return;
        }
        held.shrink(1);
        if (held.isEmpty()) {
            player.setItemInHand(hand, replacement);
        } else if (!player.getInventory().add(replacement)) {
            player.drop(replacement, false);
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState s, Level level, BlockPos pos, Player p, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SourceVeinBlockEntity v) {
            SourceVeinBlockEntity.ClaimResult claim = v.claimFor(p);
            if (claim == SourceVeinBlockEntity.ClaimResult.DENIED) {
                p.displayClientMessage(Component.translatable("message.immortalstorage.source_vein.claim.denied"), true);
                return InteractionResult.CONSUME;
            }
            if (claim == SourceVeinBlockEntity.ClaimResult.STAGE_TOO_LOW) {
                p.displayClientMessage(Component.translatable("message.immortalstorage.source_vein.claim.stage", kind.minStage), true);
                return InteractionResult.CONSUME;
            }
            if (claim == SourceVeinBlockEntity.ClaimResult.CLAIMED_OTHER) {
                p.displayClientMessage(Component.translatable("message.immortalstorage.source_vein.claim.success"), true);
            }
            if (p.isShiftKeyDown() && !v.fluidSource()) {
                v.takeManualBatch(p);
                return InteractionResult.CONSUME;
            }
            if (p instanceof ServerPlayer sp) {
                sp.openMenu(v, pos);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(state, builder);
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof SourceVeinBlockEntity source)) return drops;
        ItemStack preservedDrop = ItemStack.EMPTY;
        for (ItemStack drop : drops) {
            if (drop.getItem() != asItem()) continue;
            preservedDrop = drop;
            break;
        }
        if (preservedDrop.isEmpty()) {
            drops = new ArrayList<>(drops);
            preservedDrop = new ItemStack(this);
            drops.add(preservedDrop);
        }
        source.saveToItem(preservedDrop, builder.getLevel().registryAccess());
        if (genericDefinitionCarrier) {
            preservedDrop.set(ModDataComponents.SOURCE_DEFINITION_ID.get(), source.sourceDefinitionId());
        }
        return drops;
    }
}
