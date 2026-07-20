package com.immortalstorage.immortalstorage.menu;

import java.util.function.Supplier;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoStorageMenu;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoInterfaceMenu;
import com.immortalstorage.immortalstorage.menu.custom.ImmortalFurnaceMenu;
import com.immortalstorage.immortalstorage.menu.custom.KongqiaoMenu;
import com.immortalstorage.immortalstorage.menu.custom.SourceVeinMenu;
import com.immortalstorage.immortalstorage.menu.custom.TreasureBasinMenu;
import com.immortalstorage.immortalstorage.menu.custom.SourceVeinManagerMenu;
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

    public static final Supplier<MenuType<ImmortalFurnaceMenu>> IMMORTAL_FURNACE =
            MENUS.register("immortal_furnace", () -> IMenuTypeExtension.create(ImmortalFurnaceMenu::new));

    public static final Supplier<MenuType<SourceVeinMenu>> SOURCE_VEIN =
            MENUS.register("source_vein", () -> IMenuTypeExtension.create(SourceVeinMenu::new));

    public static final Supplier<MenuType<TreasureBasinMenu>> TREASURE_BASIN =
            MENUS.register("treasure_basin", () -> IMenuTypeExtension.create(TreasureBasinMenu::new));

    public static final Supplier<MenuType<SourceVeinManagerMenu>> SOURCE_VEIN_MANAGER =
            MENUS.register("source_vein_manager", () -> IMenuTypeExtension.create(SourceVeinManagerMenu::new));

    private ModMenus() {}
}
