package com.immortalstorage.immortalstorage;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression boundary for the long-FE crystal and the source-only yuan charge path. */
final class EnergyCrystalContractTest {
    private static final Path PROJECT = locateProject();
    private static final Path MAIN = PROJECT.resolve(Path.of("src", "main", "java",
            "com", "immortalstorage", "immortalstorage"));
    private static final Path RESOURCES = PROJECT.resolve(Path.of("src", "main", "resources"));

    @Test
    void crystalUsesTheSimulatedMachineLayoutWithAStandaloneLongFeTank() throws IOException {
        String block = source("block", "custom", "EnergyCrystalBlock.java");
        String entity = source("block", "entity", "EnergyCrystalBlockEntity.java");
        String menu = source("menu", "custom", "EnergyCrystalMenu.java");
        String screen = source("client", "screen", "EnergyCrystalScreen.java");
        String registrations = source("block", "entity", "ModBlockEntities.java");
        String blocks = source("block", "ModBlocks.java");
        String client = source("client", "ClientSetup.java");
        String recipe = Files.readString(RESOURCES.resolve(Path.of("data", "immortalstorage", "recipe",
                "energy_crystal.json")));

        assertTrue(block.contains("public static final BooleanProperty LIT"));
        assertTrue(blocks.contains("state.getValue(EnergyCrystalBlock.LIT) ? 15 : 0"));
        assertTrue(entity.contains("private long energy"));
        assertTrue(entity.contains("ENERGY_CRYSTAL_FE_CAPACITY"));
        assertTrue(entity.contains("ExternalResourceChannels.FE"));
        assertTrue(entity.contains("getSlotsForFace"));
        assertTrue(entity.contains("Direction.DOWN"));
        assertTrue(entity.contains("TOP_PROCESSING_INPUT = {INPUT_SLOT}"),
                "the top face must expose only the processing slot");
        assertTrue(entity.contains("SIDE_FUEL_INPUT = {FUEL_SLOT}"),
                "the four side faces must expose only the fuel slot");
        assertTrue(entity.contains("return side == Direction.UP ? TOP_PROCESSING_INPUT : SIDE_FUEL_INPUT"),
                "face slots must route top processing and side fuel independently");
        assertTrue(entity.contains("return slot == INPUT_SLOT && canPlaceItem(slot, stack)"),
                "the top face must accept only processing input");
        assertTrue(entity.contains("return slot == FUEL_SLOT && canPlaceItem(slot, stack)"),
                "side faces must accept only fuel input");
        assertTrue(entity.contains("EnergyCrystalItemAccess.energy(input)"));
        assertTrue(entity.contains("input.shrink(1)"),
                "a full input stack must be consumed one completed item at a time");
        assertTrue(entity.contains("private ItemStack rechargeableOutputPreview(ItemStack input)"),
                "rechargeable stack processing must preserve a completed-output preview");
        assertFalse(entity.contains("slot == INPUT_SLOT ? 1"),
                "the input container must no longer be limited to one item");
        assertTrue(entity.contains("broadcastOpenMenu(serverLevel)"));
        assertTrue(entity.contains("menu.blockPos().equals(worldPosition)"));
        assertTrue(entity.contains("menu.broadcastChanges()"),
                "direct processing writes must become visible in the open output slot");
        assertFalse(entity.contains("if (slot == EXTRA_SLOT && !stack.isEmpty()) return;"),
                "client container sync must be allowed to populate the read-only output slot");
        assertTrue(entity.contains("if (slot == EXTRA_SLOT) return ReinforcementPluginHost.isPlugin(stack)"),
                "the output slot must accept only the reinforcement plugin through the container contract");
        assertTrue(menu.contains("ReinforcementPluginHost.isPlugin(stack)"),
                "the output slot must expose the reinforcement plugin exception through the menu");
        assertTrue(registrations.contains("Capabilities.EnergyStorage.BLOCK"));
        assertTrue(menu.contains("new InputSlot(container, EnergyCrystalBlockEntity.INPUT_SLOT, 26, 26)"));
        assertTrue(menu.contains("getMaxStackSize(ItemStack stack)"),
                "the processing slot must accept the source item's complete stack");
        String inputSlot = menu.substring(menu.indexOf("class InputSlot"), menu.indexOf("class FuelSlot"));
        assertFalse(inputSlot.contains("getMaxStackSize() { return 1; }"),
                "the processing slot must not force a single-item input");
        assertTrue(menu.contains("new FuelSlot(container, EnergyCrystalBlockEntity.FUEL_SLOT, 26, 62)"));
        assertTrue(menu.contains("new ExtraSlot(container, EnergyCrystalBlockEntity.EXTRA_SLOT, 59, 44)"));
        assertTrue(screen.contains("imageWidth = 230"));
        assertTrue(screen.contains("TANK_WIDTH = 72"));
        assertTrue(screen.contains("TANK_HEIGHT = 54"));
        assertTrue(screen.contains("stored"));
        assertTrue(screen.contains("capacity"));
        assertTrue(client.contains("ModMenus.ENERGY_CRYSTAL.get(), EnergyCrystalScreen::new"));
        assertTrue(recipe.contains("\"pattern\""));
        assertTrue(recipe.contains("\"CN\""));
        assertTrue(recipe.contains("\"NY\""));
        assertTrue(recipe.contains("nurturing_crystal"));
        assertTrue(recipe.contains("spirit_iron_nugget"));
        assertTrue(recipe.contains("immortal_yuan"));
        String crystalBlock = source("block", "custom", "EnergyCrystalBlock.java");
        assertTrue(crystalBlock.contains("MODEL_BOUNDS = box(5, 0, 5, 11, 12, 11)"),
                "the crystal collision box must follow the model bounds");
        assertTrue(crystalBlock.contains("getCollisionShape"));
    }

