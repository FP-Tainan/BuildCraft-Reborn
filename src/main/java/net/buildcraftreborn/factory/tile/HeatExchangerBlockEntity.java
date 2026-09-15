package net.buildcraftreborn.factory.tile;

import net.buildcraftreborn.factory.block.HeatExchangerBlock;
import net.buildcraftreborn.factory.refinery.RefineryRecipes;
import net.buildcraftreborn.lib.fluid.BCTank;
import net.buildcraftreborn.lib.fluid.FluidText;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.craftenergy.api.MultimeterReadable;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Trocador de calor ({@code TileHeatExchange}). Cada ponta tem entrada e saída de 2.000 CL; quem trabalha é o
 * início: o fluido do fim precisa estar mais quente que o do início. Leva 6 segundos esquentando os tubos e
 * então troca 5, 10 ou 20 CL por tick (1, 2 ou 3 blocos do meio): o do início sobe uma temperatura e o do fim
 * desce. Água só esquenta (e some), lava só esfria (e some).
 */
public class HeatExchangerBlockEntity extends BCBlockEntity implements ServerTicking, MultimeterReadable {
    public static final long TANK_CL = 2_000;
    public static final int PREPARE_TICKS = 120;
    public static final int MAX_MIDDLES = 3;
    private static final long[] FLUID_PER_TICK = {5, 10, 20};
    private static final long PUSH_CL = 100;

    public enum Progress {
        OFF,
        PREPARING,
        RUNNING,
        STOPPING
    }

    private final BCTank input = new BCTank(TANK_CL, this::acceptsInput, this::changed);
    private final BCTank output = new BCTank(TANK_CL, this::changed);
    private Progress progress = Progress.OFF;
    private int progressTicks;
    private boolean dirty;

