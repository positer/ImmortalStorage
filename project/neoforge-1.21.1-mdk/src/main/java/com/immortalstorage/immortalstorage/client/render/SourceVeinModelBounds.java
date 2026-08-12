package com.immortalstorage.immortalstorage.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Reads the actual baked-quad bounds used by a source block/item model. */
final class SourceVeinModelBounds {
    private static final Center DEFAULT_CENTER = new Center(0.5F, 0.5F, 0.5F);
    private static final int BLOCK_VERTEX_STRIDE =
            DefaultVertexFormat.BLOCK.getVertexSize() / Integer.BYTES;
    private static final int POSITION_OFFSET =
            DefaultVertexFormat.BLOCK.getOffset(VertexFormatElement.POSITION) / Integer.BYTES;

    private SourceVeinModelBounds() {
    }

    static Center center(@Nullable BakedModel model, @Nullable BlockState state) {
        if (model == null) return DEFAULT_CENTER;
        Bounds bounds = bounds(model, state);
        return bounds == null ? DEFAULT_CENTER : bounds.center();
    }

    private static @Nullable Bounds bounds(BakedModel model, @Nullable BlockState state) {
        Bounds bounds = null;
        bounds = include(bounds, model.getQuads(state, null, RandomSource.create(0L)));
        for (Direction direction : Direction.values()) {
            bounds = include(bounds, model.getQuads(state, direction, RandomSource.create(0L)));
        }
        return bounds;
    }

    private static @Nullable Bounds include(@Nullable Bounds current, List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices();
            for (int vertex = POSITION_OFFSET;
                 vertex + 2 < vertices.length;
                 vertex += BLOCK_VERTEX_STRIDE) {
                float x = Float.intBitsToFloat(vertices[vertex]);
                float y = Float.intBitsToFloat(vertices[vertex + 1]);
                float z = Float.intBitsToFloat(vertices[vertex + 2]);
                if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) continue;
                current = current == null
                        ? new Bounds(x, y, z, x, y, z)
                        : current.include(x, y, z);
            }
        }
        return current;
    }

    record Center(float x, float y, float z) {
    }

    private record Bounds(float minX, float minY, float minZ,
                          float maxX, float maxY, float maxZ) {
        private Bounds include(float x, float y, float z) {
            return new Bounds(Math.min(minX, x), Math.min(minY, y), Math.min(minZ, z),
                    Math.max(maxX, x), Math.max(maxY, y), Math.max(maxZ, z));
        }

        private Center center() {
            return new Center((minX + maxX) * 0.5F,
                    (minY + maxY) * 0.5F,
                    (minZ + maxZ) * 0.5F);
        }
    }
}
