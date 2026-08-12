package com.immortalstorage.immortalstorage.compat.refinedstorage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(com.immortalstorage.immortalstorage.compat.CompatTestBootstrapExtension.class)
final class RefinedStorageTargetApiContractTest {
    @Test
    void storageApiKeepsLongInsertAndExtractOperations() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        Class<?> resourceKey = Class.forName(
                "com.refinedmods.refinedstorage.api.resource.ResourceKey", false, loader);
        Class<?> action = Class.forName(
                "com.refinedmods.refinedstorage.api.core.Action", false, loader);
        Class<?> actor = Class.forName(
                "com.refinedmods.refinedstorage.api.storage.Actor", false, loader);

        Method insert = Class.forName(
                "com.refinedmods.refinedstorage.api.storage.InsertableStorage", false, loader)
                .getMethod("insert", resourceKey, long.class, action, actor);
        Method extract = Class.forName(
                "com.refinedmods.refinedstorage.api.storage.ExtractableStorage", false, loader)
                .getMethod("extract", resourceKey, long.class, action, actor);

        assertEquals(long.class, insert.getReturnType());
        assertEquals(long.class, extract.getReturnType());
        assertTrue(RefinedStorageApiDescriptor.probe(loader).supportsLongAmounts());
    }

    @Test
    void resourceRenderingUsesBothGridAndWorldSignatures() throws Exception {
        ClassLoader loader = getClass().getClassLoader();
        Class<?> resourceKey = Class.forName(
                "com.refinedmods.refinedstorage.api.resource.ResourceKey", false, loader);
        Class<?> rendering = Class.forName(
                "com.refinedmods.refinedstorage.common.api.support.resource.ResourceRendering", false, loader);
        Class<?> gui = Class.forName("net.minecraft.client.gui.GuiGraphicsExtractor", false, loader);
        Class<?> pose = Class.forName("com.mojang.blaze3d.vertex.PoseStack", false, loader);
        Class<?> submitter = Class.forName(
                "net.minecraft.client.renderer.SubmitNodeCollector", false, loader);

        assertNotNull(rendering.getMethod("render", resourceKey, gui, int.class, int.class));
        assertNotNull(rendering.getMethod(
                "render", resourceKey, pose, submitter, int.class, long.class));
        assertNotNull(rendering.getMethod("formatAmount", long.class, boolean.class));
    }
}
