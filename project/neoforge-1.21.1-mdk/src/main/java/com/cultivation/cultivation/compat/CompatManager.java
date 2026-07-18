package com.cultivation.cultivation.compat;

import com.cultivation.cultivation.CultivationMod;
import com.cultivation.cultivation.block.entity.XianqiaoInterfaceBlockEntity;
import com.cultivation.core.resource.AtomicEnergyRefill;
import com.cultivation.core.resource.ExternalResourceChannels;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.function.Predicate;

public final class CompatManager {
    public static final boolean AE2_LOADED = modPresent("ae2");
    public static final boolean RS_LOADED = modPresent("refinedstorage");
    public static final boolean FTB_CHUNKS_LOADED = modPresent("ftbchunks");
    public static final boolean MEKANISM_LOADED = modPresent("mekanism");
    public static final boolean FLUX_NETWORKS_LOADED = modPresent("fluxnetworks");
    public static final boolean BOTANIA_LOADED = modPresent("botania");
    public static final boolean ARS_NOUVEAU_LOADED = modPresent("ars_nouveau");
    public static final boolean IRON_SPELLS_LOADED = modPresent("irons_spellbooks");
    public static final boolean INDUSTRIAL_FOREGOING_SOULS_LOADED =
            modPresent("industrialforegoingsouls");
    public static final boolean NATURES_AURA_LOADED = modPresent("naturesaura");
    public static final boolean BEYOND_DIMENSIONS_LOADED = modPresent("beyonddimensions");

