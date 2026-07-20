package com.immortalstorage.immortalstorage.event;

import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.dimension.RealmHelper;
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
        if (ImmortalStorageDimensions.isPersonalRealmFor(player.level().dimension(), player.getUUID())) {
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
        if (ImmortalStorageDimensions.isPersonalRealmFor(event.getFrom(), player.getUUID())) {
            RealmHelper.releaseRealmTickRate(player.server, player.getUUID());
        }
        if (ImmortalStorageDimensions.isPersonalRealmFor(event.getTo(), player.getUUID())) {
            RealmHelper.refreshRealmTickRate(player);
        }
    }

    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level && ImmortalStorageDimensions.isXianqiaoRealm(level.dimension())) {
            RealmHelper.releaseUnloadingRealm(level);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        RealmHelper.releaseAllRealmTickRates(event.getServer());
    }
}
