package com.immortalstorage.immortalstorage.machine;

import com.google.gson.JsonParser;
import com.immortalstorage.immortalstorage.api.machine.MachinePolicyApi;
import com.immortalstorage.immortalstorage.api.machine.MachinePolicyApi.*;
import com.immortalstorage.immortalstorage.worldshard.WorldShardOrePool;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MachinePolicyTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    private static Identifier id(String path) { return Identifier.parse(path); }
    @BeforeAll static void bootstrap() { Bootstrap.bootStrap(); }
    @AfterEach void reset() { MachinePolicyApi.install(Map.of()); }
    private static Rule rule(int min, int max, String mode, List<String> yes, List<String> no) {
        return new Rule(Machine.WORLD_SHARD_MINER, min, max, mode == null ? null : id(mode), Target.OUTPUT_ITEM, yes, no);
    }
    private static boolean allowed(int level, String mode, String item) {
        return MachinePolicyApi.allows(Machine.WORLD_SHARD_MINER, level, id(mode), Target.OUTPUT_ITEM, id(item), tag -> tag.equals(id("c:gems")));
    }
    @Test void defaultPoolHasExactlySameRandomSequence() {
        MachinePolicyApi.install(Map.of());
        var pool = WorldShardOrePool.of(Map.of(Items.DIAMOND, 2L, Items.IRON_INGOT, 7L));
        var filtered = pool.filtered(item -> MachinePolicyApi.allowsItem(Machine.WORLD_SHARD_MINER, 4,
                id("immortalstorage:end"), Target.OUTPUT_ITEM, item));
        assertSame(pool, filtered);
        assertEquals(pool.sampleBatch(RandomSource.create(815), 4096), filtered.sampleBatch(RandomSource.create(815), 4096));
    }
    @Test void levelAndModeAreIsolatedAndBlacklistWins() {
        MachinePolicyApi.install(Map.of(id("test:rule"), rule(2, 3, "immortalstorage:nether",
                List.of("minecraft:diamond", "minecraft:iron_ingot"), List.of("minecraft:diamond"))));
        assertTrue(allowed(1,"immortalstorage:nether","minecraft:diamond"));
        assertTrue(allowed(4,"immortalstorage:nether","minecraft:diamond"));
        assertTrue(allowed(2,"immortalstorage:overworld","minecraft:diamond"));
        assertFalse(allowed(2,"immortalstorage:nether","minecraft:diamond"));
        assertTrue(allowed(3,"immortalstorage:nether","minecraft:iron_ingot"));
        assertFalse(allowed(3,"immortalstorage:nether","minecraft:coal"));
    }
    @Test void matchingWhitelistsIntersectRatherThanUndoGlobalRestrictions() {
        MachinePolicyApi.install(Map.of(id("test:global"),rule(1,4,null,List.of(),List.of("minecraft:diamond")),
                id("test:specific"),rule(3,3,"test:mode",List.of("minecraft:diamond","minecraft:coal"),List.of())));
        assertFalse(allowed(3,"test:mode","minecraft:diamond"));
        assertTrue(allowed(3,"test:mode","minecraft:coal"));
        assertFalse(allowed(3,"test:mode","minecraft:iron_ingot"));
    }
    @Test void selectorsConsultCurrentTagsAndSnapshotIsImmutable() {
        var rule=rule(1,4,null,List.of("#c:gems"),List.of());
        assertTrue(rule.permits(id("test:gem"),tag -> true));
        assertFalse(rule.permits(id("test:gem"),tag -> false));
        MachinePolicyApi.install(Map.of(id("test:rule"),rule));
        assertThrows(UnsupportedOperationException.class, () -> MachinePolicyApi.rules().clear());
    }
    @Test void reloadRemovalRestoresDefaultsAndAdvancesGeneration() {
        MachinePolicyApi.install(Map.of(id("test:r"),rule(1,4,null,List.of(),List.of("minecraft:coal"))));
        long generation=MachinePolicyApi.generation();
        assertFalse(allowed(1,"test:mode","minecraft:coal"));
        MachinePolicyApi.install(Map.of());
        assertTrue(MachinePolicyApi.generation()>generation);
        assertTrue(allowed(1,"test:mode","minecraft:coal"));
    }
    @Test void resourceListenerReplacesAndRemovesTheWholePackSnapshot() {
        var listener = new MachinePolicyReloadListener();
        var key = id("test:loaded_rule");
        listener.apply(Map.of(key, JsonParser.parseString("{\"machine\":\"world_shard_miner\",\"target\":\"output_item\",\"blacklist\":[\"minecraft:coal\"]}")), null, null);
        assertFalse(allowed(1,"test:mode","minecraft:coal"));
        listener.apply(Map.of(key, JsonParser.parseString("{\"enabled\":false}")), null, null);
        assertFalse(MachinePolicyApi.rules().containsKey(key));
        assertTrue(allowed(1,"test:mode","minecraft:coal"));
        listener.apply(Map.of(), null, null);
        assertFalse(MachinePolicyApi.rules().containsKey(key));
    }
    @Test void filterBeforeSamplingMaintainsWholeYieldAndHandlesEmptyPool() {
        var pool=WorldShardOrePool.of(Map.of(Items.DIAMOND,1L,Items.IRON_INGOT,999L));
        assertEquals(Map.of(Items.DIAMOND,4096),pool.filtered(item -> item==Items.DIAMOND).sampleBatch(RandomSource.create(1),4096));
        assertTrue(pool.filtered(item -> false).sampleBatch(RandomSource.create(1),4096).isEmpty());
    }
    @Test void machineAndTargetAreIsolated() {
        var rule=new Rule(Machine.SIMULATED_SPIRIT_FIELD,1,1,id("minecraft:wheat"),Target.SUBSTRATE_BLOCK,List.of("minecraft:farmland"),List.of());
        assertFalse(rule.applies(Machine.SIMULATED_REINCARNATION_FURNACE,1,id("minecraft:wheat"),Target.SUBSTRATE_BLOCK));
        assertFalse(rule.applies(Machine.SIMULATED_SPIRIT_FIELD,1,id("minecraft:wheat"),Target.INPUT_BLOCK));
        assertTrue(rule.applies(Machine.SIMULATED_SPIRIT_FIELD,1,id("minecraft:wheat"),Target.SUBSTRATE_BLOCK));
    }
    @Test void capturedEntitiesAndSpawnEggsUseTheSameTypePolicy() throws Exception {
        var furnace = new com.immortalstorage.immortalstorage.block.entity.SimulatedReincarnationFurnaceBlockEntity(
                net.minecraft.core.BlockPos.ZERO,
                com.immortalstorage.immortalstorage.block.ModBlocks.SIMULATED_REINCARNATION_FURNACE.get().defaultBlockState());
        var method = furnace.getClass().getDeclaredMethod("hasSpecimenSource");
        method.setAccessible(true);
        MachinePolicyApi.install(Map.of(id("test:pig"), new Rule(Machine.SIMULATED_REINCARNATION_FURNACE,
                1,1,id("minecraft:pig"),Target.INPUT_ENTITY,List.of(),List.of("minecraft:pig"))));
        furnace.setItem(0, captured("minecraft:pig"));
        assertFalse((boolean)method.invoke(furnace));
        furnace.setItem(0, new net.minecraft.world.item.ItemStack(Items.PIG_SPAWN_EGG));
        assertFalse((boolean)method.invoke(furnace));
        furnace.setItem(0, captured("minecraft:cow"));
        assertTrue((boolean)method.invoke(furnace));
        MachinePolicyApi.install(Map.of());
        furnace.setItem(0, captured("minecraft:pig"));
        assertTrue((boolean)method.invoke(furnace));
        furnace.setItem(0, captured("Invalid Id"));
        assertFalse((boolean)method.invoke(furnace));
    }
    private static net.minecraft.world.item.ItemStack captured(String type) {
        var entity = new net.minecraft.nbt.CompoundTag(); entity.putString("id",type);
        var root = new net.minecraft.nbt.CompoundTag(); root.put("containedEntity",entity);
        var stack = new net.minecraft.world.item.ItemStack(com.immortalstorage.immortalstorage.item.ModItems.SOUL_CATCHER.get());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(root));
        return stack;
    }
    @Test void parserRejectsInvalidRangesSelectorsAndUnsupportedTargets() {
        assertThrows(IllegalArgumentException.class, () -> rule(0,4,null,List.of(),List.of()));
        assertThrows(IllegalArgumentException.class, () -> rule(4,1,null,List.of(),List.of()));
        assertThrows(IllegalArgumentException.class, () -> rule(1,4,null,List.of("#Bad Tag"),List.of()));
        assertThrows(IllegalArgumentException.class, () -> new Rule(Machine.WORLD_SHARD_MINER,1,4,null,Target.INPUT_ENTITY,List.of(),List.of()));
        var json=JsonParser.parseString("{\"machine\":\"world_shard_miner\",\"target\":\"output_item\",\"min_level\":2,\"max_level\":4,\"blacklist\":[\"minecraft:diamond\"]}").getAsJsonObject();
        assertEquals(rule(2,4,null,List.of(),List.of("minecraft:diamond")), MachinePolicyReloadListener.parse(json));
        json.addProperty("min_level",1.5);
        assertThrows(RuntimeException.class, () -> MachinePolicyReloadListener.parse(json));
    }
}
