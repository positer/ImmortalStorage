package com.immortalstorage.immortalstorage.menu.provider;

import com.immortalstorage.immortalstorage.menu.custom.XianqiaoStorageMenu;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

public record XianqiaoStorageProvider(java.util.UUID playerId) implements MenuProvider {
    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.immortalstorage.xianqiao_storage");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
        return new XianqiaoStorageMenu(id, inv, player);
    }

    public boolean matches(Player p) {
        return p.getUUID().equals(playerId);
    }

    public static boolean isUnlocked(Player p) {
        return ImmortalStoragePlayerData.get(p).getStage() >= 6;
    }
}
