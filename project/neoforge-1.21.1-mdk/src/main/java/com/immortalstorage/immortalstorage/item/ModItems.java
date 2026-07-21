package com.immortalstorage.immortalstorage.item;

import java.util.function.Supplier;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.item.custom.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ImmortalStorageMod.MODID);

    public static Item.Properties setItemId(String name, Item.Properties props) {
        return props;
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
    public static final Supplier<Item> SPIRIT_CORE = registerItem("spirit_core", p -> new SpiritCoreItem(p.stacksTo(16)));
    public static final Supplier<Item> PREMIXED_HEAVY_COMPOUND = registerItem("premixed_heavy_compound", Item::new);
    public static final Supplier<Item> SUBSTITUTE_PUPPET = registerItem("substitute_puppet", SubstitutePuppetItem::new,
            new Item.Properties().stacksTo(1).durability(SubstitutePuppetItem.MAX_DURABILITY));
    public static final Supplier<Item> MINIATURE_IMMORTAL_RUIN = registerItem(
            "miniature_immortal_ruin", MiniatureImmortalRuinItem::new, new Item.Properties().stacksTo(1));

    public static final Supplier<Item> SPIRIT_SWORD = registerItem("spirit_sword", SpiritSwordItem::new);
    public static final Supplier<Item> IMMORTAL_RUIN_FORGED_SPIRIT_SWORD = registerItem(
            "immortal_ruin_forged_spirit_sword", ImmortalRuinForgedSpiritSwordItem::new);
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
