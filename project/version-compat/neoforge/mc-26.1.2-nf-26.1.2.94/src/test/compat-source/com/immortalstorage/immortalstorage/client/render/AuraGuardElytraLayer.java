package com.immortalstorage.immortalstorage.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;

public final class AuraGuardElytraLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    public static final ContextKey<Boolean> ACTIVE = new ContextKey<>(Identifier.fromNamespaceAndPath(
            "immortalstorage", "spiritual_aura_guard"));
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "immortalstorage", "textures/entity/spiritual_aura_elytra.png");
    private final ElytraModel model;

    public AuraGuardElytraLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent,
                                EntityModelSet models) {
        super(parent);
        model = new ElytraModel(models.bakeLayer(ModelLayers.ELYTRA));
    }

    @Override
    public void submit(PoseStack poses, SubmitNodeCollector collector, int light,
                       AvatarRenderState state, float yRot, float xRot) {
        if (!state.getRenderDataOrDefault(ACTIVE, false)) return;
        poses.pushPose();
        poses.translate(0.0F, 0.0F, 0.125F);
        collector.submitModel(model, state, poses, RenderTypes.entityTranslucent(TEXTURE), light,
                OverlayTexture.NO_OVERLAY, 0xA8FFFFFF, null, 0,
                (ModelFeatureRenderer.CrumblingOverlay) null);
        poses.popPose();
    }
}
