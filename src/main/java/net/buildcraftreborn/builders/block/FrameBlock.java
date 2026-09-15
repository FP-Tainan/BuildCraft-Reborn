package net.buildcraftreborn.builders.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Armação da pedreira: uma haste que se liga às armações vizinhas. Não cai item ao quebrar. */
public class FrameBlock extends Block {
    public static final Map<Direction, BooleanProperty> CONNECTIONS = new EnumMap<>(Direction.class);
    private static final VoxelShape CORE = Block.box(4, 4, 4, 12, 12, 12);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        CONNECTIONS.put(Direction.UP, BlockStateProperties.UP);
        CONNECTIONS.put(Direction.DOWN, BlockStateProperties.DOWN);
        CONNECTIONS.put(Direction.NORTH, BlockStateProperties.NORTH);
        CONNECTIONS.put(Direction.SOUTH, BlockStateProperties.SOUTH);
        CONNECTIONS.put(Direction.EAST, BlockStateProperties.EAST);
        CONNECTIONS.put(Direction.WEST, BlockStateProperties.WEST);
        ARMS.put(Direction.UP, Block.box(4, 12, 4, 12, 16, 12));
        ARMS.put(Direction.DOWN, Block.box(4, 0, 4, 12, 4, 12));
        ARMS.put(Direction.NORTH, Block.box(4, 4, 0, 12, 12, 4));
        ARMS.put(Direction.SOUTH, Block.box(4, 4, 12, 12, 12, 16));
        ARMS.put(Direction.EAST, Block.box(12, 4, 4, 16, 12, 12));
        ARMS.put(Direction.WEST, Block.box(0, 4, 4, 4, 12, 12));
    }

    public FrameBlock(Properties properties) {
        super(properties.noOcclusion());
        BlockState state = this.stateDefinition.any();
        for (BooleanProperty property : CONNECTIONS.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.UP, BlockStateProperties.DOWN, BlockStateProperties.NORTH,
                BlockStateProperties.SOUTH, BlockStateProperties.EAST, BlockStateProperties.WEST);
    }

    private BlockState connected(BlockState state, BlockGetter level, BlockPos pos) {
        for (Map.Entry<Direction, BooleanProperty> entry : CONNECTIONS.entrySet()) {
            state = state.setValue(entry.getValue(), level.getBlockState(pos.relative(entry.getKey())).is(this));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connected(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        BlockState joined = connected(state, level, pos);
        if (joined != state) level.setBlock(pos, joined, Block.UPDATE_CLIENTS);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return state.setValue(CONNECTIONS.get(direction), neighbor.is(this));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Map.Entry<Direction, BooleanProperty> entry : CONNECTIONS.entrySet()) {
            if (state.getValue(entry.getValue())) shape = Shapes.or(shape, ARMS.get(entry.getKey()));
        }
        return shape;
    }
}
