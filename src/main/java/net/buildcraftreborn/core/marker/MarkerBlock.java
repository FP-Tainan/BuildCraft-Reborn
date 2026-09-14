package net.buildcraftreborn.core.marker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Marcadores do BuildCraft, parecidos com tochas. O de área (Land Mark) liga com outros alinhados
 * nos eixos X, Y e Z e forma a caixa que a pedreira e o preenchedor usam; o de caminho (Path Mark)
 * forma uma corrente de pontos. Clique direito tenta ligar; com sinal de redstone o marcador de área
 * mostra os eixos para ajudar a posicionar os outros.
 */
public class MarkerBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        SHAPES.put(Direction.UP, Block.box(6, 0, 6, 10, 10, 10));
        SHAPES.put(Direction.DOWN, Block.box(6, 6, 6, 10, 16, 10));
        SHAPES.put(Direction.NORTH, Block.box(6, 6, 6, 10, 10, 16));
        SHAPES.put(Direction.SOUTH, Block.box(6, 6, 0, 10, 10, 10));
        SHAPES.put(Direction.WEST, Block.box(6, 6, 6, 16, 10, 10));
        SHAPES.put(Direction.EAST, Block.box(0, 6, 6, 10, 10, 10));
    }

    public enum Kind { VOLUME, PATH }

    private final Kind kind;

    public MarkerBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MarkerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        // o conector de marcadores age pelo item
        if (stack.getItem() instanceof MarkerConnectorItem) return InteractionResult.PASS;
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MarkerBlockEntity marker) {
            marker.connectManually();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
