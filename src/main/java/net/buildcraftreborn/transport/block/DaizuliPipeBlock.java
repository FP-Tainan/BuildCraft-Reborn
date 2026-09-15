package net.buildcraftreborn.transport.block;

import net.buildcraftreborn.transport.PipeFlow;
import net.buildcraftreborn.transport.PipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Tubo daizuli ({@code PipeBehaviourDaizuli}): itens da cor do tubo só saem pela face especial; os outros (e os sem
 * cor) saem por qualquer face menos ela. Chave no centro ou na face especial troca a cor; em outro braço, move a
 * face especial para lá.
 */
public class DaizuliPipeBlock extends DirectionalPipeBlock {
    public DaizuliPipeBlock(Properties properties, PipeType type, PipeFlow flow) {
        super(properties, type, flow);
        registerDefaultState(defaultBlockState().setValue(ColoredPipeBlock.COLOR, DyeColor.WHITE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ColoredPipeBlock.COLOR);
    }

    /** A face especial pode apontar para qualquer lado, ligado ou não. */
    @Override
    public boolean isValidSpecial(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return true;
    }

    @Override
    public boolean onWrenchAt(Level level, BlockPos pos, BlockState state, @Nullable Player player, Direction face, Vec3 hit) {
        Vec3 local = hit.subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        double max = Math.max(Math.abs(local.x), Math.max(Math.abs(local.y), Math.abs(local.z)));
        Direction part = max <= 0.26 ? null : Direction.getApproximateNearest(local.x, local.y, local.z);
        if (part == null || part == state.getValue(SPECIAL)) return ColoredPipeBlock.cycleColour(level, pos, state, 1);
        level.setBlock(pos, state.setValue(SPECIAL, part), Block.UPDATE_ALL);
        return true;
    }

    @Override
    public boolean onWrench(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        return ColoredPipeBlock.cycleColour(level, pos, state, 1);
    }

    @Override
    public boolean onWrenchBack(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        return ColoredPipeBlock.cycleColour(level, pos, state, -1);
    }
}
