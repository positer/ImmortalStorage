package com.immortalstorage.immortalstorage.mixin.ae2;

import com.immortalstorage.immortalstorage.block.entity.XianqiaoManagerBlockEntity;
import com.immortalstorage.immortalstorage.compat.ae2.XianqiaoAe2GridNodes;
import com.immortalstorage.immortalstorage.compat.ae2.XianqiaoAe2NodeAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * AE2 lifecycle for the Xianqiao Manager, applied only when AE2 is loaded
 * ({@link Ae2MixinConfigPlugin}).
 *
 * <p>Discovery itself does not need this mixin: AE2 19.2.17 resolves in-world node hosts through
 * {@code AECapabilities.IN_WORLD_GRID_NODE_HOST} ({@code GridHelper.getNodeHost} bytecode), which
 * {@code XianqiaoAe2GridNodes.registerCapabilities} registers. This mixin supplies the parts a
 * capability cannot: node creation, destruction and NBT persistence.</p>
 *
 * <p>Injection points and why they were chosen (all verified present on
 * {@code XianqiaoManagerBlockEntity}; it does not override {@code onLoad} or
 * {@code onChunkUnloaded}, so Mixin cannot target those):</p>
 * <ul>
 *   <li>{@code <init>} TAIL - allocate the host object, no level needed yet;</li>
 *   <li>{@code serverTick} HEAD - lazy {@code IManagedGridNode.create(Level, BlockPos)}. This is the
 *       manager's existing every-tick server hook ({@code XianqiaoManagerBlock#getTicker}); the
 *       modulo gate inside the method body is after HEAD, so HEAD really does run every tick;</li>
 *   <li>{@code setRemoved} TAIL - {@code IManagedGridNode.destroy()};</li>
 *   <li>{@code saveAdditional} / {@code loadAdditional} TAIL - {@code saveToNBT} / {@code loadFromNBT}.
 *       Loading before the node exists is safe: {@code ManagedGridNode.loadFromNBT} parks the tag in
 *       its {@code InitData} (verified bytecode).</li>
 * </ul>
 *
 * <p><b>Unverified risk:</b> chunk unload. AE2's own block entities also destroy the node in
 * {@code onChunkUnloaded}, which this class cannot target (the method is not declared by the target).
 * If vanilla does not call {@code setRemoved()} while unloading a chunk, the node outlives the chunk
 * until the block entity is removed; the tick hook re-creates and the destroy guard keeps it
 * idempotent, but a stale node in the grid until then is possible. Fix if it shows up: add an
 * {@code onChunkUnloaded} override to the block entity itself (a one-line hook in a shared file).</p>
 */
@Mixin(XianqiaoManagerBlockEntity.class)
public abstract class XianqiaoManagerAe2NodeMixin implements XianqiaoAe2NodeAccess {
    @Unique
    private @Nullable XianqiaoAe2GridNodes.Host immortalstorage$ae2Host;

    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void immortalstorage$ae2Attach(BlockPos pos, BlockState state, CallbackInfo callback) {
        this.immortalstorage$ae2Host =
                XianqiaoAe2GridNodes.create((XianqiaoManagerBlockEntity) (Object) this);
    }

    @Inject(method = "serverTick", at = @At("HEAD"), require = 0)
    private static void immortalstorage$ae2Tick(ServerLevel level, BlockPos pos,
            XianqiaoManagerBlockEntity manager, CallbackInfo callback) {
        XianqiaoAe2GridNodes.tick(manager);
    }

    @Inject(method = "setRemoved", at = @At("TAIL"), require = 0)
    private void immortalstorage$ae2Detach(CallbackInfo callback) {
        XianqiaoAe2GridNodes.detach((XianqiaoManagerBlockEntity) (Object) this);
    }

    // The persistence hook pair differs between the two target lines: the older line keeps the
    // plain names and the 26.1.2 line renames them to their *Legacy forms.
    @Inject(method = "saveAdditional", at = @At("TAIL"), require = 0)
    private void immortalstorage$ae2Save(CompoundTag tag, HolderLookup.Provider registries,
            CallbackInfo callback) {
        XianqiaoAe2GridNodes.save((XianqiaoManagerBlockEntity) (Object) this, tag);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"), require = 0)
    private void immortalstorage$ae2Load(CompoundTag tag, HolderLookup.Provider registries,
            CallbackInfo callback) {
        XianqiaoAe2GridNodes.load((XianqiaoManagerBlockEntity) (Object) this, tag);
    }


    @Override
    public @Nullable XianqiaoAe2GridNodes.Host immortalstorage$ae2NodeHost() {
        return this.immortalstorage$ae2Host;
    }

    @Override
    public void immortalstorage$ae2SetNodeHost(@Nullable XianqiaoAe2GridNodes.Host host) {
        this.immortalstorage$ae2Host = host;
    }
}
