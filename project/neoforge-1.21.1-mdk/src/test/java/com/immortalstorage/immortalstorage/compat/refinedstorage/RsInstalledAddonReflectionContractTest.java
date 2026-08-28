package com.immortalstorage.immortalstorage.compat.refinedstorage;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Pins every reflected member to the actual supported addon artifacts on the test classpath. */
final class RsInstalledAddonReflectionContractTest {
    private static final ClassLoader ADDON_LOADER = createAddonLoader();

    @Test
    void refinedTypesZeroPointThreePointTwoExposesCanonicalSingletonKeys() throws Exception {
        assumeTrue(!System.getProperty("immortalstorage.rsAddonProbe", "").isBlank(), "optional addon is not installed in the selected PCL instance");
        assertPublicStaticField(
                "com.ultramega.refinedtypes.type.energy.EnergyResource", "ENERGY_RESOURCE");
        assertPublicStaticField(
                "com.ultramega.refinedtypes.type.source.SourceResource", "SOURCE_RESOURCE");
        assertPublicStaticField(
                "com.ultramega.refinedtypes.type.soul.SoulResource", "SOUL_RESOURCE");
    }

    @Test
    void officialMekanismIntegrationOnePointOnePointOneExposesChemicalIdentity() throws Exception {
        assumeTrue(!System.getProperty("immortalstorage.rsAddonProbe", "").isBlank(), "optional addon is not installed in the selected PCL instance");
        Class<?> type = loadWithoutInitialization(
                "com.refinedmods.refinedstorage.mekanism.ChemicalResource");
        assertEquals("chemical", type.getMethod("chemical").getName());
        assertTrue(type.getConstructors().length > 0);
        assertEquals(1, type.getConstructors()[0].getParameterCount());
    }

    private static void assertPublicStaticField(String className, String fieldName) throws Exception {
        var field = loadWithoutInitialization(className).getField(fieldName);
        assertTrue(Modifier.isPublic(field.getModifiers()));
        assertTrue(Modifier.isStatic(field.getModifiers()));
    }

    private static Class<?> loadWithoutInitialization(String className) throws ClassNotFoundException {
        return Class.forName(className, false, ADDON_LOADER);
    }

    private static ClassLoader createAddonLoader() {
        String classpath = System.getProperty("immortalstorage.rsAddonProbe", "");
        URL[] urls = Arrays.stream(classpath.split(java.io.File.pathSeparator))
                .filter(path -> !path.isBlank())
                .map(Path::of)
                .map(Path::toUri)
                .map(uri -> {
                    try {
                        return uri.toURL();
                    } catch (java.net.MalformedURLException exception) {
                        throw new IllegalArgumentException(exception);
                    }
                })
                .toArray(URL[]::new);
        return new URLClassLoader(urls,
                RsInstalledAddonReflectionContractTest.class.getClassLoader());
    }
}
