package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.item.custom.ImmortalYuanItem;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EmbeddedImmortalFurnaceBackendTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void onlyImmortalStorageYuanItemsHaveImmortalFurnaceBurnTime() {
        assertEquals(150, EmbeddedImmortalFurnaceBackend.fuelTicks(
                ResourceLocation.fromNamespaceAndPath("immortalstorage", "true_yuan")));
        assertEquals(500, EmbeddedImmortalFurnaceBackend.fuelTicks(
                ResourceLocation.fromNamespaceAndPath("immortalstorage", "immortal_yuan")));
        assertEquals(0, EmbeddedImmortalFurnaceBackend.fuelTicks(
                ResourceLocation.fromNamespaceAndPath("minecraft", "coal")));
        assertEquals(0, EmbeddedImmortalFurnaceBackend.fuelTicks(
                ResourceLocation.fromNamespaceAndPath("other", "true_yuan")));
    }

    @Test
    void spiritDriveIsNotAcceptedByTheEmbeddedFurnace() {
        EmbeddedImmortalFurnaceBackend backend = new EmbeddedImmortalFurnaceBackend();
        ItemStack drive = new ItemStack(ModItems.SPIRIT_DRIVE.get());
        assertFalse(backend.isFuel(drive));
    }

    @Test
    void resultCapacityRequiresMatchingFullComponentsAndStackRoom() {
        assertTrue(EmbeddedImmortalFurnaceBackend.canAcceptResult(
                ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT, 2)));
        assertTrue(EmbeddedImmortalFurnaceBackend.canAcceptResult(
                new ItemStack(Items.IRON_INGOT, 62), new ItemStack(Items.IRON_INGOT, 2)));
        assertFalse(EmbeddedImmortalFurnaceBackend.canAcceptResult(
                new ItemStack(Items.IRON_INGOT, 63), new ItemStack(Items.IRON_INGOT, 2)));
        assertFalse(EmbeddedImmortalFurnaceBackend.canAcceptResult(
                new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.IRON_INGOT)));
    }

    @Test
    void dataAccessProducesVanillaFlameAndArrowScales() {
        EmbeddedImmortalFurnaceBackend backend = new EmbeddedImmortalFurnaceBackend();
        backend.dataAccess().set(0, 75);
        backend.dataAccess().set(1, 150);
        backend.dataAccess().set(2, 100);
        backend.dataAccess().set(3, 200);

        assertTrue(backend.isLit());
        assertEquals(7, backend.litProgress());
        assertEquals(12, backend.burnProgress());
        assertEquals(10, backend.dataAccess().getCount());
    }

    @Test
    void automaticImmortalFuelStagesOneRealItemOnlyWhenEnabledAndNeeded() {
        EmbeddedImmortalFurnaceBackend backend = new EmbeddedImmortalFurnaceBackend();
        AtomicInteger requests = new AtomicInteger();
        ImmortalYuanItem immortalYuan = (ImmortalYuanItem) ModItems.IMMORTAL_YUAN.get();
        java.util.function.Supplier<ItemStack> source = () -> {
            requests.incrementAndGet();
            return new ItemStack(immortalYuan);
        };

        assertFalse(backend.tryAutoRefuel(source));
        backend.setAutoConsume(true);
        assertFalse(backend.tryAutoRefuel(source), "an empty furnace must not reserve fuel");
        backend.setItem(EmbeddedImmortalFurnaceBackend.INPUT, new ItemStack(Items.IRON_ORE));

        assertTrue(backend.tryAutoRefuel(source));
        assertEquals(1, requests.get());
        assertEquals(immortalYuan, backend.getItem(EmbeddedImmortalFurnaceBackend.FUEL).getItem());
        assertFalse(backend.tryAutoRefuel(source), "a staged fuel item must not be duplicated");
        assertEquals(1, requests.get());
    }

    @Test
    void embeddedAutomaticFuelSelectsTrueYuanBeforeAscensionAndImmortalYuanAfterward()
            throws Exception {
        String backend = Files.readString(source("EmbeddedImmortalFurnaceBackend.java"));
        assertTrue(backend.contains("storage.getStage() < 6"));
        assertTrue(backend.contains("storage.consumeTrueYuan(1L)"));
        assertTrue(backend.contains("new ItemStack(ModItems.TRUE_YUAN.get())"));
        assertTrue(backend.contains("storage.consumeImmortalYuan(1L)"));
        assertTrue(backend.contains("new ItemStack(ModItems.IMMORTAL_YUAN.get())"));
    }

    @Test
    void autoFillPersistsRememberedInputsAndReturnsOutputsToPersonalStorage() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();
        data.setStage(5);
        EmbeddedImmortalFurnaceBackend backend = data.getEmbeddedImmortalFurnace();
        backend.setItem(EmbeddedImmortalFurnaceBackend.INPUT, new ItemStack(Items.IRON_ORE, 4));
        backend.setAutoFill(true);
        backend.setItem(EmbeddedImmortalFurnaceBackend.INPUT, ItemStack.EMPTY);
        data.insertStack(new ItemStack(Items.IRON_ORE, 4), true);
        backend.setItem(EmbeddedImmortalFurnaceBackend.RESULT, new ItemStack(Items.IRON_INGOT, 2));

        backend.tick(null); // Null owners remain a no-op and do not corrupt the persistent state.
        assertTrue(backend.getItem(EmbeddedImmortalFurnaceBackend.INPUT).isEmpty());
        assertEquals(2, backend.getItem(EmbeddedImmortalFurnaceBackend.RESULT).getCount());

        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        restored.deserializeNBT(REGISTRIES, data.serializeNBT(REGISTRIES));
        assertTrue(restored.getEmbeddedImmortalFurnace().isAutoFill());
    }

    @Test
    void trueYuanAdvancesThreeChannelsIndependentlyAndCooksOneItemAtFiftyTicks() {
        EmbeddedImmortalFurnaceBackend backend = threeChannelBackend(2);

        tick(backend, 0, 49, ImmortalFurnaceEngine.TRUE_YUAN);
        assertTrue(backend.getItem(EmbeddedImmortalFurnaceBackend.RESULT).isEmpty());
        assertEquals(49, backend.channelProgress(0));
        assertEquals(49, backend.channelProgress(1));
        assertEquals(49, backend.channelProgress(2));

        tick(backend, 49, 50, ImmortalFurnaceEngine.TRUE_YUAN);

        assertEquals(1, backend.getItem(EmbeddedImmortalFurnaceBackend.INPUT).getCount());
        assertEquals(1, backend.getItem(EmbeddedImmortalFurnaceBackend.INPUT_2).getCount());
        assertEquals(1, backend.getItem(EmbeddedImmortalFurnaceBackend.INPUT_3).getCount());
        assertEquals(Items.IRON_INGOT, backend.getItem(EmbeddedImmortalFurnaceBackend.RESULT).getItem());
        assertEquals(Items.GOLD_INGOT, backend.getItem(EmbeddedImmortalFurnaceBackend.RESULT_2).getItem());
        assertEquals(Items.COPPER_INGOT, backend.getItem(EmbeddedImmortalFurnaceBackend.RESULT_3).getItem());
        assertEquals(Map.of(id("iron"), 1), backend.recipeUsage(0));
        assertEquals(Map.of(id("gold"), 1), backend.recipeUsage(1));
        assertEquals(Map.of(id("copper"), 1), backend.recipeUsage(2));
    }

    @Test
    void embeddedPluginAcceleratesProgressWithoutChangingFuelOrRecipeYield() {
        EmbeddedImmortalFurnaceBackend backend = new EmbeddedImmortalFurnaceBackend();
        backend.setItem(EmbeddedImmortalFurnaceBackend.INPUT, new ItemStack(Items.IRON_ORE, 2));
        backend.setItem(EmbeddedImmortalFurnaceBackend.FUEL, new ItemStack(Items.COAL, 2));
        backend.setItem(EmbeddedImmortalFurnaceBackend.PLUGIN,
                new ItemStack(ModItems.DIMENSIONAL_PEEKING_ORDER.get()));

        tick(backend, 0, 12, ImmortalFurnaceEngine.TRUE_YUAN);
        assertEquals(48, backend.channelProgress(0));
        assertTrue(backend.getItem(EmbeddedImmortalFurnaceBackend.RESULT).isEmpty());

        tick(backend, 12, 13, ImmortalFurnaceEngine.TRUE_YUAN);
        assertEquals(1, backend.getItem(EmbeddedImmortalFurnaceBackend.INPUT).getCount());
        assertEquals(1, backend.getItem(EmbeddedImmortalFurnaceBackend.RESULT).getCount(),
                "the plugin accelerates one normal recipe completion instead of multiplying output");
        assertEquals(1, backend.getItem(EmbeddedImmortalFurnaceBackend.FUEL).getCount(),
                "the plugin does not multiply fuel consumption");
        assertEquals(138, backend.data(0), "burn time still decreases by exactly one after the ignition tick");
    }

    @Test
    void immortalYuanCooksTheWholeInputStackAtomicallyAtTwentyFiveTicks() {
        EmbeddedImmortalFurnaceBackend backend = new EmbeddedImmortalFurnaceBackend();
        backend.setItem(EmbeddedImmortalFurnaceBackend.INPUT, new ItemStack(Items.IRON_ORE, 64));
        backend.setItem(EmbeddedImmortalFurnaceBackend.FUEL, new ItemStack(Items.COAL));

        tick(backend, 0, 24, ImmortalFurnaceEngine.IMMORTAL_YUAN);
        assertEquals(64, backend.getItem(EmbeddedImmortalFurnaceBackend.INPUT).getCount());
        assertTrue(backend.getItem(EmbeddedImmortalFurnaceBackend.RESULT).isEmpty());
        assertEquals(24, backend.channelProgress(0));

        tick(backend, 24, 25, ImmortalFurnaceEngine.IMMORTAL_YUAN);

        assertTrue(backend.getItem(EmbeddedImmortalFurnaceBackend.INPUT).isEmpty());
        assertEquals(64, backend.getItem(EmbeddedImmortalFurnaceBackend.RESULT).getCount());
        assertEquals(Map.of(id("iron"), 64), backend.recipeUsage(0));
    }

    @Test
    void fullResultPausesExistingProgressWithoutConsumingOrDuplicatingInput() {
        EmbeddedImmortalFurnaceBackend backend = new EmbeddedImmortalFurnaceBackend();
        backend.setItem(EmbeddedImmortalFurnaceBackend.INPUT, new ItemStack(Items.IRON_ORE, 2));
        backend.setItem(EmbeddedImmortalFurnaceBackend.FUEL, new ItemStack(Items.COAL));
        tick(backend, 0, 25, ImmortalFurnaceEngine.TRUE_YUAN);
        backend.setItem(EmbeddedImmortalFurnaceBackend.RESULT, new ItemStack(Items.IRON_INGOT, 64));

        tick(backend, 25, 100, ImmortalFurnaceEngine.TRUE_YUAN);

        assertEquals(25, backend.channelProgress(0), "blocked output pauses instead of decaying progress");
        assertEquals(2, backend.getItem(EmbeddedImmortalFurnaceBackend.INPUT).getCount());
        assertEquals(64, backend.getItem(EmbeddedImmortalFurnaceBackend.RESULT).getCount());
        assertTrue(backend.recipeUsage(0).isEmpty());
    }

    @Test
    void immortalWholeStackDoesNotIgniteWhenTheCompleteBatchCannotFit() {
        EmbeddedImmortalFurnaceBackend backend = new EmbeddedImmortalFurnaceBackend();
        backend.setItem(EmbeddedImmortalFurnaceBackend.INPUT, new ItemStack(Items.IRON_ORE, 64));
        backend.setItem(EmbeddedImmortalFurnaceBackend.FUEL, new ItemStack(Items.COAL));
        backend.setItem(EmbeddedImmortalFurnaceBackend.RESULT, new ItemStack(Items.IRON_INGOT));

        tick(backend, 0, 100, ImmortalFurnaceEngine.IMMORTAL_YUAN);

        assertFalse(backend.isLit());
        assertEquals(1, backend.getItem(EmbeddedImmortalFurnaceBackend.FUEL).getCount());
        assertEquals(64, backend.getItem(EmbeddedImmortalFurnaceBackend.INPUT).getCount());
        assertEquals(1, backend.getItem(EmbeddedImmortalFurnaceBackend.RESULT).getCount());
    }

    @Test
    void fallbackAcceptsNonImmortalStorageSmeltingAndBlastingOnly() {
        assertTrue(EmbeddedImmortalFurnaceBackend.isAllowedFallbackRecipe(
                RecipeType.SMELTING, ResourceLocation.withDefaultNamespace("iron_ingot")));
        assertTrue(EmbeddedImmortalFurnaceBackend.isAllowedFallbackRecipe(
                RecipeType.BLASTING, ResourceLocation.fromNamespaceAndPath("example", "alloy")));
        assertFalse(EmbeddedImmortalFurnaceBackend.isAllowedFallbackRecipe(
                RecipeType.SMELTING, ResourceLocation.fromNamespaceAndPath("immortalstorage", "pill")));
        assertFalse(EmbeddedImmortalFurnaceBackend.isAllowedFallbackRecipe(
                RecipeType.CRAFTING, ResourceLocation.withDefaultNamespace("iron_block")));
        assertTrue(EmbeddedImmortalFurnaceBackend.recipeSourcePriority(
                EmbeddedImmortalFurnaceBackend.RecipeSource.IMMORTAL_FURNACE)
                < EmbeddedImmortalFurnaceBackend.recipeSourcePriority(
                EmbeddedImmortalFurnaceBackend.RecipeSource.SMELTING));
        assertTrue(EmbeddedImmortalFurnaceBackend.recipeSourcePriority(
                EmbeddedImmortalFurnaceBackend.RecipeSource.SMELTING)
                < EmbeddedImmortalFurnaceBackend.recipeSourcePriority(
                EmbeddedImmortalFurnaceBackend.RecipeSource.BLASTING));
    }

    @Test
    void playerOwnsOneEmbeddedFurnaceSharedByBothTerminalMenus() {
        ImmortalStoragePlayerData data = new ImmortalStoragePlayerData();

        assertSame(data.getEmbeddedImmortalFurnace(), data.getEmbeddedImmortalFurnace());
        EmbeddedImmortalFurnaceBackend furnace = data.getEmbeddedImmortalFurnace();
        data.setStage(5);
        data.setStage(6);
        assertSame(furnace, data.getEmbeddedImmortalFurnace(),
                "the 5 -> 6 terminal transition must retain the same eight real slots");
    }

    @Test
    void allEightSlotsAndRunningRecipeStateSurvivePlayerNbtRoundTrip() throws Exception {
        ImmortalStoragePlayerData original = new ImmortalStoragePlayerData();
        EmbeddedImmortalFurnaceBackend furnace = original.getEmbeddedImmortalFurnace();
        furnace.setAutoConsume(true);
        furnace.setAutoFill(true);
        furnace.setItem(EmbeddedImmortalFurnaceBackend.INPUT, new ItemStack(Items.IRON_ORE, 2));
        furnace.setItem(EmbeddedImmortalFurnaceBackend.INPUT_2, new ItemStack(Items.GOLD_ORE, 2));
        furnace.setItem(EmbeddedImmortalFurnaceBackend.INPUT_3, new ItemStack(Items.COPPER_ORE, 2));
        furnace.setItem(EmbeddedImmortalFurnaceBackend.FUEL, new ItemStack(Items.COAL));
        tick(furnace, 0, 50, ImmortalFurnaceEngine.TRUE_YUAN);

        furnace.setItem(EmbeddedImmortalFurnaceBackend.INPUT, new ItemStack(Items.IRON_ORE, 2));
        furnace.setItem(EmbeddedImmortalFurnaceBackend.FUEL, new ItemStack(Items.BLAZE_POWDER, 5));
        furnace.setItem(EmbeddedImmortalFurnaceBackend.RESULT, new ItemStack(Items.IRON_INGOT, 6));
        furnace.setItem(EmbeddedImmortalFurnaceBackend.INPUT_2, new ItemStack(Items.GOLD_ORE, 3));
        furnace.setItem(EmbeddedImmortalFurnaceBackend.RESULT_2, new ItemStack(Items.GOLD_INGOT, 7));
        furnace.setItem(EmbeddedImmortalFurnaceBackend.INPUT_3, new ItemStack(Items.COPPER_ORE, 4));
        furnace.setItem(EmbeddedImmortalFurnaceBackend.RESULT_3, new ItemStack(Items.COPPER_INGOT, 8));
        furnace.setItem(EmbeddedImmortalFurnaceBackend.PLUGIN,
                new ItemStack(ModItems.DIMENSIONAL_PARALLEL_TALISMAN.get()));
        furnace.dataAccess().set(0, 91);
        furnace.dataAccess().set(1, 150);
        furnace.dataAccess().set(2, 17);
        furnace.dataAccess().set(3, 50);
        furnace.dataAccess().set(4, 18);
        furnace.dataAccess().set(5, 50);
        furnace.dataAccess().set(6, 19);
        furnace.dataAccess().set(7, 50);

        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();
        restored.deserializeNBT(REGISTRIES, original.serializeNBT(REGISTRIES));
        EmbeddedImmortalFurnaceBackend reopened = restored.getEmbeddedImmortalFurnace();

        assertEquals(2, reopened.getItem(EmbeddedImmortalFurnaceBackend.INPUT).getCount());
        assertEquals(5, reopened.getItem(EmbeddedImmortalFurnaceBackend.FUEL).getCount());
        assertEquals(6, reopened.getItem(EmbeddedImmortalFurnaceBackend.RESULT).getCount());
        assertEquals(3, reopened.getItem(EmbeddedImmortalFurnaceBackend.INPUT_2).getCount());
        assertEquals(7, reopened.getItem(EmbeddedImmortalFurnaceBackend.RESULT_2).getCount());
        assertEquals(4, reopened.getItem(EmbeddedImmortalFurnaceBackend.INPUT_3).getCount());
        assertEquals(8, reopened.getItem(EmbeddedImmortalFurnaceBackend.RESULT_3).getCount());
        assertEquals(16, reopened.reinforcementMultiplier());
        assertTrue(reopened.isAutoConsume());
        assertTrue(reopened.isAutoFill());
        assertEquals(91, reopened.data(0));
        assertEquals(150, reopened.data(1));
        assertEquals(17, reopened.channelProgress(0));
        assertEquals(18, reopened.channelProgress(1));
        assertEquals(19, reopened.channelProgress(2));
        assertEquals(Map.of(id("iron"), 1), reopened.recipeUsage(0));
        assertEquals(Map.of(id("gold"), 1), reopened.recipeUsage(1));
        assertEquals(Map.of(id("copper"), 1), reopened.recipeUsage(2));

        reopened.tickCore(50L, stack -> ImmortalFurnaceEngine.NO_FUEL,
                EmbeddedImmortalFurnaceBackendTest::recipe);
        assertEquals(33, reopened.channelProgress(0), "the active recipe resumes with plugin acceleration");
        assertEquals(34, reopened.channelProgress(1));
        assertEquals(35, reopened.channelProgress(2));
        assertEquals(90, reopened.data(0), "the remaining burn time resumes from the persisted value");
    }

    @Test
    void legacyPlayerSaveWithoutEmbeddedFurnaceLoadsAnEmptyIdleBackend() {
        ImmortalStoragePlayerData restored = new ImmortalStoragePlayerData();

        restored.deserializeNBT(REGISTRIES, new net.minecraft.nbt.CompoundTag());

        EmbeddedImmortalFurnaceBackend furnace = restored.getEmbeddedImmortalFurnace();
        assertEquals(8, furnace.getContainerSize());
        assertTrue(furnace.isEmpty());
        assertFalse(furnace.isLit());
        assertFalse(furnace.isAutoConsume());
        assertFalse(furnace.isAutoFill());
        assertTrue(furnace.recipeUsage(0).isEmpty());
        assertTrue(furnace.recipeUsage(1).isEmpty());
        assertTrue(furnace.recipeUsage(2).isEmpty());
    }

    @Test
    void closingEitherTerminalDoesNotDrainOrReturnThePersistentFurnace() throws Exception {
        String kongqiao = Files.readString(source("KongqiaoMenu.java"));
        String xianqiao = Files.readString(source("XianqiaoStorageMenu.java"));

        assertTrue(kongqiao.contains("this.furnace = data.getEmbeddedImmortalFurnace();"));
        assertTrue(xianqiao.contains("this.furnace = data.getEmbeddedImmortalFurnace();"));
        assertFalse(kongqiao.contains("furnace.drainContents()"));
        assertFalse(xianqiao.contains("furnace.returnContents("));
    }

    @Test
    void playerTickOwnsBackgroundFurnaceProgressInsteadOfAnOpenMenu() throws Exception {
        String kongqiao = Files.readString(source("KongqiaoMenu.java"));
        String xianqiao = Files.readString(source("XianqiaoStorageMenu.java"));
        String events = Files.readString(projectSource("event", "CommonEvents.java"));

        assertFalse(kongqiao.contains("furnace.tick(serverPlayer)"));
        assertFalse(xianqiao.contains("furnace.tick(serverPlayer)"));
        assertTrue(events.contains("d.getEmbeddedImmortalFurnace().tick(p)"));
        assertTrue(events.contains("d.getStage() >= 5"));
    }

    @Test
    void spiritSwordRecallUsesPersistentAuthoritativeReservations() throws Exception {
        String backend = Files.readString(source("EmbeddedImmortalFurnaceBackend.java"));
        String engine = Files.readString(source("ImmortalFurnaceEngine.java"));
        assertTrue(backend.contains("SpiritSwordReservations"));
        assertTrue(backend.contains("UUID[] recallTokens"));
        assertTrue(backend.contains("PersistentPlayerIdentity.id(player)"));
        assertTrue(backend.contains("PersistentPlayerIdentity") && backend.contains(".matches(player, identity.owner())"));
        assertTrue(backend.contains("matchingReservedChannel(player, identity)"));
        assertTrue(backend.contains("if (isRecallReserved(channel)) continue;"),
                "auto fill and template capture must leave recalled channels idle");
        assertTrue(engine.contains("if (suspendedChannel.test(channel)) continue;"),
                "reserved channels must preserve their active recipe and progress");
    }

    private static EmbeddedImmortalFurnaceBackend threeChannelBackend(int inputCount) {
        EmbeddedImmortalFurnaceBackend backend = new EmbeddedImmortalFurnaceBackend();
        backend.setItem(EmbeddedImmortalFurnaceBackend.INPUT, new ItemStack(Items.IRON_ORE, inputCount));
        backend.setItem(EmbeddedImmortalFurnaceBackend.INPUT_2, new ItemStack(Items.GOLD_ORE, inputCount));
        backend.setItem(EmbeddedImmortalFurnaceBackend.INPUT_3, new ItemStack(Items.COPPER_ORE, inputCount));
        backend.setItem(EmbeddedImmortalFurnaceBackend.FUEL, new ItemStack(Items.COAL));
        return backend;
    }

    private static void tick(EmbeddedImmortalFurnaceBackend backend, int fromInclusive, int toExclusive,
                             ImmortalFurnaceEngine.FuelProfile fuel) {
        for (int tick = fromInclusive; tick < toExclusive; tick++) {
            backend.tickCore(tick, stack -> stack.isEmpty() ? ImmortalFurnaceEngine.NO_FUEL : fuel,
                    EmbeddedImmortalFurnaceBackendTest::recipe);
        }
    }

    private static Optional<ImmortalFurnaceEngine.RecipePlan> recipe(ItemStack input) {
        if (input.is(Items.IRON_ORE)) {
            return Optional.of(new ImmortalFurnaceEngine.RecipePlan(id("iron"), new ItemStack(Items.IRON_INGOT)));
        }
        if (input.is(Items.GOLD_ORE)) {
            return Optional.of(new ImmortalFurnaceEngine.RecipePlan(id("gold"), new ItemStack(Items.GOLD_INGOT)));
        }
        if (input.is(Items.COPPER_ORE)) {
            return Optional.of(new ImmortalFurnaceEngine.RecipePlan(id("copper"), new ItemStack(Items.COPPER_INGOT)));
        }
        return Optional.empty();
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("test", path);
    }

    private static Path source(String fileName) {
        return projectSource("menu", "custom", fileName);
    }

    private static Path projectSource(String... parts) {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("src/main/java/com/immortalstorage/immortalstorage");
            for (String part : parts) candidate = candidate.resolve(part);
            if (Files.isRegularFile(candidate)) return candidate;
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate project source " + String.join("/", parts));
    }
}
