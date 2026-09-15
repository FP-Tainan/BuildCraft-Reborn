package net.buildcraftreborn.builders.tile;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.builders.block.QuarryBlock;
import net.buildcraftreborn.core.marker.MarkerBlock;
import net.buildcraftreborn.core.marker.MarkerBlockEntity;
import net.buildcraftreborn.factory.FactoryUtil;
import net.buildcraftreborn.factory.tile.MiningWellBlockEntity;
import net.buildcraftreborn.lib.energy.MachineEnergy;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.buildcraftreborn.registry.BCBlocks;
import net.craftenergy.api.EnergyUnits;
import net.craftenergy.api.MultimeterReadable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Pedreira do BuildCraft: primeiro levanta a armação nas bordas da área (24 CWh por peça, quebrando o
 * que estiver no caminho), depois cava camada por camada dentro dela até a rocha-mãe (dureza × 16 CWh
 * por bloco + 1 CWh de movimento da broca). Itens vão para os inventários ao lado. Mantém os chunks da
 * área carregados enquanto trabalha.
 */
public class QuarryBlockEntity extends BCBlockEntity implements ServerTicking, MultimeterReadable, net.buildcraftreborn.lib.tile.ItemPipeConnectable {
    public static final int VOLTAGE = 1_000;
    /** Aceita desde os motores de 220 MV. */
    public static final int MIN_VOLTAGE = 200;
    public static final long MAX_INPUT = 20_000;
    public static final long FRAME_ENERGY = EnergyUnits.fromCWh(24);
    public static final long MOVE_ENERGY = EnergyUnits.fromCWh(1);
    public static final int DEFAULT_SIZE = 11;
    public static final int MIN_HEIGHT = 4;
    private static final int CHECKS_PER_TASK = 512;
    private static final int SYNC_INTERVAL = 5;

    public enum Stage { FRAME, MINE, DONE }

    private final MachineEnergy energy = new MachineEnergy(this, VOLTAGE, EnergyUnits.fromCWh(1_000), MAX_INPUT, MIN_VOLTAGE);
    private @Nullable BlockPos min;
    private @Nullable BlockPos max;
    private Stage stage = Stage.FRAME;
    private int frameIndex;
    private @Nullable BlockPos cursor;
    private @Nullable BlockPos drill;
    private long minedBlocks;
    private @Nullable List<BlockPos> frames;
    private boolean chunksForced;
    private boolean dirty;
    private int syncTimer;
    // cliente
    private @Nullable Vec3 shownDrill;

