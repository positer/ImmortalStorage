package com.immortalstorage.immortalstorage.compat.ae2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(com.immortalstorage.immortalstorage.compat.CompatTestBootstrapExtension.class)
final class Ae2TargetApiContractTest {
    @Test
    void meStorageKeepsTheOfficialLongValuedInsertAndExtractSurface() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        Class<?> meStorage = Class.forName("appeng.api.storage.MEStorage", false, loader);
        Class<?> aeKey = Class.forName("appeng.api.stacks.AEKey", false, loader);
        Class<?> actionable = Class.forName("appeng.api.config.Actionable", false, loader);
        Class<?> actionSource = Class.forName(
                "appeng.api.networking.security.IActionSource", false, loader);

        Method insert = meStorage.getMethod(
                "insert", aeKey, long.class, actionable, actionSource);
        Method extract = meStorage.getMethod(
                "extract", aeKey, long.class, actionable, actionSource);

        assertEquals(long.class, insert.getReturnType());
        assertEquals(long.class, extract.getReturnType());
        assertTrue(Ae2StorageApiDescriptor.probe(loader).supportsLongAmounts());
    }

    @Test
    void clientRenderingUsesThe26GuiExtractorAndSubmitCollectorContracts() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        Class<?> rendering = Class.forName("appeng.client.api.AEKeyRendering", false, loader);
        Class<?> keyType = Class.forName("appeng.api.stacks.AEKeyType", false, loader);
        Class<?> keyClass = Class.forName("appeng.api.stacks.AEKey", false, loader);
        Class<?> renderer = Class.forName("appeng.client.api.AEKeyRenderer", false, loader);
        Class<?> gui = Class.forName("net.minecraft.client.gui.GuiGraphicsExtractor", false, loader);
        Class<?> submitter = Class.forName(
                "net.minecraft.client.renderer.SubmitNodeCollector", false, loader);

        Method register = rendering.getMethod("register", keyType, Class.class, renderer);
        assertNotNull(register);
        assertNotNull(renderer.getMethod("drawInGui",
                Class.forName("net.minecraft.client.Minecraft", false, loader),
                gui, int.class, int.class, Object.class));
        assertNotNull(renderer.getMethod("submit",
                Class.forName("com.mojang.blaze3d.vertex.PoseStack", false, loader),
                Object.class, submitter, int.class));
    }
}
