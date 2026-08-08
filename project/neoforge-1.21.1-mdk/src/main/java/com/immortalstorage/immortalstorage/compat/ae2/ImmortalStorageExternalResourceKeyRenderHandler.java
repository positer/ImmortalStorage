package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.client.AEKeyRenderHandler;
import com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Client-only AE2 grid representation for the ImmortalStorage external-resource
 * channel.
 *
 * <p>AE2 terminal screens call {@code AEKeyRendering.getOrThrow} for every
 * visible entry. Without a registered handler for this channel the client
 * crashed with {@code Missing render handler for channel immortalstorage:external_resource}
 * while rendering a storage-entry tooltip. This handler must therefore be
 * registered before any ME screen can open.</p>
 */
final class ImmortalStorageExternalResourceKeyRenderHandler
        implements AEKeyRenderHandler<ImmortalStorageExternalResourceKey> {
    static final ImmortalStorageExternalResourceKeyRenderHandler INSTANCE =
            new ImmortalStorageExternalResourceKeyRenderHandler();

    @Override
    public void drawInGui(Minecraft minecraft, GuiGraphics graphics, int x, int y,
                          ImmortalStorageExternalResourceKey key) {
        ExternalResourceCatalog.Definition definition =
                ExternalResourceCatalog.definition(key.resource());
        if (definition.solidColor()) {
            graphics.fill(x, y, x + 16, y + 16, definition.color());
        } else {
            graphics.blit(definition.icon(), x, y, 0.0F, 0.0F, 16, 16, 16,
                    externalTextureHeight(key.resource()));
        }
    }

    @Override
    public void drawOnBlockFace(PoseStack poseStack, MultiBufferSource buffers,
                                ImmortalStorageExternalResourceKey key, float partialTick,
                                int light, Level level) {
        // Non-item resources have no world-block representation.
    }

    @Override
    public Component getDisplayName(ImmortalStorageExternalResourceKey key) {
        return ExternalResourceCatalog.displayName(key.resource());
    }

    @Override
    public List<Component> getTooltip(ImmortalStorageExternalResourceKey key) {
        return List.of(getDisplayName(key));
    }

    private static int externalTextureHeight(ResourceChannelKey key) {
        return switch (key.channel()) {
            case "botania_mana" -> 512;
            case "ars_nouveau_source" -> 320;
            default -> 16;
        };
    }

    private ImmortalStorageExternalResourceKeyRenderHandler() {}
}
