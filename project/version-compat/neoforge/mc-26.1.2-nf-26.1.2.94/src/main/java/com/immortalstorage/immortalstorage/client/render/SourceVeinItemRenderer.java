package com.immortalstorage.immortalstorage.client.render;

import java.util.function.Consumer;

import com.immortalstorage.immortalstorage.block.custom.SourceVeinBlock;
import com.immortalstorage.immortalstorage.compat.mc2612.SpecialModelGeometry;
import com.immortalstorage.immortalstorage.item.ModDataComponents;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinition;
import com.immortalstorage.immortalstorage.source.definition.SourceDefinitions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** 26.1 special-model implementation for source-vein output previews. */
public final class SourceVeinItemRenderer implements SpecialModelRenderer<ItemStack> {
    public static final SourceVeinItemRenderer INSTANCE = new SourceVeinItemRenderer();

    private SourceVeinItemRenderer() {}

    @Override
    public void submit(ItemStack source, PoseStack poses, SubmitNodeCollector collector,
                       int packedLight, int packedOverlay, boolean foil, int outlineColor) {
        SpecialModelGeometry.submitBlockBase(source, poses, collector,
                packedLight, packedOverlay, outlineColor);
        Minecraft minecraft = Minecraft.getInstance();
        float ticks = minecraft.level == null
                ? (System.currentTimeMillis() % 100_000L) / 50.0F
                : minecraft.level.getGameTime()
                        + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        SourceVeinDisplayRenderer.submit(definition(source), ticks, poses, collector,
                packedLight, packedOverlay);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        consumer.accept(new Vector3f(-0.5F, -0.5F, -0.5F));
        consumer.accept(new Vector3f(0.5F, 0.5F, 0.5F));
    }

    @Override
    public ItemStack extractArgument(ItemStack stack) {
        return stack;
    }

    private static SourceDefinition definition(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof SourceVeinBlock block)) return null;
        var id = block.isGenericDefinitionCarrier()
                ? stack.get(ModDataComponents.SOURCE_DEFINITION_ID.get())
                : SourceDefinitions.legacyId(block.getKind());
        return SourceDefinitions.find(id).orElse(null);
    }

    public static final class Unbaked implements SpecialModelRenderer.Unbaked<ItemStack> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<ItemStack> bake(SpecialModelRenderer.BakingContext context) {
            return INSTANCE;
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
