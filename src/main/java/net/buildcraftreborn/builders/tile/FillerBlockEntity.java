package net.buildcraftreborn.builders.tile;

import net.buildcraftreborn.builders.block.FillerBlock;
import net.buildcraftreborn.builders.filler.FillerPattern;
import net.buildcraftreborn.builders.filler.FillerTemplates;
import net.buildcraftreborn.core.marker.MarkerBlock;
import net.buildcraftreborn.core.marker.MarkerBlockEntity;
import net.buildcraftreborn.factory.tile.MiningWellBlockEntity;
import net.buildcraftreborn.lib.energy.MachineEnergy;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.craftenergy.api.EnergyUnits;
import net.craftenergy.api.MultimeterReadable;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.List;

/**
 * Preenchedor ({@code TileFiller}): segue o padrão dentro da caixa. Primeiro quebra, de cima para baixo, o que
 * deveria ser ar (se "escavar" está ligado; os blocos vão para o inventário), depois coloca, de baixo para cima,
 * os blocos dos 27 slots onde deveria haver bloco. "Inverter" troca bloco por ar. Bateria de 16.000 CWh
 * (1.000 MV, aceita a partir de 200 MV); quebrar custa como no poço de mineração e colocar 8 CWh.
 */
public class FillerBlockEntity extends BCBlockEntity implements ServerTicking, MultimeterReadable {
    public static final int SLOTS = 27;
    public static final int PARAMS = 4;
    public static final long PLACE_ENERGY = EnergyUnits.fromCWh(8);
    public static final long MAX_INPUT = 16_000;
    private static final int CHECKS_PER_TICK = 64;
    private static final int ACTIONS_PER_TICK = 4;
    private static final int IDLE_TICKS = 40;

    public static final int DATA_PATTERN = 0;
    public static final int DATA_PARAM = 1;
    public static final int DATA_FLAGS = DATA_PARAM + PARAMS;
    public static final int DATA_TO_BREAK = DATA_FLAGS + 1;
    public static final int DATA_TO_PLACE = DATA_FLAGS + 2;
    public static final int DATA_COUNT = DATA_FLAGS + 3;
    public static final int FLAG_EXCAVATE = 1;
    public static final int FLAG_INVERT = 2;
    public static final int FLAG_BOX = 4;

    public static final int BUTTON_NEXT_PATTERN = 0;
    public static final int BUTTON_PREVIOUS_PATTERN = 1;
    public static final int BUTTON_NEXT_PARAM = 10;
    public static final int BUTTON_PREVIOUS_PARAM = 20;
    public static final int BUTTON_EXCAVATE = 30;
    public static final int BUTTON_INVERT = 31;

