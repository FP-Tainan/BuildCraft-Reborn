package net.buildcraftreborn.transport.block;

import net.buildcraftreborn.lib.tile.ItemPipeConnectable;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.buildcraftreborn.transport.PipeFilterMenu;
import net.buildcraftreborn.transport.PipeType;
import net.buildcraftreborn.transport.tile.PipeBlockEntity;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tubo do BuildCraft: um núcleo com braços para os vizinhos que ele aceita (outros tubos compatíveis
 * e inventórios). Os itens que passam por ele ficam no {@link PipeBlockEntity}.
 */
public class PipeBlock extends Block implements EntityBlock {
    public static final Map<Direction, BooleanProperty> CONNECTIONS = new EnumMap<>(Direction.class);
    private static final VoxelShape CORE = Block.box(4, 4, 4, 12, 12, 12);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        CONNECTIONS.put(Direction.DOWN, BlockStateProperties.DOWN);
        CONNECTIONS.put(Direction.UP, BlockStateProperties.UP);
        CONNECTIONS.put(Direction.NORTH, BlockStateProperties.NORTH);
        CONNECTIONS.put(Direction.SOUTH, BlockStateProperties.SOUTH);
        CONNECTIONS.put(Direction.WEST, BlockStateProperties.WEST);
        CONNECTIONS.put(Direction.EAST, BlockStateProperties.EAST);
        ARMS.put(Direction.UP, Block.box(4, 12, 4, 12, 16, 12));
        ARMS.put(Direction.DOWN, Block.box(4, 0, 4, 12, 4, 12));
        ARMS.put(Direction.NORTH, Block.box(4, 4, 0, 12, 12, 4));
        ARMS.put(Direction.SOUTH, Block.box(4, 4, 12, 12, 12, 16));
        ARMS.put(Direction.EAST, Block.box(12, 4, 4, 16, 12, 12));
        ARMS.put(Direction.WEST, Block.box(0, 4, 4, 4, 12, 12));
    }

    private final PipeType type;

    public PipeBlock(Properties properties, PipeType type) {
        super(properties.noOcclusion());
        this.type = type;
        BlockState state = this.stateDefinition.any();
        for (BooleanProperty property : CONNECTIONS.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }

    public PipeType type() {
        return this.type;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.DOWN, BlockStateProperties.UP, BlockStateProperties.NORTH,
                BlockStateProperties.SOUTH, BlockStateProperties.WEST, BlockStateProperties.EAST);
    }

    /** Se o tubo de {@code type} em {@code pos} liga para {@code direction}. */
    public static boolean canConnect(BlockGetter level, BlockPos pos, PipeType type, Direction direction) {
        BlockPos other = pos.relative(direction);
        BlockState neighbor = level.getBlockState(other);
        if (neighbor.getBlock() instanceof PipeBlock pipe) return PipeType.canPipesConnect(type, pipe.type);
        if (!type.connectsToInventories()) return false;
        BlockEntity blockEntity = level.getBlockEntity(other);
        if (blockEntity instanceof ItemPipeConnectable || blockEntity instanceof Container) return true;
        return level instanceof Level world && ItemStorage.SIDED.find(world, other, direction.getOpposite()) != null;
    }

    protected BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        for (Map.Entry<Direction, BooleanProperty> entry : CONNECTIONS.entrySet()) {
            state = state.setValue(entry.getValue(), canConnect(level, pos, this.type, entry.getKey()));
        }
        return state;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        BlockState connected = withConnections(state, level, pos);
        if (connected != state) level.setBlock(pos, connected, Block.UPDATE_CLIENTS);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return state.setValue(CONNECTIONS.get(direction), canConnect(level, pos, this.type, direction));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Map.Entry<Direction, BooleanProperty> entry : CONNECTIONS.entrySet()) {
            if (state.getValue(entry.getValue())) shape = Shapes.or(shape, ARMS.get(entry.getKey()));
        }
        return shape;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != BCBlockEntities.PIPE.get() || !this.type.carriesItems()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<PipeBlockEntity>) (tickLevel, pos, tickState, pipe) -> pipe.tick();
    }

    /** Tubo de diamante: abre os filtros. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (this.type != PipeType.DIAMOND || !(level.getBlockEntity(pos) instanceof PipeBlockEntity pipe)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new ExtendedMenuProvider<BlockPos>() {
                @Override
                public BlockPos getScreenOpeningData(ServerPlayer opener) {
                    return pos;
                }

                @Override
                public Component getDisplayName() {
                    return state.getBlock().getName();
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
                    return new PipeFilterMenu(containerId, inventory, pipe.filters(), pos);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}
