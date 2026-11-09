package com.immortalstorage.immortalstorage.api.machine;

import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.EntityType;
import java.util.*;
import java.util.function.Predicate;

/** Server-side additive filtering contract. No matching rule means vanilla IS behavior. */
public final class MachinePolicyApi {
    public enum Machine { WORLD_SHARD_MINER, SIMULATED_SPIRIT_FIELD, SIMULATED_REINCARNATION_FURNACE }
    public enum Target { INPUT_ITEM, INPUT_BLOCK, SUBSTRATE_BLOCK, INPUT_ENTITY, OUTPUT_ITEM }
    /** Null mode selects every mode; levels are structural levels, not reinforcement multipliers. */
    public record Rule(Machine machine, int minLevel, int maxLevel, Identifier mode,
                       Target target, List<String> whitelist, List<String> blacklist) {
        public Rule {
            Objects.requireNonNull(machine); Objects.requireNonNull(target);
            if (minLevel < 1 || maxLevel < minLevel) throw new IllegalArgumentException("invalid level range");
            if (machine == Machine.WORLD_SHARD_MINER && target != Target.OUTPUT_ITEM
                    || machine == Machine.SIMULATED_SPIRIT_FIELD && target == Target.INPUT_ENTITY
                    || machine == Machine.SIMULATED_REINCARNATION_FURNACE && (target == Target.INPUT_BLOCK || target == Target.SUBSTRATE_BLOCK))
                throw new IllegalArgumentException("unsupported machine/target combination");
            whitelist = List.copyOf(whitelist); blacklist = List.copyOf(blacklist);
            for (String selector : concat(whitelist, blacklist)) {
                if (Identifier.tryParse(selector.startsWith("#") ? selector.substring(1) : selector) == null)
                    throw new IllegalArgumentException("invalid selector: " + selector);
            }
        }
        public boolean applies(Machine machine, int level, Identifier mode, Target target) {
            return this.machine == machine && this.target == target && level >= minLevel && level <= maxLevel
                    && (this.mode == null || this.mode.equals(mode));
        }
        public boolean permits(Identifier id, Predicate<Identifier> inTag) {
            return blacklist.stream().noneMatch(s -> matches(s, id, inTag))
                    && (whitelist.isEmpty() || whitelist.stream().anyMatch(s -> matches(s, id, inTag)));
        }
    }
    private static final Map<Identifier, Rule> DEFAULTS = new LinkedHashMap<>();
    private static volatile Map<Identifier, Rule> active = Map.of();
    private static volatile long generation;
    private static boolean frozen;
    private MachinePolicyApi() {}
    public static synchronized void register(Identifier id, Rule rule) {
        if (frozen) throw new IllegalStateException("machine policy registration closed");
        Objects.requireNonNull(id); Objects.requireNonNull(rule);
        if (DEFAULTS.putIfAbsent(id, rule) != null) throw new IllegalArgumentException("duplicate rule " + id);
    }
    public static synchronized void freezeRegistration() { frozen = true; }
    public static synchronized Map<Identifier, Rule> registeredDefaults() { return Map.copyOf(DEFAULTS); }
    /** Internal reload boundary; publishes one immutable snapshot, including removal of old pack rules. */
    public static synchronized void install(Map<Identifier, Rule> rules) {
        active = Map.copyOf(rules); generation++;
    }
    public static Map<Identifier, Rule> rules() { return active; }
    public static long generation() { return generation; }
    public static boolean allows(Machine machine, int level, Identifier mode, Target target,
                                 Identifier id, Predicate<Identifier> inTag) {
        Objects.requireNonNull(machine); Objects.requireNonNull(target); Objects.requireNonNull(id);
        if (level < 1) throw new IllegalArgumentException("level must be positive");
        for (Rule rule : active.values())
            if (rule.applies(machine, level, mode, target) && !rule.permits(id, inTag)) return false;
        return true;
    }
    public static boolean allowsItem(Machine machine, int level, Identifier mode, Target target, Item item) {
        if (target != Target.INPUT_ITEM && target != Target.OUTPUT_ITEM) throw new IllegalArgumentException("not item target");
        return allows(machine, level, mode, target, BuiltInRegistries.ITEM.getKey(item),
                tag -> item.builtInRegistryHolder().is(TagKey.create(net.minecraft.core.registries.Registries.ITEM, tag)));
    }
    public static boolean allowsBlock(Machine machine, int level, Identifier mode, Block block) {
        return allowsBlock(machine, level, mode, Target.INPUT_BLOCK, block);
    }
    public static boolean allowsBlock(Machine machine, int level, Identifier mode, Target target, Block block) {
        if (target != Target.INPUT_BLOCK && target != Target.SUBSTRATE_BLOCK) throw new IllegalArgumentException("not block target");
        return allows(machine, level, mode, target, BuiltInRegistries.BLOCK.getKey(block),
                tag -> block.builtInRegistryHolder().is(TagKey.create(net.minecraft.core.registries.Registries.BLOCK, tag)));
    }
    public static boolean allowsEntity(Machine machine, int level, Identifier mode, EntityType<?> type) {
        return allows(machine, level, mode, Target.INPUT_ENTITY, BuiltInRegistries.ENTITY_TYPE.getKey(type),
                tag -> type.builtInRegistryHolder().is(TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, tag)));
    }
    private static boolean matches(String s, Identifier id, Predicate<Identifier> tag) {
        return s.startsWith("#") ? tag.test(Identifier.parse(s.substring(1))) : id.equals(Identifier.parse(s));
    }
    private static List<String> concat(List<String> a, List<String> b) {
        List<String> all = new ArrayList<>(a); all.addAll(b); return all;
    }
}
