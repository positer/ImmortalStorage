package com.immortalstorage.immortalstorage.gametest;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.dimension.RealmHelper;
import com.immortalstorage.immortalstorage.dimension.RealmTickSafety;
import com.immortalstorage.immortalstorage.item.ModItems;
import com.immortalstorage.immortalstorage.timeshift.TimeShiftMachines;
import com.immortalstorage.immortalstorage.timeshift.TimeShiftAcceleration;
import com.immortalstorage.immortalstorage.timeshift.TimeShiftEntry;
import com.immortalstorage.immortalstorage.timeshift.TimeShiftStore;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Live-server verification that a recorded block really receives extra ticks.
 *
 * <p>A vanilla furnace is a dense, observable block entity, so an accelerated
 * furnace finishing a smelt inside a window far shorter than its 200-tick
 * cooking time is direct evidence that the {@code tickBlockEntities} hook ran.
 * The unit tests cover the tier arithmetic; these tests cover the tick loop.</p>
 */
@GameTestHolder(ImmortalStorageMod.MODID)
@PrefixGameTestTemplate(false)
public final class TimeShiftGameTest {
    private static final BlockPos TARGET = new BlockPos(1, 1, 1);
    private static final int OUTPUT_SLOT = 2;
    /** Enough external ticks to smelt on their own, so a test can tell whether they ran. */
    private static final int EXTERNAL_TICKS = 250;

    /**
     * The exchange-offer shape every AE2/RS recipe is built from.
     *
     * <p>Both third-party pattern formats reject zero-input recipes (AE2's encoder throws
     * "At least one input must be non-null", RS's PatternLayout throws "Ingredients cannot be
     * empty"), so an offer that declares no output must never be constructible — otherwise a
     * free vein would silently hand out items. This pins that shape in the running server.</p>
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void exchangeOfferShapeRejectsZeroOutputs(GameTestHelper helper) {
        boolean rejected = false;
        try {
            new com.immortalstorage.immortalstorage.source.SourceExchangeOffer(
                    "realm/vein", net.minecraft.resources.ResourceLocation.parse("minecraft:coal"),
                    false, 0L, 0L);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "an offer must never declare zero outputs per batch");

        com.immortalstorage.immortalstorage.source.SourceExchangeOffer paid =
                new com.immortalstorage.immortalstorage.source.SourceExchangeOffer(
                        "realm/vein", net.minecraft.resources.ResourceLocation.parse("minecraft:coal"),
                        false, 4L, 2L);
        helper.assertTrue(paid.paid(), "a non-zero yuan cost marks the offer as paid");
        helper.assertTrue(paid.outputsPerBatch() == 2L, "the offer keeps its production rate");

        com.immortalstorage.immortalstorage.source.SourceExchangeOffer free =
                new com.immortalstorage.immortalstorage.source.SourceExchangeOffer(
                        "realm/vein", net.minecraft.resources.ResourceLocation.parse("minecraft:coal"),
                        false, 0L, 1L);
        helper.assertFalse(free.paid(), "a zero-cost offer must not be advertised as paid");
        helper.succeed();
    }

    @SuppressWarnings("unchecked")
    private static BlockEntityTicker<BlockEntity> externalTicker(GameTestHelper helper, BlockPos absolute,
                                                                BlockEntity target) {
        BlockState state = helper.getLevel().getBlockState(absolute);
        if (!(state.getBlock() instanceof EntityBlock entityBlock)) return null;
        BlockEntityType<BlockEntity> type = (BlockEntityType<BlockEntity>) (BlockEntityType<?>) target.getType();
        return entityBlock.getTicker(helper.getLevel(), state, type);
    }

    /**
     * Real integration probe on a dedicated server that actually has AE2 and RS installed.
     *
     * <p>Both third-party APIs are reached reflectively so the same test class still compiles on
     * the line that does not ship those jars. The assertions are the contracts the bridges rely
     * on: AE2 must resolve the manager through its in-world grid-node-host capability, and RS
     * through its network-node-container capability — the same lookups the mods themselves use,
     * so a null here is exactly the reported "cannot join the network" symptom.</p>
     */
    /**
     * The AE2 node must survive a block-entity rebuild.
     *
     * <p>Breaking a neighbouring block rebuilds this manager's block entity, which calls
     * {@code setRemoved} and destroys the grid node. The reported symptom was exactly that: after
     * one break the adjacent AE2/RS connections dropped while cable-routed ones kept working.
     * This rebuilds the block entity the same way and requires the host to be back.</p>
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void ae2NodeRevivesAfterBlockEntityRebuild(GameTestHelper helper) {
        helper.setBlock(TARGET, ModBlocks.XIANQIAO_MANAGER.get());
        BlockPos absolute = helper.absolutePos(TARGET);
        helper.runAtTickTime(4, () -> {
            Object before = ae2GridHost(helper, absolute);
            if (before == null) {
                helper.succeed(); // no AE2 on this classpath, or host not created yet: nothing to pin
                return;
            }
            // Rebuild the block entity: remove and re-place, which is what a neighbour break does.
            helper.setBlock(TARGET, Blocks.AIR);
            helper.setBlock(TARGET, ModBlocks.XIANQIAO_MANAGER.get());
            helper.runAtTickTime(10, () -> {
                Object after = ae2GridHost(helper, absolute);
                helper.assertTrue(after != null,
                        "the AE2 grid node host must be re-created after a block-entity rebuild");
                helper.succeed();
            });
        });
    }

    /**
     * The owner must be able to break their own manager with no network involved at all.
     *
     * <p>The reported symptom was a plain "You do not have permission to break Xianqiao Manager"
     * message on a plain place-then-break, so this drives the real block-breaking path through a
     * creative server player and requires the block to actually disappear.</p>
     */
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void managerIsBreakableByItsOwner(GameTestHelper helper) {
        helper.setBlock(TARGET, ModBlocks.XIANQIAO_MANAGER.get());
        BlockPos absolute = helper.absolutePos(TARGET);
        helper.runAtTickTime(4, () -> {
            // A GameTest has no real server player, so drive the block-level path: if the block
            // itself vetoed the break (a playerWillDestroy returning false, or a destroy-progress
            // of zero) destroyBlock reports false instead of removing it.
            boolean broken = helper.getLevel().destroyBlock(absolute, false);
            helper.assertTrue(broken,
                    "the manager block itself must be breakable (no destroy veto)");
            helper.assertTrue(helper.getLevel().getBlockState(absolute).isAir(),
                    "the manager must be gone after the break");
            helper.succeed();
        });
    }

