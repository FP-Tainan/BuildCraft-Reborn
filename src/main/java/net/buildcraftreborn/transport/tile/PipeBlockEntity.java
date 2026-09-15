package net.buildcraftreborn.transport.tile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.buildcraftreborn.lib.energy.MachineEnergy;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.buildcraftreborn.transport.PipeType;
import net.buildcraftreborn.transport.block.DirectionalPipeBlock;
import net.buildcraftreborn.transport.block.PipeBlock;
import net.craftenergy.api.EnergyUnits;
import net.craftenergy.api.MultimeterReadable;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Itens viajando num tubo. Cada item entra por um lado, anda até o centro, escolhe a saída pelas
 * regras do tubo e segue até o vizinho (outro tubo ou inventário). Se não conseguir entregar, volta e
 * tenta outro lado; sem nenhum, cai no chão. O cliente recebe a lista e anima o movimento sozinho.
 */
public class PipeBlockEntity extends BCBlockEntity implements MultimeterReadable {
    /** Madeira: 1 CWh por item puxado (BuildCraft: 1 MJ). Obsidiana: 2 CWh por item sugado. */
    public static final long ENERGY_PER_ITEM = EnergyUnits.fromCWh(1);
    public static final long OBSIDIAN_ENERGY_PER_ITEM = EnergyUnits.fromCWh(2);
    public static final int FILTER_WIDTH = 9;
    public static final int FILTER_SLOTS = 6 * FILTER_WIDTH;
    private static final int EXTRACT_INTERVAL = 8;
    private static final int SUCK_INTERVAL = 5;

    public static final class TravellingItem {
        public ItemStack stack;
        public @Nullable Direction from;
        public @Nullable Direction to;
        public float progress;
        public float speed;
        final EnumSet<Direction> tried = EnumSet.noneOf(Direction.class);

        TravellingItem(ItemStack stack, @Nullable Direction from, @Nullable Direction to, float progress, float speed) {
            this.stack = stack;
            this.from = from;
            this.to = to;
            this.progress = progress;
            this.speed = speed;
        }

        private record Data(ItemStack stack, Optional<Direction> from, Optional<Direction> to, float progress, float speed) {}

