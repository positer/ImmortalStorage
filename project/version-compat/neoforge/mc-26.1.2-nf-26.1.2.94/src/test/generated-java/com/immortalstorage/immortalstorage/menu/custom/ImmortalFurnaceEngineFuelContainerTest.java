package com.immortalstorage.immortalstorage.menu.custom;

import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ImmortalFurnaceEngineFuelContainerTest {
    @org.junit.jupiter.api.BeforeAll
    static void immortalStorageTargetBootstrap() {
        com.immortalstorage.immortalstorage.compat.CompatTestBootstrap.bootstrap();
    }

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        Bootstrap.bootStrap();
    }

    @Test
    void reusableFuelContainerDrainsItsChargeWithoutDeletingTheContainer() {
        SimpleContainer inventory = new SimpleContainer(7);
        inventory.setItem(0, new ItemStack(Items.IRON_ORE, 64));
        inventory.setItem(1, new ItemStack(Items.BLAZE_ROD)); // proxy for a charged Spirit Drive
        AtomicInteger charges = new AtomicInteger(1);
        ImmortalFurnaceEngine engine = new ImmortalFurnaceEngine(
                new int[] {0, 3, 5}, 1, new int[] {2, 4, 6});

        ImmortalFurnaceEngine.FuelResolver fuel = new ImmortalFurnaceEngine.FuelResolver() {
            @Override
            public ImmortalFurnaceEngine.FuelProfile resolve(ItemStack stack) {
                return charges.get() > 0
                        ? ImmortalFurnaceEngine.IMMORTAL_YUAN
                        : ImmortalFurnaceEngine.NO_FUEL;
            }

            @Override
            public ImmortalFurnaceEngine.FuelProfile consume(
                    ItemStack stack, ImmortalFurnaceEngine.FuelProfile profile) {
                charges.decrementAndGet();
                return profile;
            }
        };
        ImmortalFurnaceEngine.RecipeResolver recipes = stack -> stack.is(Items.IRON_ORE)
                ? Optional.of(new ImmortalFurnaceEngine.RecipePlan(
                        Identifier.fromNamespaceAndPath("test", "iron"),
                        new ItemStack(Items.IRON_INGOT)))
                : Optional.empty();

        for (int tick = 0; tick < 25; tick++) {
            engine.tick(tick, inventory, fuel, recipes);
        }

        assertEquals(0, charges.get());
        assertEquals(Items.BLAZE_ROD, inventory.getItem(1).getItem());
        assertEquals(1, inventory.getItem(1).getCount());
        assertTrue(inventory.getItem(0).isEmpty());
        assertEquals(64, inventory.getItem(2).getCount());
    }

    @Test
    void trueYuanOfferCanIgniteWhenWholeStackWouldNotFit() {
        SimpleContainer inventory = new SimpleContainer(7);
        inventory.setItem(0, new ItemStack(Items.IRON_ORE, 64));
        inventory.setItem(1, new ItemStack(Items.BLAZE_ROD));
        inventory.setItem(2, new ItemStack(Items.IRON_INGOT, 63));
        ImmortalFurnaceEngine engine = new ImmortalFurnaceEngine(
                new int[] {0, 3, 5}, 1, new int[] {2, 4, 6});
        ImmortalFurnaceEngine.FuelResolver fuel = stack -> ImmortalFurnaceEngine.TRUE_YUAN;
        ImmortalFurnaceEngine.RecipeResolver recipes = stack -> stack.is(Items.IRON_ORE)
                ? Optional.of(new ImmortalFurnaceEngine.RecipePlan(
                        Identifier.fromNamespaceAndPath("test", "iron"),
                        new ItemStack(Items.IRON_INGOT)))
                : Optional.empty();

        assertTrue(engine.tick(0L, inventory, fuel, recipes));
        assertTrue(engine.isLit(), "single-item True Yuan cooking must not use whole-stack preflight");
    }

    @Test
    void suspendedChannelPreservesProgressWhileOtherChannelsContinue() {
        SimpleContainer inventory = new SimpleContainer(7);
        inventory.setItem(0, new ItemStack(Items.IRON_ORE));
        inventory.setItem(3, new ItemStack(Items.IRON_ORE));
        inventory.setItem(1, new ItemStack(Items.BLAZE_ROD));
        ImmortalFurnaceEngine engine = new ImmortalFurnaceEngine(
                new int[] {0, 3, 5}, 1, new int[] {2, 4, 6});
        engine.setProgress(0, 17);
        engine.setActiveRecipe(0, Identifier.fromNamespaceAndPath("test", "iron"));

        engine.tick(0L, inventory, stack -> ImmortalFurnaceEngine.TRUE_YUAN,
                ImmortalFurnaceEngineFuelContainerTest::ironRecipe, channel -> channel == 0);

        assertEquals(17, engine.progress(0));
        assertEquals(Identifier.fromNamespaceAndPath("test", "iron"), engine.activeRecipe(0));
        assertEquals(1, engine.progress(1));
    }

    @Test
    void rejectedCredentialPaymentDoesNotIgniteOrConsumeTheDrive() {
        SimpleContainer inventory = new SimpleContainer(7);
        inventory.setItem(0, new ItemStack(Items.IRON_ORE));
        inventory.setItem(1, new ItemStack(Items.BLAZE_ROD));
        ImmortalFurnaceEngine engine = new ImmortalFurnaceEngine(
                new int[] {0, 3, 5}, 1, new int[] {2, 4, 6});
        ImmortalFurnaceEngine.FuelResolver fuel = new ImmortalFurnaceEngine.FuelResolver() {
            @Override
            public ImmortalFurnaceEngine.FuelProfile resolve(ItemStack stack) {
                return ImmortalFurnaceEngine.IMMORTAL_YUAN;
            }

            @Override
            public ImmortalFurnaceEngine.FuelProfile consume(
                    ItemStack stack, ImmortalFurnaceEngine.FuelProfile profile) {
                return ImmortalFurnaceEngine.NO_FUEL;
            }
        };
        ImmortalFurnaceEngine.RecipeResolver recipes = stack -> Optional.of(
                new ImmortalFurnaceEngine.RecipePlan(
                        Identifier.fromNamespaceAndPath("test", "iron"),
                        new ItemStack(Items.IRON_INGOT)));

        assertFalse(engine.tick(0L, inventory, fuel, recipes));
        assertFalse(engine.isLit());
        assertEquals(1, inventory.getItem(1).getCount());
        assertEquals(1, inventory.getItem(0).getCount());
    }

    @Test
    void blockedOrInvalidRecipesNeverQueryFuelAuthorization() {
        AtomicInteger authorizationQueries = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        AtomicInteger notRunnable = new AtomicInteger();
        ImmortalFurnaceEngine.FuelResolver fuel = countingDriveResolver(
                authorizationQueries, failures, notRunnable, true, true);

        SimpleContainer invalid = new SimpleContainer(7);
        invalid.setItem(0, new ItemStack(Items.DIRT));
        invalid.setItem(1, new ItemStack(Items.BLAZE_ROD));
        ImmortalFurnaceEngine invalidEngine = new ImmortalFurnaceEngine(
                new int[] {0, 3, 5}, 1, new int[] {2, 4, 6});
        assertFalse(invalidEngine.tick(0L, invalid, fuel, stack -> Optional.empty()));

        SimpleContainer blocked = new SimpleContainer(7);
        blocked.setItem(0, new ItemStack(Items.IRON_ORE));
        blocked.setItem(1, new ItemStack(Items.BLAZE_ROD));
        blocked.setItem(2, new ItemStack(Items.GOLD_INGOT, 64));
        ImmortalFurnaceEngine blockedEngine = new ImmortalFurnaceEngine(
                new int[] {0, 3, 5}, 1, new int[] {2, 4, 6});
        assertFalse(blockedEngine.tick(0L, blocked, fuel, ImmortalFurnaceEngineFuelContainerTest::ironRecipe));

        assertEquals(0, authorizationQueries.get());
        assertEquals(0, failures.get(), "non-runnable channels must clear, not enter retry backoff");
        assertEquals(2, notRunnable.get());
    }

    @Test
    void dualBalanceFallsBackToTrueYuanWhenWholeStackIsBlocked() {
        SimpleContainer inventory = new SimpleContainer(7);
        inventory.setItem(0, new ItemStack(Items.IRON_ORE, 64));
        inventory.setItem(1, new ItemStack(Items.BLAZE_ROD));
        inventory.setItem(2, new ItemStack(Items.IRON_INGOT, 63));
        AtomicInteger authorizationQueries = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        AtomicInteger notRunnable = new AtomicInteger();
        ImmortalFurnaceEngine engine = new ImmortalFurnaceEngine(
                new int[] {0, 3, 5}, 1, new int[] {2, 4, 6});

        assertTrue(engine.tick(0L, inventory,
                countingDriveResolver(authorizationQueries, failures, notRunnable, true, true),
                ImmortalFurnaceEngineFuelContainerTest::ironRecipe));

        assertTrue(engine.isLit());
        assertEquals(ImmortalFurnaceEngine.TRUE_YUAN, engine.activeFuel());
        assertEquals(1, authorizationQueries.get(),
                "the unrunnable Immortal Yuan profile must be skipped before querying balances");
        assertEquals(0, failures.get());
        assertEquals(0, notRunnable.get());
    }

    @Test
    void failedRunnableCredentialUsesFiveTickBackoffAndBlockedOutputClearsIt() {
        SimpleContainer inventory = new SimpleContainer(7);
        inventory.setItem(0, new ItemStack(Items.IRON_ORE));
        inventory.setItem(1, new ItemStack(Items.BLAZE_ROD));
        AtomicInteger authorizationQueries = new AtomicInteger();
        AtomicInteger paymentFailures = new AtomicInteger();
        AtomicInteger notRunnable = new AtomicInteger();
        AtomicLong gameTick = new AtomicLong();
        AtomicLong nextRetry = new AtomicLong(Long.MIN_VALUE);
        ImmortalFurnaceEngine engine = new ImmortalFurnaceEngine(
                new int[] {0, 3, 5}, 1, new int[] {2, 4, 6});
        ImmortalFurnaceEngine.FuelResolver fuel = new ImmortalFurnaceEngine.FuelResolver() {
            @Override
            public ImmortalFurnaceEngine.FuelProfile resolve(ItemStack stack) {
                return ImmortalFurnaceEngine.NO_FUEL;
            }

            @Override
            public List<ImmortalFurnaceEngine.FuelProfile> candidates(ItemStack stack) {
                return List.of(ImmortalFurnaceEngine.TRUE_YUAN);
            }

            @Override
            public boolean canAttemptPayment(ItemStack stack) {
                return nextRetry.get() == Long.MIN_VALUE || gameTick.get() >= nextRetry.get();
            }

            @Override
            public ImmortalFurnaceEngine.FuelProfile authorize(
                    ItemStack stack, ImmortalFurnaceEngine.FuelProfile candidate) {
                authorizationQueries.incrementAndGet();
                return ImmortalFurnaceEngine.NO_FUEL;
            }

            @Override
            public void paymentFailed(ItemStack stack) {
                paymentFailures.incrementAndGet();
                nextRetry.set(gameTick.get() + 5L);
            }

            @Override
            public void notRunnable(ItemStack stack) {
                notRunnable.incrementAndGet();
                nextRetry.set(Long.MIN_VALUE);
            }
        };

        for (long tick = 0; tick < 5; tick++) {
            gameTick.set(tick);
            engine.tick(tick, inventory, fuel, ImmortalFurnaceEngineFuelContainerTest::ironRecipe);
        }
        assertEquals(1, authorizationQueries.get(),
                "the first failed payment alone arms retry; ticks 1-4 must not poll balances");
        assertEquals(1, paymentFailures.get());

        gameTick.set(5L);
        engine.tick(5L, inventory, fuel, ImmortalFurnaceEngineFuelContainerTest::ironRecipe);
        assertEquals(2, authorizationQueries.get(), "authorization runs immediately and again at tick 5");
        assertEquals(2, paymentFailures.get());

        inventory.setItem(2, new ItemStack(Items.GOLD_INGOT, 64));
        gameTick.set(6L);
        engine.tick(6L, inventory, fuel, ImmortalFurnaceEngineFuelContainerTest::ironRecipe);
        assertEquals(1, notRunnable.get());
        assertEquals(Long.MIN_VALUE, nextRetry.get());

        inventory.setItem(2, ItemStack.EMPTY);
        gameTick.set(7L);
        engine.tick(7L, inventory, fuel, ImmortalFurnaceEngineFuelContainerTest::ironRecipe);
        assertEquals(3, authorizationQueries.get(), "unblocking after clear retries immediately");
    }

    @Test
    void removingFuelClearsADeferredRetryWithoutQueryingAuthorization() {
        SimpleContainer inventory = new SimpleContainer(7);
        inventory.setItem(0, new ItemStack(Items.IRON_ORE));
        inventory.setItem(1, new ItemStack(Items.BLAZE_ROD));
        AtomicInteger authorizationQueries = new AtomicInteger();
        AtomicInteger notRunnable = new AtomicInteger();
        ImmortalFurnaceEngine engine = new ImmortalFurnaceEngine(
                new int[] {0, 3, 5}, 1, new int[] {2, 4, 6});
        ImmortalFurnaceEngine.FuelResolver fuel = new ImmortalFurnaceEngine.FuelResolver() {
            @Override
            public ImmortalFurnaceEngine.FuelProfile resolve(ItemStack stack) {
                return ImmortalFurnaceEngine.NO_FUEL;
            }

            @Override
            public List<ImmortalFurnaceEngine.FuelProfile> candidates(ItemStack stack) {
                return List.of(ImmortalFurnaceEngine.TRUE_YUAN);
            }

            @Override
            public ImmortalFurnaceEngine.FuelProfile authorize(
                    ItemStack stack, ImmortalFurnaceEngine.FuelProfile candidate) {
                authorizationQueries.incrementAndGet();
                return ImmortalFurnaceEngine.NO_FUEL;
            }

            @Override
            public void notRunnable(ItemStack stack) {
                notRunnable.incrementAndGet();
            }
        };

        engine.tick(0L, inventory, fuel, ImmortalFurnaceEngineFuelContainerTest::ironRecipe);
        assertEquals(1, authorizationQueries.get());
        inventory.setItem(1, ItemStack.EMPTY);
        engine.tick(1L, inventory, fuel, ImmortalFurnaceEngineFuelContainerTest::ironRecipe);
        assertEquals(1, authorizationQueries.get());
        assertEquals(1, notRunnable.get());
    }

    private static ImmortalFurnaceEngine.FuelResolver countingDriveResolver(
            AtomicInteger authorizationQueries, AtomicInteger failures, AtomicInteger notRunnable,
            boolean immortalAvailable, boolean trueAvailable) {
        return new ImmortalFurnaceEngine.FuelResolver() {
            @Override
            public ImmortalFurnaceEngine.FuelProfile resolve(ItemStack stack) {
                return ImmortalFurnaceEngine.NO_FUEL;
            }

            @Override
            public List<ImmortalFurnaceEngine.FuelProfile> candidates(ItemStack stack) {
                return List.of(ImmortalFurnaceEngine.IMMORTAL_YUAN, ImmortalFurnaceEngine.TRUE_YUAN);
            }

            @Override
            public ImmortalFurnaceEngine.FuelProfile authorize(
                    ItemStack stack, ImmortalFurnaceEngine.FuelProfile candidate) {
                authorizationQueries.incrementAndGet();
                boolean available = candidate.equals(ImmortalFurnaceEngine.IMMORTAL_YUAN)
                        ? immortalAvailable : trueAvailable;
                return available ? candidate : ImmortalFurnaceEngine.NO_FUEL;
            }

            @Override
            public ImmortalFurnaceEngine.FuelProfile consume(
                    ItemStack stack, ImmortalFurnaceEngine.FuelProfile profile) {
                return profile;
            }

            @Override
            public void paymentFailed(ItemStack stack) {
                failures.incrementAndGet();
            }

            @Override
            public void notRunnable(ItemStack stack) {
                notRunnable.incrementAndGet();
            }
        };
    }

    private static Optional<ImmortalFurnaceEngine.RecipePlan> ironRecipe(ItemStack stack) {
        return stack.is(Items.IRON_ORE)
                ? Optional.of(new ImmortalFurnaceEngine.RecipePlan(
                Identifier.fromNamespaceAndPath("test", "iron"),
                new ItemStack(Items.IRON_INGOT)))
                : Optional.empty();
    }
}
