package com.cultivation.cultivation.compat.emi;

import com.cultivation.cultivation.client.screen.XianqiaoInterfaceScreen;
import com.cultivation.cultivation.client.screen.XianqiaoInterfaceViewerConfiguration;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** EMI 1.1.24 drag targets for the nine mixed-resource configuration slots. */
final class XianqiaoInterfaceEmiGhostHandler
        extends EmiDragDropHandler.BoundsBased<XianqiaoInterfaceScreen> {
    XianqiaoInterfaceEmiGhostHandler() {
        super(XianqiaoInterfaceEmiGhostHandler::collectTargets);
    }

    private static void collectTargets(
            XianqiaoInterfaceScreen screen,
            BiConsumer<Bounds, Consumer<EmiIngredient>> consumer) {
        for (XianqiaoInterfaceViewerConfiguration.Target target
                : XianqiaoInterfaceViewerConfiguration.targets(screen)) {
            var area = target.area();
            consumer.accept(new Bounds(
                            area.getX(), area.getY(), area.getWidth(), area.getHeight()),
                    ingredient -> configure(screen, target.slot(), ingredient));
        }
    }

    private static void configure(
            XianqiaoInterfaceScreen screen, int slot, EmiIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return;
        for (EmiStack stack : ingredient.getEmiStacks()) {
            if (stack == null || stack.isEmpty()) continue;
            ItemStack item = stack.getItemStack();
            if (!item.isEmpty()) {
                XianqiaoInterfaceViewerConfiguration.configureItem(
                        screen, slot, item,
                        XianqiaoInterfaceViewerConfiguration.DEFAULT_ITEM_AMOUNT);
                return;
            }

            Fluid fluid = stack.getKeyOfType(Fluid.class);
            if (fluid != null && fluid != Fluids.EMPTY) {
                FluidStack fluidStack = new FluidStack(
                        BuiltInRegistries.FLUID.wrapAsHolder(fluid),
                        1,
                        stack.getComponentChanges());
                XianqiaoInterfaceViewerConfiguration.configureFluid(
                        screen, slot, fluidStack,
                        XianqiaoInterfaceViewerConfiguration.DEFAULT_FLUID_AMOUNT_MB);
                return;
            }
        }
    }
}
