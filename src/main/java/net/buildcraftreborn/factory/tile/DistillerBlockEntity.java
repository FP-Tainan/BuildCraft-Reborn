package net.buildcraftreborn.factory.tile;

import net.buildcraftreborn.factory.refinery.RefineryRecipes;
import net.buildcraftreborn.lib.energy.MachineEnergy;
import net.buildcraftreborn.lib.fluid.BCTank;
import net.buildcraftreborn.lib.fluid.FluidText;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.craftenergy.api.EnergyUnits;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Destilador ({@code TileDistiller_BC8}): tanques de 4.000 CL, até 6.000 CW a 1.000 MV (aceita a partir de
 * 200 MV) e bateria de 1.024 CWh. Cada lote gasta a energia da receita; o gás sobe para quem estiver em cima
 * e o líquido desce para quem estiver embaixo.
 */
public class DistillerBlockEntity extends BCBlockEntity implements ServerTicking, MultimeterReadable {
    public static final long TANK_CL = 4_000;
    public static final long MAX_POWER = 6_000;
    private static final long PUSH_CL = 100;
    public static final int TANK_INPUT = 0;
    public static final int TANK_GAS = 1;
    public static final int TANK_LIQUID = 2;

    private final MachineEnergy energy = new MachineEnergy(this, 1_000, EnergyUnits.fromCWh(1_024), MAX_POWER, 200);
    private final BCTank input = new BCTank(TANK_CL, variant -> RefineryRecipes.distillation(variant) != null, this::changed);
    private final BCTank gas = new BCTank(TANK_CL, this::changed);
    private final BCTank liquid = new BCTank(TANK_CL, this::changed);
    private final double[] shown = {-1, -1, -1};
    private long progress;
    private boolean active;
    private boolean dirty;

    public DistillerBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.DISTILLER.get(), pos, state);
    }

    private void changed() {
        this.dirty = true;
        setChanged();
    }

    public MachineEnergy energy() {
        return this.energy;
    }

    public BCTank tank(int index) {
        return switch (index) {
            case TANK_GAS -> this.gas;
            case TANK_LIQUID -> this.liquid;
            default -> this.input;
        };
    }

    public boolean isActive() {
        return this.active;
    }

    @Override
    public void serverTick() {
        if (this.level == null) return;
        push(Direction.UP, this.gas);
        push(Direction.DOWN, this.liquid);

        RefineryRecipes.Distillation recipe = RefineryRecipes.distillation(this.input.variant);
        boolean working = false;
        if (recipe == null) {
            this.progress = 0;
        } else if (canDistill(recipe)) {
            long draw = Math.min(MAX_POWER, this.energy.stored());
            if (draw > 0 && this.energy.use(draw)) {
                this.progress += draw;
                working = true;
            }
            if (this.progress >= recipe.energy()) {
                this.progress -= recipe.energy();
                distill(recipe);
            }
        }
        if (working != this.active) {
            this.active = working;
            this.dirty = true;
        }
        if (this.dirty && this.level.getGameTime() % 5 == 0) {
            this.dirty = false;
            syncToClient();
        }
    }

    private boolean canDistill(RefineryRecipes.Distillation recipe) {
        return this.input.amountCL() >= recipe.inputCL() && fits(this.gas, recipe.gas(), recipe.gasCL())
                && fits(this.liquid, recipe.liquid(), recipe.liquidCL());
    }

    private static boolean fits(BCTank tank, FluidVariant variant, long cl) {
        try (Transaction transaction = Transaction.openOuter()) {
            return tank.insert(variant, BCTank.fromCL(cl), transaction) == BCTank.fromCL(cl);
        }
    }

    private void distill(RefineryRecipes.Distillation recipe) {
        try (Transaction transaction = Transaction.openOuter()) {
            this.input.extract(recipe.input(), BCTank.fromCL(recipe.inputCL()), transaction);
            this.gas.insert(recipe.gas(), BCTank.fromCL(recipe.gasCL()), transaction);
            this.liquid.insert(recipe.liquid(), BCTank.fromCL(recipe.liquidCL()), transaction);
            transaction.commit();
        }
    }

    private void push(Direction direction, BCTank tank) {
        if (tank.isEmpty()) return;
        Storage<FluidVariant> target = FluidStorage.SIDED.find(this.level, this.worldPosition.relative(direction), direction.getOpposite());
        if (target == null) return;
        try (Transaction transaction = Transaction.openOuter()) {
            StorageUtil.move(tank, target, variant -> true, BCTank.fromCL(PUSH_CL), transaction);
            transaction.commit();
        }
    }

    /** Em cima só sai gás, embaixo só líquido, pelos lados só entra; sem lado (balde) faz tudo. */
    public Storage<FluidVariant> storage(@Nullable Direction face) {
        if (face == Direction.UP) return FilteringStorage.extractOnlyOf(this.gas);
        if (face == Direction.DOWN) return FilteringStorage.extractOnlyOf(this.liquid);
        if (face != null) return FilteringStorage.insertOnlyOf(this.input);
        return new CombinedStorage<>(List.of(FilteringStorage.insertOnlyOf(this.input),
                FilteringStorage.extractOnlyOf(this.gas), FilteringStorage.extractOnlyOf(this.liquid)));
    }

    public Component status() {
        return Component.translatable("gui.buildcraftreborn.distiller.status",
                FluidText.of(this.input), FluidText.of(this.gas), FluidText.of(this.liquid));
    }

    /** Cliente: nível mostrado andando suave até o recebido. */
    public double shownRatio(int index) {
        double target = tank(index).ratio();
        if (this.shown[index] < 0) this.shown[index] = target;
        this.shown[index] += (target - this.shown[index]) * 0.1;
        if (Math.abs(target - this.shown[index]) < 0.002) this.shown[index] = target;
        return this.shown[index];
    }

    @Override
    public void multimeterReading(List<Double> values, List<String> units) {
        MultimeterReadable.electric(values, units, this.energy.voltage(), this.energy.lastReceived());
        values.add((double) this.input.amountCL());
        units.add("CL");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.save(output);
        this.input.save(output, "Input");
        this.gas.save(output, "Gas");
        this.liquid.save(output, "Liquid");
        output.putLong("Progress", this.progress);
        output.putBoolean("Active", this.active);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.load(input);
        this.input.load(input, "Input");
        this.gas.load(input, "Gas");
        this.liquid.load(input, "Liquid");
        this.progress = input.getLongOr("Progress", 0L);
        this.active = input.getBooleanOr("Active", false);
    }
}
