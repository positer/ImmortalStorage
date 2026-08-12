package com.immortalstorage.immortalstorage.compat.mc2612;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Bridges the official 26.1 ValueInput/ValueOutput block-entity hooks to the
 * canonical CompoundTag persistence code. The vanilla tag implementations
 * expose the exact backing compound to the hook; using that official storage
 * path preserves nested lists, long arrays and data-component payloads.
 */
public abstract class CompatBlockEntity extends BlockEntity {
    protected CompatBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected void saveAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
    }

    protected void loadAdditionalLegacy(CompoundTag tag, HolderLookup.Provider registries) {
    }

    /**
     * 26.1 keeps block-entity payloads on the item's CUSTOM_DATA component.
     * The canonical 1.21.1 blocks use this helper when a block drops a
     * configured machine, so every migrated block entity preserves the same
     * inventory and configuration payload.
     */
    public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty()) return;
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        saveWithFullMetadata(output);
        removeComponentsFromTag(output);
        CompoundTag payload = output.buildResult();
        stack.set(DataComponents.CUSTOM_DATA,
                CustomData.of(payload));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag tag = CompatValueIo.rawOutput(output);
        HolderLookup.Provider registries = level != null ? level.registryAccess() : RegistryAccess.EMPTY;
        saveAdditionalLegacy(tag, registries);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        loadAdditionalLegacy(CompatValueIo.rawInput(input), input.lookup());
    }
}
