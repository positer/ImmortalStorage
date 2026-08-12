package com.immortalstorage.immortalstorage.block.entity;
import net.minecraft.core.registries.BuiltInRegistries;

import com.immortalstorage.immortalstorage.block.custom.SourceVeinBlock;
import com.immortalstorage.immortalstorage.block.custom.VeinKind;
import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.item.ModDataComponents;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinition;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinitions;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/** The manager's private 72-member source registry; never exposed as product storage. */
public final class SourceVeinManagerInventory extends ItemStackHandler {
    public static final int SLOT_COUNT = 8 * 9;
    private static final String CACHE_TAG = "CachedUnits";

    private final Runnable onChanged;
    private int inactiveDuplicateCount;

    public SourceVeinManagerInventory(Runnable onChanged) {
        super(SLOT_COUNT);
        this.onChanged = onChanged == null ? () -> {} : onChanged;
    }

    @Override
    public int getSlotLimit(int slot) {
        return validSlot(slot) ? 1 : 0;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return validSlot(slot) && sourceDefinition(stack) != null && !duplicatesExistingDefinition(slot, stack);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!validSlot(slot) || stack.isEmpty() || !stacks.get(slot).isEmpty()) return stack;
        SourceDefinition definition = sourceDefinition(stack);
        if (definition == null || duplicatesExistingDefinition(slot, stack)) return stack;

        ItemStack member = stack.copyWithCount(1);
        reconcileMemberCache(member, definition, cachedUnits(member));
        if (!simulate) setStackInSlot(slot, member);
        ItemStack remainder = stack.copy();
        remainder.shrink(1);
        return remainder;
    }

    @Override
    protected void onContentsChanged(int slot) {
        reconcileLoadedMembers();
        onChanged.run();
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (!validSlot(slot)) return;
        if (stack.isEmpty()) {
            super.setStackInSlot(slot, ItemStack.EMPTY);
            return;
        }
        SourceDefinition definition = sourceDefinition(stack);
        if (definition == null || duplicatesExistingDefinition(slot, stack)) return;
        ItemStack member = stack.copyWithCount(1);
        reconcileMemberCache(member, definition, cachedUnits(member));
        super.setStackInSlot(slot, member);
    }

    /** Rebuilds activation after NBT load without deleting legacy duplicates. */
    public void reconcileLoadedMembers() {
        int duplicates = 0;
        java.util.HashSet<Identifier> seen = new java.util.HashSet<>();
        for (int slot = 0; slot < getSlots(); slot++) {
            Identifier id = sourceDefinitionId(stacks.get(slot));
            if (id != null && !seen.add(id)) duplicates++;
        }
        if (duplicates != inactiveDuplicateCount) {
            inactiveDuplicateCount = duplicates;
            if (duplicates > 0) {
                ImmortalStorageMod.LOG.warn("Source Vein Manager retained {} duplicate legacy member(s); "
                        + "duplicates remain extractable but do not participate in aggregation", duplicates);
            }
        }
    }

    public boolean isActiveMember(int slot) {
        if (!validSlot(slot)) return false;
        Identifier id = sourceDefinitionId(stacks.get(slot));
        if (id == null) return false;
        for (int earlier = 0; earlier < slot; earlier++) {
            if (id.equals(sourceDefinitionId(stacks.get(earlier)))) return false;
        }
        return true;
    }

    public int inactiveDuplicateCount() {
        return inactiveDuplicateCount;
    }

    public static Identifier sourceDefinitionId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        VeinKind kind = sourceKind(stack);
        Identifier id = kind == null ? null : SourceDefinitions.legacyId(kind);
        if (ModDataComponents.SOURCE_DEFINITION_ID.isBound()) {
            Identifier componentId = stack.get(ModDataComponents.SOURCE_DEFINITION_ID.get());
            if (componentId != null) id = componentId;
        }
        if (id == null && stack != null && !stack.isEmpty()) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            if (tag.contains("SourceDefinitionId")) {
                id = Identifier.tryParse(tag.getStringOr("SourceDefinitionId", ""));
            }
        }
        if (id == null) return null;
        SourceDefinition definition = SourceDefinitions.find(id).orElse(null);
        return definition == null ? id : definition.id();
    }

    public static SourceDefinition sourceDefinition(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof SourceVeinBlock)) return null;
        return SourceDefinitions.find(sourceDefinitionId(stack)).orElse(null);
    }

    private boolean duplicatesExistingDefinition(int targetSlot, ItemStack candidate) {
        Identifier candidateId = sourceDefinitionId(candidate);
        if (candidateId == null) return false;
        for (int slot = 0; slot < getSlots(); slot++) {
            if (slot == targetSlot || stacks.get(slot).isEmpty()) continue;
            if (candidateId.equals(sourceDefinitionId(stacks.get(slot)))) return true;
        }
        return false;
    }

    public static VeinKind sourceKind(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof SourceVeinBlock source)) return null;
        return source.getKind();
    }

    public static long cachedUnits(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0L;
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return Math.max(0L, data.copyTag().getLongOr(CACHE_TAG, 0L));
    }

    /** Reconciles the member's actual persisted cache, not a display-only virtual amount. */
    public static long reconcileMemberCache(ItemStack stack, VeinKind kind, long paidTarget) {
        return reconcileMemberCache(stack,
                kind == null ? null : SourceDefinitions.find(SourceDefinitions.legacyId(kind)).orElse(null),
                paidTarget);
    }

    public static long reconcileMemberCache(ItemStack stack, SourceDefinition definition, long paidTarget) {
        if (stack == null || stack.isEmpty() || definition == null) return 0L;
        long target = definition.free() ? Long.MAX_VALUE
                : Math.max(cachedUnits(stack), Math.max(0L, paidTarget));
        CompoundTag tag = writableMemberTag(stack);
        tag.putLong(CACHE_TAG, target);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return target;
    }

    public static void setCachedUnits(ItemStack stack, long amount) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag tag = writableMemberTag(stack);
        tag.putLong(CACHE_TAG, Math.max(0L, amount));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * Vanilla's BLOCK_ENTITY_DATA codec rejects tags without a string {@code id}
     * key ("Missing id for entity"). Fresh creative/JEI-given members carry no
     * pre-existing tag, so the manager must stamp the source-vein block entity
     * id before persisting any cache value.
     */
    private static CompoundTag writableMemberTag(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("id")) {
            Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(ModBlockEntities.SOURCE_VEIN.get());
            if (id != null) tag.putString("id", id.toString());
        }
        return tag;
    }

    private static boolean validSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }
}
