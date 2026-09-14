package net.buildcraftreborn.lib.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Bloco virado para uma das seis direções (motores, bombas, calhas). Ao ser colocado olha para o
 * jogador; a chave inglesa gira para a próxima direção aceita por {@link #canFace}.
 */
public abstract class BCDirectionalBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    protected BCDirectionalBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Se o bloco pode ficar virado para {@code direction}; motores, por exemplo, só para quem recebe energia. */
    public boolean canFace(Level level, BlockPos pos, Direction direction) {
        return true;
    }

    /** Chave inglesa: gira para a próxima direção aceita. Devolve {@code false} se nenhuma outra serve. */
    public boolean rotateToNext(Level level, BlockPos pos, BlockState state) {
        Direction current = state.getValue(FACING);
        for (int step = 1; step < 6; step++) {
            Direction next = Direction.from3DDataValue((current.get3DDataValue() + step) % 6);
            if (canFace(level, pos, next)) {
                level.setBlock(pos, state.setValue(FACING, next), Block.UPDATE_ALL);
                return true;
            }
        }
        return false;
    }
}