    public HeatExchangerBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.HEAT_EXCHANGER.get(), pos, state);
    }

    private void changed() {
        this.dirty = true;
        setChanged();
    }

    private BlockState state() {
        return this.level != null ? this.level.getBlockState(this.worldPosition) : getBlockState();
    }

    public HeatExchangerBlock.Part part() {
        BlockState state = state();
        return state.getBlock() instanceof HeatExchangerBlock ? state.getValue(HeatExchangerBlock.PART) : HeatExchangerBlock.Part.MIDDLE;
    }

    private Direction facing() {
        BlockState state = state();
        return state.getBlock() instanceof HeatExchangerBlock ? state.getValue(HeatExchangerBlock.FACING) : Direction.NORTH;
    }

    private boolean acceptsInput(FluidVariant variant) {
        return switch (part()) {
            case START -> RefineryRecipes.heatable(variant) != null;
            case END -> RefineryRecipes.coolable(variant) != null;
            case MIDDLE -> false;
        };
    }

    public BCTank input() {
        return this.input;
    }

    public BCTank output() {
        return this.output;
    }

    public Progress progress() {
        return this.progress;
    }

    // ── tick (só o início trabalha) ───────────────────────────────────────
    @Override
    public void serverTick() {
        if (this.level == null || part() != HeatExchangerBlock.Part.START) return;
        int middles = middleCount();
        HeatExchangerBlockEntity end = middles > 0
                && this.level.getBlockEntity(this.worldPosition.relative(facing(), middles + 1)) instanceof HeatExchangerBlockEntity found
                ? found : null;
        updateProgress();
        if (end != null) {
            exchange(end, middles);
        } else {
            stop();
        }
        push(this.output, this.worldPosition, facing().getOpposite());
        if (end != null) push(end.output, end.worldPosition, Direction.UP);
        if (this.dirty && this.level.getGameTime() % 5 == 0) {
            this.dirty = false;
            syncToClient();
        }
    }

    /** Blocos do meio até o fim; 0 se a fila não fecha (sem fim, sem meio ou meios demais). */
    public int middleCount() {
        if (this.level == null) return 0;
        Direction facing = facing();
        for (int step = 1; step <= MAX_MIDDLES + 1; step++) {
            BlockState state = this.level.getBlockState(this.worldPosition.relative(facing, step));
            if (!(state.getBlock() instanceof HeatExchangerBlock) || state.getValue(HeatExchangerBlock.FACING) != facing) return 0;
            if (state.getValue(HeatExchangerBlock.PART) == HeatExchangerBlock.Part.END) return step - 1;
        }
        return 0;
    }

    private void updateProgress() {
        switch (this.progress) {
            case STOPPING -> {
                if (--this.progressTicks <= 0) {
                    this.progressTicks = 0;
                    this.progress = Progress.OFF;
                }
            }
            case PREPARING, RUNNING -> {
                if (++this.progressTicks >= PREPARE_TICKS) {
                    this.progressTicks = PREPARE_TICKS;
                    this.progress = Progress.RUNNING;
                }
            }
            default -> {
            }
        }
    }

    private void stop() {
        if (this.progress != Progress.OFF) this.progress = Progress.STOPPING;
    }

    private void exchange(HeatExchangerBlockEntity end, int middles) {
        RefineryRecipes.HeatRecipe cool = RefineryRecipes.coolable(end.input.variant);
        RefineryRecipes.HeatRecipe heat = RefineryRecipes.heatable(this.input.variant);
        if (cool == null || heat == null || cool.heatFrom() <= heat.heatFrom()) {
            stop();
            return;
        }
        long perTick = FLUID_PER_TICK[Math.min(middles, MAX_MIDDLES) - 1];
        long amount = Math.min(perTick, Math.min(this.input.amountCL(), end.input.amountCL()));
        amount = Math.min(amount, room(this.output, heat.output(), perTick));
        amount = Math.min(amount, room(end.output, cool.output(), perTick));
        if (amount <= 0) {
            stop();
            return;
        }
        if (this.progress == Progress.OFF) {
            this.progress = Progress.PREPARING;
        } else if (this.progress == Progress.RUNNING) {
            long droplets = BCTank.fromCL(amount);
            try (Transaction transaction = Transaction.openOuter()) {
                this.input.extract(this.input.variant, droplets, transaction);
                if (heat.output() != null) this.output.insert(FluidVariant.of(heat.output()), droplets, transaction);
                end.input.extract(end.input.variant, droplets, transaction);
                if (cool.output() != null) end.output.insert(FluidVariant.of(cool.output()), droplets, transaction);
                transaction.commit();
            }
        }
    }

    private static long room(BCTank tank, @Nullable Fluid fluid, long cl) {
        if (fluid == null) return cl;
        try (Transaction transaction = Transaction.openOuter()) {
            return BCTank.toCL(tank.insert(FluidVariant.of(fluid), BCTank.fromCL(cl), transaction));
        }
    }

    private void push(BCTank tank, BlockPos from, Direction direction) {
        if (tank.isEmpty()) return;
        Storage<FluidVariant> target = FluidStorage.SIDED.find(this.level, from.relative(direction), direction.getOpposite());
        if (target == null) return;
        try (Transaction transaction = Transaction.openOuter()) {
            StorageUtil.move(tank, target, variant -> true, BCTank.fromCL(PUSH_CL), transaction);
            transaction.commit();
        }
    }

    // ── Transfer API ──────────────────────────────────────────────────────
    /**
     * Início: entra por baixo, sai pelos lados. Fim: entra pelos lados, sai por cima. Meio: nada.
     * Sem lado (balde): entra e sai.
     */
    public @Nullable Storage<FluidVariant> storage(@Nullable Direction face) {
        HeatExchangerBlock.Part part = part();
        if (part == HeatExchangerBlock.Part.MIDDLE) return null;
        if (face == null) {
            return new CombinedStorage<>(List.of(FilteringStorage.insertOnlyOf(this.input), FilteringStorage.extractOnlyOf(this.output)));
        }
        if (part == HeatExchangerBlock.Part.START) {
            if (face == Direction.DOWN) return FilteringStorage.insertOnlyOf(this.input);
            return face.getAxis().isHorizontal() ? FilteringStorage.extractOnlyOf(this.output) : null;
        }
        if (face == Direction.UP) return FilteringStorage.extractOnlyOf(this.output);
        return face.getAxis().isHorizontal() ? FilteringStorage.insertOnlyOf(this.input) : null;
    }

    public Component status() {
        HeatExchangerBlock.Part part = part();
        Component name = Component.translatable("gui.buildcraftreborn.heat_exchanger.part." + part.getSerializedName());
        if (part == HeatExchangerBlock.Part.MIDDLE) return name;
        Component line = Component.translatable("gui.buildcraftreborn.heat_exchanger.status", name, FluidText.of(this.input), FluidText.of(this.output));
        if (part == HeatExchangerBlock.Part.START) {
            Component state = middleCount() == 0 ? Component.translatable("gui.buildcraftreborn.heat_exchanger.invalid")
                    : Component.translatable("gui.buildcraftreborn.heat_exchanger.state." + this.progress.name().toLowerCase(Locale.ROOT));
            return line.copy().append(" · ").append(state);
        }
        return line;
    }

    @Override
    public void multimeterReading(List<Double> values, List<String> units) {
        values.add((double) this.input.amountCL());
        units.add("CL");
        values.add((double) this.output.amountCL());
        units.add("CL");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.input.save(output, "Input");
        this.output.save(output, "Output");
        output.putInt("Progress", this.progress.ordinal());
        output.putInt("ProgressTicks", this.progressTicks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.input.load(input, "Input");
        this.output.load(input, "Output");
        this.progress = Progress.values()[Math.clamp(input.getIntOr("Progress", 0), 0, Progress.values().length - 1)];
        this.progressTicks = input.getIntOr("ProgressTicks", 0);
    }
}
