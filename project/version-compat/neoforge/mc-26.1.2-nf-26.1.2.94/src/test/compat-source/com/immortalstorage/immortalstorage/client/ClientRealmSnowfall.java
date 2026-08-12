package com.immortalstorage.immortalstorage.client;

import com.immortalstorage.immortalstorage.dimension.ImmortalStorageDimensions;
import com.immortalstorage.immortalstorage.dimension.RealmEnvironmentPolicy;
import com.immortalstorage.immortalstorage.player.ImmortalStoragePlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Snow mode uses dedicated flakes because the same fixed realm biome must also support visible rain. */
public final class ClientRealmSnowfall {
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.isPaused()) return;
        if (!ImmortalStorageDimensions.isPersonalRealmFor(
                minecraft.level.dimension(),
                com.immortalstorage.immortalstorage.player.PersistentPlayerIdentity.id(minecraft.player))) return;
        if (ImmortalStoragePlayerData.get(minecraft.player).getRealmWeatherMode()
                != RealmEnvironmentPolicy.SNOW) return;

        var random = minecraft.level.getRandom();
        for (int index = 0; index < 8; index++) {
            double x = minecraft.player.getX() + (random.nextDouble() - 0.5D) * 24.0D;
            double z = minecraft.player.getZ() + (random.nextDouble() - 0.5D) * 24.0D;
            int blockX = net.minecraft.util.Mth.floor(x);
            int blockZ = net.minecraft.util.Mth.floor(z);
            if (!minecraft.level.hasChunkAt(new BlockPos(blockX, minecraft.player.getBlockY(), blockZ))) continue;
            int surface = minecraft.level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ);
            double y = Math.max(minecraft.player.getY() + 7.0D, surface + 6.0D)
                    + random.nextDouble() * 6.0D;
            minecraft.level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z,
                    (random.nextDouble() - 0.5D) * 0.02D, -0.04D - random.nextDouble() * 0.03D,
                    (random.nextDouble() - 0.5D) * 0.02D);
        }
    }

    private ClientRealmSnowfall() {}
}
