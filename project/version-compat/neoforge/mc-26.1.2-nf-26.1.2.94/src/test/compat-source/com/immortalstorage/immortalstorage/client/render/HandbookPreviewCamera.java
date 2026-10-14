package com.immortalstorage.immortalstorage.client.render;

import org.joml.Matrix4f;

/** Centre the model and compensate the native PiP renderer's bottom-aligned scene origin. */
public final class HandbookPreviewCamera {
    private HandbookPreviewCamera() {}
    public static Matrix4f centre(Matrix4f camera, int minX, int minY, int minZ,
                                   int maxX, int maxY, int maxZ, int height) {
        // Native Patchouli translates each model to pos.getCenter(), then
        // submits its [0,1] baked vertices: model centre is pos + 1, not + 0.5.
        return new Matrix4f(camera).translate(-(minX + maxX + 2) / 2F,
                -(minY + maxY + 2) / 2F, -(minZ + maxZ + 2) / 2F)
                .translateLocal(0F, -height / 2F, 0F);
    }
}
