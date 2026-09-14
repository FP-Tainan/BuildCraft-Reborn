package net.buildcraftreborn.factory.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Tubo que a bomba e o poço de mineração descem; não cai item ao quebrar (como no BuildCraft). */
public class MiningPipeBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

    public MiningPipeBlock(Properties properties) {
        super(properties.noOcclusion());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
