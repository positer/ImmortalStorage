package com.immortalstorage.immortalstorage.compat;

import com.immortalstorage.core.resource.ExternalResourceChannels;
import com.immortalstorage.core.resource.ResourceChannelKey;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ExternalResourceCatalogTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @Test
    void providersAreDeduplicatedAndSortedWithoutOptionalApiTypes() {
        ResourceChannelKey hydrogen = ExternalResourceChannels.mekanismChemical("mekanism:hydrogen");
        ExternalResourceCatalog.register(id("z_provider"), () -> List.of(hydrogen));
        ExternalResourceCatalog.register(id("a_provider"), () -> List.of(
                ExternalResourceChannels.FE, hydrogen));

        List<ResourceChannelKey> resources = ExternalResourceCatalog.available();
        assertEquals(1L, resources.stream().filter(hydrogen::equals).count());
        assertTrue(resources.contains(ExternalResourceChannels.FE));
        assertTrue(ExternalResourceCatalog.contains(hydrogen));
    }

    @Test
    void definitionsCanPointAtOfficialResourceModTextures() {
        ResourceChannelKey source = new ResourceChannelKey(
                "ars_source", "cultivation_test:official_texture");
        ExternalResourceCatalog.registerDefinition(source,
                Identifier.fromNamespaceAndPath(
                        "ars_nouveau", "textures/item/source_gem.png"),
                "Source", 0xFF8D5BE8);

        assertEquals("ars_nouveau", ExternalResourceCatalog.definition(source).icon().getNamespace());
    }

    @Test
    void lateDefinitionsResolvePerConcreteResourceKeyAndSupplyFormalNames() {
        ResourceChannelKey oxygen = ExternalResourceChannels.mekanismChemical("cultivation_test:late_oxygen");
        ExternalResourceCatalog.registerDefinitionProvider(id("late_chemical_definition"), key ->
                oxygen.equals(key) ? new ExternalResourceCatalog.Definition(
                        id("textures/oxygen.png"), "mB", 0xFF55AAFF,
                        net.minecraft.network.chat.Component.literal("Oxygen"), true) : null);

        assertTrue(ExternalResourceCatalog.definition(oxygen).solidColor());
        assertEquals(0xFF55AAFF, ExternalResourceCatalog.definition(oxygen).color());
        assertEquals("Oxygen", ExternalResourceCatalog.displayName(oxygen).getString());
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("cultivation_test", path);
    }
}
