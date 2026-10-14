package com.immortalstorage.immortalstorage.compat.patchouli;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.world.level.block.state.BlockState;

/** Associates extracted book model states with their source state until the PiP is drawn. */
public final class HandbookPreviewStates {
    private static final Map<BlockModelRenderState, BlockState> STATES = new WeakHashMap<>();
    private HandbookPreviewStates() {}
    public static void remember(BlockModelRenderState model, BlockState state) { STATES.put(model, state); }
    public static BlockState source(BlockModelRenderState model) { return STATES.get(model); }
}
