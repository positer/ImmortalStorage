package com.cultivation.cultivation.menu.provider;

import com.cultivation.cultivation.menu.custom.KongqiaoMenu;
import com.cultivation.cultivation.player.CultivationPlayerData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.NotNull;

public record KongqiaoProvider(java.util.UUID playerId) implements MenuProvider {
    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.cultivation.kongqiao");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
        return new KongqiaoMenu(id, inv, player);
    }

    public boolean matches(Player p) {
        return p.getUUID().equals(playerId);
    }

    public static boolean isUnlocked(Player p) {
        int stage = CultivationPlayerData.get(p).getStage();
        return stage >= 1 && stage < 6;
    }
}
