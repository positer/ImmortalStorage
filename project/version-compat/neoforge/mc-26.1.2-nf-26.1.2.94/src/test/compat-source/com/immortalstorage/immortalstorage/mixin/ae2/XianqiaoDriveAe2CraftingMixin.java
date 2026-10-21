package com.immortalstorage.immortalstorage.mixin.ae2;

import com.immortalstorage.immortalstorage.compat.ae2.XianqiaoAe2DriveCrafting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes a drive that holds a Xianqiao exchange disk also publish that owner's exchange patterns.
 *
 * <p>The formatting-only mixin half of the bridge: it owns no AE2 type and forwards the block
 * entity into {@link XianqiaoAe2DriveCrafting}, because the optional-AE2 boundary test forbids
 * {@code appeng} imports outside {@code compat/ae2}. The target is named as a string for the same
 * reason; {@code Ae2MixinConfigPlugin} refuses this mixin entirely when AE2 is absent, so the
 * string is never resolved without AE2 on the classpath.</p>
 *
 * <p><b>Why the constructor tail.</b> AE2's {@code NetworkCraftingProviders.addProvider} resolves
 * a crafting provider with {@code IGridNode.getService(ICraftingProvider.class)}, and
 * {@code ManagedGridNode.addService} throws {@code IllegalStateException} after
 * {@code create(...)} cleared its init data. The drive's node is created from
 * {@code AENetworkedBlockEntity.onReady()}, so the constructor tail is the last point where the
 * service can still be registered. Requiring zero lets a future signature change degrade to
 * "no recipes" instead of a mixin apply failure.</p>
 *
 * <p><b>Injection points (all verified present on the drive in both target lines):</b></p>
 * <ul>
 *   <li>{@code <init>} tail - attach the provider service before the node exists;</li>
 *   <li>{@code getCellInventory(int)} tail - recognise a Xianqiao disk in the inspected slot and
 *       mark the published list stale;</li>
 *   <li>{@code updateState} tail - the drive just rebuilt its per-slot watchers, which happens
 *       exactly when a cell was inserted or removed; rebuild and re-index the patterns.</li>
 * </ul>
 *
 * <p>The 20-tick cadence that makes a newly connected source vein visible comes from
 * {@code XianqiaoExchangeGridService}, which already reconciles drive cells on the grid tick and
 * now refreshes these providers in the same pass; a drive block entity has no server tick of its
 * own to hook.</p>
 */
@Mixin(targets = "appeng.blockentity.storage.DriveBlockEntity")
public abstract class XianqiaoDriveAe2CraftingMixin {
    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void immortalstorage$ae2AttachExchangeProvider(BlockEntityType<?> type, BlockPos pos,
            BlockState state, CallbackInfo callback) {
        XianqiaoAe2DriveCrafting.attach((BlockEntity) (Object) this);
    }

    @Inject(method = "getCellInventory", at = @At("TAIL"), require = 0)
    private void immortalstorage$ae2NoteCellRead(int slot, CallbackInfoReturnable<Object> callback) {
        XianqiaoAe2DriveCrafting.noteSlotRead((BlockEntity) (Object) this, slot);
    }

    @Inject(method = "updateState", at = @At("TAIL"), require = 0)
    private void immortalstorage$ae2RefreshExchangePatterns(CallbackInfo callback) {
        XianqiaoAe2DriveCrafting.noteStateRefreshed((BlockEntity) (Object) this);
    }
}
