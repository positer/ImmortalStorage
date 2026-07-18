package com.cultivation.cultivation.block.entity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Internal fast path for aggregate item destinations that can accept more than
 * one normal stack in a single transaction. Standard NeoForge item capability
 * behavior remains available through {@link IItemHandler}.
 */
interface BulkItemInsertTarget extends IItemHandler {
    long insertBulk(ItemStack prototype, long amount, boolean simulate);
}
