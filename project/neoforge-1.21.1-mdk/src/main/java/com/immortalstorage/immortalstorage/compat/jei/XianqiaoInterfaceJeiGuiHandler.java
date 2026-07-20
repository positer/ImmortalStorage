package com.immortalstorage.immortalstorage.compat.jei;

import com.immortalstorage.immortalstorage.client.screen.TerminalFluidScreenAccess;
import com.immortalstorage.immortalstorage.client.screen.XianqiaoInterfaceScreen;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoInterfaceMenu;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.runtime.IClickableIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * JEI R/U lookup surface for both configuration identities and real cache
 * identities. This handler only exposes clicks; ghost drag/drop remains
 * restricted to the first nine configuration slots by the separate handler.
 */
final class XianqiaoInterfaceJeiGuiHandler
        implements IGuiContainerHandler<XianqiaoInterfaceScreen> {
    @Override
    public Optional<? extends IClickableIngredient<?>> getClickableIngredientUnderMouse(
            IClickableIngredientFactory factory, XianqiaoInterfaceScreen screen,
            double mouseX, double mouseY) {
        if (screen.isAmountDialogOpen()) return Optional.empty();
        Optional<TerminalFluidScreenAccess.FluidHover> fluid =
                screen.immortalstorage$getFluidAt(mouseX, mouseY);
        if (fluid.isPresent()) {
            TerminalFluidScreenAccess.FluidHover hover = fluid.get();
            return factory.createBuilder(NeoForgeTypes.FLUID_STACK, hover.stack())
                    .buildWithArea(hover.bounds());
        }

        int visibleSlots = Math.min(XianqiaoInterfaceMenu.PLAYER_START,
                screen.getMenu().slots.size());
        for (int slotIndex = 0; slotIndex < visibleSlots; slotIndex++) {
            int resourceSlot = slotIndex % XianqiaoInterfaceMenu.CONFIG_SLOT_COUNT;
            if (screen.getMenu().isFluidTarget(resourceSlot)) continue;
            Slot slot = screen.getMenu().slots.get(slotIndex);
            ItemStack identity = slot.getItem();
            if (identity.isEmpty()) continue;
            Rect2i area = new Rect2i(screen.getGuiLeft() + slot.x,
                    screen.getGuiTop() + slot.y, 16, 16);
            if (!contains(area, mouseX, mouseY)) continue;
            return factory.createBuilder(identity.copyWithCount(1)).buildWithArea(area);
        }
        return Optional.empty();
    }

    private static boolean contains(Rect2i area, double x, double y) {
        return x >= area.getX() && x < area.getX() + area.getWidth()
                && y >= area.getY() && y < area.getY() + area.getHeight();
    }
}
