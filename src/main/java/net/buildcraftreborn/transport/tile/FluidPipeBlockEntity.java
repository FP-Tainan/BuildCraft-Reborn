package net.buildcraftreborn.transport.tile;

import net.buildcraftreborn.lib.energy.MachineEnergy;
import net.buildcraftreborn.lib.fluid.BCTank;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.buildcraftreborn.transport.PipeType;
import net.buildcraftreborn.transport.block.DirectionalPipeBlock;
import net.buildcraftreborn.transport.block.PipeBlock;
import net.craftenergy.api.EnergyUnits;
import net.craftenergy.api.MultimeterReadable;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Tubo de fluido: guarda até 1.000 CL e empurra, a cada tick, a vazão do material para os vizinhos
 * ligados. O lado por onde o fluido entrou fica 3 segundos sem receber de volta, para não ficar
 * indo e voltando. Madeira puxa de tanques e máquinas gastando 1 CWh a cada 1.000 CL.
 */
public class FluidPipeBlockEntity extends BCBlockEntity implements MultimeterReadable, net.buildcraftreborn.transport.plug.PlugHolder {
    public static final long CAPACITY_CL = 1_000;
    /** 1 CW·tick por CL: 1 CWh a cada 1.000 CL (BuildCraft: 1 MJ a cada 1.000 mB). */
    public static final long ENERGY_PER_CL = 1;
    private static final int INPUT_COOLDOWN = 60;
    private static final int SYNC_INTERVAL = 5;

