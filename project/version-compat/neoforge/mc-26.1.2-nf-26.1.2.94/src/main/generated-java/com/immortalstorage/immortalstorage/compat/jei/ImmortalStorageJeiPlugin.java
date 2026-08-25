package com.immortalstorage.immortalstorage.compat.jei;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.api.storage.terminal.CraftingTransferTarget;
import com.immortalstorage.immortalstorage.api.storage.terminal.SmithingTransferTarget;
import com.immortalstorage.immortalstorage.api.storage.terminal.StorageTerminalView;
import com.immortalstorage.immortalstorage.client.screen.KongqiaoScreen;
import com.immortalstorage.immortalstorage.client.screen.TerminalScreenAccess;
import com.immortalstorage.immortalstorage.client.screen.TerminalFluidScreenAccess;
import com.immortalstorage.immortalstorage.client.screen.XianqiaoStorageScreen;
import com.immortalstorage.immortalstorage.client.screen.XianqiaoInterfaceScreen;
import com.immortalstorage.immortalstorage.client.screen.AdvancedXianqiaoInterfaceScreen;
import com.immortalstorage.immortalstorage.menu.ModMenus;
import com.immortalstorage.immortalstorage.menu.custom.KongqiaoMenu;
import com.immortalstorage.immortalstorage.menu.custom.XianqiaoStorageMenu;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IClickableIngredientFactory;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import com.immortalstorage.immortalstorage.config.ImmortalStorageClientConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Optional;

