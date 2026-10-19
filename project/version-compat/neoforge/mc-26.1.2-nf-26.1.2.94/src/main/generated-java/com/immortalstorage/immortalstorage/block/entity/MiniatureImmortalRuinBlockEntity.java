package com.immortalstorage.immortalstorage.block.entity;

import com.immortalstorage.immortalstorage.block.custom.MiniatureImmortalRuinBlock;
import com.immortalstorage.immortalstorage.compressor.RuinMachineKind;
import com.immortalstorage.immortalstorage.compressor.SpiritCompressorStructure;
import com.immortalstorage.immortalstorage.item.ModItems;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative horizontal field for the placed miniature ruin. */
public final class MiniatureImmortalRuinBlockEntity extends com.immortalstorage.immortalstorage.compat.mc2612.CompatBlockEntity implements MenuProvider {
    private final SpiritCompressorLogic compressor = new SpiritCompressorLogic(this);
    private final EnchantmentMachineLogic enchanter = new EnchantmentMachineLogic(this);
    private RuinMachineKind lastKind;
    private boolean kindKnown;
    public SpiritCompressorLogic compressor() { return compressor; }
    EnchantmentMachineLogic enchanter() { return enchanter; }
    /** The machine the shell currently runs; an unformed ruin keeps the spirit logic. */
    public RuinMachine machine() { return machineKind() == RuinMachineKind.ENCHANTMENT ? enchanter : compressor; }
    /** Derived from the block above the core every time it is read; never stored in NBT. */
    public RuinMachineKind machineKind() { return SpiritCompressorStructure.kindAt(level, worldPosition); }
    /** One ruin records one owner, so a later kind change keeps the Xianqiao destination. */
    public void placedBy(java.util.UUID id) { compressor.placedBy(id); enchanter.placedBy(id); }
    /** Every inventory this controller owns, in the order the drop paths drain them. */
    public java.util.List<ItemStackHandler> machineInventories() {
        return java.util.List.of(compressor.inventory, compressor.decompressionInventory, enchanter.inventory, enchanter.decompressionInventory);
    }
    /** v1.2 teardown rule: every path that destroys or picks up the machine voids the uncollected product. */
    public void discardUncollectedProducts() { compressor.discardPending(); enchanter.discardPending(); }
    /**
     * Polarity switch and family swap: void the uncollected product first, then hand both caches to the caller
     * (the spirit machine has nothing to void). Extracted so the order is testable without a server level.
     */
    void ejectBothCachesAfterTeardown(java.util.function.Consumer<RuinMachine> eject) {
        eject.accept(compressor);
        enchanter.discardPending();
        eject.accept(enchanter);
    }
    /**
     * One kind transition. Ruling 3 applies to any change: both machines void an uncollected product and leave
     * the materials in their slots. Only a deliberate family swap hands the caches over - a lost or re-formed
     * shell keeps them inside the block, which is the frozen 0.1.6 behaviour for the spirit machine.
     */
    void applyKindTransition(RuinMachineKind previous, RuinMachineKind kind, java.util.function.Consumer<RuinMachine> eject) {
        if (kind == previous) return;
        discardUncollectedProducts();
        if (kind == null || previous == null) return;
        compressor.invalidatePorts();
        enchanter.invalidatePorts();
        ejectBothCachesAfterTeardown(eject);
    }
    public void restoreFromItem(CompoundTag tag) { if (level != null && !level.isClientSide()) loadAdditionalLegacy(tag, level.registryAccess()); }
    private boolean affectPlayers;
    private boolean entityDamage = true;
    private boolean playerDamage;
    private int forceMode = 2;
    private BlockPos linkedPos;
    private boolean warpEnabled;
    public MiniatureImmortalRuinBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINIATURE_IMMORTAL_RUIN.get(), pos, state);
    }

    @Override public void setBlockState(BlockState nextState) {
        BlockState previous = getBlockState();
        if (compressor != null && enchanter != null && level instanceof ServerLevel server
                && previous.getBlock() == nextState.getBlock()
                && previous.hasProperty(MiniatureImmortalRuinBlock.REVERSED)
                && nextState.hasProperty(MiniatureImmortalRuinBlock.REVERSED)
                && previous.getValue(MiniatureImmortalRuinBlock.REVERSED) != nextState.getValue(MiniatureImmortalRuinBlock.REVERSED)) {
            // A polarity switch keeps the 0.1.6 behaviour: void the uncollected product, then eject both caches.
            ejectBothCachesAfterTeardown(machine -> machine.ejectCaches(server));
        }
        super.setBlockState(nextState);
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel serverLevel)) return;
        trackMachineKind(serverLevel);
        RuinMachine machine = machine();
        machine.updateFormation();
        if (!MachineRedstoneControl.allows(this)) return;
        if (machine.formed()) { machine.tick(serverLevel); return; }
        boolean reversed = getBlockState().getValue(MiniatureImmortalRuinBlock.REVERSED);
        Vec3 center = Vec3.atCenterOf(worldPosition);
        AABB area = MiniatureImmortalRuinEffectPolicy.effectArea(worldPosition);
        AABB centerArea = new AABB(worldPosition);
        if (reversed && forceMode == 4) forceMode = 0;
        for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity.isAlive() && canAffect(entity))) {
            Vec3 horizontal = center.subtract(entity.position()).multiply(1.0D, 0.0D, 1.0D);
            if (horizontal.lengthSqr() > 0.01D) {
                if (!reversed && forceMode == 4) {
                    entity.teleportTo(center.x, entity.getY(), center.z);
                } else if (forceMode > 0) {
                    double strength = switch (forceMode) { case 1 -> 0.04D; case 3 -> 0.2D; default -> 0.1D; };
                    Vec3 impulse = horizontal.normalize().scale(reversed ? -strength : strength);
                    entity.setDeltaMovement(entity.getDeltaMovement().add(impulse));
                    entity.hurtMarked = true;
                }
            }
            boolean mayDamage = entity instanceof Player ? playerDamage : entityDamage;
            if (mayDamage && entity.getBoundingBox().intersects(centerArea)) {
                entity.hurt(serverLevel.damageSources().magic(), 5.0F);
            }
        }
        if (!reversed && warpEnabled) warpLinkedEntities(serverLevel, centerArea);
    }

    /** The block above the core decides the family; the transition policy lives in applyKindTransition. */
    private void trackMachineKind(ServerLevel level) {
        RuinMachineKind kind = machineKind();
        if (!kindKnown) { kindKnown = true; lastKind = kind; return; }
        RuinMachineKind previous = lastKind;
        lastKind = kind;
        applyKindTransition(previous, kind, machine -> machine.ejectCaches(level));
    }

    private void warpLinkedEntities(ServerLevel serverLevel, AABB area) {
        if (linkedPos == null || !(serverLevel.getBlockEntity(linkedPos) instanceof MiniatureImmortalRuinBlockEntity peer)
                || !peer.isReversed()) return;
        Vec3 target = Vec3.atCenterOf(linkedPos);
        for (Entity entity : serverLevel.getEntities((Entity) null, area, entity -> entity.isAlive()
                && (entity instanceof ItemEntity
                || entity instanceof LivingEntity living && canAffect(living)))) {
            entity.teleportTo(target.x, target.y + 0.5D, target.z);
            entity.setDeltaMovement(Vec3.ZERO);
        }
    }

    private boolean canAffect(LivingEntity entity) {
        boolean isPlayer = entity instanceof Player;
        boolean holdingMiniatureRuin = isPlayer && isHoldingMiniatureRuin((Player) entity);
        return MiniatureImmortalRuinEffectPolicy.shouldAffectLivingEntity(
                isPlayer, affectPlayers, holdingMiniatureRuin);
    }

    private static boolean isHoldingMiniatureRuin(Player player) {
        return player.getMainHandItem().is(ModItems.MINIATURE_IMMORTAL_RUIN.get())
                || player.getOffhandItem().is(ModItems.MINIATURE_IMMORTAL_RUIN.get());
    }

    public boolean isReversed() { return getBlockState().getValue(MiniatureImmortalRuinBlock.REVERSED); }
    public BlockPos linkedPos() { return linkedPos; }
    public void unlinkForBreak() {
        if (linkedPos == null || level == null) return;
        if (level.getBlockEntity(linkedPos) instanceof MiniatureImmortalRuinBlockEntity peer
                && worldPosition.equals(peer.linkedPos)) {
            peer.linkedPos = null;
            peer.setChanged();
            level.sendBlockUpdated(peer.worldPosition, peer.getBlockState(), peer.getBlockState(), 3);
        }
        linkedPos = null;
        setChanged();
    }
    public void setLinkedPos(BlockPos pos) {
        linkedPos = pos == null ? null : pos.immutable();
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    public void clearLink() { setLinkedPos(null); }

    public ContainerData menuData() {
        return new ContainerData() {
            @Override public int get(int index) { return switch (index) { case 0 -> affectPlayers ? 1 : 0; case 1 -> entityDamage ? 1 : 0; case 2 -> playerDamage ? 1 : 0; case 3 -> forceMode; case 4 -> warpEnabled ? 1 : 0; default -> 0; }; }
            @Override public void set(int index, int value) { switch (index) { case 0 -> affectPlayers = value != 0; case 1 -> entityDamage = value != 0; case 2 -> playerDamage = value != 0; case 3 -> forceMode = Math.max(0, Math.min(4, value)); case 4 -> warpEnabled = value != 0; default -> { } } setChanged(); }
            @Override public int getCount() { return 5; }
        };
    }
    @Override public Component getDisplayName() {
        RuinMachine machine = machine();
        return Component.translatable(machine.formed() ? machine.titleKey() : "item.immortalstorage.miniature_immortal_ruin"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        if (machine().formed()) {
            return new com.immortalstorage.immortalstorage.menu.custom.SpiritCompressorMenu(id, inventory, this); }
        return new com.immortalstorage.immortalstorage.menu.custom.MiniatureImmortalRuinMenu(id, inventory, menuData(), this); }
    @Override protected void saveAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditionalLegacy(tag, registries); compressor.save(tag, registries); enchanter.save(tag, registries); tag.putBoolean("AffectPlayers", affectPlayers); tag.putBoolean("EntityDamage", entityDamage); tag.putBoolean("PlayerDamage", playerDamage); tag.putInt("ForceMode", forceMode); tag.putBoolean("WarpEnabled", warpEnabled); if (linkedPos != null) tag.putLong("LinkedPos", linkedPos.asLong()); }
    @Override protected void loadAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditionalLegacy(tag, registries); compressor.load(tag, registries); enchanter.load(tag, registries); affectPlayers = tag.getBooleanOr("AffectPlayers", false); entityDamage = !tag.contains("EntityDamage") || tag.getBooleanOr("EntityDamage", false); playerDamage = tag.getBooleanOr("PlayerDamage", false); forceMode = Math.max(0, Math.min(4, tag.getIntOr("ForceMode", 0))); warpEnabled = tag.getBooleanOr("WarpEnabled", false); linkedPos = tag.contains("LinkedPos") ? BlockPos.of(tag.getLongOr("LinkedPos", 0L)) : null; }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag(); saveAdditionalLegacy(tag, registries); return tag;
    }
}
