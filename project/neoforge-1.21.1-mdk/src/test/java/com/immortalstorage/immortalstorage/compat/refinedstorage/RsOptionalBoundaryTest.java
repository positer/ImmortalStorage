package com.immortalstorage.immortalstorage.compat.refinedstorage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RsOptionalBoundaryTest {
    private static final Path ROOT = locateMainSourceRoot();

    @Test
    void alwaysLoadedDiskAndFactoryDoNotReferenceRsTypes() throws IOException {
        String item = source("java/com/immortalstorage/immortalstorage/item/custom/XianqiaoRsExchangeDiskItem.java");
        String factory = source("java/com/immortalstorage/immortalstorage/item/custom/RsExchangeDiskFactory.java");
        assertFalse(item.contains("com.refinedmods"));
        assertFalse(factory.contains("import com.refinedmods"));
        assertTrue(factory.contains("CompatManager.RS_LOADED"));
        assertTrue(factory.contains("Class.forName"));
    }

    @Test
    void rsAdapterUsesTheOfficialStorageContainerContract() throws IOException {
        String adapter = source(
                "java/com/immortalstorage/immortalstorage/compat/refinedstorage/XianqiaoRsStorageContainerItem.java");
        assertTrue(adapter.contains("implements StorageContainerItem"));
        assertTrue(adapter.contains("Optional<SerializableStorage> resolve"));
        assertTrue(adapter.contains("Optional<StorageInfo> getInfo"));
    }

    @Test
    void rsRegistersAndRendersImmortalStoragesOwnExtraResourceKey() throws IOException {
        String bootstrap = source(
                "java/com/immortalstorage/immortalstorage/compat/refinedstorage/RsCompat.java");
        String storage = source(
                "java/com/immortalstorage/immortalstorage/compat/refinedstorage/XianqiaoRsStorage.java");
        String resource = source(
                "java/com/immortalstorage/immortalstorage/compat/refinedstorage/RsExternalResource.java");
        String client = source(
                "java/com/immortalstorage/immortalstorage/compat/refinedstorage/RsClientCompat.java");
        String rendering = source(
                "java/com/immortalstorage/immortalstorage/compat/refinedstorage/RsExternalResourceRendering.java");

        assertTrue(bootstrap.contains("getResourceTypeRegistry"));
        assertTrue(bootstrap.contains("RsExternalResourceType.INSTANCE"));
        assertTrue(storage.contains("externalResourceStorage"));
        assertTrue(storage.contains("RsExternalResourceKeyBridges.toResourceKey"));
        assertTrue(resource.contains("implements PlatformResourceKey"));
        assertTrue(resource.contains("ResourceChannelKey"));
        assertTrue(bootstrap.contains("addGridResourceRepositoryMapper"));
        assertTrue(bootstrap.contains("RsExternalGridResourceMapper.INSTANCE"));
        assertTrue(client.contains("registerResourceRendering"));
        assertTrue(rendering.contains("ExternalResourceCatalog.definition"));
        assertTrue(rendering.contains("graphics.blit(definition.icon()"));
        assertTrue(rendering.contains("graphics.fill"));
        int fill = rendering.indexOf("graphics.fill");
        int exclusiveElse = rendering.indexOf("} else {", fill);
        int blit = rendering.indexOf("graphics.blit(definition.icon()", fill);
        assertTrue(fill < exclusiveElse && exclusiveElse < blit,
                "solid-color sampling and textured icons must remain mutually exclusive");
        assertFalse(rendering.contains("renderItem(new ItemStack"));
    }

    @Test
    void installedResourceAddonsUseNativeKeysAndKeepTheBuiltinKeyAsFallback() throws IOException {
        String bootstrap = source(
                "java/com/immortalstorage/immortalstorage/compat/refinedstorage/RsCompat.java");
        String storage = source(
                "java/com/immortalstorage/immortalstorage/compat/refinedstorage/XianqiaoRsStorage.java");
        String addons = source(
                "java/com/immortalstorage/immortalstorage/compat/refinedstorage/InstalledAddonRsExternalResourceKeyBridges.java");
        String fallback = source(
                "java/com/immortalstorage/immortalstorage/compat/refinedstorage/ImmortalStorageRsExternalResourceKeyBridge.java");

        assertTrue(bootstrap.contains("InstalledAddonRsExternalResourceKeyBridges.registerPresent"));
        assertTrue(addons.contains("refinedtypes"));
        assertTrue(addons.contains("refinedstorage_mekanism_integration"));
        assertTrue(addons.contains("EnergyResource"));
        assertTrue(addons.contains("SourceResource"));
        assertTrue(addons.contains("SoulResource"));
        assertTrue(addons.contains("ChemicalResource"));
        assertTrue(addons.contains("ExternalResourceChannels.mekanismChemical"));
        assertTrue(fallback.contains("Integer.MIN_VALUE"));
        assertTrue(storage.contains("RsExternalResourceKeyBridges.toRsKey"));
        assertTrue(storage.contains("RsExternalResourceKeyBridges.toResourceKey"));
        assertFalse(storage.contains("result.add(new ResourceAmount(new RsExternalResource"));
    }

    @Test
    void resourceAddonMetadataIsOptionalAndOrderedBeforeImmortalStorageSetup() throws IOException {
        String modsToml = source("resources/META-INF/neoforge.mods.toml");
        assertTrue(modsToml.contains("modId=\"refinedtypes\""));
        assertTrue(modsToml.contains("modId=\"refinedstorage_mekanism_integration\""));
        assertTrue(modsToml.contains("versionRange=\"[0.3.0,0.4)\""));
        assertTrue(modsToml.contains("ordering=\"AFTER\""));
    }

    @Test
    void rsTypesRemainInsideTheirOptionalClassLoadingBoundary() throws IOException {
        Path javaRoot = ROOT.resolve("java");
        Path rsRoot = javaRoot.resolve(
                "com/immortalstorage/immortalstorage/compat/refinedstorage");
        try (var files = Files.walk(javaRoot)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (file.startsWith(rsRoot)
                        || file.toString().contains("mixin\\refinedstorage")) continue;
                String text = Files.readString(file);
                assertFalse(text.contains("import com.refinedmods"),
                        () -> "hard RS reference escaped optional boundary: " + file);
            }
        }
    }

    @Test
    void recipeAndMetadataStayOptionalAndPinnedToRsTwoPointZeroNine() throws IOException {
        String recipe = source("resources/data/immortalstorage/recipe/xianqiao_rs_exchange_disk.json");
        String modsToml = source("resources/META-INF/neoforge.mods.toml");
        assertTrue(recipe.contains("neoforge:mod_loaded"));
        assertTrue(recipe.contains("\"modid\": \"refinedstorage\""));
        assertTrue(recipe.contains("refinedstorage:storage_housing"));
        assertTrue(recipe.contains("immortalstorage:xianqiao_rs_exchange_disk"));
        assertTrue(modsToml.contains("modId=\"refinedstorage\""));
        assertTrue(modsToml.contains("versionRange=\"[2.0.9,2.0.10)\""));
        assertTrue(modsToml.contains("type=\"optional\""));
    }

    @Test
    void exactVersionMixinIsGatedAndForwardsOfficialCompositeLifecycle() throws IOException {
        String config = source("resources/immortalstorage.refinedstorage.mixins.json");
        String plugin = source(
                "java/com/immortalstorage/immortalstorage/mixin/refinedstorage/RsMixinConfigPlugin.java");
        String trackedBridge = source(
                "java/com/immortalstorage/immortalstorage/mixin/refinedstorage/StateTrackedStorageCompositeBridgeMixin.java");
        String compositeBridge = source(
                "java/com/immortalstorage/immortalstorage/mixin/refinedstorage/CompositeStorageLifecycleMixin.java");
        assertTrue(config.contains("RsMixinConfigPlugin"));
        assertTrue(config.contains("StateTrackedStorageCompositeBridgeMixin"));
        assertTrue(config.contains("CompositeStorageLifecycleMixin"));
        assertTrue(plugin.contains("LoadingModList"));
        assertTrue(plugin.contains("getModFileById(\"refinedstorage\")"));
        assertFalse(plugin.contains("com.refinedmods"));
        assertTrue(trackedBridge.contains("implements CompositeAwareChild"));
        assertTrue(compositeBridge.contains("immortalstorage$rebuildCache"));
        assertTrue(compositeBridge.contains("RsNetworkDeduplicator.rebalanceFrom"));
    }

    private static String source(String relative) throws IOException {
        return Files.readString(ROOT.resolve(relative));
    }

    private static Path locateMainSourceRoot() {
        Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (cursor != null) {
            Path direct = cursor.resolve("src/main");
            if (isImmortalStorageMainSource(direct)) return direct;

            Path workspaceProject = cursor.resolve("project/neoforge-1.21.1-mdk/src/main");
            if (isImmortalStorageMainSource(workspaceProject)) return workspaceProject;
            cursor = cursor.getParent();
        }
        throw new IllegalStateException("Unable to locate NeoForge ImmortalStorage src/main from user.dir="
                + System.getProperty("user.dir"));
    }

    private static boolean isImmortalStorageMainSource(Path candidate) {
        return Files.isRegularFile(candidate.resolve(
                "java/com/immortalstorage/immortalstorage/ImmortalStorageMod.java"));
    }
}
