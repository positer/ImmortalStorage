package com.cultivation.cultivation.compat.jei;

import com.cultivation.cultivation.client.screen.XianqiaoInterfaceScreen;
import com.cultivation.cultivation.client.screen.XianqiaoInterfaceViewerConfiguration;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.neoforge.NeoForgeTypes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** JEI 19.37 ghost targets for the nine mixed-resource configuration slots. */
final class XianqiaoInterfaceJeiGhostHandler
        implements IGhostIngredientHandler<XianqiaoInterfaceScreen> {
    @Override
    public <I> List<Target<I>> getTargetsTyped(
            XianqiaoInterfaceScreen screen, ITypedIngredient<I> ingredient,
            boolean doStart) {
        ItemStack item = ingredient.getIngredient(VanillaTypes.ITEM_STACK)
                .orElse(ItemStack.EMPTY);
        if (!item.isEmpty()) {
            ItemStack identity = item.copy();
            return targets(screen, slot ->
                    XianqiaoInterfaceViewerConfiguration.configureItem(
                            screen, slot, identity,
                            XianqiaoInterfaceViewerConfiguration.DEFAULT_ITEM_AMOUNT));
        }

        FluidStack fluid = ingredient.getIngredient(NeoForgeTypes.FLUID_STACK)
                .orElse(FluidStack.EMPTY);
        if (!fluid.isEmpty()) {
            FluidStack identity = fluid.copy();
            return targets(screen, slot ->
                    XianqiaoInterfaceViewerConfiguration.configureFluid(
                            screen, slot, identity,
                            XianqiaoInterfaceViewerConfiguration.DEFAULT_FLUID_AMOUNT_MB));
        }
        return List.of();
    }

    @Override
    public void onComplete() {
        // Each accepted target already sent one authoritative configuration request.
    }

    private static <I> List<Target<I>> targets(
            XianqiaoInterfaceScreen screen, IntConsumer configure) {
        List<Target<I>> targets = new ArrayList<>();
        for (XianqiaoInterfaceViewerConfiguration.Target target
                : XianqiaoInterfaceViewerConfiguration.targets(screen)) {
            targets.add(new Target<>() {
                @Override
                public net.minecraft.client.renderer.Rect2i getArea() {
                    return target.area();
                }

                @Override
                public void accept(I ignored) {
                    configure.accept(target.slot());
                }
            });
        }
        return List.copyOf(targets);
    }
}