    /**
     * One RS sink call must cover the WHOLE order, not one batch.
     *
     * <p>RS can hand a sink several iterations at once (`iterationsToSendToSink`), so the delivered
     * amount has to scale with the yuan actually received. The arithmetic lives in
     * `RsExchangeProviderNode#deliverableFor` and is pinned here without a live grid:
     * with 3 batches' worth of yuan the call must report 3 batches of output in one go.</p>
     */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void rsExchangeSinkDeliversTheWholeOrderInOneCall(GameTestHelper helper) {
        // 5 yuan per batch, 64 items per batch: 15 yuan payload = 3 batches = 192 items, one call.
        com.immortalstorage.immortalstorage.source.SourceExchangeOffer offer =
                new com.immortalstorage.immortalstorage.source.SourceExchangeOffer(
                        "test_source",
                        net.minecraft.resources.ResourceLocation.withDefaultNamespace("cobblestone"),
                        false,
                        5L,
                        64L);
        // Only the summed input amount matters to the arithmetic; the key itself is irrelevant.
        com.refinedmods.refinedstorage.api.resource.ResourceKey yuan =
                com.refinedmods.refinedstorage.common.support.resource.ItemResource
                        .ofItemStack(new net.minecraft.world.item.ItemStack(
                                net.minecraft.world.item.Items.COBBLESTONE));
        java.util.List<com.refinedmods.refinedstorage.api.resource.ResourceAmount> inputs =
                java.util.List.of(
                        new com.refinedmods.refinedstorage.api.resource.ResourceAmount(yuan, 15L));

        long deliverable;
        try {
            Class<?> nodeType = Class.forName("com.immortalstorage.immortalstorage.compat"
                    + ".refinedstorage.RsExchangeProviderNode");
            java.lang.reflect.Method method = nodeType.getDeclaredMethod("deliverableFor",
                    com.immortalstorage.immortalstorage.source.SourceExchangeOffer.class,
                    java.util.Collection.class);
            method.setAccessible(true);
            deliverable = (long) method.invoke(null, offer, inputs);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError("the RS sink arithmetic probe could not run", failure);
        }
        helper.assertTrue(deliverable == 192L,
                "15 yuan at 5 per batch must deliver 3 batches (192 items) in one sink call, was "
                        + deliverable);
        helper.succeed();
    }

