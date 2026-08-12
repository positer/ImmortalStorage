package com.immortalstorage.immortalstorage.progression;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.function.IntConsumer;

/** Visual/audio-only ascension effects. This class never creates a gameplay explosion. */
public final class AscensionCosmeticEffects {
    private AscensionCosmeticEffects() {}

    public static void playImmortalPillAscension(ServerPlayer player, int fromStage, int toStage) {
        int count = ImmortalStorageProgressionRules.cosmeticTntBlastCount(
                fromStage, toStage, ImmortalStorageProgressionRules.AdvancementSource.IMMORTAL_PILL);
        emitBlasts(count, index -> emitOneBlast(player, index, count));
    }

    static void emitImmortalPillBlasts(IntConsumer sink) {
        emitBlasts(ImmortalStorageProgressionRules.IMMORTAL_PILL_ASCENSION_BLASTS, sink);
    }

    private static void emitBlasts(int count, IntConsumer sink) {
        for (int index = 0; index < count; index++) {
            sink.accept(index);
        }
    }

    private static void emitOneBlast(ServerPlayer player, int index, int count) {
        ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();
        double angle = Math.PI * 2.0D * index / Math.max(1, count);
        double radius = index == 0 ? 0.0D : 1.15D;
        double x = player.getX() + Math.cos(angle) * radius;
        double y = player.getY() + 0.35D + (index % 2) * 0.45D;
        double z = player.getZ() + Math.sin(angle) * radius;

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 1.8F, 0.86F + index * 0.055F);
    }
}
