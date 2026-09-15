package net.buildcraftreborn.builders.tile;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.builders.block.BuilderBlock;
import net.buildcraftreborn.builders.item.SnapshotItem;
import net.buildcraftreborn.builders.snapshot.BlueprintRules;
import net.buildcraftreborn.builders.snapshot.Snapshot;
import net.buildcraftreborn.builders.snapshot.SnapshotHeader;
import net.buildcraftreborn.builders.snapshot.SnapshotStore;
import net.buildcraftreborn.lib.energy.MachineEnergy;
import net.buildcraftreborn.lib.fluid.BCTank;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.craftenergy.api.EnergyUnits;
import net.craftenergy.api.MultimeterReadable;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Construtor ({@code TileBuilder}): lê o molde ou a planta do slot e constrói atrás de si, girado para a direção
 * do construtor. Primeiro quebra, de cima para baixo, o que atrapalha (se "escavar"; os blocos vão para o
 * inventário), depois coloca, de baixo para cima, com os itens dos 27 slots e fluidos dos 4 tanques. O que falta
 * aparece na lista da direita. Bateria de 16.000 CWh (1.000 MV, aceita a partir de 200 MV); quebrar custa o dobro
 * do poço de mineração e colocar 2 CWh por bloco de distância (mínimo 8).
 */
public class BuilderBlockEntity extends BCBlockEntity implements ServerTicking, MultimeterReadable, ShowsArea {
    public static final int SLOTS = 27;
    public static final int TANKS = 4;
    public static final long TANK_CL = 8_000;
    public static final int DISPLAY = 24;
    public static final long MAX_INPUT = 64_000;
    private static final int CHECKS_PER_TICK = 64;
    private static final int ACTIONS_PER_TICK = 4;
    private static final int IDLE_TICKS = 40;
    private static final int SYNC_INTERVAL = 10;

    public static final int DATA_TO_BREAK = 0;
    public static final int DATA_TO_PLACE = 1;
    public static final int DATA_FLAGS = 2;
    public static final int DATA_COUNT = 3;
    public static final int FLAG_SNAPSHOT = 1;
    public static final int FLAG_DONE = 2;