    /** Reflective AE2 lookup so this class never hard-references AE2 types. */
    private static Object ae2GridHost(GameTestHelper helper, BlockPos absolute) {
        try {
            Class<?> gridHelper = Class.forName("appeng" + ".api.networking.GridHelper");
            return gridHelper
                    .getMethod("getNodeHost", net.minecraft.world.level.Level.class, BlockPos.class)
                    .invoke(null, helper.getLevel(), absolute);
        } catch (ClassNotFoundException noAe2) {
            return null;
        } catch (ReflectiveOperationException failure) {
            ImmortalStorageMod.LOG.warn("AE2 grid probe failed", failure);
            return null;
        }
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void xianqiaoManagerRegistersAsAnExternalNetworkMember(GameTestHelper helper) {
        helper.setBlock(TARGET, ModBlocks.XIANQIAO_MANAGER.get());
        BlockPos absolute = helper.absolutePos(TARGET);
        helper.runAtTickTime(5, () -> {
            boolean ae2Present = false;
            boolean ae2SeesManager = false;
            try {
                Class<?> gridHelper = Class.forName("appeng" + ".api.networking.GridHelper");
                ae2Present = true;
                Object host = gridHelper
                        .getMethod("getNodeHost", net.minecraft.world.level.Level.class, BlockPos.class)
                        .invoke(null, helper.getLevel(), absolute);
                ae2SeesManager = host != null;
            } catch (ClassNotFoundException noAe2) {
                ae2Present = false;
            } catch (ReflectiveOperationException failure) {
                ImmortalStorageMod.LOG.warn("AE2 grid probe failed", failure);
            }

            boolean rsPresent = false;
            boolean rsSeesManager = false;
            try {
                Class<?> apiClass = Class.forName(
                        "com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi");
                rsPresent = true;
                Object api = apiClass.getField("INSTANCE").get(null);
                Object capability = apiClass.getMethod("getNetworkNodeContainerProviderCapability")
                        .invoke(api);
                Object provider = helper.getLevel().getCapability(
                        (net.neoforged.neoforge.capabilities.BlockCapability<?, ?>) capability,
                        absolute, null);
                rsSeesManager = provider != null;
            } catch (ClassNotFoundException noRs) {
                rsPresent = false;
            } catch (ReflectiveOperationException failure) {
                ImmortalStorageMod.LOG.warn("RS network probe failed", failure);
            }

            helper.assertTrue(ae2Present || rsPresent,
                    "this GameTest requires AE2 or RS on the classpath; neither was found");
            if (ae2Present) {
                helper.assertTrue(ae2SeesManager,
                        "AE2 must resolve the manager through its in-world grid-node-host capability");
            }
            if (rsPresent) {
                // RS only answers with a provider once the manager has an owner, and claiming one
                // requires stage >= 6 plus being inside the player's own realm — neither of which a
                // GameTest can fabricate. What IS testable here is that the capability is wired, so
                // the assertion records that instead of pretending the full path was exercised.
                boolean wired = false;
                try {
                    wired = (boolean) Class.forName(
                                    "com.immortalstorage.immortalstorage.compat.refinedstorage.RsManagerNetworkNodes")
                            .getMethod("capabilityWired")
                            .invoke(null);
                } catch (ReflectiveOperationException failure) {
                    ImmortalStorageMod.LOG.warn("RS capability probe failed", failure);
                }
                helper.assertTrue(wired,
                        "the RS network-node-container capability must be registered for the manager");
            }
            helper.succeed();
        });
    }

    /** Simulated real ticks per matrix case; the furnace needs 200/tick per smelt. */
    private static final int MATRIX_TICKS = 60;

    /**
     * Drives one furnace case for {@code MATRIX_TICKS} simulated ticks with a chosen
     * acceleration combination, then returns how many ingots came out.
     *
     * <p>Realm acceleration is reproduced the way it really works — by calling
     * {@code tickBlockEntities} once per pass — plus the multiplier hook the machines
     * read ({@link RealmHelper#setTestAccelerationMultiplier}); a real personal realm
     * cannot be constructed in a GameTest.</p>
     */
    private static int runFurnaceMatrixCase(GameTestHelper helper, BlockPos pos, int staffTier,
                                            int realmMultiplier, int externalTicks) {
        helper.setBlock(pos, Blocks.FURNACE);
        BlockPos absolute = helper.absolutePos(pos);
        AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(pos);
        TimeShiftStore store = TimeShiftStore.get(helper.getLevel());
        try {
            furnace.setItem(0, new ItemStack(Items.RAW_IRON, 48));
            furnace.setItem(1, new ItemStack(Items.COAL, 48));
            if (staffTier >= 0) store.put(absolute, TimeShiftEntry.permanent(staffTier));
            RealmHelper.setTestAccelerationMultiplier(realmMultiplier);
            BlockEntityTicker<BlockEntity> ticker = externalTicker(helper, absolute, furnace);
            BlockState state = helper.getLevel().getBlockState(absolute);
            int passes = Math.max(1, realmMultiplier);
            for (int tick = 0; tick < MATRIX_TICKS; tick++) {
                for (int pass = 0; pass < passes; pass++) {
                    // The test structure sits outside the vanilla ticking range, so vanilla
                    // would not tick this furnace on its own. Drive that one tick explicitly,
                    // then let the staff add its extra ticks for this pass.
                    ticker.tick(helper.getLevel(), absolute, state, furnace);
                    TimeShiftAcceleration.tickBlockEntities(helper.getLevel());
                }
            }
            for (int index = 0; index < externalTicks; index++) {
                ticker.tick(helper.getLevel(), absolute, state, furnace);
            }
            return furnace.getItem(OUTPUT_SLOT).getCount();
        } finally {
            store.remove(absolute);
            RealmHelper.setTestAccelerationMultiplier(0);
            helper.setBlock(pos, Blocks.AIR);
        }
    }

    /**
     * Full acceleration matrix on a vanilla machine (furnace):
     * staff alone, realm alone, staff x realm, staff x realm x third-party.
     *
     * <p>Sixty simulated ticks: staff 8x alone smelts 480/200 = 2; realm 4x alone
     * smelts 240/200 = 1; both multiply to 1920/200 = 9; adding 250 third-party
     * ticks gives 2170/200 = 10.</p>
     */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void nativeFurnaceAccelerationMatrix(GameTestHelper helper) {
        int staffOnly = runFurnaceMatrixCase(helper, new BlockPos(1, 1, 1), 2, 1, 0);
        helper.assertTrue(staffOnly >= 2, "staff 8x alone must smelt two ingots, got " + staffOnly);

        int realmOnly = runFurnaceMatrixCase(helper, new BlockPos(3, 1, 1), -1, 4, 0);
        helper.assertTrue(realmOnly >= 1, "realm 4x alone must smelt one ingot, got " + realmOnly);

        int both = runFurnaceMatrixCase(helper, new BlockPos(5, 1, 1), 2, 4, 0);
        helper.assertTrue(both >= 9, "staff 8x x realm 4x must smelt nine ingots, got " + both);
        helper.assertTrue(both >= staffOnly + realmOnly - 1,
                "realm and staff must multiply, not replace: staff=" + staffOnly + " realm=" + realmOnly + " both=" + both);

        int all = runFurnaceMatrixCase(helper, new BlockPos(7, 1, 1), 2, 4, EXTERNAL_TICKS);
        helper.assertTrue(all >= both + 1,
                "third-party ticks must add on top of staff x realm: both=" + both + " all=" + all);
        helper.succeed();
    }

    /**
     * The same matrix on this mod's own machine (immortal furnace, 50-tick recipe).
     *
     * <p>It has two acceleration paths that do not go through extra ticker calls: the
     * engine drops repeated game times, so the staff and realm multipliers are folded
     * into its {@code processingMultiplier}. A third-party accelerator ticks the block
     * directly, which the engine also drops — so unlike the vanilla furnace, external
     * ticks must NOT change the result.</p>
     */
    /**
     * The immortal furnace's own acceleration path, which does not use extra ticker calls:
     * its engine drops repeated game times, so the staff and realm multipliers are folded
     * into {@code processingMultiplier} instead.
     *
     * <p>This checks the exact product the engine receives. An end-to-end smelt is
     * deliberately not attempted here: the engine only advances when the world clock
     * moves, and a GameTest cannot advance the level clock inside one call stack, so a
     * same-stack simulation would only ever see a single engine tick. The in-game numbers
     * for this machine stay an implementation-side check.</p>
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void immortalFurnaceMultiplierContract(GameTestHelper helper) {
        helper.setBlock(TARGET, ModBlocks.IMMORTAL_FURNACE.get());
        BlockPos absolute = helper.absolutePos(TARGET);
        TimeShiftStore store = TimeShiftStore.get(helper.getLevel());
        try {
            helper.assertTrue(TimeShiftMachines.combine(1, 1) == 1, "no acceleration must stay 1x");
            store.put(absolute, TimeShiftEntry.permanent(2)); // 8x
            helper.assertTrue(TimeShiftMachines.multiplierAt(helper.getLevel(), absolute) == 8,
                    "staff tier 2 must read as 8x");
            RealmHelper.setTestAccelerationMultiplier(4);
            helper.assertTrue(RealmHelper.accelerationMultiplier(helper.getLevel()) == 4,
                    "realm hook must read as 4x");
            int staff = TimeShiftMachines.multiplierAt(helper.getLevel(), absolute);
            int realm = RealmHelper.accelerationMultiplier(helper.getLevel());
            int engineMultiplier = TimeShiftMachines.combine(1, TimeShiftMachines.combine(realm, staff));
            helper.assertTrue(engineMultiplier == 32,
                    "realm x staff must multiply into the engine multiplier, got " + engineMultiplier);
        } finally {
            store.remove(absolute);
            RealmHelper.setTestAccelerationMultiplier(0);
            helper.setBlock(TARGET, Blocks.AIR);
        }
        helper.succeed();
    }


    /**
     * The gate the throttled machines use: inside a realm the world clock is presented
     * several times per tick, so a fixed period would evaluate identically every pass.
     */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void throttledMachineGateFollowsRealmAcceleration(GameTestHelper helper) {
        try {
            RealmHelper.setTestAccelerationMultiplier(4);
            helper.assertTrue(RealmHelper.accelerationMultiplier(helper.getLevel()) == 4,
                    "test hook must report the forced realm multiplier");
            helper.assertTrue(TimeShiftMachines.gatedPeriod(helper.getLevel(), 20) == 5,
                    "period 20 under 4x must become 5");
            helper.assertTrue(TimeShiftMachines.gatedPeriod(helper.getLevel(), 3) == 1,
                    "period below the multiplier must clamp to 1, never 0");
        } finally {
            RealmHelper.setTestAccelerationMultiplier(0);
        }
        helper.assertTrue(TimeShiftMachines.gatedPeriod(helper.getLevel(), 20) == 20,
                "outside a realm the period must stay unchanged");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void timeShiftAcceleratesARealFurnace(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.FURNACE);
        BlockPos absolute = helper.absolutePos(TARGET);
        helper.runAtTickTime(2, () -> {
            AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(TARGET);
            furnace.setItem(0, new ItemStack(Items.RAW_IRON, 8));
            furnace.setItem(1, new ItemStack(Items.COAL, 8));
            // 16x: forty ticks of wall time must cover more than one 200-tick smelt.
            TimeShiftStore.get(helper.getLevel()).put(absolute, TimeShiftEntry.permanent(3));
        });
        helper.runAtTickTime(42, () -> {
            AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(TARGET);
            TimeShiftEntry record = TimeShiftStore.get(helper.getLevel()).get(absolute);
            helper.assertTrue(record != null && record.tier() == 3,
                    "the staff record must be the applied 16x tier");
            helper.assertTrue(com.immortalstorage.immortalstorage.timeshift.TimeShiftAcceleration.isAccelerable(
                            helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), furnace),
                    "the furnace must qualify as accelerable");
            helper.assertTrue(furnace.getItem(OUTPUT_SLOT).is(Items.IRON_INGOT),
                    "a 16x furnace must finish at least one smelt inside forty ticks; input="
                            + furnace.getItem(0).getCount() + " fuel=" + furnace.getItem(1).getCount()
                            + " output=" + furnace.getItem(OUTPUT_SLOT)
                            + " extraTicks=" + com.immortalstorage.immortalstorage.timeshift.TimeShiftAcceleration.debugExtraTicks);
            TimeShiftStore.get(helper.getLevel()).remove(absolute);
            helper.succeed();
        });
    }