    @Test
    void onlyNonSourceExternalConversionIsDisabledWhileSourceVeinsKeepTheirYuanChargePlan() throws IOException {
        String config = source("config", "ImmortalStorageConfig.java");
        String xianqiao = source("block", "entity", "XianqiaoInterfaceBlockEntity.java");
        String sourceVein = source("block", "entity", "SourceVeinBlockEntity.java");

        assertTrue(config.contains("return ConversionPolicy.DISABLED"),
                "legacy external resource conversion must be disabled");
        assertFalse(xianqiao.contains("ConvertingCacheStore"),
                "the Xianqiao fallback conversion machinery must stay removed");
        assertTrue(sourceVein.contains("SourceChargeRegistry.IMMORTAL_YUAN"),
                "source blocks must retain their own yuan charge channel");
        assertTrue(sourceVein.contains("SourceChargeRegistry.reserve"),
                "source blocks must still reserve yuan when producing output");
    }

    @Test
    void xianqiaoAndSixSidedAutomaticOutputAreIndependentForAllThreeMachines() throws IOException {
        String crystal = source("block", "entity", "EnergyCrystalBlockEntity.java");
        String crystalMenu = source("menu", "custom", "EnergyCrystalMenu.java");
        String field = source("block", "entity", "SimulatedSpiritFieldBlockEntity.java");
        String fieldMenu = source("menu", "custom", "SimulatedSpiritFieldMenu.java");
        String furnace = source("block", "entity", "SimulatedReincarnationFurnaceBlockEntity.java");
        String furnaceMenu = source("menu", "custom", "SimulatedReincarnationFurnaceMenu.java");
        String registrations = source("block", "entity", "ModBlockEntities.java");
        String crystalScreen = source("client", "screen", "EnergyCrystalScreen.java");
        String fieldScreen = source("client", "screen", "SimulatedSpiritFieldScreen.java");
        String furnaceScreen = source("client", "screen", "SimulatedReincarnationFurnaceScreen.java");

        assertTrue(crystal.contains("private boolean xianqiaoOutputEnabled"));
        assertFalse(crystal.contains("private boolean xianqiaoOutputEnabled = true"),
                "Xianqiao output must start disabled until a bound FE endpoint is verified");
        assertTrue(crystal.contains("private boolean automaticOutputEnabled = true"));
        assertTrue(crystal.contains("if (automaticOutputEnabled)"),
                "crystal FE pushes must have a six-sided automatic-output gate");
        assertTrue(crystal.contains("AtomicEnergyRefill.ResourceStore bound = xianqiaoOutputEnabled"),
                "crystal owner-Xianqiao output must select the bound cache explicitly");
        assertTrue(crystal.contains("automaticOutputEnabled && side == Direction.DOWN"),
                "crystal completed items must be extractable only through the bottom face");
        assertTrue(crystal.contains("EXTRA_SLOT, EXTRA_SLOT + 1, Direction.DOWN"),
                "crystal extra-slot automation must be restricted to the bottom face");
        assertFalse(crystal.contains("MachineOutputScheduler.flushItemsToXianqiao"),
                "completed items must never be sent to Xianqiao");
        assertTrue(crystal.contains("AutomaticFaceOutput"));
        assertTrue(crystalMenu.contains("crystal.toggleXianqiaoOutput()"));
        assertTrue(crystalMenu.contains("crystal.toggleAutomaticOutput()"));
        assertTrue(crystalScreen.contains("send(0)"));
        assertTrue(crystalScreen.contains("send(1)"));
        assertTrue(crystalScreen.contains("menu.uiKey(\"automatic_on\")"));

        assertTrue(field.contains("private boolean xianqiaoOutput = true"));
        assertTrue(field.contains("private boolean automaticOutput = true"));
        assertTrue(field.contains("UUID outputOwner = field.effectiveXianqiaoOwner(level)"));
        assertTrue(field.contains("return automaticOutput && outputFace(side)"));
        assertTrue(field.contains("AutomaticFaceOutput"));
        assertTrue(fieldMenu.contains("field.toggleXianqiaoOutput()"));
        assertTrue(fieldMenu.contains("field.toggleAutomaticOutput()"));
        assertTrue(fieldScreen.contains("reincarnation.automatic_on"));

        assertTrue(furnace.contains("private boolean xianqiaoOutput = true"));
        assertTrue(furnace.contains("UUID outputOwner = furnace.effectiveXianqiaoOwner(level)"));
        assertTrue(furnace.contains("AutomaticFaceOutput"));
        assertTrue(furnaceMenu.contains("toggleXianqiaoOutput()"));
        assertTrue(furnaceMenu.contains("toggleAutomaticOutput()"));
        assertTrue(furnaceScreen.contains("reincarnation.automatic_on"));
        assertTrue(registrations.contains("!be.automaticOutput() || !be.outputFace(side)"),
                "furnace's six-sided item capability must obey the global automatic switch");
    }

