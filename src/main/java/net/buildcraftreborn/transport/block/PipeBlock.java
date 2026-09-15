package net.buildcraftreborn.transport.block;

import net.buildcraftreborn.lib.tile.ItemPipeConnectable;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.buildcraftreborn.transport.PipeFilterMenu;
import net.buildcraftreborn.transport.PipeFlow;
import net.buildcraftreborn.transport.PipeType;
import net.buildcraftreborn.transport.gate.GateLogic;
import net.buildcraftreborn.transport.gate.GateMenu;
import net.buildcraftreborn.transport.plug.PlugHolder;
import net.buildcraftreborn.transport.tile.FluidPipeBlockEntity;
import net.buildcraftreborn.transport.tile.PipeBlockEntity;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
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
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Tubo do BuildCraft: um núcleo com braços para os vizinhos que ele aceita (tubos compatíveis do mesmo
 * tipo de fluxo e inventários ou tanques). O conteúdo fica no {@link PipeBlockEntity} (itens) ou no
 * {@link FluidPipeBlockEntity} (fluidos).
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
    private final PipeFlow flow;

    public PipeBlock(Properties properties, PipeType type, PipeFlow flow) {
        super(properties.noOcclusion().dynamicShape());
        this.type = type;
        this.flow = flow;
        BlockState state = this.stateDefinition.any();
        for (BooleanProperty property : CONNECTIONS.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }

    public PipeType type() {
        return this.type;
    }

    public PipeFlow flow() {
        return this.flow;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.DOWN, BlockStateProperties.UP, BlockStateProperties.NORTH,
                BlockStateProperties.SOUTH, BlockStateProperties.WEST, BlockStateProperties.EAST);
    }

    /** Se o tubo em {@code pos} liga para {@code direction}. */
    public static boolean canConnect(BlockGetter level, BlockPos pos, PipeType type, PipeFlow flow, Direction direction) {
        BlockPos other = pos.relative(direction);
        // porta lógica (ou outro encaixe) na face tapa a ligação, dos dois lados
        if (level.getBlockEntity(pos) instanceof PlugHolder own && own.plugs().has(direction)) return false;
        BlockState neighbor = level.getBlockState(other);
        if (neighbor.getBlock() instanceof PipeBlock pipe) {
            if (level.getBlockEntity(other) instanceof PlugHolder holder && holder.plugs().has(direction.getOpposite())) return false;
            boolean sameFlow = pipe.flow == flow || type == PipeType.STRUCTURE || pipe.type == PipeType.STRUCTURE;
            return sameFlow && PipeType.canPipesConnect(type, pipe.type);
        }
        if (!type.connectsToInventories()) return false;
        if (flow == PipeFlow.FLUID) {
            return level instanceof Level world && FluidStorage.SIDED.find(world, other, direction.getOpposite()) != null;
        }
        BlockEntity blockEntity = level.getBlockEntity(other);
        if (blockEntity instanceof ItemPipeConnectable || blockEntity instanceof Container) return true;
        return level instanceof Level world && ItemStorage.SIDED.find(world, other, direction.getOpposite()) != null;
    }

    protected BlockState withConnections(BlockState state, BlockGetter level, BlockPos pos) {
        for (Map.Entry<Direction, BooleanProperty> entry : CONNECTIONS.entrySet()) {
            state = state.setValue(entry.getValue(), canConnect(level, pos, this.type, this.flow, entry.getKey()));
        }
        return state;
    }

    /** Recalcula as ligações do tubo e dos vizinhos (depois de pôr ou tirar um encaixe). */
    public static void refreshConnections(Level level, BlockPos pos) {
        refresh(level, pos);
        for (Direction direction : Direction.values()) refresh(level, pos.relative(direction));
    }

    private static void refresh(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PipeBlock pipe) {
            BlockState connected = pipe.withConnections(state, level, pos);
            if (connected != state) level.setBlock(pos, connected, Block.UPDATE_ALL);
        }
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
        return state.setValue(CONNECTIONS.get(direction), canConnect(level, pos, this.type, this.flow, direction));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Map.Entry<Direction, BooleanProperty> entry : CONNECTIONS.entrySet()) {
            if (state.getValue(entry.getValue())) shape = Shapes.or(shape, ARMS.get(entry.getKey()));
        }
        if (level.getBlockEntity(pos) instanceof PlugHolder holder) {
            for (Direction side : holder.plugs().sides()) shape = Shapes.or(shape, plugShape(holder, side));
        }
        return shape;
    }

    private static final Map<Direction, VoxelShape> GATES = new EnumMap<>(Direction.class);

    static {
        GATES.put(Direction.NORTH, Block.box(3, 3, 0, 13, 13, 3));
        GATES.put(Direction.SOUTH, Block.box(3, 3, 13, 13, 13, 16));
        GATES.put(Direction.WEST, Block.box(0, 3, 3, 3, 13, 13));
        GATES.put(Direction.EAST, Block.box(13, 3, 3, 16, 13, 13));
        GATES.put(Direction.DOWN, Block.box(3, 0, 3, 13, 3, 13));
        GATES.put(Direction.UP, Block.box(3, 13, 3, 13, 16, 13));
    }

    private static final Map<Direction, VoxelShape> PLUGS = new EnumMap<>(Direction.class);

    static {
        PLUGS.put(Direction.NORTH, Block.box(5, 5, 0, 11, 11, 4));
        PLUGS.put(Direction.SOUTH, Block.box(5, 5, 12, 11, 11, 16));
        PLUGS.put(Direction.WEST, Block.box(0, 5, 5, 4, 11, 11));
        PLUGS.put(Direction.EAST, Block.box(12, 5, 5, 16, 11, 11));
        PLUGS.put(Direction.DOWN, Block.box(5, 0, 5, 11, 4, 11));
        PLUGS.put(Direction.UP, Block.box(5, 12, 5, 11, 16, 11));
    }

    /** O encaixe (porta ou plugue) em que o jogador mirou, pelo ponto do clique. */
    public static @Nullable Direction plugAt(PlugHolder holder, BlockPos pos, net.minecraft.world.phys.Vec3 location) {
        net.minecraft.world.phys.Vec3 local = location.subtract(pos.getX(), pos.getY(), pos.getZ());
        for (Direction side : holder.plugs().sides()) {
            if (plugShape(holder, side).bounds().inflate(0.01).contains(local)) return side;
        }
        return null;
    }

    private static final Map<Direction, VoxelShape> LENSES = new EnumMap<>(Direction.class);

    static {
        LENSES.put(Direction.NORTH, Block.box(3, 3, 0, 13, 13, 2));
        LENSES.put(Direction.SOUTH, Block.box(3, 3, 14, 13, 13, 16));
        LENSES.put(Direction.WEST, Block.box(0, 3, 3, 2, 13, 13));
        LENSES.put(Direction.EAST, Block.box(14, 3, 3, 16, 13, 13));
        LENSES.put(Direction.DOWN, Block.box(3, 0, 3, 13, 2, 13));
        LENSES.put(Direction.UP, Block.box(3, 14, 3, 13, 16, 13));
    }

    private static final Map<Direction, VoxelShape> FACADES = new EnumMap<>(Direction.class);

    static {
        FACADES.put(Direction.NORTH, Block.box(0, 0, 0, 16, 16, 2));
        FACADES.put(Direction.SOUTH, Block.box(0, 0, 14, 16, 16, 16));
        FACADES.put(Direction.WEST, Block.box(0, 0, 0, 2, 16, 16));
        FACADES.put(Direction.EAST, Block.box(14, 0, 0, 16, 16, 16));
        FACADES.put(Direction.DOWN, Block.box(0, 0, 0, 16, 2, 16));
        FACADES.put(Direction.UP, Block.box(0, 14, 0, 16, 16, 16));
    }

    private static VoxelShape plugShape(PlugHolder holder, Direction side) {
        if (holder.plugs().facade(side) != null) return FACADES.get(side);
        if (holder.plugs().gate(side) != null) return GATES.get(side);
        if (holder.plugs().kind(side) != null) return PLUGS.get(side);
        return LENSES.get(side);
    }

    /** Portas, plugues, fios e o copiador cuidam do próprio clique no tubo (sem abrir a tela do tubo). */
    @Override
    protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        net.minecraft.world.item.Item item = stack.getItem();
        if (item instanceof net.buildcraftreborn.transport.gate.GateItem || item instanceof net.buildcraftreborn.transport.gate.GateCopierItem
                || item instanceof net.buildcraftreborn.transport.plug.WireItem || item instanceof net.buildcraftreborn.transport.plug.PlugItem
                || item instanceof net.buildcraftreborn.transport.plug.LensItem || item instanceof net.buildcraftreborn.transport.plug.FacadeItem) {
            return InteractionResult.PASS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    // ── redstone das portas lógicas ───────────────────────────────────────
    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // direction aponta de quem pergunta para o tubo: a face que emite é a oposta
        return level.getBlockEntity(pos) instanceof PlugHolder holder ? holder.plugs().signal(direction.getOpposite()) : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.flow == PipeFlow.FLUID ? new FluidPipeBlockEntity(pos, state) : new PipeBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // estrutura também tica: pode ter porta lógica
        if (type == BCBlockEntities.PIPE.get()) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<PipeBlockEntity>) (tickLevel, pos, tickState, pipe) -> pipe.tick();
        }
        if (type == BCBlockEntities.FLUID_PIPE.get() && !level.isClientSide()) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<FluidPipeBlockEntity>) (tickLevel, pos, tickState, pipe) -> pipe.tick();
        }
        return null;
    }

    /** Tubo de diamante: abre os filtros. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PlugHolder holder) {
            Direction plugSide = plugAt(holder, pos, hit.getLocation());
            if (plugSide != null) return usePlug(level, pos, player, holder, plugSide);
            if (player.isShiftKeyDown()) {
                int corner = net.buildcraftreborn.transport.plug.WireNetwork.cornerAt(hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ()));
                if (holder.plugs().wire(corner) != null) {
                    if (!level.isClientSide()) give(level, pos, player, holder.plugs().removeWire(corner));
                    return InteractionResult.SUCCESS;
                }
            }
        }
        if (this.type == PipeType.DIAMOND_WOOD && blockEntity instanceof FluidPipeBlockEntity fluidSpecial) {
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
                        return new net.buildcraftreborn.transport.DiamondWoodPipeMenu(containerId, inventory, fluidSpecial);
                    }
                });
            }
            return InteractionResult.SUCCESS;
        }
        if ((this.type == PipeType.DIAMOND_WOOD || this.type == PipeType.EMZULI) && blockEntity instanceof PipeBlockEntity special) {
            if (player instanceof ServerPlayer serverPlayer) {
                boolean emzuli = this.type == PipeType.EMZULI;
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
                        return emzuli ? new net.buildcraftreborn.transport.EmzuliPipeMenu(containerId, inventory, special)
                                : new net.buildcraftreborn.transport.DiamondWoodPipeMenu(containerId, inventory, special);
                    }
                });
            }
            return InteractionResult.SUCCESS;
        }
        if (this.type != PipeType.DIAMOND) return InteractionResult.PASS;
        Container filters = blockEntity instanceof PipeBlockEntity pipe ? pipe.filters()
                : blockEntity instanceof FluidPipeBlockEntity fluidPipe ? fluidPipe.filters() : null;
        if (filters == null) return InteractionResult.PASS;
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
                    return new PipeFilterMenu(containerId, inventory, filters, pos);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }

    private static void give(Level level, BlockPos pos, Player player, net.minecraft.world.item.ItemStack stack) {
        if (!player.getAbilities().instabuild && !stack.isEmpty() && !player.getInventory().add(stack)) Block.popResource(level, pos, stack);
    }

    /** Encaixe: agachado tira e devolve o item; na porta abre a tela; no pulsar liga o modo manual. */
    private static InteractionResult usePlug(Level level, BlockPos pos, Player player, PlugHolder holder, Direction side) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) {
            give(level, pos, player, holder.plugs().remove(side));
            refreshConnections(level, pos);
            level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock(), null);
            return InteractionResult.SUCCESS;
        }
        if (holder.plugs().kind(side) == net.buildcraftreborn.transport.plug.PlugKind.PULSAR) {
            boolean manual = holder.plugs().toggleManual(side);
            player.sendOverlayMessage(Component.translatable("item.buildcraftreborn.plug_pulsar." + (manual ? "manual_on" : "manual_off")));
            return InteractionResult.SUCCESS;
        }
        GateLogic gate = holder.plugs().gate(side);
        if (gate != null && player instanceof ServerPlayer serverPlayer) {
            GateMenu.Target target = new GateMenu.Target(pos, side);
            serverPlayer.openMenu(new ExtendedMenuProvider<GateMenu.Target>() {
                @Override
                public GateMenu.Target getScreenOpeningData(ServerPlayer opener) {
                    return target;
                }

                @Override
                public Component getDisplayName() {
                    return gate.variant().name();
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
                    return new GateMenu(containerId, inventory, target);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}
