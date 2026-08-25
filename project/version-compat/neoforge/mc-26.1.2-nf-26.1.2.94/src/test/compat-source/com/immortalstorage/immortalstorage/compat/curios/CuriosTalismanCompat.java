package com.immortalstorage.immortalstorage.compat.curios;

import com.immortalstorage.immortalstorage.item.ModItems;
import net.minecraft.world.entity.LivingEntity;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Official Curios bridge, loaded only when Curios is present. */
public final class CuriosTalismanCompat {
    private static final String CHARM_SLOT = "charm";

    private CuriosTalismanCompat() {
    }

    public static void initialize() {
        CuriosApi.registerCurio(ModItems.IMMORTAL_MASTER_TALISMAN.get(), new ICurioItem() {
            @Override
            public boolean canEquip(SlotContext slotContext, net.minecraft.world.item.ItemStack stack) {
                return CHARM_SLOT.equals(slotContext.identifier());
            }
        });
    }

    public static boolean isEquipped(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(handler -> handler.findFirstCurio(ModItems.IMMORTAL_MASTER_TALISMAN.get()))
                .filter(result -> CHARM_SLOT.equals(result.slotContext().identifier()))
                .isPresent();
    }
}
