package net.buildcraftreborn.transport.block;

import net.buildcraftreborn.lib.block.Wrenchable;
import net.buildcraftreborn.transport.PipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Tubos com uma direção especial: no de ferro é a única saída; no de madeira é o inventário de onde ele
 * puxa. A chave inglesa passa para a próxima direção válida.
 */
public class DirectionalPipeBlock extends PipeBlock implements Wrenchable {
    public static final EnumProperty<Direction> SPECIAL = EnumProperty.create("special", Direction.class);

    public DirectionalPipeBlock(Properties properties, PipeType type) {
        super(properties, type);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SPECIAL);
    }

    /** Ferro: qualquer lado ligado. Madeira: um lado ligado que não seja outro tubo (um inventário). */
    public boolean isValidSpecial(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        if (!state.getValue(CONNECTIONS.get(direction))) return false;
        return type() != PipeType.WOOD || !(level.getBlockState(pos.relative(direction)).getBlock() instanceof PipeBlock);
    }

    private BlockState fixSpecial(BlockState state, BlockGetter level, BlockPos pos) {
        if (isValidSpecial(state, level, pos, state.getValue(SPECIAL))) return state;
        for (Direction direction : Direction.values()) {
            if (isValidSpecial(state, level, pos, direction)) return state.setValue(SPECIAL, direction);
        }
        return state;
    }

    @Override
    protected BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        return fixSpecial(super.withConnections(state, level, pos), level, pos);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return fixSpecial(super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random), level, pos);
    }

    @Override
    public boolean onWrench(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        Direction current = state.getValue(SPECIAL);
        for (int step = 1; step <= 6; step++) {
            Direction next = Direction.from3DDataValue((current.get3DDataValue() + step) % 6);
            if (next != current && isValidSpecial(state, level, pos, next)) {
                level.setBlock(pos, state.setValue(SPECIAL, next), Block.UPDATE_ALL);
                return true;
            }
        }
        return false;
    }
}
