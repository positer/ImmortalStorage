package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.client.api.AEKeyRenderer;
import com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        implements AEKeyRenderer<ImmortalStorageExternalResourceKey, Void> {
    static final ImmortalStorageExternalResourceKeyRenderHandler INSTANCE =
            new ImmortalStorageExternalResourceKeyRenderHandler();

    @Override
    public void drawInGui(Minecraft minecraft, GuiGraphicsExtractor graphics, int x, int y,
                          ImmortalStorageExternalResourceKey key) {
        ExternalResourceCatalog.Definition definition =
                ExternalResourceCatalog.definition(key.resource());
        if (definition.solidColor()) {
            graphics.fill(x, y, x + 16, y + 16, definition.color());
        } else {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitTexture(graphics, definition.icon(), x, y, 0.0F, 0.0F, 16, 16, 16,
                    externalTextureHeight(key.resource()));
        }
    }

    @Override
    public Class<Void> stateClass() { return Void.class; }

    @Override
    public Void createState() { return null; }

    @Override
    public void extract(Void state, ImmortalStorageExternalResourceKey key, Level level, int seed) {}

    @Override
    public void submit(PoseStack poseStack, Void state, net.minecraft.client.renderer.SubmitNodeCollector collector, int light) {}

@Override
    public List<Component> getTooltip(ImmortalStorageExternalResourceKey key) {
        return List.of(ExternalResourceCatalog.displayName(key.resource()));
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
