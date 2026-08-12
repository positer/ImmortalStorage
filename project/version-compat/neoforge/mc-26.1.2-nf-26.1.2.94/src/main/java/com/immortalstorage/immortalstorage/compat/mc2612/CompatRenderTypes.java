package com.immortalstorage.immortalstorage.compat.mc2612;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Official 26.1 render-type factories replacing the removed RenderType helpers. */
public final class CompatRenderTypes {
    private CompatRenderTypes() {}

    public static RenderType entityCutoutNoCull(Identifier texture) {
        return RenderTypes.entityCutout(texture, false);
    }

    public static RenderType entityTranslucentEmissive(Identifier texture) {
        return RenderTypes.entityTranslucentEmissive(texture);
    }

    public static RenderType entityTranslucent(Identifier texture) {
        return RenderTypes.entityTranslucent(texture);
    }

    public static RenderType lines() {
        return RenderTypes.lines();
    }

    public static RenderType lightning() {
        return RenderTypes.lightning();
    }
}
