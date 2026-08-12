package com.immortalstorage.immortalstorage.compat.mc2612;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Data-component-era serialization bridge. The 26.1 public wrappers are
 * selected by type; reflection is used only for the private backing compound
 * of the official TagValueInput/TagValueOutput implementation because those
 * classes intentionally expose a Value* interface rather than a raw tag.
 */
public final class CompatValueIo {
    private CompatValueIo() {
    }

    public static CompoundTag rawInput(ValueInput input) {
        if (!(input instanceof TagValueInput)) {
            throw new IllegalStateException("26.1 block entity input is not TagValueInput");
        }
        return rawField(input, "input");
    }

    public static CompoundTag rawOutput(ValueOutput output) {
        if (!(output instanceof TagValueOutput)) {
            throw new IllegalStateException("26.1 block entity output is not TagValueOutput");
        }
        return rawField(output, "output");
    }

    private static CompoundTag rawField(Object value, String name) {
        try {
            Field field = value.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (CompoundTag) field.get(value);
        } catch (ReflectiveOperationException | ClassCastException e) {
            throw new IllegalStateException("Official 26.1 tag storage shape changed: " + name, e);
        }
    }

    public static CompoundTag serialize(Object value, HolderLookup.Provider registries) {
        if (value == null) return new CompoundTag();
        if (value instanceof LegacyNbtSerializable<?> legacy) {
            @SuppressWarnings("unchecked")
            CompoundTag result = (CompoundTag) ((LegacyNbtSerializable<?>) legacy).serializeNBT(registries);
            return result;
        }
        if (value instanceof ValueIOSerializable valueIo) {
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
            valueIo.serialize(output);
            return output.buildResult();
        }
        Object result = invokeSerializer(value, registries);
        return result instanceof CompoundTag tag ? tag : new CompoundTag();
    }

    public static void deserialize(Object value, HolderLookup.Provider registries, CompoundTag tag) {
        if (value == null || tag == null) return;
        if (value instanceof LegacyNbtSerializable<?> legacy) {
            @SuppressWarnings("unchecked")
            LegacyNbtSerializable<CompoundTag> typed = (LegacyNbtSerializable<CompoundTag>) legacy;
            typed.deserializeNBT(registries, tag);
            return;
        }
        if (value instanceof ValueIOSerializable valueIo) {
            valueIo.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
            return;
        }
        invokeDeserializer(value, registries, tag);
    }

    public static void saveItems(CompoundTag tag, Object items, HolderLookup.Provider registries) {
        if (items instanceof NonNullList<?> list) {
            saveItemList(tag, list, registries);
            return;
        }
        if (items instanceof Container container) {
            saveContainer(tag, container, registries);
            return;
        }
        tag.put("Items", serialize(items, registries));
    }

    public static void loadItems(CompoundTag tag, Object items, HolderLookup.Provider registries) {
        if (items instanceof NonNullList<?> list) {
            loadItemList(tag, list, registries);
            return;
        }
        if (items instanceof Container container) {
            loadContainer(tag, container, registries);
            return;
        }
        deserialize(items, registries, tag.getCompoundOrEmpty("Items"));
    }

    private static void saveItemList(CompoundTag tag, List<?> items,
                                     HolderLookup.Provider registries) {
        ListTag encoded = new ListTag();
        for (int slot = 0; slot < items.size(); slot++) {
            Object value = items.get(slot);
            if (!(value instanceof ItemStack stack) || stack.isEmpty()) continue;
            CompoundTag row = CompatCodec.saveItemStack(registries, stack)
                    .asCompound().orElseGet(CompoundTag::new);
            row.putByte("Slot", (byte) slot);
            encoded.add(row);
        }
        tag.put("Items", encoded);
    }

    private static void saveContainer(CompoundTag tag, Container container,
                                      HolderLookup.Provider registries) {
        java.util.ArrayList<ItemStack> values = new java.util.ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) values.add(container.getItem(i));
        saveItemList(tag, values, registries);
    }

    private static void loadItemList(CompoundTag tag, List<?> items,
                                     HolderLookup.Provider registries) {
        ListTag encoded = tag.getListOrEmpty("Items");
        for (Tag element : encoded) {
            if (!(element instanceof CompoundTag row)) continue;
            int slot = row.getByteOr("Slot", (byte) -1);
            if (slot < 0 || slot >= items.size()) continue;
            ItemStack stack = CompatCodec.parseItemStack(registries, row);
            @SuppressWarnings("unchecked")
            List<ItemStack> typed = (List<ItemStack>) items;
            typed.set(slot, stack);
        }
    }

    private static void loadContainer(CompoundTag tag, Container container,
                                      HolderLookup.Provider registries) {
        ListTag encoded = tag.getListOrEmpty("Items");
        for (Tag element : encoded) {
            if (!(element instanceof CompoundTag row)) continue;
            int slot = row.getByteOr("Slot", (byte) -1);
            if (slot < 0 || slot >= container.getContainerSize()) continue;
            container.setItem(slot, CompatCodec.parseItemStack(registries, row));
        }
    }

    private static Object invokeSerializer(Object value, HolderLookup.Provider registries) {
        try {
            Method withRegistries = value.getClass().getMethod("serializeNBT", HolderLookup.Provider.class);
            return withRegistries.invoke(value, registries);
        } catch (ReflectiveOperationException ignored) {
            try {
                Method legacy = value.getClass().getMethod("serializeNBT");
                return legacy.invoke(value);
            } catch (ReflectiveOperationException ignoredAgain) {
                return new CompoundTag();
            }
        }
    }

    private static void invokeDeserializer(Object value, HolderLookup.Provider registries, CompoundTag tag) {
        try {
            Method withRegistries = value.getClass().getMethod("deserializeNBT", HolderLookup.Provider.class,
                    CompoundTag.class);
            withRegistries.invoke(value, registries, tag);
            return;
        } catch (ReflectiveOperationException ignored) {
            try {
                Method legacy = value.getClass().getMethod("deserializeNBT", CompoundTag.class);
                legacy.invoke(value, tag);
            } catch (ReflectiveOperationException ignoredAgain) {
                // A target object with no NBT contract is intentionally left unchanged.
            }
        }
    }
}
