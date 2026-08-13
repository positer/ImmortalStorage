package com.immortalstorage.immortalstorage.client.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the 26.1.2 first-person item-render pipeline used by the One-Qi beam muzzle. */
final class TargetOneQiMuzzleContractTest {
    @Test
    void targetCapturesTheRenderedSwordCenterInsteadOfUsingTheFallbackHandOffset() throws IOException {
        Path mixinPath = targetSource(
                "src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "mixin", "core", "ItemInHandRendererOneQiMuzzleMixin.java");
        assertTrue(Files.isRegularFile(mixinPath),
                "26.1.2 must replace the removed ItemRenderer hook with an ItemInHandRenderer hook");

        String mixin = Files.readString(mixinPath);
        assertTrue(mixin.contains("@Mixin(ItemInHandRenderer.class)"));
        assertTrue(mixin.contains("method = \"renderItem\""));
        assertTrue(mixin.contains("ItemStackRenderState;submit"));
        assertTrue(mixin.contains("state.getModelBoundingBox().getCenter()"));
        assertTrue(mixin.contains("OneQiHeldItemMuzzle.capture(poseStack,")
                && mixin.contains("modelCenter)"));
        assertTrue(mixin.contains("state.submit(poseStack, collector, lightCoords, overlayCoords, outlineColor)"));

        String muzzle = Files.readString(targetSource(
                "src", "main", "java", "com", "immortalstorage", "immortalstorage",
                "client", "render", "OneQiHeldItemMuzzle.java"));
        assertTrue(muzzle.contains("capture(PoseStack poses, ItemDisplayContext context,"));
        assertTrue(muzzle.contains("Vec3 modelCenter"));

        String manifest = Files.readString(targetSource(
                "src", "main", "resources", "immortalstorage.core.mixins.json"));
        assertTrue(manifest.contains("ItemInHandRendererOneQiMuzzleMixin"));
    }

    private static Path targetSource(String... parts) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path targetRoot = current.resolve(Path.of(
                    "..", "version-compat", "neoforge", "mc-26.1.2-nf-26.1.2.94"));
            if (Files.isDirectory(targetRoot)) {
                Path resolved = targetRoot;
                for (String part : parts) resolved = resolved.resolve(part);
                return resolved;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate 26.1.2 compatibility project");
    }
}
