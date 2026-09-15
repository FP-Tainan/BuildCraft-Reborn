package net.buildcraftreborn.energy.world;

import net.buildcraftreborn.BCConfig;
import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.energy.fluid.BCFluids;
import net.buildcraftreborn.registry.BCBlocks;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FlowingFluid;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Petróleo no mundo ({@code OilGenerator} do BuildCraft). Cada chunk sorteia, com semente própria, se tem
 * um poço grande, médio ou (nos campos de petróleo) só um lago. Como um poço passa de um chunk, cada
 * chunk refaz o sorteio dos vizinhos num raio de 5 e coloca só a parte que cai dentro dele:
 * <ul>
 *   <li>lago na superfície com "tentáculos" de petróleo;</li>
 *   <li>esfera de petróleo entre y 20 e 30;</li>
 *   <li>jorro: coluna saindo da esfera e subindo acima do chão;</li>
 *   <li>poço grande: tubo descendo até uma fonte de petróleo na rocha-mãe.</li>
 * </ul>
 * O 26.2 não deixa criar biomas no overworld sem outra biblioteca, então desertos, badlands e oceanos
 * fazem o papel dos biomas "campo de petróleo".
 */
public class OilWellFeature extends Feature<NoneFeatureConfiguration> {
    /** BuildCraft: 0,04% dos chunks. */
    public static final double LARGE_PROBABILITY = 0.0004;
    /** BuildCraft: 0,1% dos chunks. */
    public static final double MEDIUM_PROBABILITY = 0.001;
    /** BuildCraft: 2% dos chunks dos biomas de depósito, que ainda geram 3× mais. */
    public static final double LAKE_PROBABILITY = 0.02;
    public static final double SURFACE_MULTIPLIER = 3.0;
    private static final long MAGIC = -3438862373895731249L;
    private static final int CHUNK_RADIUS = 5;

    public enum Type {
        LARGE,
        MEDIUM,
        LAKE
    }

    /** O que o bioma do centro do chunk permite. */
    public enum Ground {
        NORMAL,
        OIL_FIELD,
        OIL_FIELD_OCEAN
    }

    /** Altura do terreno numa coluna (heightmap do chunk ou estimativa do gerador fora dele). */
    @FunctionalInterface
    public interface Terrain {
        int height(int x, int z, Heightmap.Types type);
    }

    /** Sorteio de um poço: igual em todos os chunks que o calculam. */
    public record Plan(Type type, int x, int z, int lakeRadius, int tendrilLength, int sphereY, int sphereRadius,
                       int spoutHeight, int tubeRadius, long seed) {
        public int reach() {
            return Math.max(this.lakeRadius + this.tendrilLength + 1, this.sphereRadius + 1);
        }
    }

    public OilWellFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BCConfig config = BuildCraftReborn.config;
        ChunkGenerator generator = context.chunkGenerator();
        if (!config.oilWorldgen || generator instanceof FlatLevelSource) return false;
        WorldGenLevel level = context.level();
        RandomState randomState = level.getLevel().getChunkSource().randomState();
        int chunkX = SectionPos.blockToSectionCoord(context.origin().getX());
        int chunkZ = SectionPos.blockToSectionCoord(context.origin().getZ());
        BoundingBox box = new BoundingBox(chunkX << 4, level.getMinY(), chunkZ << 4,
                (chunkX << 4) + 15, level.getMaxY(), (chunkZ << 4) + 15);
        Terrain terrain = (x, z, type) -> box.isInside(x, box.minY(), z)
                ? level.getHeight(type, x, z)
                : generator.getBaseHeight(x, z, type, level, randomState);

