package com.immortalstorage.immortalstorage.compat;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceBlockEntity;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import com.immortalstorage.core.resource.AtomicEnergyRefill;
import com.immortalstorage.core.resource.ExternalResourceChannels;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.List;

public final class CompatManager {
    public static final boolean AE2_LOADED = modPresent("ae2");
    public static final boolean RS_LOADED = modPresent("refinedstorage");
    public static final boolean FTB_CHUNKS_LOADED = modPresent("ftbchunks");
    public static final boolean MEKANISM_LOADED = modPresent("mekanism");
    public static final boolean FLUX_NETWORKS_LOADED = modPresent("fluxnetworks");
    public static final boolean BOTANIA_LOADED = modPresent("botania");
    public static final boolean ARS_NOUVEAU_LOADED = modPresent("ars_nouveau");
    public static final boolean INDUSTRIAL_FOREGOING_SOULS_LOADED =
            modPresent("industrialforegoingsouls");
    public static final boolean NATURES_AURA_LOADED = modPresent("naturesaura");
    public static final boolean BEYOND_DIMENSIONS_LOADED = modPresent("beyonddimensions");
    public static final boolean CREATE_LOADED = modPresent("create");
    public static final boolean CURIOS_LOADED = modPresent("curios");

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
                + ", Industrial Foregoing Souls=" + INDUSTRIAL_FOREGOING_SOULS_LOADED
                + ", Nature's Aura=" + NATURES_AURA_LOADED
                + ", Beyond Dimensions=" + BEYOND_DIMENSIONS_LOADED
                + ", Create=" + CREATE_LOADED
                + ", Curios=" + CURIOS_LOADED;
    }

    public static void logCompat() {
        ImmortalStorageMod.LOG.info("[Compat] {}", summary());
    }

    /** Called only from the physical client setup class. */
    public static void initializeClientIntegrations() {
        if (AE2_LOADED) {
            invokeOptionalBootstrap(
                    "com.immortalstorage.immortalstorage.compat.ae2.Ae2ClientCompat");
        }
        if (RS_LOADED) {
            invokeOptionalBootstrap(
                    "com.immortalstorage.immortalstorage.compat.refinedstorage.RsClientCompat");
        }
    }

    /**
     * Registers lifecycle hooks without putting optional-mod types in this
     * always-loaded class. An integration is loaded by name only after the
     * corresponding mod is known to be present.
     */
    public static void initializeOptionalIntegrations(IEventBus modBus) {
        registerExternalResourceCatalogues();
        if (AE2_LOADED) {
            modBus.addListener(CompatManager::registerAe2ExternalResourceKeyType);
            modBus.addListener(CompatManager::registerAe2Capabilities);
            modBus.addListener(CompatManager::initializeAe2);
        }
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
        if (ARS_NOUVEAU_LOADED) {
            installArsNouveauBridge();
            modBus.addListener(CompatManager::initializeArsNouveau);
            modBus.addListener(CompatManager::registerArsNouveauCapabilities);
        }
        if (CREATE_LOADED) {
            invokeOptionalMethod(
                    "com.immortalstorage.immortalstorage.compat.create.CreateNurturingCompat",
                    "register",
                    new Class<?>[]{IEventBus.class},
                    modBus);
        }
        if (CURIOS_LOADED) modBus.addListener(CompatManager::initializeCurios);
    }

    private static void initializeCurios(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> invokeOptionalBootstrap(
                "com.immortalstorage.immortalstorage.compat.curios.CuriosTalismanCompat"));
    }

    private static void registerExternalResourceCatalogues() {
        ExternalResourceCatalog.register(
                Identifier.fromNamespaceAndPath(ImmortalStorageMod.MODID, "builtin_external_resources"),
                CompatManager::availableBuiltinExternalResources);
        registerResource(ExternalResourceChannels.FE,
                builtinAddonTexture("ae2_fe"), "FE", 0xFFFF4B35,
                "resource.immortalstorage.external.name.energy");
        registerResource(ExternalResourceChannels.BOTANIA_MANA,
                builtinAddonTexture("ae2_botania_mana"),
                "Mana", 0xFF2CC6FF, "resource.immortalstorage.external.name.botania_mana");
        registerResource(ExternalResourceChannels.ARS_NOUVEAU_SOURCE,
                builtinAddonTexture("ae2_ars_source"),
                "Source", 0xFF8D5BE8, "resource.immortalstorage.external.name.ars_source");
        registerResource(ExternalResourceChannels.INDUSTRIAL_FOREGOING_SOUL,
                builtinAddonTexture("soul_surge"),
                "Soul", 0xFF6FD6E8, "resource.immortalstorage.external.name.soul");
    }

    private static List<com.immortalstorage.core.resource.ResourceChannelKey>
    availableBuiltinExternalResources() {
        List<com.immortalstorage.core.resource.ResourceChannelKey> resources = new ArrayList<>();
        resources.add(ExternalResourceChannels.FE);
        if (BOTANIA_LOADED) resources.add(ExternalResourceChannels.BOTANIA_MANA);
        if (ARS_NOUVEAU_LOADED) resources.add(ExternalResourceChannels.ARS_NOUVEAU_SOURCE);
        if (INDUSTRIAL_FOREGOING_SOULS_LOADED) {
            resources.add(ExternalResourceChannels.INDUSTRIAL_FOREGOING_SOUL);
        }
        return List.copyOf(resources);
    }

    private static Identifier builtinAddonTexture(String name) {
        return Identifier.fromNamespaceAndPath(ImmortalStorageMod.MODID,
                "textures/gui/external_resource/" + name + ".png");
    }

    private static void registerResource(
            com.immortalstorage.core.resource.ResourceChannelKey key,
            Identifier texture, String unit, int color, String translationKey) {
        ExternalResourceCatalog.registerDefinition(key, texture, unit, color,
                Component.translatable(translationKey), false);
    }

    private static void initializeAe2(InterModEnqueueEvent event) {
        event.enqueueWork(() -> invokeOptionalBootstrap(
                "com.immortalstorage.immortalstorage.compat.ae2.Ae2Compat"));
    }

    private static void registerAe2Capabilities(RegisterCapabilitiesEvent event) {
        invokeOptionalMethod(
                "com.immortalstorage.immortalstorage.compat.ae2.Ae2Compat",
                "registerCapabilities",
                new Class<?>[]{RegisterCapabilitiesEvent.class},
                event);
    }

    private static void registerAe2ExternalResourceKeyType(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.BLOCK)) return;
        invokeOptionalMethod(
                "com.immortalstorage.immortalstorage.compat.ae2.Ae2Compat",
                "registerExternalResourceKeyType",
                new Class<?>[0]);
    }

    private static void initializeRs(InterModEnqueueEvent event) {
        event.enqueueWork(() -> invokeOptionalBootstrap(
                "com.immortalstorage.immortalstorage.compat.refinedstorage.RsCompat"));
    }

    private static void initializeArsNouveau(InterModEnqueueEvent event) {
        event.enqueueWork(() -> invokeOptionalBootstrap(
                "com.immortalstorage.immortalstorage.compat.arsnouveau.ArsNouveauCompat"));
    }

    private static void registerArsNouveauCapabilities(RegisterCapabilitiesEvent event) {
        invokeOptionalMethod(
                "com.immortalstorage.immortalstorage.compat.arsnouveau.ArsNouveauCompat",
                "registerCapabilities",
                new Class<?>[]{RegisterCapabilitiesEvent.class},
                event);
    }

    private static void installArsNouveauBridge() {
        Function<XianqiaoInterfaceBlockEntity, AtomicEnergyRefill.ResourceStore> resolver =
                blockEntity -> blockEntity.resolveDirectionlessExternalResource(
                        ExternalResourceChannels.ARS_NOUVEAU_SOURCE);
        invokeOptionalMethod(
                "com.immortalstorage.immortalstorage.compat.arsnouveau.ArsNouveauCompat",
                "installBridge",
                new Class<?>[]{Function.class},
                resolver);
    }

    private static void installBotaniaBridge() {
        Function<XianqiaoInterfaceBlockEntity, AtomicEnergyRefill.ResourceStore> resolver =
                CompatManager::resolveBotaniaManaStorage;
        invokeOptionalMethod(
                "com.immortalstorage.immortalstorage.compat.botania.BotaniaCompat",
                "installBridge",
                new Class<?>[]{Function.class},
                resolver);
    }

    private static void installMekanismBridge() {
        Function<XianqiaoInterfaceBlockEntity, AtomicEnergyRefill.ResourceStore> resolver =
                blockEntity -> blockEntity.resolveExternalResourceCache(
                        ExternalResourceChannels.FE);
        invokeOptionalMethod(
                "com.immortalstorage.immortalstorage.compat.mekanism.MekanismCompat",
                "installBridge",
                new Class<?>[]{Function.class},
                resolver);
    }

    private static void registerMekanismCapabilities(RegisterCapabilitiesEvent event) {
        invokeOptionalMethod(
                "com.immortalstorage.immortalstorage.compat.mekanism.MekanismCompat",
                "registerCapabilities",
                new Class<?>[]{RegisterCapabilitiesEvent.class},
                event);
    }

    private static void registerBotaniaCapabilities(RegisterCapabilitiesEvent event) {
        invokeOptionalMethod(
                "com.immortalstorage.immortalstorage.compat.botania.BotaniaCompat",
                "registerCapabilities",
                new Class<?>[]{RegisterCapabilitiesEvent.class},
                event);
    }

    private static void installIndustrialForegoingSoulsBridge() {
        Function<XianqiaoInterfaceBlockEntity, AtomicEnergyRefill.ResourceStore> resolver =
                blockEntity -> blockEntity.resolveExternalResourceCache(
                        ExternalResourceChannels.INDUSTRIAL_FOREGOING_SOUL);
        invokeOptionalMethod(
                "com.immortalstorage.immortalstorage.compat.ifsouls.IndustrialForegoingSoulsCompat",
                "installBridge",
                new Class<?>[]{Function.class},
                resolver);
    }

    private static void registerIndustrialForegoingSoulsCapabilities(
            RegisterCapabilitiesEvent event) {
        invokeOptionalMethod(
                "com.immortalstorage.immortalstorage.compat.ifsouls.IndustrialForegoingSoulsCompat",
                "registerCapabilities",
                new Class<?>[]{RegisterCapabilitiesEvent.class},
                event);
    }

    private static @Nullable AtomicEnergyRefill.ResourceStore resolveBotaniaManaStorage(
            XianqiaoInterfaceBlockEntity blockEntity) {
        return blockEntity.resolveDirectionlessExternalResource(
                ExternalResourceChannels.BOTANIA_MANA);
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
