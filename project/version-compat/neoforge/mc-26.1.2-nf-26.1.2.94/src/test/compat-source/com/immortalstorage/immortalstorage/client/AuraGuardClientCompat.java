package com.immortalstorage.immortalstorage.client;

import com.immortalstorage.immortalstorage.client.render.AuraGuardElytraLayer;
import com.immortalstorage.immortalstorage.combat.ImmortalMasterTalismanService;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

public final class AuraGuardClientCompat {
    private AuraGuardClientCompat() {
    }

    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            var renderer = event.getPlayerRenderer(skin);
            renderer.addLayer(new AuraGuardElytraLayer(renderer, event.getEntityModels()));
        }
    }

    public static void registerStateModifier(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override
            public <T extends net.minecraft.world.entity.Avatar & net.minecraft.client.entity.ClientAvatarEntity>
            void accept(T entity, AvatarRenderState state) {
                state.setRenderData(AuraGuardElytraLayer.ACTIVE,
                        (entity.isFallFlying() && entity.getPersistentData()
                                .getBoolean("ImmortalStorageVirtualElytra").orElse(false))
                                || ImmortalMasterTalismanService.hasAuraGuard(entity));
            }
        });
    }
}
