package net.buildcraftreborn.transport.tile;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.buildcraftreborn.lib.energy.MachineEnergy;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.buildcraftreborn.transport.PipeColours;
import net.buildcraftreborn.transport.PipeType;
import net.buildcraftreborn.transport.block.ColoredPipeBlock;
import net.buildcraftreborn.transport.block.DirectionalPipeBlock;
import net.buildcraftreborn.transport.block.PipeBlock;
import net.buildcraftreborn.transport.plug.PipePlugs;
import net.buildcraftreborn.transport.plug.PlugHolder;
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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Itens viajando num tubo. Cada item entra por um lado, anda até o centro, escolhe a saída pelas
 * regras do tubo e segue até o vizinho (outro tubo ou inventário). Se não conseguir entregar, volta e
 * tenta outro lado; sem nenhum, cai no chão. O cliente recebe a lista e anima o movimento sozinho.
 * Itens podem estar pintados (tubo lápis, lentes, emzuli); filtros e o daizuli usam a cor.
 */
public class PipeBlockEntity extends BCBlockEntity implements MultimeterReadable, PlugHolder {
    /** Madeira: 1 CWh por item puxado (BuildCraft: 1 MJ). Obsidiana: 2 CWh por item sugado. */
    public static final long ENERGY_PER_ITEM = EnergyUnits.fromCWh(1);
    public static final long OBSIDIAN_ENERGY_PER_ITEM = EnergyUnits.fromCWh(2);
    public static final int FILTER_WIDTH = 9;
    public static final int FILTER_SLOTS = 6 * FILTER_WIDTH;
    public static final int MODE_WHITELIST = 0;
    public static final int MODE_BLACKLIST = 1;
    public static final int MODE_ROUND_ROBIN = 2;
    public static final int PRESETS = 4;
    public static final int DATA_MODE = 0;
    public static final int DATA_CURRENT_FILTER = 1;
    public static final int DATA_FILTER_VALID = 2;
    public static final int DATA_PRESET_COLOUR = 3;
    public static final int DATA_ACTIVE_PRESETS = DATA_PRESET_COLOUR + PRESETS;
    public static final int DATA_CURRENT_PRESET = DATA_ACTIVE_PRESETS + 1;
    public static final int DATA_COUNT = DATA_CURRENT_PRESET + 1;
    private static final int EXTRACT_INTERVAL = 8;
    private static final int SUCK_INTERVAL = 5;

    public static final class TravellingItem {
        public ItemStack stack;
        public @Nullable Direction from;
        public @Nullable Direction to;
        public float progress;
        public float speed;
        public @Nullable DyeColor colour;
        final EnumSet<Direction> tried = EnumSet.noneOf(Direction.class);

        TravellingItem(ItemStack stack, @Nullable Direction from, @Nullable Direction to, float progress, float speed) {
            this.stack = stack;
            this.from = from;
            this.to = to;
            this.progress = progress;
            this.speed = speed;
        }

        private record Data(ItemStack stack, Optional<Direction> from, Optional<Direction> to, float progress, float speed,
                            Optional<DyeColor> colour) {}

