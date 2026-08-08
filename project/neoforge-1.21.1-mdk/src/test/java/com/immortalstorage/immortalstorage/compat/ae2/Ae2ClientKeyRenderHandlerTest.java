package com.immortalstorage.immortalstorage.compat.ae2;

import appeng.api.client.AEKeyRendering;
import com.immortalstorage.core.resource.ResourceChannelKey;
import com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression for the AE2 client crash:
 * {@code java.lang.IllegalArgumentException: Missing render handler for channel
 * immortalstorage:external_resource}. AE2 ME terminal screens call
 * {@code AEKeyRendering.getOrThrow} for every visible entry, so the client
 * bootstrap must register a handler for the external-resource channel before
 * any terminal can render.
 */
final class Ae2ClientKeyRenderHandlerTest {

    @Test
    void clientBootstrapRegistersRenderHandlerForTheExternalResourceChannel() {
        Ae2ClientCompat.initialize();
        assertNotNull(AEKeyRendering.getOrThrow(ImmortalStorageExternalResourceKeyType.TYPE),
                "AE2 must resolve a render handler for the external-resource channel");
    }

    @Test
    void registeredHandlerNamesAndNamespacesKeysFromTheCatalog() {
        Ae2ClientCompat.initialize();
        ResourceChannelKey channel = new ResourceChannelKey("energy", "immortalstorage:test_tooltip_probe");
        ExternalResourceCatalog.registerDefinition(channel,
                ResourceLocation.fromNamespaceAndPath("immortalstorage",
                        "textures/gui/external_resource/ae2_fe.png"),
                "FE", 0xFFFF4B35, Component.literal("Probe FE"), false);

        ImmortalStorageExternalResourceKey key = new ImmortalStorageExternalResourceKey(channel);
        List<Component> tooltip = AEKeyRendering.getTooltip(key);
        assertNotNull(tooltip);
        assertEquals(1, tooltip.size());
        assertEquals("Probe FE", tooltip.get(0).getString());
    }
}
