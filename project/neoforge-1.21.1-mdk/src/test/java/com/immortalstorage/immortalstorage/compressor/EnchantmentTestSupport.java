package com.immortalstorage.immortalstorage.compressor;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Builds bound, keyed enchantment holders the way the datapack loader does, without a server. */
public final class EnchantmentTestSupport {
    public static Enchantment value(String id) {
        ResourceLocation location = ResourceLocation.parse(id);
        return Enchantment.enchantment(new Enchantment.EnchantmentDefinition(
                        HolderSet.direct(), Optional.empty(), 1, 5,
                        new Enchantment.Cost(1, 1), new Enchantment.Cost(21, 1), 1, List.of(EquipmentSlotGroup.ANY)))
                .build(location);
    }

    public static Holder<Enchantment> enchantment(String id) {
        return new KeyedHolder(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(id)), value(id));
    }

    /** Writes through the same helper the machine uses, so a book lands in the stored component. */
    public static ItemStack enchanted(ItemStack stack, Holder<Enchantment> enchantment, int level) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(stack));
        mutable.set(enchantment, level);
        EnchantmentHelper.setEnchantments(stack, mutable.toImmutable());
        return stack;
    }

    /** The single registry name a one-enchantment output book carries. */
    public static String singleName(ItemStack stack) {
        List<Holder<Enchantment>> ordered = EnchantmentStackRules.ordered(EnchantmentStackRules.enchantments(stack));
        if (ordered.size() != 1) throw new AssertionError("expected exactly one enchantment, found " + ordered.size());
        return EnchantmentStackRules.registryName(ordered.getFirst());
    }

    private record KeyedHolder(ResourceKey<Enchantment> key, Enchantment enchantment) implements Holder<Enchantment> {
        @Override public Enchantment value() { return enchantment; }
        @Override public boolean isBound() { return true; }
        @Override public boolean is(ResourceLocation location) { return key.location().equals(location); }
        @Override public boolean is(ResourceKey<Enchantment> other) { return key.equals(other); }
        @Override public boolean is(TagKey<Enchantment> tag) { return false; }
        @Override public boolean is(Holder<Enchantment> other) { return other.is(key); }
        @Override public boolean is(Predicate<ResourceKey<Enchantment>> predicate) { return predicate.test(key); }
        @Override public Stream<TagKey<Enchantment>> tags() { return Stream.of(); }
        @Override public Either<ResourceKey<Enchantment>, Enchantment> unwrap() { return Either.left(key); }
        @Override public Optional<ResourceKey<Enchantment>> unwrapKey() { return Optional.of(key); }
        @Override public Holder.Kind kind() { return Holder.Kind.REFERENCE; }
        @Override public boolean canSerializeIn(HolderOwner<Enchantment> owner) { return true; }
    }

    private EnchantmentTestSupport() {}
}