    private final MachineEnergy energy = new MachineEnergy(this, 1_000, EnergyUnits.fromCWh(16_000), MAX_INPUT, 200);
    private final SimpleContainer resources = new SimpleContainer(SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            FillerBlockEntity.this.setChanged();
            FillerBlockEntity.this.idle = 0;
        }
    };
    private @Nullable BlockPos min;
    private @Nullable BlockPos max;
    private FillerPattern pattern = FillerPattern.NONE;
    private final int[] params = new int[PARAMS];
    private boolean excavate = true;
    private boolean inverted;
    private @Nullable BitSet template;
    private boolean templateDirty = true;
    private boolean placing;
    private int cursor;
    private int idle;
    private int passBreak;
    private int passPlace;
    private int toBreak;
    private int toPlace;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            if (index >= DATA_PARAM && index < DATA_PARAM + PARAMS) return FillerBlockEntity.this.params[index - DATA_PARAM];
            return switch (index) {
                case DATA_PATTERN -> FillerBlockEntity.this.pattern.ordinal();
                case DATA_FLAGS -> (FillerBlockEntity.this.excavate ? FLAG_EXCAVATE : 0) | (FillerBlockEntity.this.inverted ? FLAG_INVERT : 0)
                        | (hasBox() ? FLAG_BOX : 0);
                case DATA_TO_BREAK -> Math.min(Short.MAX_VALUE, FillerBlockEntity.this.toBreak);
                case DATA_TO_PLACE -> Math.min(Short.MAX_VALUE, FillerBlockEntity.this.toPlace);
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

    public FillerBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.FILLER.get(), pos, state);
    }

    public MachineEnergy energy() {
        return this.energy;
    }

    public SimpleContainer resources() {
        return this.resources;
    }

    public ContainerData data() {
        return this.data;
    }

    public boolean hasBox() {
        return this.min != null && this.max != null;
    }

    public FillerPattern pattern() {
        return this.pattern;
    }

    // ── área ──────────────────────────────────────────────────────────────
    /** Pega a caixa de um marcador de área encostado e recolhe os marcadores. */
    public void initArea() {
        if (this.level == null) return;
        for (Direction direction : Direction.values()) {
            BlockPos markerPos = this.worldPosition.relative(direction);
            BoundingBox box = MarkerBlockEntity.volumeAt(this.level, markerPos);
            if (box == null) continue;
            MarkerBlockEntity marker = MarkerBlockEntity.at(this.level, markerPos, MarkerBlock.Kind.VOLUME);
            if (marker != null) {
                for (BlockPos member : marker.group()) this.level.destroyBlock(member, true);
            }
            setArea(new BlockPos(box.minX(), box.minY(), box.minZ()), new BlockPos(box.maxX(), box.maxY(), box.maxZ()));
            return;
        }
    }

    public void setArea(BlockPos min, BlockPos max) {
        this.min = new BlockPos(Math.min(min.getX(), max.getX()), Math.min(min.getY(), max.getY()), Math.min(min.getZ(), max.getZ()));
        this.max = new BlockPos(Math.max(min.getX(), max.getX()), Math.max(min.getY(), max.getY()), Math.max(min.getZ(), max.getZ()));
        restart();
    }

    // ── configuração ──────────────────────────────────────────────────────
    public void setPattern(FillerPattern pattern) {
        this.pattern = pattern;
        System.arraycopy(pattern.defaults, 0, this.params, 0, pattern.defaults.length);
        for (int i = pattern.defaults.length; i < PARAMS; i++) this.params[i] = 0;
        restart();
    }

    public void setExcavate(boolean excavate) {
        this.excavate = excavate;
        restart();
    }

    public void setInverted(boolean inverted) {
        this.inverted = inverted;
        restart();
    }

    /** Botões da tela (ids em {@code BUTTON_*}). */
    public void handleButton(int id) {
        if (id == BUTTON_NEXT_PATTERN || id == BUTTON_PREVIOUS_PATTERN) {
            setPattern(FillerPattern.byIndex(this.pattern.ordinal() + (id == BUTTON_NEXT_PATTERN ? 1 : -1)));
        } else if (id >= BUTTON_NEXT_PARAM && id < BUTTON_NEXT_PARAM + PARAMS) {
            cycleParam(id - BUTTON_NEXT_PARAM, 1);
        } else if (id >= BUTTON_PREVIOUS_PARAM && id < BUTTON_PREVIOUS_PARAM + PARAMS) {
            cycleParam(id - BUTTON_PREVIOUS_PARAM, -1);
        } else if (id == BUTTON_EXCAVATE) {
            setExcavate(!this.excavate);
        } else if (id == BUTTON_INVERT) {
            setInverted(!this.inverted);
        }
    }

    private void cycleParam(int index, int step) {
        if (index >= this.pattern.params.length) return;
        this.params[index] = Math.floorMod(this.params[index] + step, this.pattern.params[index].values.length);
        restart();
    }

    private void restart() {
        this.templateDirty = true;
        this.placing = false;
        this.cursor = 0;
        this.idle = 0;
        this.passBreak = 0;
        this.passPlace = 0;
        setChanged();
    }

    // ── trabalho ──────────────────────────────────────────────────────────
    @Override
    public void serverTick() {
        if (!(this.level instanceof ServerLevel server) || !hasBox()) return;
        int sx = this.max.getX() - this.min.getX() + 1;
        int sy = this.max.getY() - this.min.getY() + 1;
        int sz = this.max.getZ() - this.min.getZ() + 1;
        if (this.templateDirty) {
            this.template = FillerTemplates.build(this.pattern, this.params, sx, sy, sz);
            this.templateDirty = false;
        }
        if (this.template == null) {
            this.toBreak = 0;
            this.toPlace = 0;
            return;
        }
        if (this.idle > 0) {
            this.idle--;
            return;
        }
        int volume = sx * sy * sz;
        int actions = 0;
        for (int checks = 0; checks < CHECKS_PER_TICK && actions < ACTIONS_PER_TICK; checks++) {
            if (this.cursor >= volume) {
                if (!this.placing) {
                    this.toBreak = this.passBreak;
                    this.passBreak = 0;
                    this.placing = true;
                } else {
                    this.toPlace = this.passPlace;
                    this.passPlace = 0;
                    this.placing = false;
                    if (this.toBreak == 0 && this.toPlace == 0) this.idle = IDLE_TICKS;
                }
                this.cursor = 0;
                if (this.idle > 0) return;
            }
            int layerSize = sx * sz;
            int layer = this.cursor / layerSize;
            int y = this.placing ? layer : sy - 1 - layer;
            int z = (this.cursor % layerSize) / sx;
            int x = (this.cursor % layerSize) % sx;
            BlockPos pos = this.min.offset(x, y, z);
            boolean wantBlock = this.template.get(FillerTemplates.index(x, y, z, sx, sz)) != this.inverted;
            BlockState state = this.level.getBlockState(pos);

            if (!this.placing) {
                if (!wantBlock && this.excavate && canBreak(state, pos)) {
                    this.passBreak++;
                    long cost = MiningWellBlockEntity.breakEnergy(this.level, pos, state);
                    if (this.energy.stored() < cost) return;
                    this.energy.use(cost);
                    breakBlock(server, pos, state);
                    actions++;
                }
            } else if (wantBlock && state.canBeReplaced()) {
                this.passPlace++;
                int slot = placeableSlot();
                if (slot >= 0) {
                    if (this.energy.stored() < PLACE_ENERGY) return;
                    this.energy.use(PLACE_ENERGY);
                    ItemStack stack = this.resources.getItem(slot);
                    this.level.setBlock(pos, ((BlockItem) stack.getItem()).getBlock().defaultBlockState(), Block.UPDATE_ALL);
                    this.resources.removeItem(slot, 1);
                    actions++;
                }
            }
            this.cursor++;
        }
    }

    private boolean canBreak(BlockState state, BlockPos pos) {
        if (state.isAir() || state.hasBlockEntity() || state.getBlock() instanceof LiquidBlock) return false;
        return state.getDestroySpeed(this.level, pos) >= 0 && !(state.getBlock() instanceof FillerBlock);
    }

    private void breakBlock(ServerLevel server, BlockPos pos, BlockState state) {
        List<ItemStack> drops = Block.getDrops(state, server, pos, null);
        server.destroyBlock(pos, false);
        for (ItemStack drop : drops) {
            ItemStack rest = this.resources.addItem(drop);
            if (!rest.isEmpty()) Block.popResource(server, this.worldPosition.above(), rest);
        }
    }

    private int placeableSlot() {
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = this.resources.getItem(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return slot;
        }
        return -1;
    }

    /** Funis e tubos só colocam material. */
    public Storage<ItemVariant> itemStorage(@Nullable Direction face) {
        return FilteringStorage.insertOnlyOf(ContainerStorage.of(this.resources, face));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) Containers.dropContents(this.level, pos, this.resources);
    }

    @Override
    public void multimeterReading(List<Double> values, List<String> units) {
        MultimeterReadable.electric(values, units, this.energy.voltage(), this.energy.lastReceived());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.save(output);
        for (int slot = 0; slot < SLOTS; slot++) output.store("Slot" + slot, ItemStack.OPTIONAL_CODEC, this.resources.getItem(slot));
        output.putBoolean("HasBox", hasBox());
        if (hasBox()) {
            output.putLong("Min", this.min.asLong());
            output.putLong("Max", this.max.asLong());
        }
        output.putInt("Pattern", this.pattern.ordinal());
        for (int i = 0; i < PARAMS; i++) output.putInt("Param" + i, this.params[i]);
        output.putBoolean("Excavate", this.excavate);
        output.putBoolean("Inverted", this.inverted);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.load(input);
        for (int slot = 0; slot < SLOTS; slot++) this.resources.setItem(slot, input.read("Slot" + slot, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        if (input.getBooleanOr("HasBox", false)) {
            this.min = BlockPos.of(input.getLongOr("Min", 0L));
            this.max = BlockPos.of(input.getLongOr("Max", 0L));
        }
        this.pattern = FillerPattern.byIndex(input.getIntOr("Pattern", 0));
        for (int i = 0; i < PARAMS; i++) this.params[i] = input.getIntOr("Param" + i, 0);
        this.excavate = input.getBooleanOr("Excavate", true);
        this.inverted = input.getBooleanOr("Inverted", false);
        this.templateDirty = true;
    }
}
