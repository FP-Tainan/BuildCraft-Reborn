package net.buildcraftreborn.factory.block;

import net.buildcraftreborn.factory.tile.HeatExchangerBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Locale;

/**
 * Trocador de calor multibloco: blocos em fila, virados para o mesmo lado. O primeiro da fila é o início
 * (recebe por baixo o fluido a esquentar), os do meio (1 a 3) aceleram e o último é o fim (recebe pelo lado
 * o fluido quente e solta por cima o que esfriou). Sem vizinho, a ponta aparece tampada.
 */
public class HeatExchangerBlock extends Block implements EntityBlock {
    public enum Part implements StringRepresentable {
        START,
        MIDDLE,
        END;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final BooleanProperty CONNECTED = BooleanProperty.create("connected");

    public HeatExchangerBlock(Properties properties) {
        super(properties.noOcclusion());
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.START).setValue(CONNECTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, CONNECTED);
    }

    /** A fila segue para onde o jogador olha. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withShape(defaultBlockState().setValue(FACING, context.getHorizontalDirection()), context.getLevel(), context.getClickedPos());
    }

    public static BlockState withShape(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        boolean back = inLine(level, pos.relative(facing.getOpposite()), facing);
        boolean front = inLine(level, pos.relative(facing), facing);
        Part part = !back ? Part.START : !front ? Part.END : Part.MIDDLE;
        boolean connected = switch (part) {
            case START -> front;
            case END -> back;
            case MIDDLE -> true;
        };
        return state.setValue(PART, part).setValue(CONNECTED, connected);
    }

    private static boolean inLine(LevelReader level, BlockPos pos, Direction facing) {
        BlockState other = level.getBlockState(pos);
        return other.getBlock() instanceof HeatExchangerBlock && other.getValue(FACING) == facing;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        BlockState shaped = withShape(state, level, pos);
        if (shaped != state) level.setBlock(pos, shaped, Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return direction.getAxis() == state.getValue(FACING).getAxis() ? withShape(state, level, pos) : state;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeatExchangerBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ServerTicking.ticker(level);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof HeatExchangerBlockEntity exchanger) {
            Storage<FluidVariant> storage = exchanger.storage(null);
            if (storage != null && FluidStorageUtil.interactWithFluidStorage(storage, player, hand)) return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof HeatExchangerBlockEntity exchanger)) return InteractionResult.PASS;
        if (!level.isClientSide()) player.sendOverlayMessage(exchanger.status());
        return InteractionResult.SUCCESS;
    }
}
