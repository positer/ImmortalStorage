package com.immortalstorage.immortalstorage.mixin.arsnouveau;

import com.immortalstorage.immortalstorage.block.entity.CrystalKind;
import com.immortalstorage.immortalstorage.block.entity.EnergyCrystalBlockEntity;
import com.hollingsworth.arsnouveau.api.item.IWandable;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes only the optional Ars Source crystal participate in Dominion Wand's
 * persisted first/last endpoint protocol.  The mixin is not shipped in the
 * 26.1.2 common source set because that target has no audited Ars API.
 */
@Mixin(value = EnergyCrystalBlockEntity.class)
abstract class EnergyCrystalArsWandableMixin implements IWandable {
    private EnergyCrystalBlockEntity immortalstorage$self() {
        return (EnergyCrystalBlockEntity) (Object) this;
    }

    @Override public IWandable.Result onFirstConnection(
            GlobalPos storedPos, Direction direction, LivingEntity storedEntity,
            Player player) {
        EnergyCrystalBlockEntity self = immortalstorage$self();
        if (self.kind() != CrystalKind.SOURCE) return IWandable.Result.NONE;
        self.markSourceStart();
        return IWandable.Result.SUCCESS;
    }

    @Override public IWandable.Result onLastConnection(
            GlobalPos storedPos, Direction direction, LivingEntity storedEntity,
            Player player) {
        EnergyCrystalBlockEntity self = immortalstorage$self();
        if (self.kind() != CrystalKind.SOURCE) return IWandable.Result.NONE;
        self.markSourcePriority();
        return IWandable.Result.SUCCESS;
    }

    @Override public IWandable.Result onClearConnections(Player player) {
        EnergyCrystalBlockEntity self = immortalstorage$self();
        if (self.kind() != CrystalKind.SOURCE) return IWandable.Result.NONE;
        self.clearSourceDesignation();
        return IWandable.Result.CLEAR;
    }
}
