package com.immortalstorage.immortalstorage.menu.custom;

import com.immortalstorage.immortalstorage.block.entity.AdvancedStabilizedMiniatureImmortalRuinBlockEntity;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/** Stabilized-ruin storage/menu plus the looping container-scheduling buttons. */
public final class AdvancedStabilizedMiniatureImmortalRuinMenu extends StabilizedMiniatureImmortalRuinMenu {

    public AdvancedStabilizedMiniatureImmortalRuinMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        super(ModMenus.ADVANCED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), id, inventory,
                new SimpleContainer(55), new SimpleContainerData(14), null,
                buffer == null ? net.minecraft.core.BlockPos.ZERO : buffer.readBlockPos());
    }

    public AdvancedStabilizedMiniatureImmortalRuinMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ModMenus.ADVANCED_STABILIZED_MINIATURE_IMMORTAL_RUIN.get(), id, inventory, container, data,
                container instanceof AdvancedStabilizedMiniatureImmortalRuinBlockEntity ruin ? ruin : null,
                container instanceof AdvancedStabilizedMiniatureImmortalRuinBlockEntity ruin ? ruin.getBlockPos() : net.minecraft.core.BlockPos.ZERO);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 16) { data.set(10, data.get(10) == 0 ? 1 : 0); return true; }
        if (id == 17) { data.set(11, data.get(11) == 0 ? 1 : 0); return true; }
        if (id == 18) { data.set(12, data.get(12) == 0 ? 1 : 0); return true; }
        return super.clickMenuButton(player, id);
    }

    @Override
    protected int faceDataIndex() { return 13; }
}
