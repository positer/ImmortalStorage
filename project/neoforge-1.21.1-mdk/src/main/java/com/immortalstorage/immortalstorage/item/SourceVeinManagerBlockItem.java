package com.immortalstorage.immortalstorage.item;

import com.immortalstorage.immortalstorage.block.custom.SourceVeinManagerBlock;

/** The manager is not a source block; it shares the native world renderer. */
public final class SourceVeinManagerBlockItem extends WorldRenderedBlockItem {
    public SourceVeinManagerBlockItem(SourceVeinManagerBlock block, Properties properties) { super(block, properties); }
}
