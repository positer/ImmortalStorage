package com.immortalstorage.immortalstorage.compat.refinedstorage;

import com.immortalstorage.immortalstorage.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceRendering;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** Client-only RS grid representation using ImmortalStorage-owned artwork. */
final class RsExternalResourceRendering implements ResourceRendering {
    static final RsExternalResourceRendering INSTANCE = new RsExternalResourceRendering();

    @Override public String formatAmount(long amount, boolean withUnits) {
        return Long.toString(Math.max(0L, amount));
    }
    @Override public Component getDisplayName(ResourceKey resource) {
        if (!(resource instanceof RsExternalResource external)) return Component.empty();
        return Component.translatable(
                "resource.immortalstorage.external." + external.resource().channel(),
                external.resource().resourceId());
    }
    @Override public List<Component> getTooltip(ResourceKey resource) {
        Component name = getDisplayName(resource);
        return name.getString().isEmpty() ? List.of() : List.of(name);
    }
    @Override public void render(ResourceKey resource, GuiGraphics graphics, int x, int y) {
        if (resource instanceof RsExternalResource) {
            graphics.renderItem(new ItemStack(ModItems.XIANQIAO_RS_EXCHANGE_DISK.get()), x, y);
        }
    }
    @Override public void render(
            ResourceKey resource, PoseStack poseStack, MultiBufferSource buffers,
            int light, Level level) {
        // RS uses the GuiGraphics path for grid entries; no world model is required.
    }

    private RsExternalResourceRendering() {}
}
