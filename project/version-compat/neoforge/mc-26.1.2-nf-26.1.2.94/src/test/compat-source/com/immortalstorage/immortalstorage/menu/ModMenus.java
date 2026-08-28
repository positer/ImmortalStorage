package com.immortalstorage.immortalstorage.menu;

import java.util.function.Supplier;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoStorageMenu;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoInterfaceMenu;
import com.immortalstorage.immortalstorage.menu.custom.AdvancedXianqiaoInterfaceMenu;
import com.immortalstorage.immortalstorage.menu.custom.ImmortalFurnaceMenu;
import com.immortalstorage.immortalstorage.menu.custom.KongqiaoMenu;
import com.immortalstorage.immortalstorage.menu.custom.SourceVeinMenu;
import com.immortalstorage.immortalstorage.menu.custom.TreasureBasinMenu;
import com.immortalstorage.immortalstorage.menu.custom.WorldShardMinerMenu;
import com.immortalstorage.immortalstorage.menu.custom.SourceVeinManagerMenu;
import com.immortalstorage.immortalstorage.menu.custom.StabilizedMiniatureImmortalRuinMenu;
import com.immortalstorage.immortalstorage.menu.custom.MiniatureImmortalRuinMenu;
import com.immortalstorage.immortalstorage.menu.custom.SimulatedReincarnationFurnaceMenu;
import com.immortalstorage.immortalstorage.menu.custom.EntangledMiniatureRuinMenu;
import com.immortalstorage.immortalstorage.menu.custom.AdvancedStabilizedMiniatureImmortalRuinMenu;
import com.immortalstorage.immortalstorage.menu.custom.AdvancedEntangledMiniatureRuinMenu;
import com.immortalstorage.immortalstorage.menu.custom.SimulatedSpiritFieldMenu;
import com.immortalstorage.immortalstorage.menu.custom.EnergyCrystalMenu;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoRedstoneInterfaceMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ImmortalStorageMod.MODID);

    public static final Supplier<MenuType<KongqiaoMenu>> KONGQIAO =
            MENUS.register("kongqiao", () -> IMenuTypeExtension.create(KongqiaoMenu::new));

    public static final Supplier<MenuType<XianqiaoStorageMenu>> XIANQIAO_STORAGE =
            MENUS.register("xianqiao_storage", () -> IMenuTypeExtension.create(XianqiaoStorageMenu::new));

    public static final Supplier<MenuType<XianqiaoInterfaceMenu>> XIANQIAO_INTERFACE =
            MENUS.register("xianqiao_interface", () -> IMenuTypeExtension.create(XianqiaoInterfaceMenu::new));

    public static final Supplier<MenuType<AdvancedXianqiaoInterfaceMenu>> ADVANCED_XIANQIAO_INTERFACE =
            MENUS.register("advanced_xianqiao_interface", () -> IMenuTypeExtension.create(AdvancedXianqiaoInterfaceMenu::new));
    public static final Supplier<MenuType<XianqiaoRedstoneInterfaceMenu>> XIANQIAO_REDSTONE_INTERFACE =
            MENUS.register("xianqiao_redstone_interface", () -> IMenuTypeExtension.create(XianqiaoRedstoneInterfaceMenu::new));

    public static final Supplier<MenuType<ImmortalFurnaceMenu>> IMMORTAL_FURNACE =
            MENUS.register("immortal_furnace", () -> IMenuTypeExtension.create(ImmortalFurnaceMenu::new));
    public static final Supplier<MenuType<SimulatedReincarnationFurnaceMenu>> SIMULATED_REINCARNATION_FURNACE =
            MENUS.register("simulated_reincarnation_furnace",
                    () -> IMenuTypeExtension.create(SimulatedReincarnationFurnaceMenu::new));
    public static final Supplier<MenuType<SimulatedSpiritFieldMenu>> SIMULATED_SPIRIT_FIELD =
            MENUS.register("simulated_spirit_field",
                    () -> IMenuTypeExtension.create(SimulatedSpiritFieldMenu::new));
    public static final Supplier<MenuType<EnergyCrystalMenu>> ENERGY_CRYSTAL =
            MENUS.register("energy_crystal",
                    () -> IMenuTypeExtension.create(EnergyCrystalMenu::new));

    public static final Supplier<MenuType<SourceVeinMenu>> SOURCE_VEIN =
            MENUS.register("source_vein", () -> IMenuTypeExtension.create(SourceVeinMenu::new));

    public static final Supplier<MenuType<TreasureBasinMenu>> TREASURE_BASIN =
            MENUS.register("treasure_basin", () -> IMenuTypeExtension.create(TreasureBasinMenu::new));

    public static final Supplier<MenuType<WorldShardMinerMenu>> WORLD_SHARD_MINER =
            MENUS.register("world_shard_miner", () -> IMenuTypeExtension.create(WorldShardMinerMenu::new));

    public static final Supplier<MenuType<SourceVeinManagerMenu>> SOURCE_VEIN_MANAGER =
            MENUS.register("source_vein_manager", () -> IMenuTypeExtension.create(SourceVeinManagerMenu::new));

    public static final Supplier<MenuType<StabilizedMiniatureImmortalRuinMenu>> STABILIZED_MINIATURE_IMMORTAL_RUIN =
            MENUS.register("stabilized_miniature_immortal_ruin",
                    () -> IMenuTypeExtension.create(StabilizedMiniatureImmortalRuinMenu::new));
    public static final Supplier<MenuType<MiniatureImmortalRuinMenu>> MINIATURE_IMMORTAL_RUIN =
            MENUS.register("miniature_immortal_ruin", () -> IMenuTypeExtension.create(MiniatureImmortalRuinMenu::new));
    public static final Supplier<MenuType<EntangledMiniatureRuinMenu>> ENTANGLED_MINIATURE_IMMORTAL_RUIN =
            MENUS.register("entangled_miniature_immortal_ruin", () -> IMenuTypeExtension.create(EntangledMiniatureRuinMenu::new));
    public static final Supplier<MenuType<AdvancedStabilizedMiniatureImmortalRuinMenu>> ADVANCED_STABILIZED_MINIATURE_IMMORTAL_RUIN =
            MENUS.register("advanced_stabilized_miniature_immortal_ruin", () -> IMenuTypeExtension.create(AdvancedStabilizedMiniatureImmortalRuinMenu::new));
    public static final Supplier<MenuType<AdvancedEntangledMiniatureRuinMenu>> ADVANCED_ENTANGLED_MINIATURE_IMMORTAL_RUIN =
            MENUS.register("advanced_entangled_miniature_immortal_ruin", () -> IMenuTypeExtension.create(AdvancedEntangledMiniatureRuinMenu::new));

    private ModMenus() {}
}