        boolean placed = false;
        for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
            for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                final int cx = chunkX + dx;
                final int cz = chunkZ + dz;
                Plan plan = plan(level.getSeed(), cx, cz, config.oilGenerationRate, config.oilFieldMultiplier, config.oilSpouts,
                        () -> ground(generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock((cx << 4) + 8), QuartPos.fromBlock(63),
                                QuartPos.fromBlock((cz << 4) + 8), randomState.sampler())));
                if (plan != null) placed |= generate(level, plan, box, terrain);
            }
        }
        return placed;
    }

    public static Ground ground(Holder<Biome> biome) {
        if (biome.is(ConventionalBiomeTags.IS_OCEAN) || biome.is(ConventionalBiomeTags.IS_DEEP_OCEAN)) return Ground.OIL_FIELD_OCEAN;
        if (biome.is(ConventionalBiomeTags.IS_DESERT) || biome.is(ConventionalBiomeTags.IS_BADLANDS)) return Ground.OIL_FIELD;
        return Ground.NORMAL;
    }

    /**
     * Sorteia o poço do chunk; nulo se não tem. O bioma só é consultado quando algum sorteio poderia
     * passar, porque isso é o que custa.
     */
    public static @Nullable Plan plan(long worldSeed, int chunkX, int chunkZ, double rate, double fieldMultiplier, boolean spouts,
                                      Supplier<Ground> groundAt) {
        RandomSource random = RandomSource.create(worldSeed ^ MAGIC ^ (chunkX * 341873128712L + chunkZ * 132897987541L));
        int x = (chunkX << 4) + 8 + random.nextInt(16);
        int z = (chunkZ << 4) + 8 + random.nextInt(16);
        double largeRoll = random.nextDouble();
        double mediumRoll = random.nextDouble();
        double lakeRoll = random.nextDouble();
        double best = rate * Math.max(1.0, fieldMultiplier);
        double lakeChance = LAKE_PROBABILITY * SURFACE_MULTIPLIER * rate;
        if (largeRoll > LARGE_PROBABILITY * best && mediumRoll > MEDIUM_PROBABILITY * best && lakeRoll > lakeChance) return null;

        Ground ground = groundAt.get();
        double chance = rate * (ground == Ground.NORMAL ? 1.0 : fieldMultiplier);
        Type type;
        if (largeRoll <= LARGE_PROBABILITY * chance) {
            type = Type.LARGE;
        } else if (mediumRoll <= MEDIUM_PROBABILITY * chance) {
            type = Type.MEDIUM;
        } else if (ground == Ground.OIL_FIELD && lakeRoll <= lakeChance) {
            type = Type.LAKE;
        } else {
            return null;
        }
        int lakeRadius = switch (type) {
            case LARGE -> 4;
            case LAKE -> 6;
            case MEDIUM -> 2;
        };
        int tendrilLength = type == Type.MEDIUM ? 5 + random.nextInt(10) : 25 + random.nextInt(20);
        int sphereY = 20 + random.nextInt(10);
        int sphereRadius = type == Type.LARGE ? 8 + random.nextInt(9) : 4 + random.nextInt(4);
        int spoutHeight = !spouts ? 0 : type == Type.LARGE ? 10 + random.nextInt(11) : 6 + random.nextInt(7);
        int tubeRadius = type == Type.LARGE ? 1 : 0;
        return new Plan(type, x, z, lakeRadius, tendrilLength, sphereY, sphereRadius, spoutHeight, tubeRadius, random.nextLong());
    }

    /** Coloca a parte do poço que cai dentro de {@code box}. */
    public static boolean generate(WorldGenLevel level, Plan plan, BoundingBox box, Terrain terrain) {
        int reach = plan.reach();
        if (!box.intersects(plan.x() - reach, plan.z() - reach, plan.x() + reach, plan.z() + reach)) return false;
        boolean placed = generateLake(level, plan, box, terrain);
        if (plan.type() == Type.LAKE) return placed;
        placed |= fillSphere(level, box, new BlockPos(plan.x(), plan.sphereY(), plan.z()), plan.sphereRadius());
        if (plan.spoutHeight() > 0) {
            int surface = terrain.height(plan.x(), plan.z(), Heightmap.Types.WORLD_SURFACE_WG);
            int top = Math.min(level.getMaxY() - 1, surface + plan.spoutHeight());
            placed |= fillTubeY(level, box, plan.x(), plan.z(), plan.sphereY(), surface - 1, plan.tubeRadius(), false);
            placed |= fillTubeY(level, box, plan.x(), plan.z(), surface, top, plan.tubeRadius(), true);
        }
        if (plan.type() == Type.LARGE) {
            if (box.isInside(plan.x(), box.minY(), plan.z())) placed |= placeSpring(level, plan.x(), plan.z());
            placed |= fillTubeY(level, box, plan.x(), plan.z(), level.getMinY() + 1, plan.sphereY(), plan.tubeRadius(), false);
        }
        return placed;
    }

    /** Centro redondo de borda irregular e 1 a 3 tentáculos que se espalham pelo chão. */
    private static boolean generateLake(WorldGenLevel level, Plan plan, BoundingBox box, Terrain terrain) {
        RandomSource random = RandomSource.create(plan.seed());
        int radius = plan.lakeRadius();
        int size = radius + plan.tendrilLength() + 1;
        boolean[][] pattern = new boolean[size * 2 + 1][size * 2 + 1];
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                int distance = dx * dx + dz * dz;
                if (distance <= radius * radius || (distance <= (radius + 1) * (radius + 1) && random.nextBoolean())) {
                    pattern[dx + size][dz + size] = true;
                }
            }
        }
        int tendrils = 1 + random.nextInt(3);
        for (int i = 0; i < tendrils; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int length = plan.tendrilLength() / 2 + random.nextInt(plan.tendrilLength() / 2 + 1);
            double px = 0;
            double pz = 0;
            for (int step = 0; step < length; step++) {
                px += Math.cos(angle);
                pz += Math.sin(angle);
                angle += (random.nextDouble() - 0.5) * 0.6;
                int cx = (int) Math.round(px) + size;
                int cz = (int) Math.round(pz) + size;
                if (cx < 0 || cz < 0 || cx >= pattern.length || cz >= pattern.length) break;
                pattern[cx][cz] = true;
                if (random.nextBoolean()) {
                    int nx = Math.clamp(cx + random.nextInt(3) - 1, 0, pattern.length - 1);
                    int nz = Math.clamp(cz + random.nextInt(3) - 1, 0, pattern.length - 1);
                    pattern[nx][nz] = true;
                }
            }
        }

        boolean placed = false;
        for (int cx = 0; cx < pattern.length; cx++) {
            for (int cz = 0; cz < pattern.length; cz++) {
                if (!pattern[cx][cz]) continue;
                int dx = cx - size;
                int dz = cz - size;
                int x = plan.x() + dx;
                int z = plan.z() + dz;
                if (!box.isInside(x, box.minY(), z)) continue;
                int surface = terrain.height(x, z, Heightmap.Types.WORLD_SURFACE_WG);
                // água por cima (oceano, rio): a água desceria e apagaria o petróleo
                if (surface != terrain.height(x, z, Heightmap.Types.OCEAN_FLOOR_WG)) continue;
                int y = surface - 1;
                placed |= setOil(level, box, new BlockPos(x, y, z), true);
                if (radius >= 4 && dx * dx + dz * dz <= radius * radius) placed |= setOil(level, box, new BlockPos(x, y - 1, z), false);
                BlockPos above = new BlockPos(x, y + 1, z);
                BlockState plant = level.getBlockState(above);
                if (!plant.isAir() && plant.canBeReplaced() && plant.getFluidState().isEmpty()) {
                    level.setBlock(above, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        return placed;
    }

    public static boolean fillSphere(WorldGenLevel level, BoundingBox box, BlockPos center, int radius) {
        boolean placed = false;
        double limit = radius * radius + 0.01;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= limit) placed |= setOil(level, box, center.offset(dx, dy, dz), false);
                }
            }
        }
        return placed;
    }

    public static boolean fillTubeY(WorldGenLevel level, BoundingBox box, int x, int z, int fromY, int toY, int radius, boolean flowing) {
        boolean placed = false;
        double limit = radius * radius + 0.01;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > limit || !box.isInside(x + dx, box.minY(), z + dz)) continue;
                for (int y = fromY; y <= toY; y++) placed |= setOil(level, box, new BlockPos(x + dx, y, z + dz), flowing);
            }
        }
        return placed;
    }

    /** Fonte de petróleo no topo da rocha-mãe da coluna. */
    private static boolean placeSpring(WorldGenLevel level, int x, int z) {
        int minY = level.getMinY();
        for (int y = minY + 5; y >= minY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).is(Blocks.BEDROCK) || level.getBlockState(pos.above()).is(Blocks.BEDROCK)) continue;
            level.setBlock(pos, BCBlocks.OIL_SPRING.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, BCBlocks.OIL_SPRING.get(), 5);
            return true;
        }
        return false;
    }

    /** Troca o bloco por petróleo (nunca rocha-mãe, fonte ou blocos com baú, spawner...). */
    public static boolean setOil(WorldGenLevel level, BoundingBox box, BlockPos pos, boolean flowing) {
        if (!box.isInside(pos) || pos.getY() < level.getMinY() || pos.getY() > level.getMaxY()) return false;
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.BEDROCK) || state.hasBlockEntity() || state.is(BCBlocks.OIL_SPRING.get())) return false;
        FlowingFluid oil = BCFluids.get(BCFluids.Kind.OIL).fluid();
        level.setBlock(pos, oil.defaultFluidState().createLegacyBlock(), Block.UPDATE_CLIENTS);
        if (flowing) level.scheduleTick(pos.immutable(), oil, oil.getTickDelay(level));
        return true;
    }
}
