package net.buildcraftreborn.transport.block;

import net.buildcraftreborn.lib.block.Wrenchable;
import net.buildcraftreborn.transport.PipeColours;
import net.buildcraftreborn.transport.PipeFlow;
import net.buildcraftreborn.transport.PipeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/** Tubo lápis ({@code PipeBehaviourLapis}): pinta da sua cor todo item que passa. A chave troca a cor (agachado volta). */
public class ColoredPipeBlock extends PipeBlock implements Wrenchable {
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    public ColoredPipeBlock(Properties properties, PipeType type, PipeFlow flow) {
        super(properties, type, flow);
        registerDefaultState(defaultBlockState().setValue(COLOR, DyeColor.WHITE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COLOR);
    }

    /** Muda a cor do tubo em {@code step} posições. */
    public static boolean cycleColour(Level level, BlockPos pos, BlockState state, int step) {
        if (!state.hasProperty(COLOR)) return false;
        level.setBlock(pos, state.setValue(COLOR, PipeColours.cycle(state.getValue(COLOR), step)), Block.UPDATE_ALL);
        return true;
    }

    @Override
    public boolean onWrench(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        return cycleColour(level, pos, state, 1);
    }

    @Override
    public boolean onWrenchBack(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        return cycleColour(level, pos, state, -1);
    }
}
