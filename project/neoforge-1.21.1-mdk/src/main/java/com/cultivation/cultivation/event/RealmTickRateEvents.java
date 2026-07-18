package com.cultivation.cultivation.event;

import com.cultivation.cultivation.dimension.CultivationDimensions;
import com.cultivation.cultivation.dimension.RealmHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** Lifecycle guard for the player-bound personal-realm tick budget. */
public final class RealmTickRateEvents {
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (CultivationDimensions.isPersonalRealmFor(player.level().dimension(), player.getUUID())) {
            RealmHelper.refreshRealmTickRate(player);
        } else {
            RealmHelper.releaseRealmTickRate(player.server, player.getUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RealmHelper.releaseRealmTickRate(player.server, player.getUUID());
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (CultivationDimensions.isPersonalRealmFor(event.getFrom(), player.getUUID())) {
            RealmHelper.releaseRealmTickRate(player.server, player.getUUID());
        }
        if (CultivationDimensions.isPersonalRealmFor(event.getTo(), player.getUUID())) {
            RealmHelper.refreshRealmTickRate(player);
        }
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && CultivationDimensions.isXianqiaoRealm(level.dimension())) {
            RealmHelper.releaseUnloadingRealm(level);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        RealmHelper.releaseAllRealmTickRates(event.getServer());
    }
}
