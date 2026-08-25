package com.immortalstorage.immortalstorage.client.render;

import com.immortalstorage.immortalstorage.combat.ImmortalMasterTalismanService;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

public final class AuraGuardElytraLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "immortalstorage", "textures/entity/spiritual_aura_elytra.png");
    private final ElytraModel<AbstractClientPlayer> model;

    public AuraGuardElytraLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                                EntityModelSet models) {
        super(parent);
        model = new ElytraModel<>(models.bakeLayer(ModelLayers.ELYTRA));
    }

    @Override
    public void render(PoseStack poses, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        boolean activeFlight = player.isFallFlying()
                && player.getPersistentData().getBoolean("ImmortalStorageVirtualElytra");
        if (!activeFlight && !ImmortalMasterTalismanService.hasAuraGuard(player)) return;
        poses.pushPose();
        poses.translate(0.0F, 0.0F, 0.125F);
        getParentModel().copyPropertiesTo(model);
        model.setupAnim(player, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        model.renderToBuffer(poses, buffers.getBuffer(RenderType.entityTranslucent(TEXTURE)),
                light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xA8FFFFFF);
        poses.popPose();
    }
}