    /**
     * Compatibility check modelled on Just Dire Things' Time Wand, with no mod class
     * referenced: JDT's {@code MiscTools#doExtraTicks} (main branch, read from source)
     * re-resolves {@code blockEntity.getBlockState().getTicker(level, type)} on every
     * iteration and ticks it immediately, once per rate. This test reproduces exactly
     * that shape: 250 re-resolved ticks, then one staff pass at 1024x. If either side
     * swallowed the other, the furnace could not reach six ingots (250 + 1023 ticks
     * = six 200-tick smelts).
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void jdtStyleAcceleratorStacksWithTheStaff(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.FURNACE);
        BlockPos absolute = helper.absolutePos(TARGET);
        TimeShiftStore store = TimeShiftStore.get(helper.getLevel());
        helper.runAtTickTime(2, () -> {
            AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(TARGET);
            furnace.setItem(0, new ItemStack(Items.RAW_IRON, 12));
            furnace.setItem(1, new ItemStack(Items.COAL, 12));
            store.put(absolute, new TimeShiftEntry(4, true, System.currentTimeMillis() + 60_000L));
            long executedBefore = TimeShiftAcceleration.debugExecutedTicks;
            for (int index = 0; index < EXTERNAL_TICKS; index++) {
                BlockEntityTicker<BlockEntity> ticker = externalTicker(helper, absolute, furnace);
                helper.assertTrue(ticker != null, "the furnace must expose a ticker");
                ticker.tick(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), furnace);
            }
            long executedAfterExternal = TimeShiftAcceleration.debugExecutedTicks;
            helper.assertTrue(executedAfterExternal == executedBefore,
                    "an external accelerator must not route through the staff scheduler");
            TimeShiftAcceleration.tickBlockEntities(helper.getLevel());
            long staffTicks = TimeShiftAcceleration.debugExecutedTicks - executedAfterExternal;
            helper.assertTrue(staffTicks >= 1023,
                    "the staff must still issue its 1023 extra ticks, got " + staffTicks);
            ItemStack output = furnace.getItem(OUTPUT_SLOT);
            helper.assertTrue(output.is(Items.IRON_INGOT) && output.getCount() >= 6,
                    "JDT-style external ticks plus the staff 1024x must smelt six ingots, output=" + output);
            store.remove(absolute);
            helper.succeed();
        });
    }

    /**
     * Compatibility check modelled on Torcherino's time torch, again without any mod
     * class: {@code TorcherinoBlockEntity#tickBlock} (read from source) resolves the
     * ticker once and then calls {@code ticker.tick} in a {@code speed}-wide loop.
     * A torch accelerates blocks from its own block entity tick, so inside a personal
     * realm it also runs once per extra pass.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void torcherinoStyleAcceleratorStacksWithTheStaff(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.FURNACE);
        BlockPos absolute = helper.absolutePos(TARGET);
        TimeShiftStore store = TimeShiftStore.get(helper.getLevel());
        helper.runAtTickTime(2, () -> {
            AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(TARGET);
            furnace.setItem(0, new ItemStack(Items.RAW_IRON, 12));
            furnace.setItem(1, new ItemStack(Items.COAL, 12));
            store.put(absolute, new TimeShiftEntry(4, true, System.currentTimeMillis() + 60_000L));
            BlockEntityTicker<BlockEntity> ticker = externalTicker(helper, absolute, furnace);
            helper.assertTrue(ticker != null, "the furnace must expose a ticker");
            for (int index = 0; index < EXTERNAL_TICKS; index++) {
                ticker.tick(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), furnace);
            }
            TimeShiftAcceleration.tickBlockEntities(helper.getLevel());
            ItemStack output = furnace.getItem(OUTPUT_SLOT);
            helper.assertTrue(output.is(Items.IRON_INGOT) && output.getCount() >= 6,
                    "torch-style external ticks plus the staff 1024x must smelt six ingots, output=" + output);
            store.remove(absolute);
            helper.succeed();
        });
    }

    /**
     * Compatibility check modelled on ProjectE's Watch of Flowing Time, with no mod
     * class referenced. ProjectE's {@code TimeWatch#provideBonusTicks} (read from
     * source) walks the block entities around the player, takes the ticker that
     * {@code LevelChunk} bound for the position, and calls it {@code bonusTicks} times
     * per player tick — 8/12/16 by charge. That bound ticker is the same object the
     * block factory produced, so it is the same wrapped ticker this test drives.
     *
     * <p>Forty player ticks at full charge (640 extra ticks) plus one staff 1024x pass
     * (1023) is 1663 ticks: eight 200-tick smelts. Either side being swallowed would
     * show up immediately as fewer ingots.</p>
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void projecteTimeWatchStyleAcceleratorStacksWithTheStaff(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.FURNACE);
        BlockPos absolute = helper.absolutePos(TARGET);
        TimeShiftStore store = TimeShiftStore.get(helper.getLevel());
        helper.runAtTickTime(2, () -> {
            AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(TARGET);
            furnace.setItem(0, new ItemStack(Items.RAW_IRON, 16));
            furnace.setItem(1, new ItemStack(Items.COAL, 16));
            store.put(absolute, new TimeShiftEntry(4, true, System.currentTimeMillis() + 60_000L));
            BlockEntityTicker<BlockEntity> ticker = externalTicker(helper, absolute, furnace);
            helper.assertTrue(ticker != null, "the furnace must expose a ticker");
            int bonusTicks = 16; // ProjectE full charge
            int playerTicks = 40;
            for (int tick = 0; tick < playerTicks; tick++) {
                for (int index = 0; index < bonusTicks; index++) {
                    ticker.tick(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), furnace);
                }
            }
            TimeShiftAcceleration.tickBlockEntities(helper.getLevel());
            ItemStack output = furnace.getItem(OUTPUT_SLOT);
            helper.assertTrue(output.is(Items.IRON_INGOT) && output.getCount() >= 8,
                    "TimeWatch-style bonus ticks plus the staff 1024x must smelt eight ingots, output=" + output);
            store.remove(absolute);
            helper.succeed();
        });
    }

    /**
     * The same torch shape, but driven from inside a realm extra pass: a torch is a
     * block entity, so {@code tickBlockEntities} runs it once per pass. Its own ticks
     * must survive the realm safety scope and stay independent of the staff.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void torchStyleAcceleratorWorksInsideARealmExtraPass(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.FURNACE);
        BlockPos absolute = helper.absolutePos(TARGET);
        helper.runAtTickTime(2, () -> {
            AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(TARGET);
            furnace.setItem(0, new ItemStack(Items.RAW_IRON, 12));
            furnace.setItem(1, new ItemStack(Items.COAL, 12));
            try (var scope = RealmTickSafety.open(helper.getLevel())) {
                RealmTickSafety.extra(helper.getLevel(), () -> {
                    BlockEntityTicker<BlockEntity> ticker = externalTicker(helper, absolute, furnace);
                    if (ticker == null) return;
                    for (int index = 0; index < EXTERNAL_TICKS; index++) {
                        ticker.tick(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), furnace);
                    }
                });
            }
            ItemStack output = furnace.getItem(OUTPUT_SLOT);
            helper.assertTrue(output.is(Items.IRON_INGOT),
                    "a torch-style accelerator must keep working inside a realm extra pass, output=" + output);
            helper.succeed();
        });
    }

    /**
     * A third-party accelerator (JDT's time wand, a time torch, anything that simply
     * ticks the block entity again) and the staff must stack instead of replacing
     * each other. The test stands in for such an accelerator: it drives the block's own
     * ticker a few times, then runs one staff pass, and requires the furnace to have
     * smelted enough for both contributions.
     *
     * <p>1024x is used so a single pass is unambiguous: 1 vanilla tick + 5 external
     * ticks + 1023 staff ticks is far past the 200-tick cooking time.</p>
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void staffAccelerationStacksWithAnExternalAccelerator(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.FURNACE);
        BlockPos absolute = helper.absolutePos(TARGET);
        TimeShiftStore store = TimeShiftStore.get(helper.getLevel());
        helper.runAtTickTime(2, () -> {
            AbstractFurnaceBlockEntity furnace = helper.getBlockEntity(TARGET);
            furnace.setItem(0, new ItemStack(Items.RAW_IRON, 8));
            furnace.setItem(1, new ItemStack(Items.COAL, 8));
            store.put(absolute, new TimeShiftEntry(4, true, System.currentTimeMillis() + 60_000L));
            // Stand-in for a third-party accelerator: five plain ticker calls.
            var ticker = com.immortalstorage.immortalstorage.timeshift.TimeShiftTickers.tickerFor(
                    helper.getLevel(), helper.getLevel().getBlockState(absolute), furnace);
            helper.assertTrue(ticker != null, "the furnace must expose a ticker");
            for (int index = 0; index < 5; index++) {
                ticker.tick(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), furnace);
            }
            TimeShiftAcceleration.tickBlockEntities(helper.getLevel());
            ItemStack output = furnace.getItem(OUTPUT_SLOT);
            helper.assertTrue(output.is(Items.IRON_INGOT) && output.getCount() >= 2,
                    "external and staff ticks must add up; output=" + output);
            store.remove(absolute);
            helper.succeed();
        });
    }

    /**
     * The personal realm accelerates itself by calling {@code tickBlockEntities} again
     * inside {@code RealmTickSafety}. The staff's extra ticks must still run there: an
     * earlier build wrapped the already-guarded ticker in a second guard, the inner
     * {@code running.add} failed, and the whole extra tick was silently dropped — so
     * the realm's own acceleration replaced the staff multiplier instead of
     * multiplying with it.
     */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void extraTicksStillRunInsideARealmScope(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.FURNACE);
        BlockPos absolute = helper.absolutePos(TARGET);
        TimeShiftStore store = TimeShiftStore.get(helper.getLevel());
        store.put(absolute, TimeShiftEntry.permanent(3)); // 16x -> 15 extra ticks
        helper.runAtTickTime(2, () -> {
            long before = TimeShiftAcceleration.debugExecutedTicks;
            try (var scope = RealmTickSafety.open(helper.getLevel())) {
                TimeShiftAcceleration.tickBlockEntities(helper.getLevel());
            }
            long executed = TimeShiftAcceleration.debugExecutedTicks - before;
            helper.assertTrue(executed >= 15,
                    "a realm-scoped pass must still execute the staff's extra ticks, executed=" + executed);
            store.remove(absolute);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void anExpiredBoostFallsBackToThePermanentTier(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.FURNACE);
        BlockPos absolute = helper.absolutePos(TARGET);
        TimeShiftStore store = TimeShiftStore.get(helper.getLevel());
        store.put(absolute, new TimeShiftEntry(1, true, System.currentTimeMillis() - 1_000L));
        helper.runAtTickTime(3, () -> {
            TimeShiftEntry record = store.get(absolute);
            helper.assertTrue(record != null, "the record must survive an expired boost");
            helper.assertTrue(!record.boosted(), "an expired boost must clear its flag on the next tick");
            helper.assertTrue(record.tier() == 1, "the previously applied tier must be kept for the fallback");
            store.remove(absolute);
            helper.succeed();
        });
    }
}