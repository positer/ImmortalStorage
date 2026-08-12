package com.immortalstorage.immortalstorage.dimension;

import com.immortalstorage.immortalstorage.ImmortalStorageMod;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.GenerationStep;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Chunk generator for the    (      ).  Fills every column with a fixed
 * profile (2 bedrock  ?50 stone  ?3 dirt  ?1 grass) regardless of the chunk
 * coordinates.  Player isolation is handled by registering one runtime
 * dimension per player UUID, so every personal realm can use the origin.
 */
public class XianqiaoRealmChunkGenerator extends ChunkGenerator {
    public static final int MIN_Y = 0;
    public static final int HEIGHT = 256;
    public static final int BEDROCK = 2;
    public static final int STONE = 50;
    public static final int DIRT = 3;
    public static final int GRASS = 1;
    public static final int SURFACE_Y = BEDROCK + STONE + DIRT;   // y where dirt ends, grass starts
    public static final int TOP_Y = SURFACE_Y + GRASS;             // y where grass ends (top)

    public static final MapCodec<XianqiaoRealmChunkGenerator> CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance
                    .group(RegistryOps.retrieveGetter(Registries.BIOME))
                    .apply(instance, instance.stable(XianqiaoRealmChunkGenerator::new)));

    private final NoiseColumn columnSample;
    private final BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
    private final BlockState stone = Blocks.STONE.defaultBlockState();
    private final BlockState dirt = Blocks.DIRT.defaultBlockState();
    private final BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
    private final BlockState air = Blocks.AIR.defaultBlockState();

    public XianqiaoRealmChunkGenerator(HolderGetter<Biome> biomeRegistry) {
        super(createBiomeSource(biomeRegistry));
        // Sample column: bedrock at 0-1, stone 2-51, dirt 52-54, grass 55, air 56+
        BlockState[] sample = new BlockState[HEIGHT];
        Arrays.fill(sample, 0, TOP_Y, air);
        for (int y = 0; y < BEDROCK; y++) sample[y] = bedrock;
        for (int y = BEDROCK; y < BEDROCK + STONE; y++) sample[y] = stone;
        for (int y = BEDROCK + STONE; y < SURFACE_Y; y++) sample[y] = dirt;
        sample[SURFACE_Y] = grass;
        for (int y = TOP_Y; y < HEIGHT; y++) sample[y] = air;
        this.columnSample = new NoiseColumn(MIN_Y, sample);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }

    private static FixedBiomeSource createBiomeSource(HolderGetter<Biome> biomeRegistry) {
        return new FixedBiomeSource(biomeRegistry.getOrThrow(ImmortalStorageDimensions.XIANQIAO_REALM_BIOME));
    }

    @Override public int getGenDepth() { return HEIGHT; }
    @Override public int getMinY() { return MIN_Y; }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager sm, RandomState rs, ChunkAccess chunk) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) {
            m.setX(x);
            for (int z = 0; z < 16; z++) {
                m.setZ(z);
                for (int y = 0; y < TOP_Y; y++) {
                    m.setY(y);
                    BlockState b;
                    if (y < BEDROCK) b = bedrock;
                    else if (y < BEDROCK + STONE) b = stone;
                    else if (y < SURFACE_Y) b = dirt;
                    else if (y == SURFACE_Y) b = grass;
                    else b = air;
                    chunk.setBlockState(m, b, 0);
                }
            }
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender b, RandomState r, StructureManager s, ChunkAccess c) {
        return CompletableFuture.completedFuture(c);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types t, LevelHeightAccessor l, RandomState r) {
        return SURFACE_Y + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor l, RandomState r) {
        return columnSample;
    }

    @Override
    public int getSeaLevel() { return SURFACE_Y; }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState rs, BlockPos p) {}

    @Override
    public void applyCarvers(WorldGenRegion l, long seed, RandomState r, BiomeManager bm, StructureManager s, ChunkAccess c) {}

    @Override
    public void spawnOriginalMobs(WorldGenRegion l) {}
}
