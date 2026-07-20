package com.immortalstorage.immortalstorage.mixin.arsnouveau;

import com.immortalstorage.immortalstorage.compat.arsnouveau.ArsNouveauCompat;
import com.hollingsworth.arsnouveau.api.source.AbstractSourceMachine;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.client.particle.ParticleUtil;
import com.hollingsworth.arsnouveau.common.block.tile.RelayTile;
import com.hollingsworth.arsnouveau.common.items.DominionWand;
import com.hollingsworth.arsnouveau.common.util.PortUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RelayTile.class, remap = false)
abstract class RelayTileXianqiaoMixin {
    @Shadow private BlockPos toPos;
    @Shadow private BlockPos fromPos;
    @Shadow public boolean disabled;

    @Inject(method = "onFinishedConnectionFirst", at = @At("HEAD"), cancellable = true)
    private void immortalstorage$connectXianqiaoTarget(
            @Nullable BlockPos storedPos,
            @Nullable LivingEntity storedEntity,
            Player player,
            CallbackInfo ci) {
        RelayTile self = (RelayTile) (Object) this;
        if (!(self.getLevel() instanceof ServerLevel level)
                || storedPos == null
                || ArsNouveauCompat.sourceAt(level, storedPos) == null) return;
        ci.cancel();
        if (self.setSendTo(storedPos.immutable())) {
            PortUtil.sendMessage(player, Component.translatable(
                    "ars_nouveau.connections.send", DominionWand.getPosString(storedPos)));
            ParticleUtil.beam(storedPos, self.getBlockPos(), level);
        } else {
            PortUtil.sendMessage(player, Component.translatable("ars_nouveau.connections.fail"));
        }
    }

    @Inject(method = "onFinishedConnectionLast", at = @At("HEAD"), cancellable = true)
    private void immortalstorage$connectXianqiaoSource(
            @Nullable BlockPos storedPos,
            @Nullable LivingEntity storedEntity,
            Player player,
            CallbackInfo ci) {
        RelayTile self = (RelayTile) (Object) this;
        if (!(self.getLevel() instanceof ServerLevel level)
                || storedPos == null
                || ArsNouveauCompat.sourceAt(level, storedPos) == null) return;
        ci.cancel();
        if (self.setTakeFrom(storedPos.immutable())) {
            // A relay cannot use the same interface as both endpoints: it
            // would pull Source and immediately return it every relay tick.
            if (storedPos.equals(this.toPos)) {
                this.toPos = null;
                self.updateBlock();
            }
            PortUtil.sendMessage(player, Component.translatable(
                    "ars_nouveau.connections.take", DominionWand.getPosString(storedPos)));
        } else {
            PortUtil.sendMessage(player, Component.translatable("ars_nouveau.connections.fail"));
        }
    }

    @Inject(method = "setSendTo", at = @At("HEAD"), cancellable = true)
    private void immortalstorage$allowXianqiaoTarget(
            BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        RelayTile self = (RelayTile) (Object) this;
        if (!(self.getLevel() instanceof ServerLevel level)
                || ArsNouveauCompat.sourceAt(level, pos) == null) return;
        if (self.getBlockPos().closerThan(pos, self.getMaxDistance() + 0.001D)
                && !pos.equals(self.getBlockPos())) {
            this.toPos = pos.immutable();
            if (pos.equals(this.fromPos)) this.fromPos = null;
            self.updateBlock();
            cir.setReturnValue(true);
        } else {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void immortalstorage$transferWithXianqiao(CallbackInfo ci) {
        RelayTile self = (RelayTile) (Object) this;
        Level rawLevel = self.getLevel();
        if (!(rawLevel instanceof ServerLevel level) || disabled || level.getGameTime() % 20 != 0) return;
        ISourceTile from = sourceAt(level, fromPos);
        ISourceTile to = sourceAt(level, toPos);
        boolean custom = fromPos != null && ArsNouveauCompat.sourceAt(level, fromPos) != null
                || toPos != null && ArsNouveauCompat.sourceAt(level, toPos) != null;
        if (!custom) return;
        ci.cancel();
        if (fromPos != null && fromPos.equals(toPos)) {
            // Repair persisted states produced by the earlier callback mapping.
            toPos = null;
            to = null;
            self.updateBlock();
        }
        if (fromPos != null && from == null) {
            fromPos = null;
            self.updateBlock();
        } else if (from != null && self.transferSource(from, self) > 0) {
            self.updateBlock();
            ParticleUtil.spawnFollowProjectile(level, fromPos, self.getBlockPos(), self.getColor());
        }
        if (toPos != null && to == null) {
            toPos = null;
            self.updateBlock();
        } else if (to != null && self.transferSource(self, to) > 0) {
            ParticleUtil.spawnFollowProjectile(level, self.getBlockPos(), toPos, self.getColor());
        }
    }

    private static ISourceTile sourceAt(ServerLevel level, BlockPos pos) {
        if (pos == null || !level.isLoaded(pos)) return null;
        ISourceTile xianqiao = ArsNouveauCompat.sourceAt(level, pos);
        if (xianqiao != null) return xianqiao;
        return level.getBlockEntity(pos) instanceof AbstractSourceMachine machine ? machine : null;
    }
}
