package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.block.entity.AdvancedXianqiaoInterfaceBlockEntity;
import com.immortalstorage.immortalstorage.block.entity.XianqiaoInterfaceBlockEntity;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Xianqiao Interface menu extended with the advanced stabilized ruin scheduling
 * options (xyz/+xzy range, frequency, preview, enabled, access/split/order).
 * The six-face PULL/PUSH/DISABLED modes and the active pull/push toggles come
 * from the plain interface menu data; the ruin data slots are appended after
 * the interface's own config data.
 */
public final class AdvancedXianqiaoInterfaceMenu extends XianqiaoInterfaceMenu {
    public static final int RUIN_DATA_START = XianqiaoInterfaceMenu.CONFIG_DATA_COUNT;

    public AdvancedXianqiaoInterfaceMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.ADVANCED_XIANQIAO_INTERFACE.get(), id, inventory,
                resolveBlockEntity(inventory, buffer));
    }

    public AdvancedXianqiaoInterfaceMenu(
            int id, Inventory inventory, AdvancedXianqiaoInterfaceBlockEntity blockEntity) {
        super(ModMenus.ADVANCED_XIANQIAO_INTERFACE.get(), id, inventory, blockEntity);
    }

    @Override
    protected int configurationDataCount() {
        return RUIN_DATA_START + AdvancedXianqiaoInterfaceBlockEntity.RUIN_DATA_COUNT;
    }

    @Override
    protected int readExtraData(int index) {
        AdvancedXianqiaoInterfaceBlockEntity be = advancedBlockEntity();
        if (be == null) return 0;
        return switch (index) {
            case 0 -> be.sizeX(); case 1 -> be.sizeY(); case 2 -> be.sizeZ();
            case 3 -> be.offsetX(); case 4 -> be.offsetY(); case 5 -> be.offsetZ();
            case 6 -> be.frequency(); case 7 -> be.previewEnabled() ? 1 : 0;
            case 8 -> be.schedulingEnabled() ? 1 : 0; case 9 -> be.accessMode();
            case 10 -> be.splitMode(); case 11 -> be.orderMode();
            default -> 0;
        };
    }

    @Override
    protected void writeExtraData(int index, int value) {
        AdvancedXianqiaoInterfaceBlockEntity be = advancedBlockEntity();
        if (be != null) be.setMenuValue(index, value);
    }

    /** Reads one ruin-style scheduling value (index 0..11). */
    public int value(int index) {
        return configurationData.get(RUIN_DATA_START + index);
    }

    /** Server-authoritative value setter used by the shared ruin payload. */
    public void setAuthoritativeValue(int index, int value) {
        if (index >= 0 && index < AdvancedXianqiaoInterfaceBlockEntity.RUIN_DATA_COUNT) {
            writeExtraData(index, value);
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < 12) {
            int index = id / 2;
            int base = configurationData.get(RUIN_DATA_START + index);
            configurationData.set(RUIN_DATA_START + index, base + (id % 2 == 0 ? -1 : 1));
            return true;
        }
        if (id == 12) {
            configurationData.set(RUIN_DATA_START + 7,
                    configurationData.get(RUIN_DATA_START + 7) == 0 ? 1 : 0);
            return true;
        }
        if (id == 13) {
            configurationData.set(RUIN_DATA_START + 8,
                    configurationData.get(RUIN_DATA_START + 8) == 0 ? 1 : 0);
            return true;
        }
        if (id == 14) {
            configurationData.set(RUIN_DATA_START + 6,
                    Math.max(1, configurationData.get(RUIN_DATA_START + 6) - 1));
            return true;
        }
        if (id == 15) {
            configurationData.set(RUIN_DATA_START + 6,
                    configurationData.get(RUIN_DATA_START + 6) + 1);
            return true;
        }
        if (id == 16) {
            configurationData.set(RUIN_DATA_START + 9,
                    configurationData.get(RUIN_DATA_START + 9) == 0 ? 1 : 0);
            return true;
        }
        if (id == 17) {
            configurationData.set(RUIN_DATA_START + 10,
                    configurationData.get(RUIN_DATA_START + 10) == 0 ? 1 : 0);
            return true;
        }
        if (id == 18) {
            configurationData.set(RUIN_DATA_START + 11,
                    configurationData.get(RUIN_DATA_START + 11) == 0 ? 1 : 0);
            return true;
        }
        return super.clickMenuButton(player, id);
    }

    private AdvancedXianqiaoInterfaceBlockEntity advancedBlockEntity() {
        return getBlockEntity() instanceof AdvancedXianqiaoInterfaceBlockEntity advanced
                ? advanced : null;
    }
}