    private final MachineEnergy energy = new MachineEnergy(this, 1_000, EnergyUnits.fromCWh(16_000), MAX_INPUT, 200);
    private final SimpleContainer snapshotSlot = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            BuilderBlockEntity.this.setChanged();
            BuilderBlockEntity.this.snapshotDirty = true;
        }
    };
    private final SimpleContainer resources = new SimpleContainer(SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            BuilderBlockEntity.this.setChanged();
            BuilderBlockEntity.this.idle = 0;
        }
    };
    private final SimpleContainer display = new SimpleContainer(DISPLAY);
    private final BCTank[] tanks = new BCTank[TANKS];

    private boolean snapshotDirty = true;
    private @Nullable Snapshot snapshot;
    private String loadedHash = "";
    private Rotation rotation = Rotation.NONE;
    private BlockPos origin = BlockPos.ZERO;
    private @Nullable BlockPos shownMin;
    private @Nullable BlockPos shownMax;
    private boolean excavate = true;
    private boolean placing;
    private int cursor;
    private int idle;
    private int passBreak;
    private int passPlace;
    private int toBreak;
    private int toPlace;
    private boolean done;
    private final Map<Item, Integer> passMissing = new LinkedHashMap<>();
    /** Posições do caminho de marcadores (vazio = atrás do construtor) e em qual delas está construindo. */
    private final List<BlockPos> bases = new ArrayList<>();
    private int baseIndex;
    private boolean tanksDirty;
    private int syncTimer;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_TO_BREAK -> Math.min(Short.MAX_VALUE, BuilderBlockEntity.this.toBreak);
                case DATA_TO_PLACE -> Math.min(Short.MAX_VALUE, BuilderBlockEntity.this.toPlace);
                case DATA_FLAGS -> (BuilderBlockEntity.this.snapshot != null ? FLAG_SNAPSHOT : 0) | (BuilderBlockEntity.this.done ? FLAG_DONE : 0);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public BuilderBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.BUILDER.get(), pos, state);
        for (int i = 0; i < TANKS; i++) {
            this.tanks[i] = new BCTank(TANK_CL, () -> {
                this.tanksDirty = true;
                this.idle = 0;
                setChanged();
            });
        }
    }

    public MachineEnergy energy() {
        return this.energy;
    }

    public SimpleContainer snapshotSlot() {
        return this.snapshotSlot;
    }

    public SimpleContainer resources() {
        return this.resources;
    }

    public SimpleContainer display() {
        return this.display;
    }

    public BCTank tank(int index) {
        return this.tanks[index];
    }

    public ContainerData data() {
        return this.data;
    }

    /** Constrói a planta uma vez em cada posição, na ordem (caminho de marcadores). */
    public void setBases(List<BlockPos> positions) {
        this.bases.clear();
        for (BlockPos position : positions) this.bases.add(position.immutable());
        this.baseIndex = 0;
        this.loadedHash = "";
        this.snapshotDirty = true;
        setChanged();
    }

    /** Caminho de marcadores atrás do construtor: vira a lista de posições e os marcadores são recolhidos. */
    public void initPath() {
        if (this.level == null) return;
        BlockPos behind = this.worldPosition.relative(facing().getOpposite());
        net.buildcraftreborn.core.marker.MarkerBlockEntity marker = net.buildcraftreborn.core.marker.MarkerBlockEntity.at(this.level, behind,
                net.buildcraftreborn.core.marker.MarkerBlock.Kind.PATH);
        if (marker == null) return;
        List<BlockPos> path = marker.path();
        if (path.size() < 2) return;
        List<BlockPos> positions = new ArrayList<>();
        positions.add(path.getFirst());
        for (int i = 1; i < path.size(); i++) {
            BlockPos from = path.get(i - 1);
            BlockPos to = path.get(i);
            int steps = Math.max(Math.abs(to.getX() - from.getX()), Math.max(Math.abs(to.getY() - from.getY()), Math.abs(to.getZ() - from.getZ())));
            for (int step = 1; step <= steps; step++) {
                positions.add(new BlockPos(from.getX() + Math.round((to.getX() - from.getX()) * step / (float) steps),
                        from.getY() + Math.round((to.getY() - from.getY()) * step / (float) steps),
                        from.getZ() + Math.round((to.getZ() - from.getZ()) * step / (float) steps)));
            }
        }
        for (BlockPos member : marker.group()) this.level.destroyBlock(member, true);
        setBases(positions);
    }

    public void setExcavate(boolean excavate) {
        this.excavate = excavate;
        restart();
    }

    private Direction facing() {
        BlockState state = this.level != null ? this.level.getBlockState(this.worldPosition) : getBlockState();
        return state.hasProperty(BuilderBlock.FACING) ? state.getValue(BuilderBlock.FACING) : Direction.NORTH;
    }

    @Override
    public @Nullable BoundingBox shownArea() {
        return this.shownMin != null && this.shownMax != null ? BoundingBox.fromCorners(this.shownMin, this.shownMax) : null;
    }

    // ── planta carregada ──────────────────────────────────────────────────
    private void refreshSnapshot(ServerLevel server) {
        this.snapshotDirty = false;
        SnapshotHeader header = SnapshotItem.header(this.snapshotSlot.getItem(0));
        String hash = header == null ? "" : header.hash();
        if (hash.equals(this.loadedHash) && (this.snapshot != null || hash.isEmpty())) return;
        this.loadedHash = hash;
        this.snapshot = hash.isEmpty() ? null : SnapshotStore.get(server, hash);
        if (this.snapshot != null) {
            Direction facing = facing();
            this.rotation = Rotation.NONE;
            for (Rotation candidate : Rotation.values()) {
                if (candidate.rotate(this.snapshot.facing) == facing) this.rotation = candidate;
            }
            BlockPos behind = this.bases.isEmpty() ? this.worldPosition.relative(facing.getOpposite())
                    : this.bases.get(Math.min(this.baseIndex, this.bases.size() - 1));
            this.origin = behind.offset(this.snapshot.offset.rotate(this.rotation));
            BlockPos far = this.origin.offset(new BlockPos(this.snapshot.sizeX - 1, this.snapshot.sizeY - 1, this.snapshot.sizeZ - 1).rotate(this.rotation));
            BoundingBox box = BoundingBox.fromCorners(this.origin, far);
            this.shownMin = new BlockPos(box.minX(), box.minY(), box.minZ());
            this.shownMax = new BlockPos(box.maxX(), box.maxY(), box.maxZ());
        } else {
            this.shownMin = null;
            this.shownMax = null;
        }
        restart();
        syncToClient();
    }

    private void restart() {
        this.placing = false;
        this.cursor = 0;
        this.idle = 0;
        this.passBreak = 0;
        this.passPlace = 0;
        this.done = false;
        this.passMissing.clear();
        setChanged();
    }

    /** Posição no mundo da posição local (x, y, z) da planta. */
    public BlockPos toWorld(int x, int y, int z) {
        return this.origin.offset(new BlockPos(x, y, z).rotate(this.rotation));
    }

    // ── trabalho ──────────────────────────────────────────────────────────
    @Override
    public void serverTick() {
        if (!(this.level instanceof ServerLevel server)) return;
        if (this.tanksDirty && ++this.syncTimer >= SYNC_INTERVAL) {
            this.syncTimer = 0;
            this.tanksDirty = false;
            syncToClient();
        }
        if (this.snapshotDirty) refreshSnapshot(server);
        Snapshot snapshot = this.snapshot;
        if (snapshot == null) {
            this.toBreak = 0;
            this.toPlace = 0;
            return;
        }
        if (this.idle > 0) {
            this.idle--;
            return;
        }
        int layerSize = snapshot.sizeX * snapshot.sizeZ;
        int volume = snapshot.volume();
        int actions = 0;
        for (int checks = 0; checks < CHECKS_PER_TICK && actions < ACTIONS_PER_TICK; checks++) {
            if (this.cursor >= volume) {
                endPass();
                if (this.idle > 0) return;
            }
            int layer = this.cursor / layerSize;
            int y = this.placing ? layer : snapshot.sizeY - 1 - layer;
            int z = (this.cursor % layerSize) / snapshot.sizeX;
            int x = (this.cursor % layerSize) % snapshot.sizeX;
            int index = snapshot.index(x, y, z);
            BlockPos pos = toWorld(x, y, z);
            BlockState current = this.level.getBlockState(pos);
            if (!this.placing) {
                if (this.excavate && shouldBreak(snapshot, index, current, pos)) {
                    this.passBreak++;
                    long cost = 2 * breakEnergy(pos, current);
                    if (this.energy.stored() < cost) return;
                    this.energy.use(cost);
                    breakBlock(server, pos, current);
                    actions++;
                }
            } else if (tryPlace(snapshot, index, current, pos)) {
                actions++;
            } else if (this.energy.stored() < placeEnergy(pos) && this.passPlace > 0 && needsPlacing(snapshot, index, current)) {
                return;
            }
            this.cursor++;
        }
    }

    private void endPass() {
        if (!this.placing) {
            this.toBreak = this.passBreak;
            this.passBreak = 0;
            this.placing = true;
        } else {
            this.toPlace = this.passPlace;
            this.passPlace = 0;
            this.placing = false;
            updateDisplay();
            this.done = this.toBreak == 0 && this.toPlace == 0;
            if (this.done && this.baseIndex < this.bases.size() - 1) {
                // caminho de marcadores: segue para a próxima posição
                this.baseIndex++;
                this.loadedHash = "";
                this.snapshotDirty = true;
                this.done = false;
                setChanged();
            } else if (this.done) {
                this.idle = IDLE_TICKS;
            }
        }
        this.cursor = 0;
    }

    private void updateDisplay() {
        List<Map.Entry<Item, Integer>> missing = new ArrayList<>(this.passMissing.entrySet());
        this.passMissing.clear();
        for (int slot = 0; slot < DISPLAY; slot++) {
            ItemStack stack = slot < missing.size() ? new ItemStack(missing.get(slot).getKey(), Math.min(99, missing.get(slot).getValue())) : ItemStack.EMPTY;
            if (!ItemStack.matches(this.display.getItem(slot), stack)) this.display.setItem(slot, stack);
        }
    }

    private boolean canBreak(BlockState state, BlockPos pos) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.getBlock() instanceof BuilderBlock) return false;
        return state.getDestroySpeed(this.level, pos) >= 0;
    }

    private boolean shouldBreak(Snapshot snapshot, int index, BlockState current, BlockPos pos) {
        if (!canBreak(current, pos)) return false;
        if (snapshot.type == Snapshot.Type.TEMPLATE) return !snapshot.filled(index);
        BlockState wanted = snapshot.state(index).rotate(this.rotation);
        if (wanted.isAir()) return true;
        return !BlueprintRules.matches(wanted, current) && !current.canBeReplaced();
    }

    private boolean needsPlacing(Snapshot snapshot, int index, BlockState current) {
        if (!snapshot.filled(index) || !current.canBeReplaced()) return false;
        return snapshot.type == Snapshot.Type.TEMPLATE || !BlueprintRules.matches(snapshot.state(index).rotate(this.rotation), current);
    }

    /** Coloca o bloco da posição se der; conta o que falta para a lista. */
    private boolean tryPlace(Snapshot snapshot, int index, BlockState current, BlockPos pos) {
        if (!needsPlacing(snapshot, index, current)) return false;
        long cost = placeEnergy(pos);
        if (snapshot.type == Snapshot.Type.TEMPLATE) {
            this.passPlace++;
            int slot = blockSlot();
            if (slot < 0) return false;
            if (this.energy.stored() < cost) return false;
            this.energy.use(cost);
            ItemStack stack = this.resources.getItem(slot);
            this.level.setBlock(pos, ((BlockItem) stack.getItem()).getBlock().defaultBlockState(), Block.UPDATE_ALL);
            this.resources.removeItem(slot, 1);
            return true;
        }
        BlockState wanted = snapshot.state(index).rotate(this.rotation);
        BlueprintRules.Requirement requirement = BlueprintRules.required(wanted);
        if (requirement == null) return false;
        this.passPlace++;
        if (!wanted.canSurvive(this.level, pos)) return false;
        if (!hasRequirement(requirement)) {
            for (ItemStack item : requirement.items()) this.passMissing.merge(item.getItem(), item.getCount(), Integer::sum);
            return false;
        }
        if (this.energy.stored() < cost) return false;
        this.energy.use(cost);
        takeRequirement(requirement);
        this.level.setBlock(pos, wanted, Block.UPDATE_ALL);
        return true;
    }

    private long breakEnergy(BlockPos pos, BlockState state) {
        float hardness = Math.max(0, state.getDestroySpeed(this.level, pos));
        return EnergyUnits.fromCWh(Math.floor(16.0 * (hardness + 1) * BuildCraftReborn.config.miningMultiplier));
    }

    private long placeEnergy(BlockPos pos) {
        return EnergyUnits.fromCWh(Math.max(8.0, 2.0 * Math.sqrt(pos.distSqr(this.worldPosition))));
    }

    private void breakBlock(ServerLevel server, BlockPos pos, BlockState state) {
        List<ItemStack> drops = Block.getDrops(state, server, pos, this.level.getBlockEntity(pos));
        server.destroyBlock(pos, false);
        for (ItemStack drop : drops) {
            ItemStack rest = this.resources.addItem(drop);
            if (!rest.isEmpty()) Block.popResource(server, this.worldPosition.above(), rest);
        }
    }

    private int blockSlot() {
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = this.resources.getItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return slot;
        }
        return -1;
    }

    private boolean hasRequirement(BlueprintRules.Requirement requirement) {
        for (ItemStack item : requirement.items()) {
            if (this.resources.countItem(item.getItem()) < item.getCount()) return false;
        }
        return requirement.fluid() == null || tankWith(requirement.fluid(), requirement.fluidCL()) != null;
    }

    private void takeRequirement(BlueprintRules.Requirement requirement) {
        for (ItemStack item : requirement.items()) {
            int left = item.getCount();
            for (int slot = 0; slot < SLOTS && left > 0; slot++) {
                ItemStack stack = this.resources.getItem(slot);
                if (!stack.is(item.getItem())) continue;
                int taken = Math.min(left, stack.getCount());
                this.resources.removeItem(slot, taken);
                left -= taken;
            }
        }
        if (requirement.fluid() != null) {
            BCTank tank = tankWith(requirement.fluid(), requirement.fluidCL());
            if (tank != null) {
                try (Transaction transaction = Transaction.openOuter()) {
                    tank.extract(requirement.fluid(), BCTank.fromCL(requirement.fluidCL()), transaction);
                    transaction.commit();
                }
            }
        }
    }

    private @Nullable BCTank tankWith(FluidVariant fluid, long amountCL) {
        for (BCTank tank : this.tanks) {
            if (tank.variant.equals(fluid) && tank.amountCL() >= amountCL) return tank;
        }
        return null;
    }

    // ── Transfer API ──────────────────────────────────────────────────────
    public Storage<ItemVariant> itemStorage(@Nullable Direction face) {
        return ContainerStorage.of(this.resources, face);
    }

    public Storage<FluidVariant> fluidStorage() {
        return new CombinedStorage<>(List.of(this.tanks));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.resources);
            Containers.dropContents(this.level, pos, this.snapshotSlot);
        }
    }

    @Override
    public void multimeterReading(List<Double> values, List<String> units) {
        MultimeterReadable.electric(values, units, this.energy.voltage(), this.energy.lastReceived());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.save(output);
        output.store("Snapshot", ItemStack.OPTIONAL_CODEC, this.snapshotSlot.getItem(0));
        for (int slot = 0; slot < SLOTS; slot++) output.store("Slot" + slot, ItemStack.OPTIONAL_CODEC, this.resources.getItem(slot));
        for (int i = 0; i < TANKS; i++) this.tanks[i].save(output, "Tank" + i);
        output.putBoolean("Excavate", this.excavate);
        output.store("Bases", com.mojang.serialization.Codec.LONG.listOf(), this.bases.stream().map(BlockPos::asLong).toList());
        output.putInt("BaseIndex", this.baseIndex);
        output.putBoolean("HasArea", this.shownMin != null && this.shownMax != null);
        if (this.shownMin != null && this.shownMax != null) {
            output.putLong("AreaMin", this.shownMin.asLong());
            output.putLong("AreaMax", this.shownMax.asLong());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.load(input);
        this.snapshotSlot.setItem(0, input.read("Snapshot", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        for (int slot = 0; slot < SLOTS; slot++) this.resources.setItem(slot, input.read("Slot" + slot, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        for (int i = 0; i < TANKS; i++) this.tanks[i].load(input, "Tank" + i);
        this.excavate = input.getBooleanOr("Excavate", true);
        this.bases.clear();
        input.read("Bases", com.mojang.serialization.Codec.LONG.listOf()).ifPresent(list -> {
            for (long value : list) this.bases.add(BlockPos.of(value));
        });
        this.baseIndex = input.getIntOr("BaseIndex", 0);
        if (input.getBooleanOr("HasArea", false)) {
            this.shownMin = BlockPos.of(input.getLongOr("AreaMin", 0L));
            this.shownMax = BlockPos.of(input.getLongOr("AreaMax", 0L));
        } else {
            this.shownMin = null;
            this.shownMax = null;
        }
        this.snapshotDirty = true;
        this.loadedHash = "";
    }
}
