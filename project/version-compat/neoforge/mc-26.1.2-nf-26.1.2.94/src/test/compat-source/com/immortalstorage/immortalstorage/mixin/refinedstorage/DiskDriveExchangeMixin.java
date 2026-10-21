package com.immortalstorage.immortalstorage.mixin.refinedstorage;

import com.immortalstorage.immortalstorage.compat.refinedstorage.RsDriveExchange;
import com.refinedmods.refinedstorage.common.storage.AbstractDiskContainerBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hands every Refined Storage disk drive to {@link RsDriveExchange} once, when it enters a level.
 *
 * <h2>Why {@code setLevel} and not the slot-changed hook</h2>
 * <p>{@code AbstractDiskContainerBlockEntity} is the drive's storage container base on both RS
 * lines (2.0.9 and 3.2.1). The obvious "a disk was inserted/removed" hook is
 * {@code onDiskChanged}, but javap shows it is <b>not</b> compatible across the two lines:
 * 2.0.9 declares {@code private void onDiskChanged(int)} while 3.2.1 declares
 * {@code private void onDiskChanged()} - different descriptors, so one injection handler cannot
 * match both. {@code public void setLevel(net.minecraft.world.level.Level)} is declared by the same
 * class in both jars with an identical descriptor and is the only life-cycle method the two lines
 * share, so it is used for registration only; the disk slots themselves are re-read from the
 * server tick by {@link RsDriveExchange}.</p>
 *
 * <p>The injection is {@code require = 0}: if a future RS line moves or renames {@code setLevel},
 * this mixin is skipped and the drive simply keeps its storage-only behaviour instead of failing
 * the whole mixin config (which is {@code required: false} and shares
 * {@code injectors.defaultRequire: 1} with the storage mixins).</p>
 */
@Mixin(value = AbstractDiskContainerBlockEntity.class, remap = false)
public abstract class DiskDriveExchangeMixin {
    @Inject(method = "setLevel", at = @At("TAIL"), require = 0)
    private void immortalstorage$registerDiskDrive(Level level, CallbackInfo callback) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        if (blockEntity.isRemoved()) return;
        RsDriveExchange.note(blockEntity, serverLevel);
    }
}