    @Test
    void enablingXianqiaoFlushesResourceCacheOnlyAndBottomItemOutputRunsBeforeFallback() throws IOException {
        String crystal = source("block", "entity", "EnergyCrystalBlockEntity.java");
        String field = source("block", "entity", "SimulatedSpiritFieldBlockEntity.java");
        String furnace = source("block", "entity", "SimulatedReincarnationFurnaceBlockEntity.java");
        String xianqiao = source("block", "entity", "XianqiaoInterfaceBlockEntity.java");

        assertTrue(crystal.contains("flushOutputCacheToXianqiao"),
                "the crystal must flush its resource cache when Xianqiao output is enabled");
        assertFalse(crystal.contains("MachineOutputScheduler.flushItemsToXianqiao"),
                "the crystal's completed output must remain outside Xianqiao");
        assertTrue(crystal.contains("ready = energy == 0L"),
                "Xianqiao binding must not depend on the extra slot being empty");
        assertTrue(crystal.contains("MachineOutputScheduler.pushItemsToFaces"),
                "the crystal must try every allowed face before its Xianqiao fallback");
        assertTrue(field.contains("flushOutputCacheToXianqiao"));
        assertTrue(field.contains("MachineOutputScheduler.pushItemsToFaces"));
        assertTrue(furnace.contains("flushOutputCacheToXianqiao"));
        assertTrue(furnace.contains("MachineOutputScheduler.pushItemsToFaces"));
        assertTrue(xianqiao.contains("XianqiaoInterfaceEnergyTransfer.pushAll"),
                "the interface FE push must drain the available cache in the same tick");
        assertTrue(xianqiao.indexOf("pushToSide(serverLevel, side)")
                        < xianqiao.indexOf("XianqiaoInterfaceCompatHooks.serverTick"),
                "all configured face pushes must precede optional integration hooks");
    }

