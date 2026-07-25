package com.immortalstorage.immortalstorage.item;

import java.util.function.Supplier;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.item.custom.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ImmortalStorageMod.MODID);

    public static Item.Properties setItemId(String name, Item.Properties props) {
        return props.rarity(rarityFor(name));
    }

    /** Keep rarity consistent for standalone items and every registered block item. */
    public static Rarity rarityFor(String name) {
        return switch (name) {
            case "ascension_dan", "white_day_thunder",
                    "immortal_ruin_forged_spirit_sword", "one_qi_returning_origin_sword",
                    "stabilized_miniature_immortal_ruin",
                    "world_shard_miner", "nether_star_vein", "enchanted_golden_apple_vein",
                    "dragon_egg_vein" -> Rarity.EPIC;
            case "immortal_yuan", "breakthrough_pill_embryo", "breakthrough_pill",
                    "immortal_pill", "nurturing_crystal", "nurturing_crystal_bedrock",
                    "qi_collecting_bottle", "primordial_qi",
                    "premixed_heavy_compound", "substitute_puppet", "miniature_immortal_ruin",
                    "soul_catcher",
                    "xianqiao_manager", "xianqiao_interface", "xianqiao_exchange_cell",
                    "xianqiao_rs_exchange_disk", "source_vein_manager", "ancient_debris_vein",
                    "diamond_vein", "emerald_vein" -> Rarity.RARE;
            case "jade_guide", "true_yuan", "refined_pill_embryo", "refined_pill",
                    "disposable_qi_collecting_bottle",
                    "spirit_crystal", "spirit_crystal_ore", "deepslate_spirit_crystal_ore",
                    "spirit_crystal_block", "spirit_core", "spirit_sword", "spirit_staff",
                    "spirit_drive", "immortal_furnace", "simulated_reincarnation_furnace", "treasure_basin",
                    "inactive_nurturing_crystal_bedrock", "small_nurturing_crystal_bud",
                    "medium_nurturing_crystal_bud", "large_nurturing_crystal_bud",
                    "nurturing_crystal_cluster", "crude_spirit_iron_vein", "spirit_crystal_vein",
                    "raw_gold_vein", "lapis_vein", "redstone_vein" -> Rarity.UNCOMMON;
            default -> Rarity.COMMON;
        };
    }

    public static <T extends Item> Supplier<T> registerItem(String name, Function<Item.Properties, T> func) {
        return registerItem(name, func, new Item.Properties());
    }

    public static <T extends Item> Supplier<T> registerItem(String name, Function<Item.Properties, T> func, Item.Properties baseProps) {
        return ITEMS.register(name, () -> func.apply(setItemId(name, baseProps)));
    }

    public static final Supplier<Item> JADE_GUIDE = ITEMS.register("jade_guide", () -> new SimpleJadeGuideItem(setItemId("jade_guide", new Item.Properties())));

    public static final Supplier<Item> TRUE_YUAN = ITEMS.register("true_yuan", () ->
            new TrueYuanItem(ModBlocks.TRUE_YUAN_LIGHT.get(), setItemId("true_yuan", new Item.Properties().stacksTo(64))));
    public static final Supplier<Item> IMMORTAL_YUAN = ITEMS.register("immortal_yuan", () ->
            new ImmortalYuanItem(ModBlocks.IMMORTAL_YUAN_LIGHT.get(), setItemId("immortal_yuan", new Item.Properties().stacksTo(64))));

    public static final Supplier<Item> CRUDE_PILL_EMBRYO = registerItem("crude_pill_embryo", p -> new CrudePillEmbryoItem(pillProps(p, 3, 0.5f)));
    public static final Supplier<Item> CRUDE_PILL = registerItem("crude_pill", p -> new CrudePillItem(pillProps(p, 3, 0.6f)));
    public static final Supplier<Item> REFINED_PILL_EMBRYO = registerItem("refined_pill_embryo", p -> new RefinedPillEmbryoItem(pillProps(p, 4, 0.8f)));
    public static final Supplier<Item> REFINED_PILL = registerItem("refined_pill", p -> new RefinedPillItem(pillProps(p, 4, 0.8f)));
    public static final Supplier<Item> BREAKTHROUGH_PILL_EMBRYO = registerItem("breakthrough_pill_embryo", BreakthroughPillEmbryoItem::new);
    public static final Supplier<Item> BREAKTHROUGH_PILL = registerItem("breakthrough_pill",
            p -> new BreakthroughPillItem(pillProps(p, 0, 0.0f)));
    public static final Supplier<Item> IMMORTAL_PILL = registerItem("immortal_pill", ImmortalPillItem::new);
    public static final Supplier<Item> ASCENSION_DAN = registerItem("ascension_dan",
            p -> new AscensionDanItem(pillProps(p, 0, 0.0f)));
    public static final Supplier<Item> WHITE_DAY_THUNDER = registerItem("white_day_thunder", WhiteDayThunderItem::new);

    public static final Supplier<Item> SPIRIT_IRON = registerItem("spirit_iron", Item::new);
    public static final Supplier<Item> SPIRIT_IRON_NUGGET = registerItem("spirit_iron_nugget", Item::new);
    public static final Supplier<Item> CRUDE_SPIRIT_IRON = registerItem("crude_spirit_iron", Item::new);
    public static final Supplier<Item> SPIRIT_CRYSTAL = registerItem("spirit_crystal", Item::new);
    public static final Supplier<Item> NURTURING_CRYSTAL = registerItem("nurturing_crystal", Item::new);
    public static final Supplier<Item> QI_COLLECTING_BOTTLE = registerItem("qi_collecting_bottle",
            p -> new QiCollectingBottleItem(p, true), new Item.Properties().stacksTo(1).durability(1024));
    public static final Supplier<Item> DISPOSABLE_QI_COLLECTING_BOTTLE = registerItem(
            "disposable_qi_collecting_bottle", p -> new QiCollectingBottleItem(p, false),
            new Item.Properties().stacksTo(16));
    public static final Supplier<Item> PRIMORDIAL_QI = registerItem("primordial_qi", PrimordialQiItem::new,
            new Item.Properties().stacksTo(64));
    public static final Supplier<Item> SPIRIT_CORE = registerItem("spirit_core", p -> new SpiritCoreItem(p.stacksTo(16)));
    public static final Supplier<Item> PREMIXED_HEAVY_COMPOUND = registerItem("premixed_heavy_compound", Item::new);
    public static final Supplier<Item> SUBSTITUTE_PUPPET = registerItem("substitute_puppet", SubstitutePuppetItem::new,
            new Item.Properties().stacksTo(1).durability(SubstitutePuppetItem.MAX_DURABILITY));
    public static final Supplier<Item> MINIATURE_IMMORTAL_RUIN = registerItem(
            "miniature_immortal_ruin", MiniatureImmortalRuinItem::new, new Item.Properties().stacksTo(1));

    public static final Supplier<Item> SPIRIT_SWORD = registerItem("spirit_sword", SpiritSwordItem::new);
    public static final Supplier<Item> IMMORTAL_RUIN_FORGED_SPIRIT_SWORD = registerItem(
            "immortal_ruin_forged_spirit_sword", ImmortalRuinForgedSpiritSwordItem::new);
    public static final Supplier<Item> ONE_QI_RETURNING_ORIGIN_SWORD = registerItem(
            "one_qi_returning_origin_sword", OneQiReturningOriginSwordItem::new);
    public static final Supplier<Item> SOUL_CATCHER = registerItem(
            "soul_catcher", SoulCatcherItem::new, new Item.Properties().stacksTo(1));
    public static final Supplier<Item> SPIRIT_STAFF = registerItem("spirit_staff", SpiritStaffItem::new,
            new Item.Properties().attributes(DiggerItem.createAttributes(Tiers.NETHERITE, 4.0F, -2.4F)));
    public static final Supplier<Item> SPIRIT_DRIVE = registerItem("spirit_drive", SpiritDriveItem::new);
    public static final Supplier<Item> XIANQIAO_EXCHANGE_CELL = registerItem(
            "xianqiao_exchange_cell", XianqiaoExchangeCellItem::new);
    public static final Supplier<Item> XIANQIAO_RS_EXCHANGE_DISK = registerItem(
            "xianqiao_rs_exchange_disk", RsExchangeDiskFactory::create);

    // NeoForge 1.21.1: alwaysEdible permits food use even when the hunger bar is full.
    // https://docs.neoforged.net/docs/1.21.1/items/#food
    static Item.Properties pillProps(Item.Properties base, int hunger, float sat) {
        return base.food(pillFood(hunger, sat));
    }

    static FoodProperties pillFood(int hunger, float sat) {
        return new FoodProperties.Builder()
                .nutrition(hunger)
                .saturationModifier(sat)
                .alwaysEdible()
                .fast()
                .build();
    }

    private ModItems() {}
}
