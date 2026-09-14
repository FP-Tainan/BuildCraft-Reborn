package net.buildcraftreborn.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Fonte de água do BuildCraft: fica no lugar da rocha-mãe e a cada 5 ticks repõe a água logo acima,
 * então uma bomba em cima dela nunca seca.
 */
public class SpringBlock extends Block {
    public static final int TICK_RATE = 5;

    public SpringBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, TICK_RATE);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.isAir() || aboveState.getFluidState().getType() == Fluids.FLOWING_WATER) {
            level.setBlock(above, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.scheduleTick(pos, this, TICK_RATE);
    }
}