    @Test
    void allXianqiaoMachinesUseRealmPriorityWithDriveFallback() throws IOException {
        String policy = source("block", "entity", "XianqiaoBindingPolicy.java");
        String crystalBlock = source("block", "custom", "EnergyCrystalBlock.java");
        String crystal = source("block", "entity", "EnergyCrystalBlockEntity.java");
        String field = source("block", "entity", "SimulatedSpiritFieldBlockEntity.java");
        String furnace = source("block", "entity", "SimulatedReincarnationFurnaceBlockEntity.java");
        String furnaceMenu = source("menu", "custom", "SimulatedReincarnationFurnaceMenu.java");

        assertTrue(policy.contains("enum BindingSource"));
        assertTrue(policy.contains("PERSONAL_REALM"));
        assertTrue(policy.contains("SPIRIT_DRIVE"));
        assertTrue(policy.contains("resolveEnergyCrystal"));
        assertTrue(policy.contains("if (realmOwner != null)"),
                "the realm binding must win before the Spirit Drive fallback");
        assertTrue(policy.contains("personalRealmOwner"));
        assertTrue(policy.contains("instanceof SpiritDriveItem"));
        assertTrue(policy.contains("PersistentPlayerIdentity.onlinePlayer"));
        assertTrue(policy.contains("public static Binding resolve(ServerLevel level, ItemStack fuel)"));
        assertTrue(policy.contains("return resolve(level, fuel)"));
        assertFalse(policy.contains("SpiritDriveItem.isBoundTo(fuel, realmOwner)"));
        assertFalse(crystalBlock.contains("crystal.setOwner("),
                "placement actor must not bind a crystal");
        assertTrue(crystal.contains("refreshBinding(level)"));
        assertTrue(crystal.contains("XianqiaoBindingPolicy.resolveEnergyCrystal"));
        assertTrue(crystal.contains("PersonalStorageApi.resolveInOwnerRealm"),
                "a crystal in the realm must resolve the owner-scoped endpoint in that realm");
        assertTrue(crystal.contains("BindingSource.PERSONAL_REALM"));
        assertTrue(crystal.contains("!xianqiaoOutputEnabled"),
                "closing Xianqiao output must stop realm-owned automatic Immortal Yuan refill");
        assertTrue(crystal.contains("boolean spiritDriveFuel = fuel.getItem() instanceof SpiritDriveItem"),
                "a Spirit Drive in the fuel slot must be an explicit refill path");
        assertTrue(crystal.contains("!spiritDriveFuel"),
                "the Xianqiao switch must gate only empty-fuel-slot automatic refill");
        assertTrue(crystal.contains("BindingSource.SPIRIT_DRIVE"),
                "Spirit Drive refill must remain an independent path");
        assertTrue(crystal.contains("flushBoundCache()"));
        assertTrue(crystal.contains("rechargeFromXianqiao"));
        assertTrue(crystal.contains("PersonalStorageApi.resolveXianqiao"));
        assertTrue(crystal.contains("endpoint.externalResourceStorage()"));
        assertTrue(crystal.contains("XIANQIAO_EXTERNAL_UNLOCK_STAGE"));
        assertTrue(crystal.contains("ExternalResourceChannels.FE"));
        assertTrue(crystal.contains("ResourceTransferAction.SIMULATE"));
        assertTrue(field.contains("XianqiaoBindingPolicy.resolve(level, items.get(FUEL_SLOT))"));
        assertTrue(furnace.contains("XianqiaoBindingPolicy.resolve(level, items.getStackInSlot(FUEL_SLOT))"));
        assertFalse(furnace.contains("private UUID owner"));
        assertFalse(furnace.contains("setOwner("));
        assertTrue(furnaceMenu.contains("blockEntity.dataAccess()"));
        assertTrue(crystal.contains("case 7 -> xianqiaoOutputEnabled = value != 0"));
        assertTrue(crystal.contains("case 8 -> automaticOutputEnabled = value != 0"));
        assertTrue(crystal.contains("targetOwner == null || ownerExternalEnergy(serverLevel, targetOwner) == null"),
                "the Xianqiao switch must reject an unbound or unavailable endpoint");
        assertTrue(crystal.contains("flushOutputCacheToXianqiao(serverLevel, targetOwner)"),
                "enabling Xianqiao output must migrate every local cache first");
        assertTrue(crystal.contains("private @Nullable AtomicEnergyRefill.ResourceStore boundExternalEnergy()"),
                "the FE capability must switch to the bound external container");
        assertTrue(crystal.contains("private long displayedEnergy()"),
                "the UI ledger must read the bound Xianqiao FE amount");
        assertTrue(crystal.contains("productionStore.insert(")
                        && crystal.contains("configuredOutput(crystal.kind)"),
                "all crystal variants must use the shared configured production rate");
        assertTrue(crystal.contains("return Math.max(1L, com.immortalstorage.immortalstorage.config.ImmortalStorageConfig\n                .ENERGY_CRYSTAL_FE_CAPACITY.get())"),
                "all crystal variants must use the shared configured 800M capacity");
        assertTrue(crystal.contains("if (xianqiaoOutputEnabled) flushOutputCacheToXianqiao(serverLevel, owner)"),
                "block removal must flush only while the external binding is active");
    }