    public QuarryBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.QUARRY.get(), pos, state);
    }

    public MachineEnergy energy() {
        return this.energy;
    }

    public Stage stage() {
        return this.stage;
    }

    public @Nullable BlockPos min() {
        return this.min;
    }

    public @Nullable BlockPos max() {
        return this.max;
    }

    public long minedBlocks() {
        return this.minedBlocks;
    }

    // ── área ──────────────────────────────────────────────────────────────
    /** Usa a caixa dos marcadores encostados (e recolhe os marcadores); sem eles, 11×11 atrás da pedreira. */
    public void initArea() {
        if (this.level == null) return;
        this.min = null;
        for (Direction direction : Direction.values()) {
            BlockPos markerPos = this.worldPosition.relative(direction);
            BoundingBox box = MarkerBlockEntity.volumeAt(this.level, markerPos);
            if (box == null || box.getXSpan() < 3 || box.getZSpan() < 3) continue;
            MarkerBlockEntity marker = MarkerBlockEntity.at(this.level, markerPos, MarkerBlock.Kind.VOLUME);
            if (marker != null) {
                for (BlockPos member : marker.group()) this.level.destroyBlock(member, true);
            }
            this.min = new BlockPos(box.minX(), box.minY(), box.minZ());
            this.max = new BlockPos(box.maxX(), Math.max(box.maxY(), box.minY() + MIN_HEIGHT - 1), box.maxZ());
            break;
        }
        if (this.min == null) {
            Direction back = getBlockState().getValue(QuarryBlock.FACING).getOpposite();
            Direction side = back.getClockWise();
            int half = DEFAULT_SIZE / 2;
            BlockPos a = this.worldPosition.relative(back).relative(side, half);
            BlockPos b = this.worldPosition.relative(back, DEFAULT_SIZE).relative(side, -half).above(MIN_HEIGHT - 1);
            this.min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
            this.max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        }
        this.stage = Stage.FRAME;
        this.frameIndex = 0;
        this.cursor = null;
        this.drill = null;
        this.frames = null;
        syncToClient();
    }

    /** Posições das bordas da caixa (onde vai a armação), de baixo para cima. */
    private List<BlockPos> frames() {
        if (this.frames == null) {
            List<BlockPos> list = new ArrayList<>();
            if (this.min != null && this.max != null) {
                for (int y = this.min.getY(); y <= this.max.getY(); y++) {
                    for (int x = this.min.getX(); x <= this.max.getX(); x++) {
                        for (int z = this.min.getZ(); z <= this.max.getZ(); z++) {
                            int edges = (x == this.min.getX() || x == this.max.getX() ? 1 : 0)
                                    + (y == this.min.getY() || y == this.max.getY() ? 1 : 0)
                                    + (z == this.min.getZ() || z == this.max.getZ() ? 1 : 0);
                            BlockPos pos = new BlockPos(x, y, z);
                            if (edges >= 2 && !pos.equals(this.worldPosition)) list.add(pos);
                        }
                    }
                }
            }
            this.frames = list;
        }
        return this.frames;
    }

    // ── tick ──────────────────────────────────────────────────────────────
    @Override
    public void serverTick() {
        if (!(this.level instanceof ServerLevel server)) return;
        if (this.min == null || this.max == null) initArea();
        forceChunks(server, this.stage != Stage.DONE);
        for (int task = 0; task < BuildCraftReborn.config.quarryMaxTasksPerTick; task++) {
            if (!step(server)) break;
        }
        if (this.dirty && ++this.syncTimer >= SYNC_INTERVAL) {
            this.syncTimer = 0;
            this.dirty = false;
            syncToClient();
        }
    }

    /** Uma tarefa (quebrar, pôr armação ou minerar um bloco). Devolve {@code false} quando não dá para continuar. */
    private boolean step(ServerLevel server) {
        switch (this.stage) {
            case FRAME -> {
                List<BlockPos> list = frames();
                while (this.frameIndex < list.size()) {
                    BlockPos pos = list.get(this.frameIndex);
                    BlockState state = server.getBlockState(pos);
                    if (state.is(BCBlocks.FRAME.get())) {
                        this.frameIndex++;
                        continue;
                    }
                    if (!state.isAir() && !state.canBeReplaced()) {
                        if (state.getDestroySpeed(server, pos) < 0) {
                            this.frameIndex++;
                            continue;
                        }
                        if (!this.energy.use(MiningWellBlockEntity.breakEnergy(server, pos, state))) return false;
                        breakBlock(server, pos, state);
                        return true;
                    }
                    if (!this.energy.use(FRAME_ENERGY)) return false;
                    server.setBlock(pos, BCBlocks.FRAME.get().defaultBlockState(), Block.UPDATE_ALL);
                    this.frameIndex++;
                    this.dirty = true;
                    return true;
                }
                this.stage = Stage.MINE;
                this.cursor = new BlockPos(this.min.getX() + 1, this.max.getY(), this.min.getZ() + 1);
                this.dirty = true;
                return true;
            }
            case MINE -> {
                if (this.max.getX() - this.min.getX() < 2 || this.max.getZ() - this.min.getZ() < 2) {
                    finish();
                    return false;
                }
                for (int checks = 0; checks < CHECKS_PER_TASK; checks++) {
                    if (this.cursor.getY() < server.getMinY()
                            || this.worldPosition.getY() - this.cursor.getY() > BuildCraftReborn.config.miningMaxDepth) {
                        finish();
                        return false;
                    }
                    BlockState state = server.getBlockState(this.cursor);
                    if (canMine(server, this.cursor, state)) {
                        long cost = MiningWellBlockEntity.breakEnergy(server, this.cursor, state) + MOVE_ENERGY;
                        if (!this.energy.use(cost)) return false;
                        breakBlock(server, this.cursor, state);
                        this.drill = this.cursor.immutable();
                        this.minedBlocks++;
                        advance();
                        this.dirty = true;
                        return true;
                    }
                    advance();
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean canMine(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir() && !(state.getBlock() instanceof LiquidBlock) && !state.is(BCBlocks.FRAME.get())
                && !pos.equals(this.worldPosition) && state.getDestroySpeed(level, pos) >= 0;
    }

    /** Anda pela camada (x, depois z) e desce quando ela acaba. */
    private void advance() {
        int x = this.cursor.getX() + 1;
        int y = this.cursor.getY();
        int z = this.cursor.getZ();
        if (x > this.max.getX() - 1) {
            x = this.min.getX() + 1;
            z++;
            if (z > this.max.getZ() - 1) {
                z = this.min.getZ() + 1;
                y--;
            }
        }
        this.cursor = new BlockPos(x, y, z);
    }

    private void finish() {
        this.stage = Stage.DONE;
        this.drill = null;
        this.dirty = true;
    }

    private void breakBlock(ServerLevel level, BlockPos pos, BlockState state) {
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), null, new ItemStack(Items.DIAMOND_PICKAXE));
        level.destroyBlock(pos, false);
        for (ItemStack drop : drops) FactoryUtil.output(level, this.worldPosition, drop);
    }

    private void forceChunks(ServerLevel level, boolean enable) {
        if (enable == this.chunksForced || this.min == null || this.max == null) return;
        for (int chunkX = SectionPos.blockToSectionCoord(this.min.getX()); chunkX <= SectionPos.blockToSectionCoord(this.max.getX()); chunkX++) {
            for (int chunkZ = SectionPos.blockToSectionCoord(this.min.getZ()); chunkZ <= SectionPos.blockToSectionCoord(this.max.getZ()); chunkZ++) {
                level.setChunkForced(chunkX, chunkZ, enable);
            }
        }
        this.chunksForced = enable;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (!(this.level instanceof ServerLevel server)) return;
        forceChunks(server, false);
        for (BlockPos frame : frames()) {
            if (server.getBlockState(frame).is(BCBlocks.FRAME.get())) server.removeBlock(frame, false);
        }
    }

    // ── cliente e informações ─────────────────────────────────────────────
    public @Nullable BlockPos drill() {
        return this.drill;
    }

    /** Cliente: posição da broca andando suave até o último bloco minerado. */
    public @Nullable Vec3 shownDrill() {
        if (this.drill == null) {
            this.shownDrill = null;
            return null;
        }
        Vec3 target = Vec3.atCenterOf(this.drill);
        this.shownDrill = this.shownDrill == null ? target : this.shownDrill.add(target.subtract(this.shownDrill).scale(0.15));
        return this.shownDrill;
    }

    public Component status() {
        return switch (this.stage) {
            case FRAME -> Component.translatable("message.buildcraftreborn.quarry.frame", this.frameIndex, frames().size());
            case MINE -> Component.translatable("message.buildcraftreborn.quarry.mine", this.minedBlocks,
                    this.cursor == null ? "-" : this.cursor.getY());
            case DONE -> Component.translatable("message.buildcraftreborn.quarry.done", this.minedBlocks);
        };
    }

    @Override
    public void multimeterReading(List<Double> values, List<String> units) {
        MultimeterReadable.electric(values, units, VOLTAGE, this.energy.lastReceived());
    }

    // ── salvar ────────────────────────────────────────────────────────────
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.save(output);
        output.storeNullable("Min", BlockPos.CODEC, this.min);
        output.storeNullable("Max", BlockPos.CODEC, this.max);
        output.storeNullable("Cursor", BlockPos.CODEC, this.cursor);
        output.storeNullable("Drill", BlockPos.CODEC, this.drill);
        output.putString("Stage", this.stage.name());
        output.putInt("FrameIndex", this.frameIndex);
        output.putLong("Mined", this.minedBlocks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.load(input);
        this.min = input.read("Min", BlockPos.CODEC).orElse(null);
        this.max = input.read("Max", BlockPos.CODEC).orElse(null);
        this.cursor = input.read("Cursor", BlockPos.CODEC).orElse(null);
        this.drill = input.read("Drill", BlockPos.CODEC).orElse(null);
        try {
            this.stage = Stage.valueOf(input.getStringOr("Stage", Stage.FRAME.name()));
        } catch (IllegalArgumentException e) {
            this.stage = Stage.FRAME;
        }
        this.frameIndex = Math.max(0, input.getIntOr("FrameIndex", 0));
        this.minedBlocks = Math.max(0, input.getLongOr("Mined", 0L));
        if (this.stage == Stage.MINE && this.cursor == null) this.stage = Stage.FRAME;
        this.frames = null;
    }
}
