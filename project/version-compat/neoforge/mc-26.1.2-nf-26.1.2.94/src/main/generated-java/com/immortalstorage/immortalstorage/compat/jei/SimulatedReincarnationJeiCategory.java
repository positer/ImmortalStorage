package com.immortalstorage.immortalstorage.compat.jei;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.item.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class SimulatedReincarnationJeiCategory
        implements IRecipeCategory<SimulatedReincarnationJeiCategory.Entry> {
    public record Entry(ItemStack source) {}
    public static final IRecipeType<Entry> TYPE = IRecipeType.create(
            ImmortalStorageMod.MODID, "simulated_reincarnation", Entry.class);
    private final IDrawable icon;

    public SimulatedReincarnationJeiCategory(IGuiHelper guiHelper) {
        icon = guiHelper.createDrawableItemLike(ModBlocks.SIMULATED_REINCARNATION_FURNACE.get());
    }
    @Override public IRecipeType<Entry> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.immortalstorage.simulated_reincarnation"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 112; }
    @Override public int getHeight() { return 28; }
    @Override public void setRecipe(IRecipeLayoutBuilder builder, Entry entry, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 6).addItemStack(entry.source());
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 25, 6).addItemStacks(java.util.List.of(
                new ItemStack(ModItems.TRUE_YUAN.get()), new ItemStack(ModItems.IMMORTAL_YUAN.get()),
                new ItemStack(ModItems.SPIRIT_DRIVE.get())));
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 49, 6)
                .addItemStack(new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 91, 6)
                .addItemStack(new ItemStack(ModBlocks.SIMULATED_REINCARNATION_FURNACE.get()));
    }
}
