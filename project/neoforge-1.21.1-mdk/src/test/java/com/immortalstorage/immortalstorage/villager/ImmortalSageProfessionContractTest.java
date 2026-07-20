package com.immortalstorage.immortalstorage.villager;

import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImmortalSageProfessionContractTest {
    private static final String ACQUIRABLE_JOB_SITE_TAG =
            "data/minecraft/tags/point_of_interest_type/acquirable_job_site.json";

    @Test
    void unemployedVillagersCanDiscoverTheImmortalFurnacePoi() throws Exception {
        Enumeration<URL> resources = getClass().getClassLoader().getResources(ACQUIRABLE_JOB_SITE_TAG);
        boolean foundExtension = false;
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            try (var stream = resource.openStream()) {
                JsonObject tag = JsonParser.parseReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                JsonArray values = tag.getAsJsonArray("values");
                if (values != null && values.asList().stream()
                        .anyMatch(value -> "immortalstorage:immortal_furnace_poi".equals(value.getAsString()))) {
                    assertTrue(!tag.has("replace") || !tag.get("replace").getAsBoolean(),
                            "the mod must extend, not replace, vanilla villager workstations");
                    foundExtension = true;
                }
            }
        }
        assertTrue(foundExtension,
                "VillagerProfession.NONE only scans acquirable_job_site; the mod must extend that tag");
    }

    @Test
    void poiCoversEveryFurnaceStateAndProfessionMatchesItsRegistryHolder() {
        PoiType poi = ModVillagers.IMMORTAL_FURNACE_POI.get();
        Set<BlockState> expectedStates = new HashSet<>(
                ModBlocks.IMMORTAL_FURNACE.get().getStateDefinition().getPossibleStates());
        assertEquals(expectedStates, poi.matchingStates(),
                "every facing/lit furnace state must remain the same job site");

        Holder<PoiType> holder = BuiltInRegistries.POINT_OF_INTEREST_TYPE
                .getHolderOrThrow(ModVillagers.IMMORTAL_FURNACE_POI_KEY);
        for (BlockState state : expectedStates) {
            Holder<PoiType> mapped = PoiTypes.forState(state).orElseThrow(
                    () -> new AssertionError("NeoForge POI state map omitted " + state));
            assertTrue(mapped.is(ModVillagers.IMMORTAL_FURNACE_POI_KEY),
                    "all placed/lit variants must resolve to the immortal furnace POI");
        }
        assertTrue(ModVillagers.IMMORTAL_SAGE.get().heldJobSite().test(holder));
        assertTrue(ModVillagers.IMMORTAL_SAGE.get().acquirableJobSite().test(holder));
    }
}
