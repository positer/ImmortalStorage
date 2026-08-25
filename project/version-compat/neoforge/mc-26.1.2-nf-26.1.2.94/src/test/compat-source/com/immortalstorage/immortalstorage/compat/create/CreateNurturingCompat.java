package com.immortalstorage.immortalstorage.compat.create;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.immortalstorage.immortalstorage.block.ModBlocks;
import com.immortalstorage.immortalstorage.recipe.ModRecipes;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/** Optional Create 6.0.x fan-processing bridge, loaded only when Create exists. */
public final class CreateNurturingCompat {
    private static final DeferredRegister<FanProcessingType> TYPES = DeferredRegister.create(
            CreateBuiltInRegistries.FAN_PROCESSING_TYPE, ImmortalStorageMod.MODID);

    static {
        TYPES.register("nurturing", NurturingType::new);
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
    }

    static final class NurturingType implements FanProcessingType {
        @Override
        public boolean isValidAt(Level level, BlockPos pos) {
            var state = level.getBlockState(pos);
            return state.is(ModBlocks.TRUE_YUAN_LIGHT.get())
                    || state.is(ModBlocks.IMMORTAL_YUAN_LIGHT.get());
        }

        @Override
        public int getPriority() {
            return 450;
        }

        @Override
        public boolean canProcess(ItemStack stack, Level level) {
            return level.recipeAccess().getRecipeFor(
                    ModRecipes.IMMORTAL_FURNACE_TYPE.get(), new SingleRecipeInput(stack), level).isPresent();
        }

        @Override
        public List<ItemStack> process(ItemStack stack, Level level) {
            return level.recipeAccess().getRecipeFor(
                            ModRecipes.IMMORTAL_FURNACE_TYPE.get(), new SingleRecipeInput(stack), level)
                    .map(holder -> holder.value().assemble(new SingleRecipeInput(stack), level.registryAccess()))
                    .filter(result -> !result.isEmpty())
                    .map(result -> List.of(result.copy()))
                    .orElse(null);
        }

        @Override
        public void spawnProcessingParticles(Level level, Vec3 pos) {
            if (level.getRandom().nextInt(4) != 0) return;
            level.addParticle(ParticleTypes.END_ROD,
                    pos.x, pos.y + 0.35D, pos.z,
                    0.0D, 0.02D, 0.0D);
            if (level.getRandom().nextBoolean()) {
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        pos.x, pos.y + 0.2D, pos.z,
                        0.0D, 0.01D, 0.0D);
            }
        }

        @Override
        public void morphAirFlow(AirFlowParticleAccess access, RandomSource random) {
            int cyanWhite = random.nextBoolean() ? 0xD7FFFF : 0x83F4EF;
            access.setColor(cyanWhite);
            access.setAlpha(0.9F);
            if (random.nextFloat() < 0.04F) access.spawnExtraParticle(ParticleTypes.END_ROD, 0.12F);
        }

        @Override
        public void affectEntity(Entity entity, Level level) {
            if (!level.isClientSide() && entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                        30, 4, true, true));
            }
        }
    }

    private CreateNurturingCompat() {
    }
}