        static final Codec<TravellingItem> CODEC = RecordCodecBuilder.<Data>create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("stack").forGetter(Data::stack),
                Direction.CODEC.optionalFieldOf("from").forGetter(Data::from),
                Direction.CODEC.optionalFieldOf("to").forGetter(Data::to),
                Codec.FLOAT.fieldOf("progress").forGetter(Data::progress),
                Codec.FLOAT.fieldOf("speed").forGetter(Data::speed),
                DyeColor.CODEC.optionalFieldOf("colour").forGetter(Data::colour)
        ).apply(instance, Data::new)).xmap(
                data -> {
                    TravellingItem item = new TravellingItem(data.stack(), data.from().orElse(null), data.to().orElse(null), data.progress(), data.speed());
                    item.colour = data.colour().orElse(null);
                    return item;
                },
                item -> new Data(item.stack, Optional.ofNullable(item.from), Optional.ofNullable(item.to), item.progress, item.speed,
                        Optional.ofNullable(item.colour)));
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
    private final PipePlugs plugs = new PipePlugs(this);
    private int timer;
    private boolean dirty;
    // madeira-diamante
    private int filterMode = MODE_WHITELIST;
    private int currentFilter;
    // emzuli
    private final @Nullable DyeColor[] presetColours = new DyeColor[PRESETS];
    private int activePresets;
    private final int[] presetTtl = new int[PRESETS];
    private int currentPreset = -1;
    // stripes
    private long stripesProgress;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_PRESET_COLOUR && index < DATA_PRESET_COLOUR + PRESETS) {
                DyeColor colour = PipeBlockEntity.this.presetColours[index - DATA_PRESET_COLOUR];
                return colour == null ? 0 : colour.ordinal() + 1;
            }
            return switch (index) {
                case DATA_MODE -> PipeBlockEntity.this.filterMode;
                case DATA_CURRENT_FILTER -> PipeBlockEntity.this.currentFilter;
                case DATA_FILTER_VALID -> anyFilter(FILTER_WIDTH) ? 1 : 0;
                case DATA_ACTIVE_PRESETS -> PipeBlockEntity.this.activePresets;
                case DATA_CURRENT_PRESET -> PipeBlockEntity.this.currentPreset;
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

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.PIPE.get(), pos, state);
        this.energy = state.getBlock() instanceof PipeBlock pipe && pipe.type().usesEnergy()
                ? new MachineEnergy(this, 220, EnergyUnits.fromCWh(pipe.type() == PipeType.STRIPES ? 256 : 64), 4_000, 1) : null;
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

    public ContainerData data() {
        return this.data;
    }

    @Override
    public PipePlugs plugs() {
        return this.plugs;
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
        if (!this.plugs.isEmpty()) this.plugs.tick(this.level, this.worldPosition);
        PipeType type = type();
        if (!type.carriesItems()) return;
        if (type == PipeType.EMZULI) tickPresets();
        if (type.extractsItems() && this.timer % EXTRACT_INTERVAL == 0) extract();
        if (type == PipeType.OBSIDIAN && this.timer % SUCK_INTERVAL == 0) suck();
        Direction stripes = type == PipeType.STRIPES ? stripesDirection() : null;
        if (stripes != null) breakWithStripes(stripes);
        ItemStack extension = null;
        List<ItemStack> returned = new ArrayList<>();

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
                if (type == PipeType.LAPIS) item.colour = state().getValue(ColoredPipeBlock.COLOR);
                Direction destination = chooseDestination(item);
                if (destination == null) {
                    if (stripes != null && extension == null && isItemPipe(item.stack)) {
                        extension = item.stack;
                    } else {
                        List<ItemStack> leftovers = stripes != null ? useWithStripes(item.stack, stripes) : null;
                        if (leftovers == null) {
                            Block.popResource(this.level, this.worldPosition, item.stack);
                        } else {
                            returned.addAll(leftovers);
                        }
                    }
                    iterator.remove();
                    this.dirty = true;
                    continue;
                }
                item.to = destination;
                item.progress = 0.5F;
                this.dirty = true;
            }
            if (item.to != null && item.progress >= 1.0F) {
                ItemStack rest = deliver(item);
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
        for (ItemStack back : returned) receive(back, stripes, PipeType.BASE_SPEED);
        if (extension != null) {
            moveStripes(extension, stripes);
            return;
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
            if (canDeliver(item, direction)) options.add(direction);
        }
        PipeType type = type();
        if (type == PipeType.IRON) {
            Direction output = state().getValue(DirectionalPipeBlock.SPECIAL);
            options.removeIf(direction -> direction != output);
        } else if (type == PipeType.DAIZULI) {
            BlockState state = state();
            Direction special = state.getValue(DirectionalPipeBlock.SPECIAL);
            if (item.colour == state.getValue(ColoredPipeBlock.COLOR)) {
                options.removeIf(direction -> direction != special);
            } else {
                options.remove(special);
            }
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
        if (options.size() > 1) options = preferredByFilters(item, options);
        return options.isEmpty() ? null : options.get(this.level.getRandom().nextInt(options.size()));
    }

    /** Filtros coloridos: a cor igual tem preferência; item sem cor fica para depois ({@code sideCheck}). */
    private List<Direction> preferredByFilters(TravellingItem item, List<Direction> options) {
        int best = Integer.MIN_VALUE;
        List<Direction> chosen = new ArrayList<>();
        for (Direction direction : options) {
            int priority = 0;
            PipePlugs.Lens own = this.plugs.lens(direction);
            if (own != null && own.filter()) priority += filterPriority(item.colour, own.colour());
            DyeColor exit = own != null && !own.filter() ? own.colour() : item.colour;
            if (this.level.getBlockEntity(this.worldPosition.relative(direction)) instanceof PipeBlockEntity next) {
                PipePlugs.Lens theirs = next.plugs().lens(direction.getOpposite());
                if (theirs != null && theirs.filter()) priority += filterPriority(exit, theirs.colour());
            }
            if (priority > best) {
                best = priority;
                chosen.clear();
            }
            if (priority == best) chosen.add(direction);
        }
        return chosen;
    }

    private static int filterPriority(@Nullable DyeColor item, @Nullable DyeColor filter) {
        if (Objects.equals(item, filter)) return 1;
        return item == null ? -1 : 0;
    }

    /** Filtro na face: item pintado de outra cor não passa. */
    public boolean acceptsColour(Direction side, @Nullable DyeColor colour) {
        PipePlugs.Lens lens = this.plugs.lens(side);
        return lens == null || !lens.filter() || colour == null || colour == lens.colour();
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

    private boolean canDeliver(TravellingItem item, Direction direction) {
        if (!acceptsColour(direction, item.colour)) return false;
        PipePlugs.Lens own = this.plugs.lens(direction);
        DyeColor exit = own != null && !own.filter() ? own.colour() : item.colour;
        BlockPos other = this.worldPosition.relative(direction);
        if (this.level.getBlockEntity(other) instanceof PipeBlockEntity pipe) {
            return pipe.type().carriesItems() && pipe.connected(direction.getOpposite()) && pipe.acceptsColour(direction.getOpposite(), exit);
        }
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(this.level, other, direction.getOpposite());
        if (storage == null) return false;
        try (Transaction transaction = Transaction.openOuter()) {
            return storage.insert(ItemVariant.of(item.stack), item.stack.getCount(), transaction) > 0;
        }
    }

    private ItemStack deliver(TravellingItem item) {
        Direction direction = item.to;
        PipePlugs.Lens own = this.plugs.lens(direction);
        if (own != null && !own.filter()) item.colour = own.colour();
        BlockPos other = this.worldPosition.relative(direction);
        if (this.level.getBlockEntity(other) instanceof PipeBlockEntity pipe && pipe.type().carriesItems() && pipe.connected(direction.getOpposite())
                && pipe.acceptsColour(direction.getOpposite(), item.colour)) {
            pipe.receive(item.stack, direction.getOpposite(), item.speed, item.colour);
            return ItemStack.EMPTY;
        }
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(this.level, other, direction.getOpposite());
        if (storage == null) return item.stack;
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = storage.insert(ItemVariant.of(item.stack), item.stack.getCount(), transaction);
            transaction.commit();
            ItemStack rest = item.stack.copy();
            rest.shrink((int) inserted);
            return rest;
        }
    }

    /** Um item entrando pelo lado {@code from} (nulo = direto no centro). */
    public void receive(ItemStack stack, @Nullable Direction from, float speed) {
        receive(stack, from, speed, null);
    }

    /** Item (pintado ou não) entrando; uma lente na face de entrada pinta. */
    public void receive(ItemStack stack, @Nullable Direction from, float speed, @Nullable DyeColor colour) {
        if (stack.isEmpty() || !type().carriesItems()) return;
        PipePlugs.Lens lens = from == null ? null : this.plugs.lens(from);
        TravellingItem item = new TravellingItem(stack.copy(), from, null, from == null ? 0.5F : 0.0F, Math.max(PipeType.BASE_SPEED, speed));
        item.colour = lens != null && !lens.filter() ? lens.colour() : colour;
        this.items.add(item);
        this.dirty = true;
        setChanged();
    }

    // ── extração (madeira, madeira-diamante, emzuli) ──────────────────────
    /** Puxa do inventário da direção especial, 1 CWh por item. */
    private void extract() {
        if (this.energy == null || this.energy.stored() < ENERGY_PER_ITEM) return;
        Direction source = state().getValue(DirectionalPipeBlock.SPECIAL);
        if (!connected(source) || this.level.getBlockState(this.worldPosition.relative(source)).getBlock() instanceof PipeBlock) return;
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(this.level, this.worldPosition.relative(source), source.getOpposite());
        if (storage == null) return;
        PipeType type = type();
        Predicate<ItemVariant> filter = variant -> true;
        long maxCount = Long.MAX_VALUE;
        DyeColor paint = null;
        if (type == PipeType.DIAMOND_WOOD) {
            filter = diamondWoodFilter();
            maxCount = 1;
        } else if (type == PipeType.EMZULI) {
            if (this.currentPreset < 0 && this.activePresets != 0) this.currentPreset = nextPreset();
            if (this.currentPreset < 0) return;
            ItemStack preset = this.filters.getItem(this.currentPreset);
            filter = variant -> variant.isOf(preset.getItem());
            paint = this.presetColours[this.currentPreset];
        }
        long affordable = this.energy.stored() / ENERGY_PER_ITEM;
        try (Transaction transaction = Transaction.openOuter()) {
            ItemVariant resource = StorageUtil.findExtractableResource(storage, filter, transaction);
            if (resource == null) return;
            long wanted = Math.min(Math.min(affordable, maxCount), resource.toStack().getMaxStackSize());
            long extracted = storage.extract(resource, wanted, transaction);
            if (extracted <= 0) return;
            transaction.commit();
            this.energy.use(extracted * ENERGY_PER_ITEM);
            TravellingItem item = new TravellingItem(resource.toStack((int) extracted), source, null, 0.0F, PipeType.EXTRACT_SPEED);
            item.colour = paint;
            this.items.add(item);
            this.dirty = true;
        }
        if (type == PipeType.DIAMOND_WOOD && this.filterMode == MODE_ROUND_ROBIN) advanceFilter();
        if (type == PipeType.EMZULI) this.currentPreset = nextPreset();
    }

    private boolean anyFilter(int slots) {
        for (int slot = 0; slot < slots; slot++) {
            if (!this.filters.getItem(slot).isEmpty()) return true;
        }
        return false;
    }

    private boolean matchesFilters(ItemVariant variant) {
        for (int slot = 0; slot < FILTER_WIDTH; slot++) {
            ItemStack filter = this.filters.getItem(slot);
            if (!filter.isEmpty() && variant.isOf(filter.getItem())) return true;
        }
        return false;
    }

    /** Lista branca (vazia = tudo), lista negra ou um filtro por vez em rodízio. */
    private Predicate<ItemVariant> diamondWoodFilter() {
        return switch (this.filterMode) {
            case MODE_BLACKLIST -> variant -> !matchesFilters(variant);
            case MODE_ROUND_ROBIN -> {
                if (this.filters.getItem(this.currentFilter).isEmpty()) advanceFilter();
                ItemStack filter = this.filters.getItem(this.currentFilter);
                yield filter.isEmpty() ? variant -> false : variant -> variant.isOf(filter.getItem());
            }
            default -> anyFilter(FILTER_WIDTH) ? this::matchesFilters : variant -> true;
        };
    }

    private void advanceFilter() {
        for (int step = 1; step <= FILTER_WIDTH; step++) {
            int slot = (this.currentFilter + step) % FILTER_WIDTH;
            if (!this.filters.getItem(slot).isEmpty()) {
                this.currentFilter = slot;
                return;
            }
        }
    }

    public void setFilterMode(int mode) {
        if (mode < MODE_WHITELIST || mode > MODE_ROUND_ROBIN) return;
        this.filterMode = mode;
        setChanged();
    }

    /** Próximo preset ativo com filtro, na ordem vermelho, verde, azul, amarelo; -1 se nenhum. */
    private int nextPreset() {
        int start = this.currentPreset < 0 ? PRESETS - 1 : this.currentPreset;
        for (int step = 1; step <= PRESETS; step++) {
            int slot = (start + step) % PRESETS;
            if ((this.activePresets >> slot & 1) != 0 && !this.filters.getItem(slot).isEmpty()) return slot;
        }
        return -1;
    }

    private void tickPresets() {
        for (int slot = 0; slot < PRESETS; slot++) {
            if (this.presetTtl[slot] > 0 && --this.presetTtl[slot] == 0) {
                this.activePresets &= ~(1 << slot);
                if (this.currentPreset == slot) this.currentPreset = nextPreset();
            }
        }
    }

    /** Ação de porta lógica: mantém o preset ativo enquanto ela continuar ligada. */
    public void activatePreset(int slot) {
        if (slot < 0 || slot >= PRESETS) return;
        this.activePresets |= 1 << slot;
        this.presetTtl[slot] = 2;
    }

    public @Nullable DyeColor presetColour(int slot) {
        return this.presetColours[slot];
    }

    /** Botões de cor da tela do emzuli: {@code slot * 3 + op} (0 próxima, 1 anterior, 2 sem cor). */
    public void handlePresetButton(int id) {
        int slot = id / 3;
        int op = id % 3;
        if (slot < 0 || slot >= PRESETS) return;
        DyeColor current = this.presetColours[slot];
        if (op == 2) {
            this.presetColours[slot] = null;
        } else if (current == null) {
            this.presetColours[slot] = op == 0 ? DyeColor.WHITE : DyeColor.BLACK;
        } else if ((op == 0 && current == DyeColor.BLACK) || (op == 1 && current == DyeColor.WHITE)) {
            this.presetColours[slot] = null;
        } else {
            this.presetColours[slot] = PipeColours.cycle(current, op == 0 ? 1 : -1);
        }
        setChanged();
    }

    /** Ação de porta lógica dos tubos lápis e daizuli. */
    public void setPipeColour(DyeColor colour) {
        BlockState state = state();
        if (this.level != null && state.hasProperty(ColoredPipeBlock.COLOR) && state.getValue(ColoredPipeBlock.COLOR) != colour) {
            this.level.setBlock(this.worldPosition, state.setValue(ColoredPipeBlock.COLOR, colour), Block.UPDATE_ALL);
        }
    }

    // ── stripes ───────────────────────────────────────────────────────────
    /** A ponta do stripes: oposta à única ligação (sem ligação ou com várias, não trabalha). */
    public @Nullable Direction stripesDirection() {
        Direction connection = null;
        for (Direction direction : Direction.values()) {
            if (!connected(direction)) continue;
            if (connection != null) return null;
            connection = direction;
        }
        return connection == null ? null : connection.getOpposite();
    }

    /** Quebra o bloco da ponta (BuildCraft: 16 MJ × (dureza + 1) × 2, até 10 MJ por tick); os drops voltam pelo tubo. */
    private void breakWithStripes(Direction direction) {
        if (!(this.level instanceof ServerLevel server) || this.energy == null) return;
        BlockPos target = this.worldPosition.relative(direction);
        BlockState state = this.level.getBlockState(target);
        float hardness = state.getDestroySpeed(this.level, target);
        if (state.isAir() || hardness < 0 || state.getBlock() instanceof PipeBlock || !state.getFluidState().isEmpty() && state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
            this.stripesProgress = 0;
            return;
        }
        long power = EnergyUnits.fromCWh(Math.floor(32.0 * (hardness + 1) * net.buildcraftreborn.BuildCraftReborn.config.miningMultiplier));
        long take = Math.min(Math.min(power - this.stripesProgress, EnergyUnits.fromCWh(10)), this.energy.stored());
        if (take > 0 && this.energy.use(take)) this.stripesProgress += take;
        if (this.stripesProgress < power) {
            if (this.stripesProgress > 0) server.destroyBlockProgress(this.worldPosition.hashCode(), target, (int) (this.stripesProgress * 9 / power));
            return;
        }
        List<ItemStack> drops = Block.getDrops(state, server, target, server.getBlockEntity(target), null, new ItemStack(Items.DIAMOND_PICKAXE));
        server.destroyBlock(target, false);
        server.destroyBlockProgress(this.worldPosition.hashCode(), target, -1);
        this.stripesProgress = 0;
        for (ItemStack drop : drops) receive(drop, direction, PipeType.BASE_SPEED);
    }

    private static boolean isItemPipe(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof PipeBlock pipe
                && pipe.flow() == net.buildcraftreborn.transport.PipeFlow.ITEM && pipe.type() != PipeType.STRIPES;
    }

    /**
     * Item que chegou à ponta do stripes: um jogador falso usa o item no bloco da ponta, embaixo dele (plantar,
     * arar) ou no ar. O que sobrar volta pelo tubo; {@code null} se nada aconteceu (o item cai).
     */
    private @Nullable List<ItemStack> useWithStripes(ItemStack stack, Direction direction) {
        if (!(this.level instanceof ServerLevel server)) return null;
        FakePlayer player = FakePlayer.get(server);
        Inventory inventory = player.getInventory();
        inventory.clearContent();
        player.setPos(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5 - player.getEyeHeight(), this.worldPosition.getZ() + 0.5);
        player.setYRot(direction.getAxis().isHorizontal() ? direction.toYRot() : 0);
        player.setXRot(direction == Direction.UP ? -90 : direction == Direction.DOWN ? 90 : 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
        BlockPos target = this.worldPosition.relative(direction);
        boolean used = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(target), direction.getOpposite(), target, false))).consumesAction();
        if (!used) {
            BlockPos below = target.below();
            used = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false))).consumesAction();
        }
        if (!used) used = player.getMainHandItem().use(server, player, InteractionHand.MAIN_HAND).consumesAction();
        List<ItemStack> leftovers = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack left = inventory.getItem(slot);
            if (!left.isEmpty()) leftovers.add(left.copy());
        }
        inventory.clearContent();
        return used ? leftovers : null;
    }

    /** Tubo de itens na ponta: o stripes anda um bloco e deixa o tubo no lugar; tubo vazio recolhe o tubo de trás. */
    private void moveStripes(ItemStack pipeItem, Direction direction) {
        if (!(this.level instanceof ServerLevel server)) return;
        PipeBlock fed = (PipeBlock) ((BlockItem) pipeItem.getItem()).getBlock();
        BlockState stripesState = state();
        if (fed.type() == PipeType.VOID) {
            BlockPos behind = this.worldPosition.relative(direction.getOpposite());
            BlockState behindState = this.level.getBlockState(behind);
            if (!(behindState.getBlock() instanceof PipeBlock behindPipe) || behindPipe.type() == PipeType.STRIPES) {
                receive(pipeItem, direction, PipeType.BASE_SPEED);
                syncToClient();
                return;
            }
            PipeBlockEntity moved = relocate(server, behind, stripesState);
            this.level.setBlock(this.worldPosition, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            if (moved != null) {
                moved.receive(new ItemStack(behindState.getBlock()), null, PipeType.BASE_SPEED);
                moved.receive(pipeItem, null, PipeType.BASE_SPEED);
            }
            return;
        }
        BlockPos target = this.worldPosition.relative(direction);
        if (!this.level.getBlockState(target).canBeReplaced()) {
            receive(pipeItem, direction, PipeType.BASE_SPEED);
            syncToClient();
            return;
        }
        PipeBlockEntity moved = relocate(server, target, stripesState);
        this.level.setBlock(this.worldPosition, fed.defaultBlockState(), Block.UPDATE_ALL);
        if (moved != null && pipeItem.getCount() > 1) moved.receive(pipeItem.copyWithCount(pipeItem.getCount() - 1), null, PipeType.BASE_SPEED);
    }

    /** Copia este stripes (itens, energia, encaixes) para {@code to} e esvazia o daqui, para não soltar nada em dobro. */
    private @Nullable PipeBlockEntity relocate(ServerLevel server, BlockPos to, BlockState stripesState) {
        CompoundTag data = saveCustomOnly(server.registryAccess());
        this.items.clear();
        this.plugs.load(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), new CompoundTag()));
        server.setBlock(to, stripesState.getBlock().defaultBlockState(), Block.UPDATE_ALL);
        if (!(server.getBlockEntity(to) instanceof PipeBlockEntity moved)) return null;
        moved.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, server.registryAccess(), data));
        moved.syncToClient();
        return moved;
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
        for (ItemStack gate : this.plugs.drops()) Block.popResource(this.level, pos, gate);
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
        this.plugs.save(output);
        output.putInt("FilterMode", this.filterMode);
        output.putInt("CurrentFilter", this.currentFilter);
        for (int slot = 0; slot < PRESETS; slot++) {
            output.putInt("PresetColour" + slot, this.presetColours[slot] == null ? -1 : this.presetColours[slot].ordinal());
        }
        output.putInt("ActivePresets", this.activePresets);
        output.putInt("CurrentPreset", this.currentPreset);
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
        this.plugs.load(input);
        this.filterMode = input.getIntOr("FilterMode", MODE_WHITELIST);
        this.currentFilter = Math.floorMod(input.getIntOr("CurrentFilter", 0), FILTER_WIDTH);
        DyeColor[] colours = DyeColor.values();
        for (int slot = 0; slot < PRESETS; slot++) {
            int ordinal = input.getIntOr("PresetColour" + slot, -1);
            this.presetColours[slot] = ordinal >= 0 && ordinal < colours.length ? colours[ordinal] : null;
        }
        this.activePresets = input.getIntOr("ActivePresets", 0);
        this.currentPreset = input.getIntOr("CurrentPreset", -1);
    }
}