        static final Codec<TravellingItem> CODEC = RecordCodecBuilder.<Data>create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("stack").forGetter(Data::stack),
                Direction.CODEC.optionalFieldOf("from").forGetter(Data::from),
                Direction.CODEC.optionalFieldOf("to").forGetter(Data::to),
                Codec.FLOAT.fieldOf("progress").forGetter(Data::progress),
                Codec.FLOAT.fieldOf("speed").forGetter(Data::speed)
        ).apply(instance, Data::new)).xmap(
                data -> new TravellingItem(data.stack(), data.from().orElse(null), data.to().orElse(null), data.progress(), data.speed()),
                item -> new Data(item.stack, Optional.ofNullable(item.from), Optional.ofNullable(item.to), item.progress, item.speed));
    }

    private final List<TravellingItem> items = new ArrayList<>();
    private final SimpleContainer filters = new SimpleContainer(FILTER_SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            PipeBlockEntity.this.setChanged();
        }
    };
    private final @Nullable MachineEnergy energy;
    private int timer;
    private boolean dirty;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.PIPE.get(), pos, state);
        this.energy = state.getBlock() instanceof PipeBlock pipe && pipe.type().usesEnergy()
                ? new MachineEnergy(this, 220, EnergyUnits.fromCWh(64), 4_000, 1) : null;
    }

    /**
     * Estado atual do bloco no mundo. O cache do block entity pode ficar com o estado de antes das
     * ligações serem recalculadas na hora de colocar o tubo.
     */
    private BlockState state() {
        return this.level != null ? this.level.getBlockState(this.worldPosition) : getBlockState();
    }

    public PipeType type() {
        return state().getBlock() instanceof PipeBlock pipe ? pipe.type() : PipeType.STRUCTURE;
    }

    public @Nullable MachineEnergy energy() {
        return this.energy;
    }

    public SimpleContainer filters() {
        return this.filters;
    }

    public List<TravellingItem> items() {
        return this.items;
    }

    public boolean connected(Direction direction) {
        BlockState state = state();
        return state.getBlock() instanceof PipeBlock && state.getValue(PipeBlock.CONNECTIONS.get(direction));
    }

    // ── tick ──────────────────────────────────────────────────────────────
    public void tick() {
        if (this.level == null) return;
        if (this.level.isClientSide()) {
            for (TravellingItem item : this.items) {
                item.progress = Math.min(item.to == null ? 0.5F : 1.0F, item.progress + item.speed);
            }
            return;
        }
        this.timer++;
        PipeType type = type();
        if (type == PipeType.WOOD && this.timer % EXTRACT_INTERVAL == 0) extract();
        if (type == PipeType.OBSIDIAN && this.timer % SUCK_INTERVAL == 0) suck();

        Iterator<TravellingItem> iterator = this.items.iterator();
        while (iterator.hasNext()) {
            TravellingItem item = iterator.next();
            item.progress += item.speed;
            if (item.to == null && item.progress >= 0.5F) {
                if (type == PipeType.VOID) {
                    iterator.remove();
                    this.dirty = true;
                    continue;
                }
                item.speed = type.modifySpeed(item.speed);
                Direction destination = chooseDestination(item);
                if (destination == null) {
                    Block.popResource(this.level, this.worldPosition, item.stack);
                    iterator.remove();
                    this.dirty = true;
                    continue;
                }
                item.to = destination;
                item.progress = 0.5F;
                this.dirty = true;
            }
            if (item.to != null && item.progress >= 1.0F) {
                ItemStack rest = deliver(item.stack, item.to, item.speed);
                if (rest.isEmpty()) {
                    iterator.remove();
                } else {
                    // não coube: volta pelo mesmo lado e tenta outro
                    item.stack = rest;
                    item.tried.add(item.to);
                    item.from = item.to;
                    item.to = null;
                    item.progress = 0.0F;
                }
                this.dirty = true;
            }
        }
        if (this.dirty) {
            this.dirty = false;
            syncToClient();
        }
    }

    private @Nullable Direction chooseDestination(TravellingItem item) {
        List<Direction> options = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            if (direction == item.from || item.tried.contains(direction) || !connected(direction)) continue;
            if (canDeliver(item.stack, direction)) options.add(direction);
        }
        PipeType type = type();
        if (type == PipeType.IRON) {
            Direction output = state().getValue(DirectionalPipeBlock.SPECIAL);
            options.removeIf(direction -> direction != output);
        } else if (type == PipeType.DIAMOND) {
            List<Direction> matching = new ArrayList<>(options);
            matching.removeIf(direction -> !filterMatches(direction, item.stack));
            if (matching.isEmpty()) {
                options.removeIf(direction -> !filterEmpty(direction));
            } else {
                options = matching;
            }
        } else if (type == PipeType.CLAY) {
            List<Direction> inventories = new ArrayList<>(options);
            inventories.removeIf(direction -> this.level.getBlockEntity(this.worldPosition.relative(direction)) instanceof PipeBlockEntity);
            if (!inventories.isEmpty()) options = inventories;
        }
        return options.isEmpty() ? null : options.get(this.level.getRandom().nextInt(options.size()));
    }

    private boolean filterEmpty(Direction side) {
        for (int slot = 0; slot < FILTER_WIDTH; slot++) {
            if (!this.filters.getItem(side.ordinal() * FILTER_WIDTH + slot).isEmpty()) return false;
        }
        return true;
    }

    public boolean filterMatches(Direction side, ItemStack stack) {
        for (int slot = 0; slot < FILTER_WIDTH; slot++) {
            ItemStack filter = this.filters.getItem(side.ordinal() * FILTER_WIDTH + slot);
            if (!filter.isEmpty() && ItemStack.isSameItem(filter, stack)) return true;
        }
        return false;
    }

    private boolean canDeliver(ItemStack stack, Direction direction) {
        BlockPos other = this.worldPosition.relative(direction);
        if (this.level.getBlockEntity(other) instanceof PipeBlockEntity pipe) {
            return pipe.type().carriesItems() && pipe.connected(direction.getOpposite());
        }
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(this.level, other, direction.getOpposite());
        if (storage == null) return false;
        try (Transaction transaction = Transaction.openOuter()) {
            return storage.insert(ItemVariant.of(stack), stack.getCount(), transaction) > 0;
        }
    }

    private ItemStack deliver(ItemStack stack, Direction direction, float speed) {
        BlockPos other = this.worldPosition.relative(direction);
        if (this.level.getBlockEntity(other) instanceof PipeBlockEntity pipe && pipe.type().carriesItems() && pipe.connected(direction.getOpposite())) {
            pipe.receive(stack, direction.getOpposite(), speed);
            return ItemStack.EMPTY;
        }
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(this.level, other, direction.getOpposite());
        if (storage == null) return stack;
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            transaction.commit();
            ItemStack rest = stack.copy();
            rest.shrink((int) inserted);
            return rest;
        }
    }

    /** Um item entrando pelo lado {@code from} (nulo = direto no centro). */
    public void receive(ItemStack stack, @Nullable Direction from, float speed) {
        if (stack.isEmpty() || !type().carriesItems()) return;
        this.items.add(new TravellingItem(stack.copy(), from, null, from == null ? 0.5F : 0.0F, Math.max(PipeType.BASE_SPEED, speed)));
        this.dirty = true;
        setChanged();
    }

    /** Madeira: puxa do inventário da direção especial, 1 CWh por item, até uma pilha por vez. */
    private void extract() {
        if (this.energy == null || this.energy.stored() < ENERGY_PER_ITEM) return;
        Direction source = state().getValue(DirectionalPipeBlock.SPECIAL);
        if (!connected(source) || this.level.getBlockState(this.worldPosition.relative(source)).getBlock() instanceof PipeBlock) return;
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(this.level, this.worldPosition.relative(source), source.getOpposite());
        if (storage == null) return;
        long affordable = this.energy.stored() / ENERGY_PER_ITEM;
        try (Transaction transaction = Transaction.openOuter()) {
            ItemVariant resource = StorageUtil.findExtractableResource(storage, transaction);
            if (resource == null) return;
            long wanted = Math.min(affordable, resource.toStack().getMaxStackSize());
            long extracted = storage.extract(resource, wanted, transaction);
            if (extracted <= 0) return;
            transaction.commit();
            this.energy.use(extracted * ENERGY_PER_ITEM);
            this.items.add(new TravellingItem(resource.toStack((int) extracted), source, null, 0.0F, PipeType.EXTRACT_SPEED));
            this.dirty = true;
        }
    }

    /** Obsidiana: suga itens soltos em volta, 2 CWh por item. */
    private void suck() {
        if (this.energy == null) return;
        List<ItemEntity> entities = this.level.getEntitiesOfClass(ItemEntity.class, new AABB(this.worldPosition).inflate(1.5),
                entity -> entity.isAlive() && !entity.hasPickUpDelay() && !entity.getItem().isEmpty());
        for (ItemEntity entity : entities) {
            long affordable = this.energy.stored() / OBSIDIAN_ENERGY_PER_ITEM;
            if (affordable <= 0) return;
            ItemStack stack = entity.getItem().copy();
            int taken = (int) Math.min(stack.getCount(), affordable);
            this.energy.use(taken * OBSIDIAN_ENERGY_PER_ITEM);
            receive(stack.split(taken), null, PipeType.BASE_SPEED);
            if (stack.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(stack);
            }
        }
    }

    // ── entrada pelo Transfer API (máquinas, funis) ───────────────────────
    public @Nullable Storage<ItemVariant> insertion(@Nullable Direction side) {
        return type().acceptsInsertion() ? new Insertion(side) : null;
    }

    private final class Insertion implements InsertionOnlyStorage<ItemVariant> {
        private final @Nullable Direction side;

        Insertion(@Nullable Direction side) {
            this.side = side;
        }

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0) return 0;
            int count = (int) Math.min(maxAmount, resource.toStack().getMaxStackSize());
            transaction.addOuterCloseCallback(result -> {
                if (result == TransactionContext.Result.COMMITTED) receive(resource.toStack(count), this.side, PipeType.BASE_SPEED);
            });
            return count;
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level == null || this.level.isClientSide()) return;
        for (TravellingItem item : this.items) Block.popResource(this.level, pos, item.stack);
        this.items.clear();
    }

    @Override
    public void multimeterReading(List<Double> values, List<String> units) {
        if (this.energy != null) MultimeterReadable.electric(values, units, this.energy.voltage(), this.energy.lastReceived());
    }

    // ── salvar ────────────────────────────────────────────────────────────
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Items", TravellingItem.CODEC.listOf(), List.copyOf(this.items));
        List<ItemStack> filterList = new ArrayList<>(FILTER_SLOTS);
        for (int slot = 0; slot < FILTER_SLOTS; slot++) filterList.add(this.filters.getItem(slot));
        output.store("Filters", ItemStack.OPTIONAL_CODEC.listOf(), filterList);
        if (this.energy != null) this.energy.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items.clear();
        this.items.addAll(input.read("Items", TravellingItem.CODEC.listOf()).orElse(List.of()));
        List<ItemStack> filterList = input.read("Filters", ItemStack.OPTIONAL_CODEC.listOf()).orElse(List.of());
        for (int slot = 0; slot < FILTER_SLOTS; slot++) {
            this.filters.setItem(slot, slot < filterList.size() ? filterList.get(slot) : ItemStack.EMPTY);
        }
        if (this.energy != null) this.energy.load(input);
    }
}