    @Test
    void highSpeedMachinesCoalesceOnlyNetworkSyncAndNeverStopAFullTransferOnPartialAcceptance()
            throws IOException {
        String scheduler = source("block", "entity", "MachineOutputScheduler.java");
        String crystal = source("block", "entity", "EnergyCrystalBlockEntity.java");
        String field = source("block", "entity", "SimulatedSpiritFieldBlockEntity.java");
        String furnace = source("block", "entity", "SimulatedReincarnationFurnaceBlockEntity.java");
        String sourceVein = source("block", "entity", "SourceVeinBlockEntity.java");

        assertFalse(scheduler.contains("if (committed < accepted) break"),
                "a partial execution must not be an artificial per-tick FE/item rate limit");
        assertTrue(scheduler.contains("hasItems(source"),
                "empty output caches must not trigger repeated neighbour capability scans");
        assertFalse(crystal.contains("if (accepted < offered) break"),
                "the crystal must continue draining a target that accepts in chunks");
        assertTrue(crystal.contains("UUID outputOwner = crystal.effectiveOutputOwner(level)"),
                "the crystal should resolve its binding once per logical tick");
        assertTrue(crystal.contains("MachineTickSync"));
        assertTrue(field.contains("MachineTickSync"));
        assertTrue(furnace.contains("MachineTickSync"));
        assertTrue(sourceVein.contains("lastCacheIndexChangeTick"),
                "one accelerated tick may touch several faces but should invalidate the source index once");
        assertTrue(sourceVein.contains("cachedDefinitionGeneration"),
                "definition and charge-plan lookup must be memoized between reloads");
        assertTrue(sourceVein.contains("cachedOutputItem"),
                "source output registry lookups must be memoized between definition reloads");
        assertTrue(field.contains("pendingHarvestDrops"),
                "a blocked harvest must retain its generated drops instead of rebuilding loot every tick");
        assertTrue(crystal.contains("XianqiaoInterfaceEnergyTransfer.pushAll"),
                "crystal charging and FE face interaction must use the Xianqiao transaction bridge");
        String charging = crystal.substring(crystal.indexOf("private boolean processInput"),
                crystal.indexOf("private ItemStack rechargeableOutputPreview"));
        assertFalse(charging.contains("configuredOutput()"),
                "charging must consume the complete available cache, not the production-rate setting");
        assertTrue(sourceVein.contains("internalOutputPass"),
                "source PUSH output must recognize repeated external accelerator ticker calls");
        assertTrue(sourceVein.contains("Arrays.fill(internalFaceOutputSpent, 0L)"),
                "each source ticker invocation must receive a fresh accelerated face allowance");
    }