@JeiPlugin
public final class ImmortalStorageJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(ImmortalStorageMod.MODID, "terminal_jei");
    private static IJeiRuntime runtime;

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        com.immortalstorage.immortalstorage.compat.mc2612.TargetClientRecipeCache.bindJei(ImmortalStorageJeiPlugin::refreshImmortalFurnaceRecipes);
    }

    @Override
    public void onRuntimeUnavailable() {
        com.immortalstorage.immortalstorage.compat.mc2612.TargetClientRecipeCache.unbindJei();
        runtime = null;
    }

    public static void setSearchText(String text) {
        if (runtime != null && ImmortalStorageClientConfig.SYNC_RECIPE_VIEWER_SEARCH.get()) {
            runtime.getIngredientFilter().setFilterText(text == null ? "" : text);
        }
    }

    public static String getSearchText() {
        return runtime == null ? null : runtime.getIngredientFilter().getFilterText();
    }

    public static boolean isSearchFocused() {
        return runtime != null && runtime.getIngredientListOverlay().hasKeyboardFocus();
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(XianqiaoStorageScreen.class, new TerminalGuiHandler<>());
        registration.addGuiContainerHandler(XianqiaoInterfaceScreen.class,
                new XianqiaoInterfaceJeiGuiHandler());
        registration.addGuiContainerHandler(AdvancedXianqiaoInterfaceScreen.class,
                new AdvancedXianqiaoInterfaceJeiGuiHandler());
        registration.addGhostIngredientHandler(XianqiaoInterfaceScreen.class,
                new XianqiaoInterfaceJeiGhostHandler());
        registration.addGhostIngredientHandler(
                com.immortalstorage.immortalstorage.client.screen.StabilizedMiniatureImmortalRuinScreen.class,
                new StabilizedRuinJeiGhostHandler());
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ImmortalFurnaceJeiCategory(
                registration.getJeiHelpers().getGuiHelper()), new SimulatedReincarnationJeiCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ImmortalFurnaceJeiCategory.TYPE,
                com.immortalstorage.immortalstorage.compat.mc2612.TargetClientRecipeCache.immortalFurnaceRecipes());
        registration.addRecipes(SimulatedReincarnationJeiCategory.TYPE,
                net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                        .filter(net.minecraft.world.item.SpawnEggItem.class::isInstance)
                        .map(item -> new SimulatedReincarnationJeiCategory.Entry(new ItemStack(item))).toList());
    }
    public static void refreshImmortalFurnaceRecipes(java.util.List<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.AbstractCookingRecipe>> recipes) {
        if (runtime != null) runtime.getRecipeManager().addRecipes(ImmortalFurnaceJeiCategory.TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(com.immortalstorage.immortalstorage.block.ModBlocks.IMMORTAL_FURNACE.get(),
                ImmortalFurnaceJeiCategory.TYPE);
        registration.addRecipeCatalyst(com.immortalstorage.immortalstorage.block.ModBlocks.SIMULATED_REINCARNATION_FURNACE.get(),
                SimulatedReincarnationJeiCategory.TYPE);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        IRecipeTransferHandlerHelper helper = registration.getTransferHelper();
        registration.addRecipeTransferHandler(new TerminalTransferHandler<>(
                KongqiaoMenu.class, ModMenus.KONGQIAO.get(), helper), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new TerminalTransferHandler<>(
                XianqiaoStorageMenu.class, ModMenus.XIANQIAO_STORAGE.get(), helper), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new TerminalSmithingHandler<>(
                KongqiaoMenu.class, ModMenus.KONGQIAO.get(), helper), RecipeTypes.SMITHING);
        registration.addRecipeTransferHandler(new TerminalSmithingHandler<>(
                XianqiaoStorageMenu.class, ModMenus.XIANQIAO_STORAGE.get(), helper), RecipeTypes.SMITHING);
    }

    private static final class TerminalGuiHandler<S extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>>
            implements IGuiContainerHandler<S> {
        @Override
        public Optional<? extends mezz.jei.api.runtime.IClickableIngredient<?>> getClickableIngredientUnderMouse(
                IClickableIngredientFactory factory, S screen, double mouseX, double mouseY) {
            if (screen instanceof TerminalFluidScreenAccess fluids) {
                Optional<TerminalFluidScreenAccess.FluidHover> hover =
                        fluids.immortalstorage$getFluidAt(mouseX, mouseY);
                if (hover.isPresent()) {
                    TerminalFluidScreenAccess.FluidHover fluid = hover.get();
                    return factory.createBuilder(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK, fluid.stack())
                            .buildWithArea(fluid.bounds());
                }
            }
            Slot slot = ((TerminalScreenAccess) screen).immortalstorage$getSlotAt(mouseX, mouseY);
            if (slot == null || !slot.hasItem()) {
                return Optional.empty();
            }
            return factory.createBuilder(slot.getItem()).buildWithArea(
                    ((TerminalScreenAccess) screen).immortalstorage$getSlotBounds(slot));
        }
    }

    private static final class TerminalTransferHandler<M extends AbstractContainerMenu & StorageTerminalView & CraftingTransferTarget>
            implements IRecipeTransferHandler<M, RecipeHolder<CraftingRecipe>> {
        private final Class<M> menuClass;
        private final MenuType<M> menuType;
        private final IRecipeTransferHandlerHelper helper;

        private TerminalTransferHandler(Class<M> menuClass, MenuType<M> menuType, IRecipeTransferHandlerHelper helper) {
            this.menuClass = menuClass;
            this.menuType = menuType;
            this.helper = helper;
        }

        @Override public Class<? extends M> getContainerClass() { return menuClass; }
        @Override public Optional<MenuType<M>> getMenuType() { return Optional.of(menuType); }
        @Override public mezz.jei.api.recipe.types.IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() { return RecipeTypes.CRAFTING; }

        @Override
        public IRecipeTransferError transferRecipe(M menu, RecipeHolder<CraftingRecipe> recipe,
                                                   IRecipeSlotsView recipeSlots, net.minecraft.world.entity.player.Player player,
                                                   boolean maxTransfer, boolean doTransfer) {
            if (!menu.isCraftingUnlocked() || !menu.isCraftingVisible()) {
                return helper.createUserErrorWithTooltip(
                        Component.translatable("container.immortalstorage.terminal.recipe_transfer_locked"));
            }
            if (recipeSlots.getSlotViews(RecipeIngredientRole.INPUT).size() > 9) {
                return helper.createInternalError();
            }
            if (doTransfer) {
                ClientPacketDistributor.sendToServer(new com.immortalstorage.immortalstorage.network.ModPayloads.TransferTerminalRecipe(
                        menu.containerId, menu.viewport().revision(), recipe.id().identifier(), maxTransfer ? 64 : 1));
            }
            return null;
        }
    }

    private static final class TerminalSmithingHandler<M extends AbstractContainerMenu & StorageTerminalView & SmithingTransferTarget>
            implements IRecipeTransferHandler<M, RecipeHolder<SmithingRecipe>> {
        private final Class<M> menuClass;
        private final MenuType<M> menuType;
        private final IRecipeTransferHandlerHelper helper;
        private TerminalSmithingHandler(Class<M> menuClass, MenuType<M> menuType, IRecipeTransferHandlerHelper helper) {
            this.menuClass = menuClass; this.menuType = menuType; this.helper = helper;
        }
        @Override public Class<? extends M> getContainerClass() { return menuClass; }
        @Override public Optional<MenuType<M>> getMenuType() { return Optional.of(menuType); }
        @Override public mezz.jei.api.recipe.types.IRecipeType<RecipeHolder<SmithingRecipe>> getRecipeType() { return RecipeTypes.SMITHING; }
        @Override public IRecipeTransferError transferRecipe(M menu, RecipeHolder<SmithingRecipe> recipe,
                IRecipeSlotsView recipeSlots, net.minecraft.world.entity.player.Player player,
                boolean maxTransfer, boolean doTransfer) {
            if (!menu.isSmithingUnlocked() || !menu.isSmithingVisible()) return helper.createUserErrorWithTooltip(
                    Component.translatable("container.immortalstorage.terminal.recipe_transfer_locked"));
            if (doTransfer) ClientPacketDistributor.sendToServer(new com.immortalstorage.immortalstorage.network.ModPayloads.TransferTerminalRecipe(
                    menu.containerId, menu.viewport().revision(), recipe.id().identifier(), 1));
            return null;
        }
    }
}
