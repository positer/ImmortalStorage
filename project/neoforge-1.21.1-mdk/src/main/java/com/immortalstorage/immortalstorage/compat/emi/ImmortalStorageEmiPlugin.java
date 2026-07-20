package com.immortalstorage.immortalstorage.compat.emi;

import com.immortalstorage.immortalstorage.api.storage.terminal.CraftingTransferTarget;
import com.immortalstorage.immortalstorage.api.storage.terminal.StorageTerminalView;
import com.immortalstorage.immortalstorage.client.screen.KongqiaoScreen;
import com.immortalstorage.immortalstorage.client.screen.TerminalScreenAccess;
import com.immortalstorage.immortalstorage.client.screen.TerminalFluidScreenAccess;
import com.immortalstorage.immortalstorage.client.screen.XianqiaoStorageScreen;
import com.immortalstorage.immortalstorage.client.screen.XianqiaoInterfaceScreen;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import com.immortalstorage.immortalstorage.menu.custom.KongqiaoMenu;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoInterfaceMenu;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoStorageMenu;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EmiEntrypoint
public final class ImmortalStorageEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(ImmortalFurnaceEmiRecipe.CATEGORY);
        registry.addWorkstation(ImmortalFurnaceEmiRecipe.CATEGORY,
                EmiStack.of(com.immortalstorage.immortalstorage.block.ModBlocks.IMMORTAL_FURNACE.get()));
        for (var holder : registry.getRecipeManager().getAllRecipesFor(
                com.immortalstorage.immortalstorage.recipe.ModRecipes.IMMORTAL_FURNACE_TYPE.get())) {
            registry.addRecipe(new ImmortalFurnaceEmiRecipe(holder));
        }
        registerScreen(registry, KongqiaoScreen.class);
        registerScreen(registry, XianqiaoStorageScreen.class);
        registry.addDragDropHandler(XianqiaoInterfaceScreen.class,
                new XianqiaoInterfaceEmiGhostHandler());
        registry.addStackProvider(XianqiaoInterfaceScreen.class, (screen, mouseX, mouseY) -> {
            if (screen.isAmountDialogOpen()) return EmiStackInteraction.EMPTY;
            var fluid = screen.immortalstorage$getFluidAt(mouseX, mouseY);
            if (fluid.isPresent()) {
                var hover = fluid.get();
                return TerminalEmiInteraction.lookupOnly(
                        dev.emi.emi.api.neoforge.NeoForgeEmiStack.of(hover.stack())
                                .setAmount(hover.amountMb()));
            }

            int visibleSlots = Math.min(XianqiaoInterfaceMenu.PLAYER_START,
                    screen.getMenu().slots.size());
            for (int slotIndex = 0; slotIndex < visibleSlots; slotIndex++) {
                int resourceSlot = slotIndex % XianqiaoInterfaceMenu.CONFIG_SLOT_COUNT;
                if (screen.getMenu().isFluidTarget(resourceSlot)) continue;
                Slot slot = screen.getMenu().slots.get(slotIndex);
                if (!slot.hasItem()) continue;
                int x = screen.getGuiLeft() + slot.x;
                int y = screen.getGuiTop() + slot.y;
                if (mouseX < x || mouseX >= x + 16 || mouseY < y || mouseY >= y + 16) continue;
                long amount = slotIndex < XianqiaoInterfaceMenu.CONFIG_SLOT_COUNT
                        ? screen.getMenu().getConfiguredAmount(resourceSlot)
                        : screen.getMenu().getCachedAmount(resourceSlot);
                return TerminalEmiInteraction.lookupOnly(
                        EmiStack.of(slot.getItem().copyWithCount(1))
                                .setAmount(Math.max(1L, amount)));
            }
            return EmiStackInteraction.EMPTY;
        });
        registry.addRecipeHandler(ModMenus.KONGQIAO.get(), new TerminalRecipeHandler<KongqiaoMenu>());
        registry.addRecipeHandler(ModMenus.XIANQIAO_STORAGE.get(), new TerminalRecipeHandler<XianqiaoStorageMenu>());
    }

    private static <S extends AbstractContainerScreen<?>> void registerScreen(
            EmiRegistry registry, Class<S> screenClass) {
        registry.addExclusionArea(screenClass, (screen, consumer) -> {
            for (net.minecraft.client.renderer.Rect2i area : ((TerminalScreenAccess) screen).immortalstorage$getExtraAreas()) {
                consumer.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
            }
        });
        registry.addStackProvider(screenClass, (screen, mouseX, mouseY) -> {
            if (screen instanceof TerminalFluidScreenAccess fluids) {
                var hover = fluids.immortalstorage$getFluidAt(mouseX, mouseY);
                if (hover.isPresent()) {
                    var fluid = hover.get();
                    return TerminalEmiInteraction.lookupOnly(
                            dev.emi.emi.api.neoforge.NeoForgeEmiStack.of(fluid.stack())
                                    .setAmount(fluid.amountMb()));
                }
            }
            Slot slot = ((TerminalScreenAccess) screen).immortalstorage$getSlotAt(mouseX, mouseY);
            return slot == null || !slot.hasItem()
                    ? EmiStackInteraction.EMPTY
                    : TerminalEmiInteraction.lookupOnly(EmiStack.of(slot.getItem()));
        });
    }

    private static final class TerminalRecipeHandler<M extends AbstractContainerMenu & StorageTerminalView & CraftingTransferTarget>
            implements StandardRecipeHandler<M> {
        @Override
        public EmiPlayerInventory getInventory(AbstractContainerScreen<M> screen) {
            List<EmiStack> stacks = new java.util.ArrayList<>(getInputSources(screen.getMenu()).stream()
                    .filter(Slot::hasItem)
                    .map(slot -> EmiStack.of(slot.getItem()))
                    .toList());
            for (CraftingTransferTarget.TransferIngredient entry : screen.getMenu().craftingStorageIngredients()) {
                stacks.add(EmiStack.of(entry.stack(), entry.amount()));
            }
            return new EmiPlayerInventory(stacks);
        }

        @Override
        public List<Slot> getInputSources(M menu) {
            List<Slot> sources = new java.util.ArrayList<>(menu.craftingSourceSlots());
            sources.addAll(menu.craftingInputSlots());
            return List.copyOf(sources);
        }

        @Override
        public List<Slot> getCraftingSlots(M menu) {
            return menu.isCraftingUnlocked() && menu.isCraftingVisible() ? menu.craftingInputSlots() : List.of();
        }

        @Override
        public Slot getOutputSlot(M menu) {
            return menu.craftingResultSlotView();
        }

        @Override
        public boolean supportsRecipe(EmiRecipe recipe) {
            return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING
                    && recipe.getBackingRecipe() instanceof RecipeHolder<?> holder
                    && holder.value() instanceof CraftingRecipe;
        }

        @Override
        public boolean canCraft(EmiRecipe recipe, EmiCraftContext<M> context) {
            return context.getScreenHandler().isCraftingUnlocked()
                    && context.getScreenHandler().isCraftingVisible()
                    && context.getInventory().canCraft(recipe, Math.max(1, context.getAmount()));
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean craft(EmiRecipe recipe, EmiCraftContext<M> context) {
            if (!supportsRecipe(recipe) || !canCraft(recipe, context)) return false;
            RecipeHolder<CraftingRecipe> holder = (RecipeHolder<CraftingRecipe>) recipe.getBackingRecipe();
            M menu = context.getScreenHandler();
            PacketDistributor.sendToServer(new com.immortalstorage.immortalstorage.network.ModPayloads.TransferTerminalRecipe(
                    menu.containerId, menu.viewport().revision(), holder.id(), Math.max(1, context.getAmount())));
            return true;
        }
    }
}