    private static boolean modPresent(String id) {
        try {
            ModList runtimeMods = ModList.get();
            if (runtimeMods != null && runtimeMods.isLoaded(id)) {
                return true;
            }
            LoadingModList loadingMods = LoadingModList.get();
            return loadingMods != null && loadingMods.getModFileById(id) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public static String summary() {
        return "AE2=" + AE2_LOADED
                + ", RS=" + RS_LOADED
                + ", FTB-Chunks=" + FTB_CHUNKS_LOADED
                + ", Mekanism=" + MEKANISM_LOADED
                + ", Flux Networks=" + FLUX_NETWORKS_LOADED
                + ", Botania=" + BOTANIA_LOADED
                + ", Ars Nouveau=" + ARS_NOUVEAU_LOADED
                + ", Iron's Spells=" + IRON_SPELLS_LOADED
                + ", Industrial Foregoing Souls=" + INDUSTRIAL_FOREGOING_SOULS_LOADED
                + ", Nature's Aura=" + NATURES_AURA_LOADED
                + ", Beyond Dimensions=" + BEYOND_DIMENSIONS_LOADED;
    }

    public static void logCompat() {
        CultivationMod.LOG.info("[Compat] {}", summary());
    }

    /**
     * Registers lifecycle hooks without putting optional-mod types in this
     * always-loaded class. An integration is loaded by name only after the
     * corresponding mod is known to be present.
     */
    public static void initializeOptionalIntegrations(IEventBus modBus) {
        if (AE2_LOADED) modBus.addListener(CompatManager::initializeAe2);
        if (RS_LOADED) modBus.addListener(CompatManager::initializeRs);
        if (MEKANISM_LOADED) {
            installMekanismBridge();
            modBus.addListener(CompatManager::registerMekanismCapabilities);
        }
        if (BOTANIA_LOADED) {
            installBotaniaBridge();
            modBus.addListener(CompatManager::registerBotaniaCapabilities);
        }
        if (INDUSTRIAL_FOREGOING_SOULS_LOADED) {
            installIndustrialForegoingSoulsBridge();
            modBus.addListener(CompatManager::registerIndustrialForegoingSoulsCapabilities);
        }
        if (BEYOND_DIMENSIONS_LOADED) {
            modBus.addListener(CompatManager::initializeBeyondDimensions);
        }
    }

    private static void initializeAe2(InterModEnqueueEvent event) {
        event.enqueueWork(() -> invokeOptionalBootstrap(
                "com.cultivation.cultivation.compat.ae2.Ae2Compat"));
    }

    private static void initializeRs(InterModEnqueueEvent event) {
        event.enqueueWork(() -> invokeOptionalBootstrap(
                "com.cultivation.cultivation.compat.refinedstorage.RsCompat"));
    }

    private static void initializeBeyondDimensions(InterModEnqueueEvent event) {
        event.enqueueWork(() -> invokeOptionalBootstrap(
                "com.cultivation.cultivation.compat.beyonddimensions.BeyondDimensionsCompat"));
    }

    private static void installBotaniaBridge() {
        Function<XianqiaoInterfaceBlockEntity, AtomicEnergyRefill.ResourceStore> resolver =
                CompatManager::resolveBotaniaManaStorage;
        Predicate<XianqiaoInterfaceBlockEntity> outputting =
                CompatManager::hasBotaniaPushFace;
        invokeOptionalMethod(
                "com.cultivation.cultivation.compat.botania.BotaniaCompat",
                "installBridge",
                new Class<?>[]{Function.class, Predicate.class},
                resolver, outputting);
    }

    private static void installMekanismBridge() {
        Function<XianqiaoInterfaceBlockEntity, AtomicEnergyRefill.ResourceStore> resolver =
                blockEntity -> blockEntity.resolveExternalResourceStore(ExternalResourceChannels.FE);
        invokeOptionalMethod(
                "com.cultivation.cultivation.compat.mekanism.MekanismCompat",
                "installBridge",
                new Class<?>[]{Function.class},
                resolver);
    }

    private static void registerMekanismCapabilities(RegisterCapabilitiesEvent event) {
        invokeOptionalMethod(
                "com.cultivation.cultivation.compat.mekanism.MekanismCompat",
                "registerCapabilities",
                new Class<?>[]{RegisterCapabilitiesEvent.class},
                event);
    }

    private static void registerBotaniaCapabilities(RegisterCapabilitiesEvent event) {
        invokeOptionalMethod(
                "com.cultivation.cultivation.compat.botania.BotaniaCompat",
                "registerCapabilities",
                new Class<?>[]{RegisterCapabilitiesEvent.class},
                event);
    }

    private static void installIndustrialForegoingSoulsBridge() {
        Function<XianqiaoInterfaceBlockEntity, AtomicEnergyRefill.ResourceStore> resolver =
                blockEntity -> blockEntity.resolveExternalResourceStore(
                        ExternalResourceChannels.INDUSTRIAL_FOREGOING_SOUL);
        invokeOptionalMethod(
                "com.cultivation.cultivation.compat.ifsouls.IndustrialForegoingSoulsCompat",
                "installBridge",
                new Class<?>[]{Function.class},
                resolver);
    }

    private static void registerIndustrialForegoingSoulsCapabilities(
            RegisterCapabilitiesEvent event) {
        invokeOptionalMethod(
                "com.cultivation.cultivation.compat.ifsouls.IndustrialForegoingSoulsCompat",
                "registerCapabilities",
                new Class<?>[]{RegisterCapabilitiesEvent.class},
                event);
    }

    private static @Nullable AtomicEnergyRefill.ResourceStore resolveBotaniaManaStorage(
            XianqiaoInterfaceBlockEntity blockEntity) {
        return blockEntity.resolveExternalResourceStore(ExternalResourceChannels.BOTANIA_MANA);
    }

    private static boolean hasBotaniaPushFace(XianqiaoInterfaceBlockEntity blockEntity) {
        for (Direction side : Direction.values()) {
            if (blockEntity.getSideMode(side) == XianqiaoInterfaceBlockEntity.SideMode.PUSH) {
                return true;
            }
        }
        return false;
    }

    private static void invokeOptionalBootstrap(String className) {
        invokeOptionalMethod(className, "initialize", new Class<?>[0]);
    }

    private static void invokeOptionalMethod(
            String className, String methodName, Class<?>[] parameterTypes, Object... arguments) {
        try {
            Class<?> bootstrap = Class.forName(className, true, CompatManager.class.getClassLoader());
            Method method = bootstrap.getMethod(methodName, parameterTypes);
            method.invoke(null, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw new IllegalStateException(
                    "Optional integration failed: " + className + "#" + methodName, cause);
        } catch (ReflectiveOperationException | LinkageError exception) {
            throw new IllegalStateException(
                    "Optional integration is incompatible: " + className + "#" + methodName,
                    exception);
        }
    }

    private CompatManager() {}
}
