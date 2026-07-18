package com.cultivation.cultivation.client.screen;

import com.cultivation.cultivation.menu.custom.XianqiaoInterfaceMenu;
import com.cultivation.cultivation.network.ModPayloads;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Dependency-neutral client bridge shared by optional recipe viewers.
 *
 * <p>The bridge deliberately derives targets only from the first nine menu
 * slots, which are the ghost configuration row. The following nine real cache
 * slots never enter this list. A drop only copies the resource identity into a
 * revision-checked C2S request; it never mutates or consumes a client stack.</p>
 */
public final class XianqiaoInterfaceViewerConfiguration {
    private static final int GHOST_SIZE = 16;
    /** Recipe-viewer item drops configure one item before amount editing. */
    public static final long DEFAULT_ITEM_AMOUNT = 1L;
    /** Recipe-viewer fluid drops configure one bucket before server clamping. */
    public static final long DEFAULT_FLUID_AMOUNT_MB = 1_000L;

    private XianqiaoInterfaceViewerConfiguration() {
    }

    /** Absolute screen-space target rectangles for exactly the configuration row. */
    public static List<Target> targets(XianqiaoInterfaceScreen screen) {
        if (screen == null || screen.isAmountDialogOpen() || screen.getMenu().slots.size()
                < XianqiaoInterfaceMenu.CONFIG_SLOT_COUNT) {
            return List.of();
        }
        List<Target> targets = new ArrayList<>(XianqiaoInterfaceMenu.CONFIG_SLOT_COUNT);
        for (int slotIndex = 0;
             slotIndex < XianqiaoInterfaceMenu.CONFIG_SLOT_COUNT;
             slotIndex++) {
            Slot slot = screen.getMenu().slots.get(slotIndex);
            targets.add(new Target(slotIndex, new Rect2i(
                    screen.getGuiLeft() + slot.x,
                    screen.getGuiTop() + slot.y,
                    GHOST_SIZE,
                    GHOST_SIZE)));
        }
        return List.copyOf(targets);
    }

    public static boolean configureItem(
            XianqiaoInterfaceScreen screen, int slot, ItemStack identity,
            long requestedAmount) {
        XianqiaoInterfaceMenu menu = validMenu(screen, slot);
        if (menu == null || identity == null || identity.isEmpty()) return false;
        PacketDistributor.sendToServer(new ModPayloads.SetXianqiaoInterfaceItemTarget(
                menu.containerId,
                menu.getBlockEntity().getBlockPos(),
                menu.getConfigRevision(),
                slot,
                identity.copyWithCount(1),
                positiveOrDefault(requestedAmount, DEFAULT_ITEM_AMOUNT)));
        return true;
    }

    public static boolean configureFluid(
            XianqiaoInterfaceScreen screen, int slot, FluidStack identity,
            long requestedAmountMb) {
        XianqiaoInterfaceMenu menu = validMenu(screen, slot);
        if (menu == null || identity == null || identity.isEmpty()) return false;
        PacketDistributor.sendToServer(new ModPayloads.SetXianqiaoInterfaceFluidTarget(
                menu.containerId,
                menu.getBlockEntity().getBlockPos(),
                menu.getConfigRevision(),
                slot,
                identity.copyWithAmount(1),
                positiveOrDefault(requestedAmountMb, DEFAULT_FLUID_AMOUNT_MB)));
        return true;
    }

    private static XianqiaoInterfaceMenu validMenu(
            XianqiaoInterfaceScreen screen, int slot) {
        if (screen == null || slot < 0
                || slot >= XianqiaoInterfaceMenu.CONFIG_SLOT_COUNT) return null;
        XianqiaoInterfaceMenu menu = screen.getMenu();
        return menu.getBlockEntity() == null ? null : menu;
    }

    private static long positiveOrDefault(long requested, long fallback) {
        return requested > 0L ? requested : fallback;
    }

    public record Target(int slot, Rect2i area) {
        public Target {
            if (slot < 0 || slot >= XianqiaoInterfaceMenu.CONFIG_SLOT_COUNT) {
                throw new IllegalArgumentException("configuration slot out of range: " + slot);
            }
            if (area == null) throw new IllegalArgumentException("target area is required");
        }
    }
}
