package com.cultivation.cultivation.mixin.core;

import com.cultivation.cultivation.compat.merchant.MerchantStoragePayments;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.inventory.MerchantMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Extends the vanilla trade-selection path without changing merchant APIs. */
@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin {
    @Shadow @Final private Merchant trader;

    @Inject(method = "tryMoveItems", at = @At("TAIL"))
    private void cultivation$fillFromPersonalStorage(int offerIndex, CallbackInfo ci) {
        MerchantStoragePayments.fill((MerchantMenu) (Object) this, trader, offerIndex);
    }
}