    @Test
    void synchronizationUsesBothWorldTimeAndTickerInvocationClocks() throws IOException {
        String sync = source("block", "entity", "MachineTickSync.java");
        String crystal = source("block", "entity", "EnergyCrystalBlockEntity.java");
        String field = source("block", "entity", "SimulatedSpiritFieldBlockEntity.java");
        String furnace = source("block", "entity", "SimulatedReincarnationFurnaceBlockEntity.java");

        assertTrue(sync.contains("lastSyncInvocation"));
        assertTrue(sync.contains("invocations >= FAST_SYNC_INTERVAL_TICKS"));
        for (String machine : new String[]{crystal, field, furnace}) {
            assertTrue(machine.contains("syncInvocationCount"));
            assertTrue(machine.contains("MachineTickSync.due(serverLevel, lastSyncTick, invocation, lastSyncInvocation)"));
        }
    }

    @Test
    void theCrystalCapabilityCarriesOneCanonicalOwnerAwareFeIdentityForPipeAndAeRsDeduplication()
            throws IOException {
        String api = source("api", "storage", "CanonicalEnergyStorage.java");
        String crystal = source("block", "entity", "EnergyCrystalBlockEntity.java");

        assertTrue(api.contains("ResourceChannelKey canonicalChannel()"));
        assertTrue(api.contains("UUID canonicalOwner()"));
        assertTrue(crystal.contains("CanonicalEnergyStorage"));
        assertTrue(crystal.contains("ExternalResourceChannels.FE"));
        assertTrue(crystal.contains("canonicalOwner()"));
        assertTrue(crystal.contains("return xianqiaoOutputEnabled ? owner : null"),
                "the bound cache and the physical capability must expose one owner-aware identity");
    }

    @Test
    void outputSwitchPanelLeavesTheFaceGridClear() throws IOException {
        String crystal = source("client", "screen", "EnergyCrystalScreen.java");
        String field = source("client", "screen", "SimulatedSpiritFieldScreen.java");
        String furnace = source("client", "screen", "SimulatedReincarnationFurnaceScreen.java");
        for (String screen : new String[]{crystal, field, furnace}) {
            assertTrue(screen.contains("topPos + 108"), "switch heading must be below face buttons");
            assertTrue(screen.contains("topPos + 120"), "first switch must be below heading");
            assertTrue(screen.contains("topPos + 143"), "second switch must be below first switch");
        }
    }

    private static String source(String... relative) throws IOException {
        Path path = MAIN;
        for (String segment : relative) path = path.resolve(segment);
        return Files.readString(path);
    }

    private static Path locateProject() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve(Path.of("src", "main", "resources")))
                    && Files.isDirectory(current.resolve(Path.of("src", "main", "java")))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("cannot locate ImmortalStorage project");
    }
}
