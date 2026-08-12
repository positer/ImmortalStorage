package com.immortalstorage.immortalstorage.compat;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.tags.TagKey;
import com.mojang.serialization.Lifecycle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.equipment.trim.MaterialAssetGroup;
import net.minecraft.world.item.equipment.trim.TrimMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicBoolean;
import sun.misc.Unsafe;

/**
 * Target-only bootstrap for migrated unit tests.
 *
 * <p>The 26.1 server binds default data components during its resource reload
 * phase.  The lightweight NeoForge JUnit runner performs registry bootstrap
 * but does not create a reloadable server resource set, so tests that create
 * {@code ItemStack}s must apply the same official initializer output locally.</p>
 */
public final class CompatTestBootstrap {
    private static final Pattern MISSING_TAG = Pattern.compile(
            "Missing tag TagKey\\[([^\\]]+?) / ([^\\]]+)\\]");
    private static final Pattern MISSING_ELEMENT = Pattern.compile(
            "Missing element ResourceKey\\[([^\\]]+?) / ([^\\]]+)\\]");
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();
    private static final Unsafe UNSAFE = loadUnsafe();
    private static RegistryAccess registryAccess;

    private CompatTestBootstrap() {
    }

    public static void bootstrap() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        try {
            Bootstrap.bootStrap();
            RegistryAccess access = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
            for (int attempt = 0; attempt < 64; attempt++) {
                try {
                    BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(access)
                            .forEach(DataComponentInitializers.PendingComponents::apply);
                    registryAccess = access;
                    return;
                } catch (IllegalStateException missingTag) {
                    RegistryAccess updated = bindMissingTag(access, missingTag.getMessage());
                    if (updated == null) {
                        updated = bindMissingElement(access, missingTag.getMessage());
                    }
                    if (updated == null) {
                        throw missingTag;
                    }
                    access = updated;
                }
            }
            throw new IllegalStateException("Could not bind all vanilla test tags");
        } catch (RuntimeException failure) {
            INITIALIZED.set(false);
            throw failure;
        }
    }

    /**
     * Applies vanilla component initializers for items registered by a
     * migrated test's own {@code @BeforeAll} method.  Those items are created
     * after the common bootstrap hook, so they are not present in the first
     * initializer pass.
     */
    public static void rebindItemComponents() {
        bootstrap();
        if (registryAccess == null) {
            throw new IllegalStateException("Compatibility registry access was not initialized");
        }
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(registryAccess)
                .forEach(DataComponentInitializers.PendingComponents::apply);
    }

    public static RegistryAccess registryAccess() {
        bootstrap();
        if (registryAccess == null) {
            throw new IllegalStateException("Compatibility registry access was not initialized");
        }
        return registryAccess;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static RegistryAccess bindMissingTag(RegistryAccess access, String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = MISSING_TAG.matcher(message);
        if (!matcher.find()) {
            return null;
        }

        Identifier registryId = Identifier.parse(matcher.group(1));
        Identifier tagId = Identifier.parse(matcher.group(2));
        ResourceKey registryKey = ResourceKey.createRegistryKey(registryId);
        Registry<Object> registry = (Registry<Object>) access.lookup(registryKey).orElse(null);
        if (registry == null) {
            MappedRegistry<Object> synthetic = new MappedRegistry<>(registryKey, Lifecycle.stable());
            TagKey<Object> tag = TagKey.create(registryKey, tagId);
            synthetic.bindTags(Map.of(tag, List.of()));
            synthetic.freeze();

            List<Registry<?>> registries = new ArrayList<>();
            access.registries().forEach(entry -> registries.add(entry.value()));
            registries.add(synthetic);
            return new RegistryAccess.ImmutableRegistryAccess(registries);
        }

        if (!(registry instanceof MappedRegistry<?> mappedRegistry)) {
            return null;
        }

        MappedRegistry writable = mappedRegistry;
        writable.unfreeze(true);
        TagKey<Object> tag = TagKey.create(registryKey, tagId);
        writable.bindTags(Map.of(tag, List.of()));
        writable.freeze();
        return access;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static RegistryAccess bindMissingElement(RegistryAccess access, String message) {
        if (message == null) {
            return null;
        }
        Matcher matcher = MISSING_ELEMENT.matcher(message);
        if (!matcher.find()) {
            return null;
        }

        Identifier registryId = Identifier.parse(matcher.group(1));
        Identifier elementId = Identifier.parse(matcher.group(2));
        ResourceKey registryKey = ResourceKey.createRegistryKey(registryId);
        Registry<Object> registry = (Registry<Object>) access.lookup(registryKey).orElse(null);
        if (registry == null) {
            MappedRegistry<Object> synthetic = new MappedRegistry<>(registryKey, Lifecycle.stable());
            synthetic.freeze();
            List<Registry<?>> registries = new ArrayList<>();
            access.registries().forEach(entry -> registries.add(entry.value()));
            registries.add(synthetic);
            access = new RegistryAccess.ImmutableRegistryAccess(registries);
            registry = synthetic;
        }
        if (!(registry instanceof MappedRegistry<?> mappedRegistry)) {
            return null;
        }

        Object value = placeholderElement(registryId, elementId);
        if (value == null) {
            return null;
        }

        MappedRegistry writable = mappedRegistry;
        writable.unfreeze(true);
        writable.register(ResourceKey.create(registryKey, elementId), value, RegistrationInfo.BUILT_IN);
        writable.freeze();
        return access;
    }

    private static Object placeholderElement(Identifier registryId, Identifier elementId) {
        if (registryId.equals(net.minecraft.core.registries.Registries.TRIM_MATERIAL.identifier())) {
            return new TrimMaterial(
                    MaterialAssetGroup.create(elementId.getPath()),
                    Component.literal("compatibility test material"));
        }

        // These registries are normally populated by the server's datapack
        // reload.  The lightweight unit-test runner has no datapack layer,
        // while Item.Properties still resolves their default component
        // holders.  Allocate a correctly typed, inert value so the holder
        // can be bound; migrated tests do not call variant behaviour during
        // bootstrap.  This is deliberately test-only and never shipped.
        String className = switch (registryId.toString()) {
            case "minecraft:damage_type" ->
                    "net.minecraft.world.damagesource.DamageType";
            case "minecraft:chicken_variant" ->
                    "net.minecraft.world.entity.animal.chicken.ChickenVariant";
            case "minecraft:cow_variant" ->
                    "net.minecraft.world.entity.animal.cow.CowVariant";
            case "minecraft:pig_variant" ->
                    "net.minecraft.world.entity.animal.pig.PigVariant";
            case "minecraft:wolf_variant" ->
                    "net.minecraft.world.entity.animal.wolf.WolfVariant";
            case "minecraft:cat_variant" ->
                    "net.minecraft.world.entity.animal.feline.CatVariant";
            case "minecraft:frog_variant" ->
                    "net.minecraft.world.entity.animal.frog.FrogVariant";
            case "minecraft:zombie_nautilus_variant" ->
                    "net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariant";
            case "minecraft:painting_variant" ->
                    "net.minecraft.world.entity.decoration.painting.PaintingVariant";
            case "minecraft:chicken_sound_variant" ->
                    "net.minecraft.world.entity.animal.chicken.ChickenSoundVariant";
            case "minecraft:cow_sound_variant" ->
                    "net.minecraft.world.entity.animal.cow.CowSoundVariant";
            case "minecraft:pig_sound_variant" ->
                    "net.minecraft.world.entity.animal.pig.PigSoundVariant";
            case "minecraft:wolf_sound_variant" ->
                    "net.minecraft.world.entity.animal.wolf.WolfSoundVariant";
            case "minecraft:cat_sound_variant" ->
                    "net.minecraft.world.entity.animal.feline.CatSoundVariant";
            case "minecraft:trim_pattern" ->
                    "net.minecraft.world.item.equipment.trim.TrimPattern";
            case "minecraft:jukebox_song" ->
                    "net.minecraft.world.item.JukeboxSong";
            case "minecraft:instrument" ->
                    "net.minecraft.world.item.Instrument";
            default -> null;
        };
        if (className == null) {
            return null;
        }
        try {
            return UNSAFE.allocateInstance(Class.forName(className));
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException(
                    "Could not allocate compatibility placeholder for " + registryId, failure);
        }
    }

    private static Unsafe loadUnsafe() {
        try {
            var field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }
}
