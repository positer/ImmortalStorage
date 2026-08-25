package com.immortalstorage.immortalstorage.network;

import com.immortalstorage.immortalstorage.block.entity.SourceVeinBlockEntity;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceBlockEntity;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceLimits;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceInventory;
import com.immortalstorage.immortalstorage.api.storage.terminal.TerminalRecipeAvailability;
import com.immortalstorage.immortalstorage.menu.custom.KongqiaoMenu;
import com.immortalstorage.immortalstorage.menu.custom.SourceVeinMenu;
import com.immortalstorage.immortalstorage.menu.custom.SourceFluxValue;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoStorageMenu;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoInterfaceMenu;
import com.immortalstorage.immortalstorage.menu.provider.KongqiaoProvider;
import com.immortalstorage.immortalstorage.menu.provider.XianqiaoStorageProvider;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public final class ModNetwork {
    public static final String PROTOCOL = "8";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL).optional();
        registrar.playToServer(ModPayloads.OpenKongqiao.TYPE, ModPayloads.OpenKongqiao.STREAM_CODEC, ModNetwork::handleOpenKongqiao);
        registrar.playToServer(ModPayloads.OpenXianqiaoStorage.TYPE, ModPayloads.OpenXianqiaoStorage.STREAM_CODEC, ModNetwork::handleOpenXianqiao);
        registrar.playToServer(ModPayloads.TriggerTribulation.TYPE, ModPayloads.TriggerTribulation.STREAM_CODEC, ModNetwork::handleTriggerTribulation);
        registrar.playToServer(ModPayloads.TimeFlow.TYPE, ModPayloads.TimeFlow.STREAM_CODEC, ModNetwork::handleTimeFlow);
        registrar.playToServer(ModPayloads.RealmEnvironment.TYPE, ModPayloads.RealmEnvironment.STREAM_CODEC,
                ModNetwork::handleRealmEnvironment);
        registrar.playToServer(ModPayloads.SetStorageModule.TYPE, ModPayloads.SetStorageModule.STREAM_CODEC, ModNetwork::handleSetStorageModule);
        registrar.playToServer(ModPayloads.SetTerminalViewport.TYPE, ModPayloads.SetTerminalViewport.STREAM_CODEC, ModNetwork::handleSetTerminalViewport);
        registrar.playToServer(ModPayloads.SetTerminalQuery.TYPE, ModPayloads.SetTerminalQuery.STREAM_CODEC, ModNetwork::handleSetTerminalQuery);
        registrar.playToServer(ModPayloads.TerminalEntryAction.TYPE, ModPayloads.TerminalEntryAction.STREAM_CODEC, ModNetwork::handleTerminalEntryAction);
        registrar.playToServer(ModPayloads.SetTerminalChannel.TYPE, ModPayloads.SetTerminalChannel.STREAM_CODEC, ModNetwork::handleSetTerminalChannel);
        registrar.playToServer(ModPayloads.TerminalFluidEntryAction.TYPE, ModPayloads.TerminalFluidEntryAction.STREAM_CODEC, ModNetwork::handleTerminalFluidEntryAction);
        registrar.playToServer(ModPayloads.TerminalExternalResourceEntryAction.TYPE,
                ModPayloads.TerminalExternalResourceEntryAction.STREAM_CODEC,
                ModNetwork::handleTerminalExternalResourceEntryAction);
        registrar.playToServer(ModPayloads.TransferTerminalRecipe.TYPE, ModPayloads.TransferTerminalRecipe.STREAM_CODEC, ModNetwork::handleTransferTerminalRecipe);
        registrar.playToServer(ModPayloads.ToggleRealm.TYPE, ModPayloads.ToggleRealm.STREAM_CODEC, ModNetwork::handleToggleRealm);
        registrar.playToServer(ModPayloads.RealmCenterTeleport.TYPE, ModPayloads.RealmCenterTeleport.STREAM_CODEC, ModNetwork::handleRealmCenterTeleport);
        registrar.playToServer(ModPayloads.DomainToggle.TYPE, ModPayloads.DomainToggle.STREAM_CODEC, ModNetwork::handleDomainToggle);
        registrar.playToServer(ModPayloads.CycleStaffMode.TYPE, ModPayloads.CycleStaffMode.STREAM_CODEC, ModNetwork::handleCycleStaffMode);
        registrar.playToServer(ModPayloads.AdjustStaffTeleportDistance.TYPE,
                ModPayloads.AdjustStaffTeleportDistance.STREAM_CODEC, ModNetwork::handleAdjustStaffTeleportDistance);
        registrar.playToServer(ModPayloads.AuraGuardLeap.TYPE,
                ModPayloads.AuraGuardLeap.STREAM_CODEC, ModNetwork::handleAuraGuardLeap);
        registrar.playToServer(ModPayloads.AuraGuardFlightState.TYPE,
                ModPayloads.AuraGuardFlightState.STREAM_CODEC, ModNetwork::handleAuraGuardFlightState);
        registrar.playToServer(ModPayloads.AuraGuardBoost.TYPE,
                ModPayloads.AuraGuardBoost.STREAM_CODEC, ModNetwork::handleAuraGuardBoost);
        registrar.playToServer(ModPayloads.RequestSpiritStaffBuildPreview.TYPE,
                ModPayloads.RequestSpiritStaffBuildPreview.STREAM_CODEC,
                ModNetwork::handleSpiritStaffBuildPreview);
        registrar.playToServer(ModPayloads.RemoveSpiritStaffBuildLayer.TYPE,
                ModPayloads.RemoveSpiritStaffBuildLayer.STREAM_CODEC,
                ModNetwork::handleSpiritStaffBuildRemoval);
        registrar.playToServer(ModPayloads.RestoreImmortalArtifactBuildLayer.TYPE,
                ModPayloads.RestoreImmortalArtifactBuildLayer.STREAM_CODEC,
                ModNetwork::handleImmortalArtifactBuildRestore);
        registrar.playToServer(ModPayloads.SpiritSwordFurnaceOperation.TYPE,
                ModPayloads.SpiritSwordFurnaceOperation.STREAM_CODEC,
                ModNetwork::handleSpiritSwordFurnaceOperation);
        registrar.playToServer(ModPayloads.SetSourceSideMode.TYPE, ModPayloads.SetSourceSideMode.STREAM_CODEC, ModNetwork::handleSetSourceSideMode);
        registrar.playToServer(ModPayloads.AdjustSourceFlux.TYPE, ModPayloads.AdjustSourceFlux.STREAM_CODEC, ModNetwork::handleAdjustSourceFlux);
        registrar.playToServer(ModPayloads.SetSourceFluxLimit.TYPE, ModPayloads.SetSourceFluxLimit.STREAM_CODEC, ModNetwork::handleSetSourceFluxLimit);
        registrar.playToServer(ModPayloads.SetXianqiaoInterfaceSideMode.TYPE, ModPayloads.SetXianqiaoInterfaceSideMode.STREAM_CODEC, ModNetwork::handleSetXianqiaoInterfaceSideMode);
        registrar.playToServer(ModPayloads.SetXianqiaoInterfaceTargetAmount.TYPE, ModPayloads.SetXianqiaoInterfaceTargetAmount.STREAM_CODEC, ModNetwork::handleSetXianqiaoInterfaceTargetAmount);
        registrar.playToServer(ModPayloads.SetXianqiaoInterfaceSlotFaceMask.TYPE, ModPayloads.SetXianqiaoInterfaceSlotFaceMask.STREAM_CODEC, ModNetwork::handleSetXianqiaoInterfaceSlotFaceMask);
        registrar.playToServer(ModPayloads.SetXianqiaoInterfaceActiveTransfer.TYPE, ModPayloads.SetXianqiaoInterfaceActiveTransfer.STREAM_CODEC, ModNetwork::handleSetXianqiaoInterfaceActiveTransfer);
        registrar.playToServer(ModPayloads.SetXianqiaoInterfaceItemTarget.TYPE, ModPayloads.SetXianqiaoInterfaceItemTarget.STREAM_CODEC, ModNetwork::handleSetXianqiaoInterfaceItemTarget);
        registrar.playToServer(ModPayloads.SetXianqiaoInterfaceFluidTarget.TYPE, ModPayloads.SetXianqiaoInterfaceFluidTarget.STREAM_CODEC, ModNetwork::handleSetXianqiaoInterfaceFluidTarget);
        registrar.playToServer(ModPayloads.SetXianqiaoInterfaceExternalTarget.TYPE, ModPayloads.SetXianqiaoInterfaceExternalTarget.STREAM_CODEC, ModNetwork::handleSetXianqiaoInterfaceExternalTarget);
        registrar.playToServer(ModPayloads.SetStabilizedRuinValue.TYPE,
                ModPayloads.SetStabilizedRuinValue.STREAM_CODEC, ModNetwork::handleSetStabilizedRuinValue);
        registrar.playToServer(ModPayloads.SetStabilizedRuinFilter.TYPE,
                ModPayloads.SetStabilizedRuinFilter.STREAM_CODEC, ModNetwork::handleSetStabilizedRuinFilter);
        registrar.playToServer(ModPayloads.ToggleStabilizedRuinFilterMode.TYPE,
                ModPayloads.ToggleStabilizedRuinFilterMode.STREAM_CODEC, ModNetwork::handleToggleStabilizedRuinFilterMode);
        registrar.playToServer(ModPayloads.SetEntangledRuinFilter.TYPE,
                ModPayloads.SetEntangledRuinFilter.STREAM_CODEC, ModNetwork::handleSetEntangledRuinFilter);
        registrar.playToServer(ModPayloads.ToggleEntangledRuinFilterMode.TYPE,
                ModPayloads.ToggleEntangledRuinFilterMode.STREAM_CODEC, ModNetwork::handleToggleEntangledRuinFilterMode);
        registrar.playToServer(ModPayloads.SetEntangledRuinValue.TYPE,
                ModPayloads.SetEntangledRuinValue.STREAM_CODEC, ModNetwork::handleSetEntangledRuinValue);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            com.immortalstorage.immortalstorage.client.ClientNetworkHandlers.register(registrar);
        }
    }

    private static ServerPlayer serverPlayer(IPayloadContext ctx) {
        Player player = ctx.player();
        return player instanceof ServerPlayer sp ? sp : null;
    }

    private static void handleSetStabilizedRuinValue(ModPayloads.SetStabilizedRuinValue payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || player.containerMenu.containerId != payload.containerId()) return;
            if (player.containerMenu instanceof com.immortalstorage.immortalstorage.menu.custom.StabilizedMiniatureImmortalRuinMenu menu) {
                menu.setAuthoritativeValue(payload.index(), payload.value());
            } else if (player.containerMenu instanceof com.immortalstorage.immortalstorage.menu.custom.AdvancedXianqiaoInterfaceMenu menu) {
                menu.setAuthoritativeValue(payload.index(), payload.value());
            }
        });
    }

    private static void handleSetStabilizedRuinFilter(ModPayloads.SetStabilizedRuinFilter payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player != null && player.containerMenu.containerId == payload.containerId()
                    && player.containerMenu instanceof com.immortalstorage.immortalstorage.menu.custom.StabilizedMiniatureImmortalRuinMenu menu
                    && menu.stillValid(player)) menu.setFilter(payload.slot(), payload.stack());
        });
    }

    private static void handleToggleStabilizedRuinFilterMode(ModPayloads.ToggleStabilizedRuinFilterMode payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player != null && player.containerMenu.containerId == payload.containerId()
                    && player.containerMenu instanceof com.immortalstorage.immortalstorage.menu.custom.StabilizedMiniatureImmortalRuinMenu menu
                    && menu.stillValid(player)) menu.toggleFilterMode(payload.mode());
        });
    }

    private static void handleSetEntangledRuinFilter(ModPayloads.SetEntangledRuinFilter payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player != null && player.containerMenu.containerId == payload.containerId()
                    && player.containerMenu instanceof com.immortalstorage.immortalstorage.menu.custom.SideFilterMenu menu
                    && menu instanceof net.minecraft.world.inventory.AbstractContainerMenu container
                    && container.stillValid(player)) menu.setFilter(payload.side(), payload.slot(), payload.stack());
        });
    }

    private static void handleToggleEntangledRuinFilterMode(ModPayloads.ToggleEntangledRuinFilterMode payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player != null && player.containerMenu.containerId == payload.containerId()
                    && player.containerMenu instanceof com.immortalstorage.immortalstorage.menu.custom.SideFilterMenu menu
                    && menu instanceof net.minecraft.world.inventory.AbstractContainerMenu container
                    && container.stillValid(player)) menu.toggleFilterMode(payload.side(), payload.mode());
        });
    }

    private static void handleSetEntangledRuinValue(ModPayloads.SetEntangledRuinValue payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || player.containerMenu.containerId != payload.containerId()) return;
            if (player.containerMenu instanceof com.immortalstorage.immortalstorage.menu.custom.EntangledMiniatureRuinMenu menu) {
                menu.setAuthoritativeValue(payload.side(), payload.index(), payload.value());
            } else if (player.containerMenu instanceof com.immortalstorage.immortalstorage.menu.custom.AdvancedEntangledMiniatureRuinMenu advanced) {
                advanced.setAuthoritativeValue(payload.side(), payload.index(), payload.value());
            }
        });
    }

    private static boolean hasLiveXianqiaoMenu(ServerPlayer player, XianqiaoStorageMenu menu) {
        return player != null && menu != null && menu.hasLiveTerminalAccess(player);
    }

    private static void handleOpenKongqiao(ModPayloads.OpenKongqiao m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = serverPlayer(ctx);
            if (sp == null) return;
            ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(sp);
            if (d.getStage() < 1 || d.getStage() >= 6) {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(sp, Component.literal("Kongqiao is available only at stages 1-5"), true);
                return;
            }
            sp.openMenu(new KongqiaoProvider(sp.getUUID()));
        });
    }

    private static void handleOpenXianqiao(ModPayloads.OpenXianqiaoStorage m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = serverPlayer(ctx);
            if (sp == null) return;
            ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(sp);
            if (d.getStage() < 6) {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(sp, Component.literal("Ascension stage required"), true);
                return;
            }
            sp.openMenu(new XianqiaoStorageProvider(sp.getUUID()));
            if (sp.containerMenu instanceof XianqiaoStorageMenu menu) sendTerminalSnapshot(sp, menu);
        });
    }

    private static void handleTriggerTribulation(ModPayloads.TriggerTribulation m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = serverPlayer(ctx);
            if (sp == null || sp.containerMenu.containerId != m.containerId()
                    || !(sp.containerMenu instanceof XianqiaoStorageMenu menu)
                    || !hasLiveXianqiaoMenu(sp, menu)
                    || menu.getActiveModule() != 1) return;
            ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(sp);
            int st = d.getStage();
            if (!com.immortalstorage.immortalstorage.progression.TribulationPolicy.canStart(
                    st, com.immortalstorage.immortalstorage.progression.TribulationPolicy.configuredMaximumStage())) {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(sp, Component.literal("Current stage cannot start tribulation"), true);
                return;
            }
            if (d.isTribulationActive()) {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(sp, Component.literal("Tribulation already active"), true);
                return;
            }

            if (!com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions
                    .isPersonalRealmFor(sp.level().dimension(),
                            com.immortalstorage.immortalstorage.dimension.RealmHelper.realmId(sp))) {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(sp, Component.literal("Tribulation can begin only in your personal Xianqiao realm"), true);
                return;
            }
            if (com.immortalstorage.immortalstorage.event.TribulationHelper.start(sp)) {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(sp, Component.literal("Tribulation begins. Defeat the bound target."), true);
            } else {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(sp, Component.literal("Tribulation target could not be created"), true);
            }
        });
    }

    private static void handleTimeFlow(ModPayloads.TimeFlow m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = serverPlayer(ctx);
            if (sp == null || sp.containerMenu.containerId != m.containerId()
                    || !(sp.containerMenu instanceof XianqiaoStorageMenu menu)
                    || !hasLiveXianqiaoMenu(sp, menu)
                    || menu.getActiveModule() != 1) return;
            ImmortalStoragePlayerData d = ImmortalStoragePlayerData.get(sp);
            int st = d.getStage();
            if (st < 7) {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(sp, Component.literal("Stage 7+ required to adjust time flow"), true);
                return;
            }

            int nextPermille = com.immortalstorage.immortalstorage.dimension.RealmTimeScalePolicy.stepPermille(
                    st, d.getRealmTimeRatePermille(), m.delta());
            d.setRealmTimeRatePermille(nextPermille);
            com.immortalstorage.immortalstorage.dimension.RealmHelper.refreshRealmTickRate(sp);
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(sp, Component.literal("Time flow: " + d.getTimeScale() + "x"), true);
        });
    }

    private static void handleSetStorageModule(ModPayloads.SetStorageModule m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = serverPlayer(ctx);
            if (sp == null) return;
            int module = m.module();
            if (module < -1 || module > 3) {
                return;
            }
            if (sp.containerMenu instanceof XianqiaoStorageMenu menu) {
                if (!hasLiveXianqiaoMenu(sp, menu)) return;
                int target = module >= 0 && module <= 2 ? module : -1;
                if (target == -1) {
                    if (menu.getActiveModule() >= 0) menu.clickMenuButton(sp, menu.getActiveModule());
                } else if (menu.getActiveModule() != target) {
                    menu.clickMenuButton(sp, target);
                }
            } else if (sp.containerMenu instanceof KongqiaoMenu menu) {
                int target = module == 0 && menu.isCraftingUnlocked() ? 0
                        : module == 1 && menu.isFurnaceUnlocked() ? 1 : -1;
                if (target == -1) {
                    if (menu.getActiveModule() >= 0) menu.clickMenuButton(sp, menu.getActiveModule());
                } else if (menu.getActiveModule() != target) {
                    menu.clickMenuButton(sp, target);
                }
            }
        });
    }

    private static void handleSetTerminalViewport(ModPayloads.SetTerminalViewport payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || payload.baseRow() < 0) return;
            if (player.containerMenu instanceof XianqiaoStorageMenu menu) {
                if (!hasLiveXianqiaoMenu(player, menu)) return;
                menu.setViewport(payload.visibleRows(), payload.baseRow());
            } else if (player.containerMenu instanceof KongqiaoMenu menu) {
                menu.setViewport(payload.visibleRows(), payload.baseRow());
            }
        });
    }

    private static void handleSetTerminalQuery(ModPayloads.SetTerminalQuery payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || !(player.containerMenu instanceof XianqiaoStorageMenu menu)
                    || !hasLiveXianqiaoMenu(player, menu)) return;
            var query = new com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery(
                    payload.text(),
                    com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery.SortOrder.byId(payload.sortOrder()),
                    com.immortalstorage.immortalstorage.api.storage.terminal.TerminalQuery.SortDirection.byId(payload.sortDirection()));
            menu.setTerminalQuery(query);
        });
    }

    private static void handleTerminalEntryAction(ModPayloads.TerminalEntryAction payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || !(player.containerMenu instanceof XianqiaoStorageMenu menu)
                    || !hasLiveXianqiaoMenu(player, menu)) return;
            var action = com.immortalstorage.immortalstorage.api.storage.terminal.TerminalAction.byId(payload.action());
            if (menu.containerId != payload.containerId()
                    || action == null) {
                sendTerminalSnapshot(player, menu);
                return;
            }
            if (!menu.handleEntryAction(player, payload.revision(), payload.entryId(), action)) {
                sendTerminalSnapshot(player, menu);
            }
        });
    }

    private static void handleRealmEnvironment(ModPayloads.RealmEnvironment payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || player.containerMenu.containerId != payload.containerId()
                    || !(player.containerMenu instanceof XianqiaoStorageMenu menu)
                    || !hasLiveXianqiaoMenu(player, menu) || menu.getActiveModule() != 1) return;
            ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
            if (data.getStage() < 6) return;
            if (payload.action() == 0) data.toggleRealmDaytime();
            else if (payload.action() == 1) data.cycleRealmWeather();
            else return;
            com.immortalstorage.immortalstorage.dimension.RealmHelper.refreshRealmEnvironment(player);
        });
    }

    private static void handleSetTerminalChannel(ModPayloads.SetTerminalChannel payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || player.containerMenu.containerId != payload.containerId()
                    || !(player.containerMenu instanceof XianqiaoStorageMenu menu)
                    || !hasLiveXianqiaoMenu(player, menu)) return;
            menu.setTerminalChannel(payload.fluid());
        });
    }

    private static void handleTerminalFluidEntryAction(ModPayloads.TerminalFluidEntryAction payload,
                                                       IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || player.containerMenu.containerId != payload.containerId()
                    || !(player.containerMenu instanceof XianqiaoStorageMenu menu)
                    || !hasLiveXianqiaoMenu(player, menu)
                    || !menu.hasLiveFluidAccess(player)) {
                if (player != null && player.containerMenu instanceof XianqiaoStorageMenu openMenu) {
                    sendTerminalSnapshot(player, openMenu);
                }
                return;
            }
            if (!menu.handleFluidContainerAction(player, payload.revision(), payload.entryId(), payload.deposit())) {
                sendTerminalSnapshot(player, menu);
            }
        });
    }

    private static void handleTerminalExternalResourceEntryAction(
            ModPayloads.TerminalExternalResourceEntryAction payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || player.containerMenu.containerId != payload.containerId()
                    || !(player.containerMenu instanceof XianqiaoStorageMenu menu)
                    || !hasLiveXianqiaoMenu(player, menu)
                    || !menu.hasLiveExternalAccess(player)) {
                if (player != null && player.containerMenu instanceof XianqiaoStorageMenu openMenu) {
                    sendTerminalSnapshot(player, openMenu);
                }
                return;
            }
            if (!menu.handleExternalResourceContainerAction(
                    player, payload.revision(), payload.entryId(), payload.deposit())) {
                sendTerminalSnapshot(player, menu);
            }
        });
    }

    private static void handleTransferTerminalRecipe(ModPayloads.TransferTerminalRecipe payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            boolean staleXianqiao = player != null
                    && player.containerMenu instanceof XianqiaoStorageMenu menu
                    && !hasLiveXianqiaoMenu(player, menu);
            if (player == null || player.containerMenu.containerId != payload.containerId()
                    || staleXianqiao
                    || !(player.containerMenu instanceof com.immortalstorage.immortalstorage.api.storage.terminal.StorageTerminalView terminal)
                    || terminal.viewport().revision() != payload.revision()) {
                if (player != null && player.containerMenu instanceof XianqiaoStorageMenu menu) sendTerminalSnapshot(player, menu);
                return;
            }
            var holder = com.immortalstorage.immortalstorage.compat.mc2612.CompatLevel.server(player.level()).getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, payload.recipeId()));
            if (holder.isEmpty()) return;
            if (holder.get().value() instanceof net.minecraft.world.item.crafting.SmithingRecipe smithingRecipe
                    && player.containerMenu instanceof com.immortalstorage.immortalstorage.api.storage.terminal.SmithingTransferTarget smithingTarget
                    && smithingTarget.isSmithingUnlocked() && smithingTarget.isSmithingVisible()) {
                @SuppressWarnings("unchecked")
                var smithingHolder = (net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.SmithingRecipe>) (Object) holder.get();
                smithingTarget.transferSmithingRecipe(smithingHolder, payload.revision());
                return;
            }
            if (!(holder.get().value() instanceof net.minecraft.world.item.crafting.CraftingRecipe recipe)
                    || !(player.containerMenu instanceof com.immortalstorage.immortalstorage.api.storage.terminal.CraftingTransferTarget target)
                    || !terminal.isCraftingUnlocked() || !terminal.isCraftingVisible()) return;
            @SuppressWarnings("unchecked")
            var craftingHolder = (net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe>) (Object) holder.get();
            List<com.immortalstorage.immortalstorage.api.storage.terminal.CraftingTransferTarget.TransferIngredient> storage =
                    player.containerMenu instanceof XianqiaoStorageMenu xianqiao
                            ? xianqiao.serverCraftingStorageIngredients()
                            : player.containerMenu instanceof KongqiaoMenu kongqiao
                            ? kongqiao.serverCraftingStorageIngredients() : List.of();
            if (!hasRecipeIngredients(player, target, storage, recipe.placementInfo().ingredients())) return;
            target.transferCraftingRecipe(craftingHolder, Math.max(1, Math.min(64, payload.requestedSets())),
                    payload.revision());
        });
    }

    private static boolean hasRecipeIngredients(ServerPlayer player,
                                                com.immortalstorage.immortalstorage.api.storage.terminal.CraftingTransferTarget target,
                                                List<com.immortalstorage.immortalstorage.api.storage.terminal.CraftingTransferTarget.TransferIngredient> storage,
                                                List<net.minecraft.world.item.crafting.Ingredient> ingredients) {
        List<ItemStack> physicalSources = new java.util.ArrayList<>();
        for (ItemStack stack : com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayerInventory.items(player)) {
            if (!stack.isEmpty()) physicalSources.add(stack);
        }
        for (var slot : target.craftingInputSlots()) {
            if (slot.hasItem()) physicalSources.add(slot.getItem());
        }
        return TerminalRecipeAvailability.hasIngredients(storage, physicalSources, ingredients);
    }


    public static void sendTerminalSnapshot(ServerPlayer player, XianqiaoStorageMenu menu) {
        if (!hasLiveXianqiaoMenu(player, menu)) return;
        var viewport = menu.viewport();
        List<ModPayloads.TerminalViewSnapshot.Entry> entries = new java.util.ArrayList<>();
        for (var entry : menu.bufferedEntries()) {
            entries.add(new ModPayloads.TerminalViewSnapshot.Entry(entry.entryId(), entry.displayStack(), entry.amount()));
        }
        PacketDistributor.sendToPlayer(player, new ModPayloads.TerminalViewSnapshot(
                menu.containerId, viewport.revision(), viewport.visibleRows(), viewport.baseRow(),
                menu.bufferedBaseRow(), viewport.totalRows(), menu.totalItemEntries(), List.copyOf(entries)));
        sendFluidTerminalSnapshot(player, menu);
        boolean recipeSourcesSent = menu.shouldSendRecipeSources(viewport.revision());
        if (recipeSourcesSent) {
            sendRecipeSources(player, menu, menu.serverCraftingStorageIngredients(), viewport.revision());
        }
        menu.markTerminalSnapshotSent(viewport.revision(), recipeSourcesSent);
    }

    public static void sendFluidTerminalSnapshot(ServerPlayer player, XianqiaoStorageMenu menu) {
        if (!hasLiveXianqiaoMenu(player, menu)) return;
        List<ModPayloads.TerminalFluidViewSnapshot.Entry> entries = new java.util.ArrayList<>();
        for (var entry : menu.bufferedFluidEntries()) {
            entries.add(new ModPayloads.TerminalFluidViewSnapshot.Entry(
                    entry.entryId(), entry.displayStack(), entry.amountMb()));
        }
        PacketDistributor.sendToPlayer(player, new ModPayloads.TerminalFluidViewSnapshot(
                menu.containerId, menu.fluidRevision(), menu.getVisibleRows(), menu.getBaseRow(),
                menu.bufferedBaseRow(), menu.getTotalRows(), menu.totalItemEntries(),
                menu.totalFluidEntries(), List.copyOf(entries)));
        menu.markFluidTerminalSnapshotSent(menu.fluidRevision());
        List<ModPayloads.TerminalExternalViewSnapshot.Entry> external = new java.util.ArrayList<>();
        for (var entry : menu.bufferedExternalEntries()) {
            external.add(new ModPayloads.TerminalExternalViewSnapshot.Entry(
                    entry.entryId(), entry.key().channel(), entry.key().resourceId(), entry.amount()));
        }
        PacketDistributor.sendToPlayer(player, new ModPayloads.TerminalExternalViewSnapshot(
                menu.containerId, menu.externalRevision(), menu.totalExternalEntries(), List.copyOf(external)));
        menu.markExternalTerminalSnapshotSent(menu.externalRevision());
    }

    public static void sendRecipeSources(ServerPlayer player, net.minecraft.world.inventory.AbstractContainerMenu menu,
                                         List<com.immortalstorage.immortalstorage.api.storage.terminal.CraftingTransferTarget.TransferIngredient> sources,
                                         long revision) {
        int chunks = Math.max(1, (sources.size() + ModPayloads.TerminalRecipeSources.MAX_ENTRIES - 1)
                / ModPayloads.TerminalRecipeSources.MAX_ENTRIES);
        for (int chunk = 0; chunk < chunks; chunk++) {
            int from = chunk * ModPayloads.TerminalRecipeSources.MAX_ENTRIES;
            int to = Math.min(sources.size(), from + ModPayloads.TerminalRecipeSources.MAX_ENTRIES);
            List<ModPayloads.TerminalRecipeSources.Entry> entries = new java.util.ArrayList<>(to - from);
            for (int i = from; i < to; i++) {
                var source = sources.get(i);
                entries.add(new ModPayloads.TerminalRecipeSources.Entry(source.stack(), source.amount()));
            }
            PacketDistributor.sendToPlayer(player, new ModPayloads.TerminalRecipeSources(
                    menu.containerId, revision, chunk, chunks, entries));
        }
    }

    private static void handleToggleRealm(ModPayloads.ToggleRealm m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = serverPlayer(ctx);
            if (sp == null) return;
            if (com.immortalstorage.immortalstorage.dimension.RealmHelper.isInOwnRealm(sp)) {
                com.immortalstorage.immortalstorage.dimension.RealmHelper.exitRealm(sp);
            } else {
                com.immortalstorage.immortalstorage.dimension.RealmHelper.enterRealm(sp);
            }
        });
    }

    private static void handleRealmCenterTeleport(ModPayloads.RealmCenterTeleport m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = serverPlayer(ctx);
            if (sp == null) return;
            com.immortalstorage.immortalstorage.dimension.RealmHelper.teleportToRealmCenter(sp);
        });
    }

    private static void handleDomainToggle(ModPayloads.DomainToggle m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = serverPlayer(ctx);
            if (sp == null) return;
            com.immortalstorage.immortalstorage.dimension.DomainExpansionManager.toggle(sp);
        });
    }

    private static void handleCycleStaffMode(ModPayloads.CycleStaffMode m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = serverPlayer(ctx);
            if (sp == null) return;
            ItemStack stack = sp.getMainHandItem();
            if (stack.getItem() instanceof com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem) {
                com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem.cycleMode(sp, m.delta());
            }
        });
    }

    private static void handleSetSourceSideMode(ModPayloads.SetSourceSideMode m, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = serverPlayer(ctx);
            if (sp == null) return;
            BlockPos pos = m.blockPos();
            if (!(sp.containerMenu instanceof SourceVeinMenu menu) || menu.getBlockEntity() == null
                    || !menu.getBlockEntity().getBlockPos().equals(pos)) {
                return;
            }
            if (sp.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            if (!(sp.level().getBlockEntity(pos) instanceof SourceVeinBlockEntity source)
                    || source != menu.getBlockEntity() || !menu.stillValid(sp)) {
                return;
            }
            if (!com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.matches(sp, source.getOwner()) && !com.immortalstorage.immortalstorage.compat.mc2612.CompatPlayer.hasPermissions(sp, 2)) {
                com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(sp, Component.translatable("message.immortalstorage.source_vein.configure.denied"), true);
                return;
            }
            Direction[] directions = Direction.values();
            if (m.side() < 0 || m.side() >= directions.length) return;
            // The wire value is the same stable id used by menu sync and NBT;
            // legacy/unknown ids intentionally fail closed to DISABLED.
            source.setSideMode(directions[m.side()], SourceVeinBlockEntity.SourceSideMode.byId(m.mode()));
        });
    }

    private static void handleAdjustSourceFlux(ModPayloads.AdjustSourceFlux payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || (payload.direction() != -1 && payload.direction() != 1)) {
                return;
            }
            SourceVeinBlockEntity source = validateOpenOwnedSource(
                    player, payload.containerId(), payload.blockPos());
            if (source == null) return;
            source.adjustFluxLimit(payload.direction());
            player.containerMenu.broadcastChanges();
        });
    }

    private static void handleAdjustStaffTeleportDistance(ModPayloads.AdjustStaffTeleportDistance m,
                                                           IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sp = serverPlayer(ctx);
            if (sp != null) com.immortalstorage.immortalstorage.item.custom.SpiritStaffItem
                    .adjustTeleportDistance(sp, m.delta());
        });
    }

    private static void handleAuraGuardLeap(ModPayloads.AuraGuardLeap payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || player.isSpectator() || player.getAbilities().mayfly
                    || !com.immortalstorage.immortalstorage.combat.ImmortalMasterTalismanService.hasAuraGuard(player)
                    || player.getCooldowns().isOnCooldown(new ItemStack(com.immortalstorage.immortalstorage.item.ModItems.IMMORTAL_MASTER_TALISMAN.get()))) {
                return;
            }
            var velocity = player.getDeltaMovement();
            // Vanilla vertical drag/gravity integrates an initial 0.91 velocity
            // to approximately five blocks of ascent. Keep horizontal motion.
            player.setDeltaMovement(velocity.x, Math.max(0.91D, velocity.y + 0.91D), velocity.z);
            player.hurtMarked = true;
            player.resetFallDistance();
            player.startFallFlying();
            player.getCooldowns().addCooldown(
                    new ItemStack(com.immortalstorage.immortalstorage.item.ModItems.IMMORTAL_MASTER_TALISMAN.get()), 8);
        });
    }

    private static void handleAuraGuardFlightState(
            ModPayloads.AuraGuardFlightState payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null) return;
            if (!payload.expanded()) {
                if (player.isFallFlying()
                        && player.getPersistentData().getBooleanOr("ImmortalStorageVirtualElytra", false)) {
                    player.stopFallFlying();
                }
                player.getPersistentData().remove("ImmortalStorageVirtualElytra");
                return;
            }
            if (player.isSpectator() || player.onGround() || player.isPassenger()
                    || player.isInWater() || player.isFallFlying()
                    || player.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION)
                    || !com.immortalstorage.immortalstorage.combat.ImmortalMasterTalismanService.hasAuraGuard(player)) {
                return;
            }
            player.tryToStartFallFlying();
        });
    }

    private static void handleAuraGuardBoost(ModPayloads.AuraGuardBoost payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || !player.isFallFlying()
                    || !player.getPersistentData().getBooleanOr("ImmortalStorageVirtualElytra", false)) return;
            player.setDeltaMovement(player.getDeltaMovement().add(player.getLookAngle().scale(1.5D)));
            player.hurtMarked = true;
        });
    }

    private static void handleSpiritStaffBuildPreview(
            ModPayloads.RequestSpiritStaffBuildPreview request, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || request.face() < 0 || request.face() >= Direction.values().length
                    || request.hand() < 0
                    || request.hand() >= net.minecraft.world.InteractionHand.values().length) return;
            BlockPos clicked = request.blockPos();
            if (!player.level().hasChunkAt(clicked)
                    || player.distanceToSqr(clicked.getCenter()) > 64.0D) return;
            var preview = request.removal()
                    ? com.immortalstorage.immortalstorage.item.custom.SpiritStaffBuildExecutor.previewRemoval(
                    player, net.minecraft.world.InteractionHand.values()[request.hand()], clicked,
                    Direction.values()[request.face()],
                    com.immortalstorage.immortalstorage.config.ImmortalStorageConfig.SPIRIT_STAFF_BUILD_LIMIT.get())
                    : com.immortalstorage.immortalstorage.item.custom.SpiritStaffBuildExecutor.preview(
                    player, net.minecraft.world.InteractionHand.values()[request.hand()], clicked,
                    Direction.values()[request.face()],
                    com.immortalstorage.immortalstorage.config.ImmortalStorageConfig.SPIRIT_STAFF_BUILD_LIMIT.get());
            List<Long> positions = preview.positions().stream().map(BlockPos::asLong).toList();
            PacketDistributor.sendToPlayer(player, new ModPayloads.SpiritStaffBuildPreviewSnapshot(
                    request.requestId(), request.pos(), request.face(), request.hand(), request.removal(),
                    preview.failure().ordinal(), positions));
        });
    }

    private static void handleSpiritStaffBuildRemoval(
            ModPayloads.RemoveSpiritStaffBuildLayer request, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || request.face() < 0 || request.face() >= Direction.values().length
                    || request.hand() < 0
                    || request.hand() >= net.minecraft.world.InteractionHand.values().length) return;
            BlockPos clicked = request.blockPos();
            if (!player.level().hasChunkAt(clicked) || player.distanceToSqr(clicked.getCenter()) > 64.0D) return;
            var result = com.immortalstorage.immortalstorage.item.custom.SpiritStaffBuildExecutor.removeLayer(
                    player, net.minecraft.world.InteractionHand.values()[request.hand()], clicked,
                    Direction.values()[request.face()],
                    com.immortalstorage.immortalstorage.config.ImmortalStorageConfig.SPIRIT_STAFF_BUILD_LIMIT.get());
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, result.succeeded()
                    ? Component.translatable("message.immortalstorage.spirit_staff.build.removed", result.placed())
                    : Component.translatable("message.immortalstorage.spirit_staff.build.blocked"), true);
        });
    }

    private static void handleImmortalArtifactBuildRestore(
            ModPayloads.RestoreImmortalArtifactBuildLayer request, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || request.hand() < 0
                    || request.hand() >= net.minecraft.world.InteractionHand.values().length) return;
            var hand = net.minecraft.world.InteractionHand.values()[request.hand()];
            ItemStack stack = player.getItemInHand(hand);
            int restored = com.immortalstorage.immortalstorage.item.custom.ImmortalArtifactRestorationLog
                    .restore(player, stack);
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable(restored > 0
                    ? "message.immortalstorage.immortal_artifact.build.restored"
                    : "message.immortalstorage.immortal_artifact.build.restore_failed", restored), true);
        });
    }

    private static void handleSpiritSwordFurnaceOperation(
            ModPayloads.SpiritSwordFurnaceOperation request, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || request.hand() < 0
                    || request.hand() >= net.minecraft.world.InteractionHand.values().length) return;
            var hand = net.minecraft.world.InteractionHand.values()[request.hand()];
            ImmortalStoragePlayerData data = ImmortalStoragePlayerData.get(player);
            if (data.getStage() < 6) return;
            if (request.action() == ModPayloads.SpiritSwordFurnaceOperation.SUMMON) {
                if (data.getEmbeddedImmortalFurnace().summonSpiritSword(player, hand)) {
                    com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable(
                            "message.immortalstorage.spirit_sword.summoned"), true);
                }
            } else if (request.action() == ModPayloads.SpiritSwordFurnaceOperation.STORE) {
                if (data.getEmbeddedImmortalFurnace().storeSpiritSword(player, hand)) {
                    com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable(
                            "message.immortalstorage.spirit_sword.stored"), true);
                }
            }
        });
    }

    private static void handleSetXianqiaoInterfaceSideMode(
            ModPayloads.SetXianqiaoInterfaceSideMode payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            BlockPos pos = payload.blockPos();
            if (player == null || player.containerMenu.containerId != payload.containerId()
                    || !(player.containerMenu instanceof XianqiaoInterfaceMenu menu)
                    || menu.getBlockEntity() == null
                    || !menu.getBlockEntity().getBlockPos().equals(pos)
                    || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D) > 64.0D
                    || !(player.level().getBlockEntity(pos) instanceof XianqiaoInterfaceBlockEntity source)
                    || source != menu.getBlockEntity() || !source.canUse(player)
                    || !menu.stillValid(player)) return;
            if (source.getConfigRevision() != payload.configRevision()) {
                menu.broadcastChanges();
                return;
            }
            Direction[] sides = Direction.values();
            XianqiaoInterfaceBlockEntity.SideMode[] modes =
                    XianqiaoInterfaceBlockEntity.SideMode.values();
            if (payload.side() < 0 || payload.side() >= sides.length
                    || payload.mode() < 0 || payload.mode() >= modes.length) return;
            source.setSideMode(sides[payload.side()], modes[payload.mode()]);
            menu.broadcastChanges();
        });
    }

    private static void handleSetXianqiaoInterfaceTargetAmount(
            ModPayloads.SetXianqiaoInterfaceTargetAmount payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            BlockPos pos = payload.blockPos();
            if (player == null || player.containerMenu.containerId != payload.containerId()
                    || !(player.containerMenu instanceof XianqiaoInterfaceMenu menu)
                    || menu.getBlockEntity() == null
                    || !menu.getBlockEntity().getBlockPos().equals(pos)
                    || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                    pos.getZ() + 0.5D) > 64.0D
                    || !(player.level().getBlockEntity(pos) instanceof XianqiaoInterfaceBlockEntity source)
                    || source != menu.getBlockEntity() || !source.canUse(player)
                    || !menu.stillValid(player)) return;
            if (source.getConfigRevision() != payload.configRevision()) {
                menu.broadcastChanges();
                return;
            }
            if (payload.slot() < 0
                    || payload.slot() >= com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceInventory.SLOT_COUNT) {
                return;
            }
            boolean updated;
            if (source.getInventory().getExternalTarget(payload.slot()) != null) {
                updated = source.getInventory().setExternalTargetAmount(
                        payload.slot(), Math.max(0L, payload.amount()));
            } else if (!source.getInventory().getFluidTarget(payload.slot()).isEmpty()) {
                updated = source.getInventory().setFluidTargetAmount(payload.slot(), payload.amount());
            } else {
                updated = source.getInventory().setTargetAmount(payload.slot(), payload.amount());
            }
            if (!updated) {
                menu.broadcastChanges();
                return;
            }
            menu.broadcastChanges();
        });
    }

    private static void handleSetXianqiaoInterfaceSlotFaceMask(
            ModPayloads.SetXianqiaoInterfaceSlotFaceMask payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)
                    || payload.slot() < 0 || payload.slot() >= XianqiaoInterfaceInventory.SLOT_COUNT
                    || payload.side() < 0 || payload.side() >= Direction.values().length) return;
            XianqiaoInterfaceBlockEntity source = validateOpenXianqiaoInterface(
                    player, payload.containerId(), payload.blockPos());
            if (source == null) return;
            source.getInventory().setOutputFaceEnabled(
                    payload.slot(), Direction.values()[payload.side()], payload.enabled());
            player.containerMenu.broadcastChanges();
        });
    }

    private static void handleSetXianqiaoInterfaceActiveTransfer(
            ModPayloads.SetXianqiaoInterfaceActiveTransfer payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            XianqiaoInterfaceBlockEntity source = validateOpenXianqiaoInterface(
                    player, payload.containerId(), payload.blockPos(), payload.configRevision());
            if (source == null) return;
            if (payload.pull()) source.setActivePullEnabled(payload.enabled());
            else source.setActivePushEnabled(payload.enabled());
        });
    }

    private static void handleSetXianqiaoInterfaceItemTarget(
            ModPayloads.SetXianqiaoInterfaceItemTarget payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || payload.slot() < 0
                    || payload.slot() >= com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceInventory.SLOT_COUNT) {
                return;
            }
            XianqiaoInterfaceBlockEntity source = validateOpenXianqiaoInterface(
                    player, payload.containerId(), payload.blockPos(), payload.configRevision());
            if (source == null) return;
            ItemStack identity = payload.identity();
            long maximum = XianqiaoInterfaceLimits.itemTargetLimit();
            long amount = Math.min(maximum, Math.max(0L, payload.requestedAmount()));
            boolean updated = amount == 0L
                    ? source.getInventory().clearSlot(payload.slot())
                    : source.getInventory().setTarget(
                    payload.slot(), identity.copyWithCount((int) amount));
            if (updated) player.containerMenu.broadcastChanges();
        });
    }

    private static void handleSetXianqiaoInterfaceFluidTarget(
            ModPayloads.SetXianqiaoInterfaceFluidTarget payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || payload.slot() < 0
                    || payload.slot() >= com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceInventory.SLOT_COUNT) {
                return;
            }
            XianqiaoInterfaceBlockEntity source = validateOpenXianqiaoInterface(
                    player, payload.containerId(), payload.blockPos(), payload.configRevision());
            if (source == null) return;
            long amount = Math.min(XianqiaoInterfaceLimits.fluidTargetLimitMb(),
                    Math.max(0L, payload.requestedAmountMb()));
            boolean updated = amount == 0L
                    ? source.getInventory().clearSlot(payload.slot())
                    : source.getInventory().setFluidTarget(payload.slot(),
                    payload.identity().copyWithAmount((int) amount));
            if (updated) player.containerMenu.broadcastChanges();
        });
    }

    private static void handleSetXianqiaoInterfaceExternalTarget(
            ModPayloads.SetXianqiaoInterfaceExternalTarget payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null || payload.slot() < 0
                    || payload.slot() >= XianqiaoInterfaceInventory.SLOT_COUNT) return;
            XianqiaoInterfaceBlockEntity source = validateOpenXianqiaoInterface(
                    player, payload.containerId(), payload.blockPos(), payload.configRevision());
            if (source == null) return;
            com.immortalstorage.core.resource.ResourceChannelKey key;
            try {
                key = new com.immortalstorage.core.resource.ResourceChannelKey(
                        payload.channel(), payload.resourceId());
            } catch (IllegalArgumentException exception) {
                return;
            }
            if (!com.immortalstorage.immortalstorage.compat.ExternalResourceCatalog.contains(key)) return;
            long amount = com.immortalstorage.core.resource.ExternalResourceChannels
                    .clampCacheAmount(key, payload.requestedAmount());
            boolean updated = amount == 0L
                    ? source.getInventory().clearSlot(payload.slot())
                    : source.getInventory().setExternalTarget(payload.slot(), key, amount);
            if (updated) player.containerMenu.broadcastChanges();
        });
    }

    private static XianqiaoInterfaceBlockEntity validateOpenXianqiaoInterface(
            ServerPlayer player, int containerId, BlockPos pos, long configRevision) {
        XianqiaoInterfaceBlockEntity source = validateOpenXianqiaoInterface(
                player, containerId, pos);
        if (source == null) return null;
        if (source.getConfigRevision() != configRevision) {
            player.containerMenu.broadcastChanges();
            return null;
        }
        return source;
    }

    private static XianqiaoInterfaceBlockEntity validateOpenXianqiaoInterface(
            ServerPlayer player, int containerId, BlockPos pos) {
        if (player.containerMenu.containerId != containerId
                || !(player.containerMenu instanceof XianqiaoInterfaceMenu menu)
                || menu.getBlockEntity() == null
                || !menu.getBlockEntity().getBlockPos().equals(pos)
                || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D) > 64.0D
                || !(player.level().getBlockEntity(pos) instanceof XianqiaoInterfaceBlockEntity source)
                || source != menu.getBlockEntity() || !source.canUse(player)
                || !menu.stillValid(player)) return null;
        return source;
    }

    private static void handleSetSourceFluxLimit(ModPayloads.SetSourceFluxLimit payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = serverPlayer(ctx);
            if (player == null) return;
            SourceVeinBlockEntity source = validateOpenOwnedSource(
                    player, payload.containerId(), payload.blockPos());
            if (source == null) return;
            source.setFluxLimit(SourceFluxValue.clamp(payload.fluxLimit()));
            player.containerMenu.broadcastChanges();
        });
    }

    private static SourceVeinBlockEntity validateOpenOwnedSource(ServerPlayer player, int containerId, BlockPos pos) {
        if (player.containerMenu.containerId != containerId
                || !(player.containerMenu instanceof SourceVeinMenu menu)
                || menu.getBlockEntity() == null
                || !menu.getBlockEntity().getBlockPos().equals(pos)) {
            return null;
        }
        if (!(player.level().getBlockEntity(pos) instanceof SourceVeinBlockEntity source)
                || source != menu.getBlockEntity()
                || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
            return null;
        }
        if (!com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.matches(player, source.getOwner())) {
            com.immortalstorage.immortalstorage.compat.mc2612.CompatMessages.sendSystemMessage(player, Component.translatable("message.immortalstorage.source_vein.configure.denied"), true);
            return null;
        }
        return menu.stillValid(player) ? source : null;
    }

    private ModNetwork() {}
}
