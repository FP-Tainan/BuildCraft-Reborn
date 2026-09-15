package net.buildcraftreborn.factory.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Gel de água ({@code BlockWaterGel}): nasce onde o gelificador acerta a água. Nos 3 primeiros estágios ele
 * transforma as fontes de água vizinhas em gel (até 3 blocos de distância), depois endurece até virar gel
 * pronto, que quebra dando água gelificada.
 */
public class WaterGelBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 6);
    public static final int SPREAD_STAGES = 4;
    public static final int MAX_STAGE = 6;

    public WaterGelBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this) && state.getValue(STAGE) < MAX_STAGE) {
            level.scheduleTick(pos, this, delay(state.getValue(STAGE), level.getRandom()));
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        advance(level, pos, state, random);
    }

    /** Um passo: espalha (se ainda está espalhando) e passa para o próximo estágio. */
    public static void advance(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        int stage = state.getValue(STAGE);
        if (stage >= MAX_STAGE) return;
        if (stage < SPREAD_STAGES - 1) {
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (level.getBlockState(next).is(Blocks.WATER) && level.getFluidState(next).isSource()) {
                    level.setBlock(next, state.setValue(STAGE, stage + 1), Block.UPDATE_ALL);
                }
            }
        }
        level.setBlock(pos, state.setValue(STAGE, stage + 1), Block.UPDATE_ALL);
        if (stage + 1 < MAX_STAGE) level.scheduleTick(pos, state.getBlock(), delay(stage + 1, random));
    }

    private static int delay(int stage, RandomSource random) {
        return stage < SPREAD_STAGES ? 40 + random.nextInt(40) : 200 + random.nextInt(150);
    }
}
