package net.buildcraftreborn.core.world;

import net.buildcraftreborn.core.block.SpringBlock;
import net.buildcraftreborn.registry.BCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Coloca uma fonte de água no topo da camada de rocha-mãe (BuildCraft: 2,5% dos chunks), com água
 * logo acima para ela começar a correr pelas cavernas do fundo.
 */
public class SpringFeature extends Feature<NoneFeatureConfiguration> {
    private static final int SEARCH_HEIGHT = 6;

    public SpringFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int minY = level.getMinY();
        for (int y = minY + SEARCH_HEIGHT - 1; y >= minY; y--) {
            BlockPos pos = new BlockPos(origin.getX(), y, origin.getZ());
            if (!level.getBlockState(pos).is(Blocks.BEDROCK) || level.getBlockState(pos.above()).is(Blocks.BEDROCK)) continue;
            level.setBlock(pos, BCBlocks.WATER_SPRING.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(pos.above(), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, BCBlocks.WATER_SPRING.get(), SpringBlock.TICK_RATE);
            return true;
        }
        return false;
    }
}