    private final BCTank tank = new BCTank(CAPACITY_CL, this::onFluidChanged);
    private final int[] inputCooldown = new int[6];
    private final SimpleContainer filters = new SimpleContainer(PipeBlockEntity.FILTER_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            FluidPipeBlockEntity.this.setChanged();
        }
    };
    private final @Nullable MachineEnergy energy;
    private final net.buildcraftreborn.transport.plug.PipePlugs plugs = new net.buildcraftreborn.transport.plug.PipePlugs(this);
    private boolean dirty;
    private int syncTimer;
    private double shownRatio = -1;
    /** Vazão recente em CL por tick (média móvel) e o fluido que passou: o tubo de passagem fica quase vazio no fim do tick. */
    private float flow;
    private FluidVariant flowVariant = FluidVariant.blank();
    private int filterMode = PipeBlockEntity.MODE_WHITELIST;
    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PipeBlockEntity.DATA_MODE -> FluidPipeBlockEntity.this.filterMode;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return PipeBlockEntity.DATA_COUNT;
        }
    };

    public FluidPipeBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.FLUID_PIPE.get(), pos, state);
        this.energy = state.getBlock() instanceof PipeBlock pipe && (pipe.type() == PipeType.WOOD || pipe.type() == PipeType.DIAMOND_WOOD)
                ? new MachineEnergy(this, 220, EnergyUnits.fromCWh(16), 4_000, 1) : null;
    }

    private BlockState state() {
        return this.level != null ? this.level.getBlockState(this.worldPosition) : getBlockState();
    }

    public PipeType type() {
        return state().getBlock() instanceof PipeBlock pipe ? pipe.type() : PipeType.STRUCTURE;
    }

    public boolean connected(Direction direction) {
        BlockState state = state();
        return state.getBlock() instanceof PipeBlock && state.getValue(PipeBlock.CONNECTIONS.get(direction));
    }

    public BCTank tank() {
        return this.tank;
    }

    public SimpleContainer filters() {
        return this.filters;
    }

    public @Nullable MachineEnergy energy() {
        return this.energy;
    }

    @Override
    public net.buildcraftreborn.transport.plug.PipePlugs plugs() {
        return this.plugs;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level == null || this.level.isClientSide()) return;
        for (ItemStack gate : this.plugs.drops()) net.minecraft.world.level.block.Block.popResource(this.level, pos, gate);
    }

    private void onFluidChanged() {
        this.dirty = true;
        setChanged();
    }

    // ── tick (servidor) ───────────────────────────────────────────────────
    public void tick() {
        if (this.level == null) return;
        if (!this.plugs.isEmpty()) this.plugs.tick(this.level, this.worldPosition);
        for (int i = 0; i < this.inputCooldown.length; i++) {
            if (this.inputCooldown[i] > 0) this.inputCooldown[i]--;
        }
        PipeType type = type();
        if (type == PipeType.WOOD || type == PipeType.DIAMOND_WOOD) extract();
        FluidVariant passing = this.tank.variant;
        long through;
        if (type == PipeType.VOID) {
            through = this.tank.amount;
            if (!this.tank.isEmpty()) {
                this.tank.amount = 0;
                this.tank.variant = FluidVariant.blank();
                this.dirty = true;
            }
        } else {
            through = push(type);
        }
        updateFlow(BCTank.toCL(through), passing);
        if (this.dirty && ++this.syncTimer >= SYNC_INTERVAL) {
            this.syncTimer = 0;
            this.dirty = false;
            syncToClient();
        }
    }

    /** Madeira: puxa do tanque ou máquina da direção especial, na vazão do tubo e conforme a energia. */
    private void extract() {
        if (this.energy == null) return;
        Direction source = state().getValue(DirectionalPipeBlock.SPECIAL);
        if (!connected(source) || this.level.getBlockState(this.worldPosition.relative(source)).getBlock() instanceof PipeBlock) return;
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(this.level, this.worldPosition.relative(source), source.getOpposite());
        if (storage == null) return;
        long affordableCL = this.energy.stored() / ENERGY_PER_CL;
        long max = Math.min(BCTank.fromCL(Math.min(type().fluidRateCL(), affordableCL)), BCTank.fromCL(CAPACITY_CL) - this.tank.amount);
        if (max <= 0) return;
        try (Transaction transaction = Transaction.openOuter()) {
            long moved = StorageUtil.move(storage, this.tank, this::acceptsExtraction, max, transaction);
            transaction.commit();
            if (moved > 0) {
                this.energy.use(Math.max(1, BCTank.toCL(moved)) * ENERGY_PER_CL);
                this.inputCooldown[source.ordinal()] = INPUT_COOLDOWN;
            }
        }
    }

    /** Madeira-diamante: fluidos dos recipientes nos 9 filtros, em lista branca (vazia = tudo) ou negra. */
    private boolean acceptsExtraction(FluidVariant variant) {
        if (type() != PipeType.DIAMOND_WOOD) return true;
        boolean any = false;
        boolean listed = false;
        for (int slot = 0; slot < PipeBlockEntity.FILTER_WIDTH; slot++) {
            ItemStack stack = this.filters.getItem(slot);
            if (stack.isEmpty()) continue;
            any = true;
            Storage<FluidVariant> contents = ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM);
            if (contents == null) continue;
            for (StorageView<FluidVariant> view : contents) {
                if (!view.isResourceBlank() && view.getResource().equals(variant)) listed = true;
            }
        }
        return this.filterMode == PipeBlockEntity.MODE_BLACKLIST ? !listed : !any || listed;
    }

    public void setFilterMode(int mode) {
        if (mode != PipeBlockEntity.MODE_WHITELIST && mode != PipeBlockEntity.MODE_BLACKLIST) return;
        this.filterMode = mode;
        setChanged();
    }

    /** Dados da tela (mesma ordem do tubo de itens): modo, filtro atual e se há filtro. */
    public net.minecraft.world.inventory.ContainerData data() {
        return this.data;
    }

    private void updateFlow(long throughCL, FluidVariant passing) {
        float previous = this.flow;
        this.flow = this.flow * 0.75F + throughCL * 0.25F;
        if (this.flow < 0.5F) this.flow = 0;
        if (throughCL > 0 && !passing.isBlank() && !passing.equals(this.flowVariant)) {
            this.flowVariant = passing;
            this.dirty = true;
        }
        if (Math.abs(this.flow - previous) > 0.01F) this.dirty = true;
    }

    /** Empurra para os vizinhos; devolve as gotas que saíram. */
    private long push(PipeType type) {
        if (this.tank.isEmpty()) return 0;
        List<Direction> sides = new ArrayList<>();
        List<Storage<FluidVariant>> targets = new ArrayList<>();
        Direction special = state().hasProperty(DirectionalPipeBlock.SPECIAL) ? state().getValue(DirectionalPipeBlock.SPECIAL) : null;
        for (Direction direction : Direction.values()) {
            if (!connected(direction) || this.inputCooldown[direction.ordinal()] > 0) continue;
            if ((type == PipeType.WOOD || type == PipeType.DIAMOND_WOOD) && direction == special) continue;
            if (type == PipeType.IRON && direction != special) continue;
            Storage<FluidVariant> target = FluidStorage.SIDED.find(this.level, this.worldPosition.relative(direction), direction.getOpposite());
            if (target == null || !accepts(target)) continue;
            sides.add(direction);
            targets.add(target);
        }
        if (type == PipeType.DIAMOND) filterByDiamond(sides, targets);
        if (type == PipeType.CLAY) preferMachines(sides, targets);
        if (targets.isEmpty()) return 0;

        long budget = Math.min(this.tank.amount, BCTank.fromCL(type.fluidRateCL()));
        long share = Math.max(1, budget / targets.size());
        long moved = 0;
        for (Storage<FluidVariant> target : targets) {
            if (this.tank.isEmpty()) break;
            try (Transaction transaction = Transaction.openOuter()) {
                moved += StorageUtil.move(this.tank, target, variant -> true, share, transaction);
                transaction.commit();
            }
        }
        return moved;
    }

    private boolean accepts(Storage<FluidVariant> target) {
        try (Transaction transaction = Transaction.openOuter()) {
            return target.insert(this.tank.variant, Math.min(this.tank.amount, BCTank.fromCL(1)), transaction) > 0;
        }
    }

    /** Diamante: só os lados cujo filtro tem um recipiente com este fluido; sem nenhum, os lados sem filtro. */
    private void filterByDiamond(List<Direction> sides, List<Storage<FluidVariant>> targets) {
        List<Integer> matching = new ArrayList<>();
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < sides.size(); i++) {
            int state = filterState(sides.get(i));
            if (state == 1) matching.add(i);
            if (state == 0) empty.add(i);
        }
        List<Integer> keep = matching.isEmpty() ? empty : matching;
        for (int i = sides.size() - 1; i >= 0; i--) {
            if (!keep.contains(i)) {
                sides.remove(i);
                targets.remove(i);
            }
        }
    }

    /** 0 = filtro vazio, 1 = algum recipiente com o fluido do tubo, 2 = só outros fluidos. */
    private int filterState(Direction side) {
        boolean any = false;
        for (int slot = 0; slot < PipeBlockEntity.FILTER_WIDTH; slot++) {
            ItemStack stack = this.filters.getItem(side.ordinal() * PipeBlockEntity.FILTER_WIDTH + slot);
            if (stack.isEmpty()) continue;
            any = true;
            Storage<FluidVariant> contents = ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM);
            if (contents == null) continue;
            for (StorageView<FluidVariant> view : contents) {
                if (!view.isResourceBlank() && view.getResource().equals(this.tank.variant)) return 1;
            }
        }
        return any ? 2 : 0;
    }

    /** Argila: se houver tanque ou máquina ligada, só entrega para eles. */
    private void preferMachines(List<Direction> sides, List<Storage<FluidVariant>> targets) {
        boolean hasMachine = false;
        for (Direction side : sides) {
            if (!(this.level.getBlockState(this.worldPosition.relative(side)).getBlock() instanceof PipeBlock)) hasMachine = true;
        }
        if (!hasMachine) return;
        for (int i = sides.size() - 1; i >= 0; i--) {
            if (this.level.getBlockState(this.worldPosition.relative(sides.get(i))).getBlock() instanceof PipeBlock) {
                sides.remove(i);
                targets.remove(i);
            }
        }
    }

    // ── Transfer API ──────────────────────────────────────────────────────
    /** Tanque visto por um lado: o que entra por ele marca esse lado como entrada por 3 segundos. */
    public Storage<FluidVariant> storage(@Nullable Direction side) {
        return new Storage<>() {
            @Override
            public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                long inserted = FluidPipeBlockEntity.this.tank.insert(resource, maxAmount, transaction);
                if (inserted > 0 && side != null) {
                    transaction.addOuterCloseCallback(result -> {
                        if (result == TransactionContext.Result.COMMITTED) inputCooldown[side.ordinal()] = INPUT_COOLDOWN;
                    });
                }
                return inserted;
            }

            @Override
            public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                return FluidPipeBlockEntity.this.tank.extract(resource, maxAmount, transaction);
            }

            @Override
            public Iterator<StorageView<FluidVariant>> iterator() {
                return FluidPipeBlockEntity.this.tank.iterator();
            }
        };
    }

    public float flowCL() {
        return this.flow;
    }

    /** Fluido desenhado: o do tanque ou, se ele esvaziou no fim do tick, o que está passando. */
    public FluidVariant shownVariant() {
        return this.tank.variant.isBlank() ? this.flowVariant : this.tank.variant;
    }

    /** Cliente: nível mostrado andando suave até o alvo; fluido passando enche de 30% a 100% conforme a vazão. */
    public double shownRatio() {
        double target = this.tank.ratio();
        long rate = type().fluidRateCL();
        if (this.flow > 0 && rate > 0) target = Math.max(target, 0.3 + 0.7 * Math.min(1.0, this.flow / rate));
        if (this.shownRatio < 0) this.shownRatio = target;
        this.shownRatio += (target - this.shownRatio) * 0.15;
        if (Math.abs(target - this.shownRatio) < 0.002) this.shownRatio = target;
        return this.shownRatio;
    }

    @Override
    public void multimeterReading(List<Double> values, List<String> units) {
        if (this.energy != null) MultimeterReadable.electric(values, units, this.energy.voltage(), this.energy.lastReceived());
        values.add((double) this.tank.amountCL());
        units.add("CL");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.tank.save(output, "Tank");
        output.store("FlowFluid", FluidVariant.CODEC, this.flowVariant);
        output.putFloat("Flow", this.flow);
        output.putInt("FilterMode", this.filterMode);
        List<ItemStack> filterList = new ArrayList<>(PipeBlockEntity.FILTER_SLOTS);
        for (int slot = 0; slot < PipeBlockEntity.FILTER_SLOTS; slot++) filterList.add(this.filters.getItem(slot));
        output.store("Filters", ItemStack.OPTIONAL_CODEC.listOf(), filterList);
        if (this.energy != null) this.energy.save(output);
        this.plugs.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.load(input, "Tank");
        this.flowVariant = input.read("FlowFluid", FluidVariant.CODEC).orElse(FluidVariant.blank());
        this.flow = input.getFloatOr("Flow", 0.0F);
        this.filterMode = input.getIntOr("FilterMode", PipeBlockEntity.MODE_WHITELIST);
        List<ItemStack> filterList = input.read("Filters", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int slot = 0; slot < PipeBlockEntity.FILTER_SLOTS; slot++) {
            this.filters.setItem(slot, slot < filterList.size() ? filterList.get(slot) : ItemStack.EMPTY);
        }
        if (this.energy != null) this.energy.load(input);
        this.plugs.load(input);
    }
}
