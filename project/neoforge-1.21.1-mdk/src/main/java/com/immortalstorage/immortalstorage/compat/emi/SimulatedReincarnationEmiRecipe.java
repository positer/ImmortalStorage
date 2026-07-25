package com.immortalstorage.immortalstorage.compat.emi;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.item.ModItems;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;

public final class SimulatedReincarnationEmiRecipe implements EmiRecipe {
    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            ResourceLocation.fromNamespaceAndPath(ImmortalStorageMod.MODID, "simulated_reincarnation"),
            EmiStack.of(ModBlocks.SIMULATED_REINCARNATION_FURNACE.get()));
    private final Item source;
    public SimulatedReincarnationEmiRecipe(Item source) { this.source = source; }
    @Override public EmiRecipeCategory getCategory() { return CATEGORY; }
    @Override public ResourceLocation getId() { return ResourceLocation.fromNamespaceAndPath(
            ImmortalStorageMod.MODID, "simulated_reincarnation/" + BuiltInRegistries.ITEM.getKey(source).getPath()); }
    @Override public List<EmiIngredient> getInputs() { return List.of(EmiStack.of(source)); }
    @Override public List<EmiStack> getOutputs() { return List.of(); }
    @Override public int getDisplayWidth() { return 112; }
    @Override public int getDisplayHeight() { return 28; }
    @Override public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(EmiStack.of(source), 0, 5);
        widgets.addSlot(EmiIngredient.of(List.of(EmiStack.of(ModItems.TRUE_YUAN.get()),
                EmiStack.of(ModItems.IMMORTAL_YUAN.get()), EmiStack.of(ModItems.SPIRIT_DRIVE.get()))), 24, 5);
        widgets.addSlot(EmiStack.of(net.minecraft.world.item.Items.IRON_SWORD), 48, 5);
        widgets.addFillingArrow(70, 5, 50);
        widgets.addSlot(EmiStack.of(ModBlocks.SIMULATED_REINCARNATION_FURNACE.get()), 94, 5);
    }
}
