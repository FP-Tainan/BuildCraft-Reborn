package net.buildcraftreborn.factory.block;

import net.buildcraftreborn.factory.tile.TankBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Tanque do BuildCraft: 16.000 CL por bloco. Tanques empilhados viram uma coluna: o fluido escorre
 * para o de baixo e as tampas e bordas entre eles somem, parecendo um tanque só.
 */
public class TankBlock extends Block implements EntityBlock {
    public static final BooleanProperty JOINED_BELOW = BooleanProperty.create("joined_below");
    public static final BooleanProperty JOINED_ABOVE = BooleanProperty.create("joined_above");
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public TankBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(this.stateDefinition.any().setValue(JOINED_BELOW, false).setValue(JOINED_ABOVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(JOINED_BELOW, JOINED_ABOVE);
    }

    private BlockState withJoins(BlockState state, BlockGetter level, BlockPos pos) {
        return state.setValue(JOINED_BELOW, level.getBlockState(pos.below()).is(this))
                .setValue(JOINED_ABOVE, level.getBlockState(pos.above()).is(this));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withJoins(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (direction == Direction.DOWN) return state.setValue(JOINED_BELOW, neighbor.is(this));
        if (direction == Direction.UP) return state.setValue(JOINED_ABOVE, neighbor.is(this));
        return state;
    }

    /** Colocado sem jogador (comando, estrutura, pistão): confere os vizinhos de cima e de baixo. */
    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        BlockState joined = withJoins(state, level, pos);
        if (joined != state) level.setBlock(pos, joined, Block.UPDATE_CLIENTS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ServerTicking.ticker(level);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof TankBlockEntity tank && FluidStorageUtil.interactWithFluidStorage(tank.tank(), player, hand)) {
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof TankBlockEntity tank ? (int) Math.ceil(tank.tank().ratio() * 15) : 0;
    }
}
