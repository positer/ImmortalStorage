package com.immortalstorage.immortalstorage.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import java.util.List;

/**
 * 26.1.2 target override: vanilla stonecutter result grid, scrollbar and
 * selection reproduced inside the terminal smithing module workspace.  The
 * result icon uses assemble() because 26.1 removed getResultItem(registry);
 * tooltips route through CompatGui for the deferred GUI extractor.
 */
final class TerminalStonecutterGui {
    private static final Identifier SCROLLER = Identifier.withDefaultNamespace("container/stonecutter/scroller");
    private static final Identifier SCROLLER_DISABLED = Identifier.withDefaultNamespace("container/stonecutter/scroller_disabled");
    private static final Identifier RECIPE_SELECTED = Identifier.withDefaultNamespace("container/stonecutter/recipe_selected");
    private static final Identifier RECIPE_HIGHLIGHTED = Identifier.withDefaultNamespace("container/stonecutter/recipe_highlighted");
    private static final Identifier RECIPE = Identifier.withDefaultNamespace("container/stonecutter/recipe");

    private static final int COLUMNS = 4;
    private static final int ROWS = 3;
    private static final int IMAGE_W = 16;
    private static final int IMAGE_H = 18;
    private static final int GRID_TOP_OFFSET = 19;

    private float scrollOffs;
    private boolean scrolling;
    private int startIndex;

    void reset() {
        scrollOffs = 0.0F;
        startIndex = 0;
        scrolling = false;
    }

    private static ItemStack resultIcon(RecipeHolder<StonecutterRecipe> holder) {
        return holder.value().assemble(new SingleRecipeInput(ItemStack.EMPTY));
    }

    void render(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, int absoluteSlotY,
                int mouseX, int mouseY, int selectedIndex, List<RecipeHolder<StonecutterRecipe>> recipes) {
        int left = screen.getGuiLeft();
        int gridTop = absoluteSlotY - GRID_TOP_OFFSET;
        int recipesX = left + 52;

        int offscreenRows = Math.max(0, offscreenRows(recipes.size()));
        scrollOffs = Mth.clamp(scrollOffs, 0.0F, 1.0F);
        startIndex = (int) ((double) (scrollOffs * (float) offscreenRows) + 0.5D) * COLUMNS;

        boolean active = recipes.size() > 12;
        int scrollerK = (int) (41.0F * scrollOffs);
        com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitSprite(graphics, active ? SCROLLER : SCROLLER_DISABLED,
                left + 119, gridTop + 1 + scrollerK, 12, 15);

        int lastVisible = startIndex + 12;
        for (int i = startIndex; i < lastVisible && i < recipes.size(); i++) {
            int relative = i - startIndex;
            int x = recipesX + relative % COLUMNS * IMAGE_W;
            int y = gridTop + relative / COLUMNS * IMAGE_H + 1;
            Identifier background;
            if (i == selectedIndex) {
                background = RECIPE_SELECTED;
            } else if (mouseX >= x && mouseY >= y && mouseX < x + IMAGE_W && mouseY < y + IMAGE_H) {
                background = RECIPE_HIGHLIGHTED;
            } else {
                background = RECIPE;
            }
            com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.blitSprite(graphics, background, x, y, IMAGE_W, IMAGE_H);
            graphics.item(resultIcon(recipes.get(i)), x, y);
        }
    }

    boolean renderTooltip(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, int absoluteSlotY,
                          int mouseX, int mouseY, List<RecipeHolder<StonecutterRecipe>> recipes) {
        int left = screen.getGuiLeft();
        int gridTop = absoluteSlotY - GRID_TOP_OFFSET;
        for (int i = startIndex; i < startIndex + 12 && i < recipes.size(); i++) {
            int relative = i - startIndex;
            int x = left + 52 + relative % COLUMNS * IMAGE_W;
            int y = gridTop + relative / COLUMNS * IMAGE_H + 1;
            if (mouseX >= x && mouseX < x + IMAGE_W && mouseY >= y && mouseY < y + IMAGE_H) {
                Minecraft mc = Minecraft.getInstance();
                ItemStack result = resultIcon(recipes.get(i));
                com.immortalstorage.immortalstorage.compat.mc2612.CompatGui.renderTooltip(
                        graphics, mc.font, result.getHoverName(), mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    boolean mouseClicked(double mouseX, double mouseY, AbstractContainerScreen<?> screen,
                         int absoluteSlotY, List<RecipeHolder<StonecutterRecipe>> recipes) {
        scrolling = false;
        int left = screen.getGuiLeft();
        int gridTop = absoluteSlotY - GRID_TOP_OFFSET;
        int lastVisible = startIndex + 12;
        for (int i = startIndex; i < lastVisible && i < recipes.size(); i++) {
            int relative = i - startIndex;
            double dx = mouseX - (double) (left + 52 + relative % COLUMNS * IMAGE_W);
            double dy = mouseY - (double) (gridTop + relative / COLUMNS * IMAGE_H + 1);
            if (dx >= 0.0D && dy >= 0.0D && dx < IMAGE_W && dy < IMAGE_H) {
                Minecraft mc = Minecraft.getInstance();
                mc.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                mc.gameMode.handleInventoryButtonClick(screen.getMenu().containerId, i + 20);
                return true;
            }
        }
        int scrollerX = left + 119;
        int scrollerY = gridTop - 5;
        if (recipes.size() > 12 && mouseX >= scrollerX && mouseX < scrollerX + 12
                && mouseY >= scrollerY && mouseY < scrollerY + 54) {
            scrolling = true;
        }
        return false;
    }

    boolean mouseDragged(double mouseY, int absoluteSlotY, int recipeCount) {
        if (scrolling && recipeCount > 12) {
            int top = absoluteSlotY - GRID_TOP_OFFSET;
            int bottom = top + 54;
            scrollOffs = ((float) mouseY - (float) top - 7.5F) / ((float) (bottom - top) - 15.0F);
            scrollOffs = Mth.clamp(scrollOffs, 0.0F, 1.0F);
            startIndex = (int) ((double) (scrollOffs * (float) offscreenRows(recipeCount)) + 0.5D) * COLUMNS;
            return true;
        }
        return false;
    }

    boolean mouseScrolled(double delta, int recipeCount) {
        if (recipeCount > 12) {
            int offscreen = Math.max(1, offscreenRows(recipeCount));
            scrollOffs = Mth.clamp(scrollOffs - (float) delta / (float) offscreen, 0.0F, 1.0F);
            startIndex = (int) ((double) (scrollOffs * (float) offscreen) + 0.5D) * COLUMNS;
        }
        return true;
    }

    private static int offscreenRows(int recipeCount) {
        return (recipeCount + COLUMNS - 1) / COLUMNS - ROWS;
    }
}
