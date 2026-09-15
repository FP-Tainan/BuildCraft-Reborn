package net.buildcraftreborn.energy.engine;

import net.buildcraftreborn.energy.fluid.BCFuels;
import net.buildcraftreborn.lib.fluid.BCTank;
import net.craftenergy.api.EnergyUnits;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.InsertionOnlyStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/**
 * Motor a combustão ({@code TileEngineIron_BC8}): tanques de combustível, refrigerante e resíduo com
 * 10.000 CL cada. Queimando, esquenta até o ideal de 100 CCº e passa disso; o refrigerante segura o calor
 * em 100. Superaquecido, para e só volta depois de esfriar até 20 CCº.
 */
public final class CombustionEngine {
    public static final long TANK_CL = 10_000;
    public static final int VOLTAGE = 1_000;
    /** BuildCraft: buffer de 10.000 MJ. */
    public static final long CAPACITY = EnergyUnits.fromCWh(10_000);
    public static final long MAX_OUTPUT = 10_000;
    public static final double MIN_HEAT = 20.0;
    public static final double IDEAL_HEAT = 100.0;
    public static final double MAX_HEAT = 250.0;
    /** BuildCraft: 0,0023 CCº por MJ gerado (por 1.000 CW a cada tick). */
    public static final double HEAT_PER_KCW = 0.0023;
    public static final int MAX_COOLANT_PER_TICK = 40;
    public static final double PASSIVE_COOLING = 0.05;
    public static final int PENALTY_TICKS = 10;

    private final Runnable onChange;
    private final BCTank fuel;
    private final BCTank coolant;
    private final BCTank residue;
    private final Storage<FluidVariant> fluidStorage;
    private final Storage<ItemVariant> itemStorage;

    private double heat = MIN_HEAT;
    private double burnTime;
    private double residueAmount;
    private int penaltyCooling;
    private long currentOutput;
    private FluidVariant burning = FluidVariant.blank();
    private boolean dirty;

    public CombustionEngine(Runnable onChange) {
        this.onChange = onChange;
        this.fuel = new BCTank(TANK_CL, variant -> BCFuels.fuel(variant) != null, this::changed);
        this.coolant = new BCTank(TANK_CL, variant -> BCFuels.cooling(variant) > 0, this::changed);
        this.residue = new BCTank(TANK_CL, variant -> true, this::changed);
        this.fluidStorage = new CombinedStorage<>(List.of(FilteringStorage.insertOnlyOf(this.fuel),
                FilteringStorage.insertOnlyOf(this.coolant), FilteringStorage.extractOnlyOf(this.residue)));
        this.itemStorage = (InsertionOnlyStorage<ItemVariant>) this::insertSolidCoolant;
    }

    private void changed() {
        this.dirty = true;
        this.onChange.run();
    }

    // ── tick ──────────────────────────────────────────────────────────────
    /** Um tick do servidor; devolve a energia gerada (CW·tick). */
    public long tick(boolean powered, boolean hasRoom) {
        long generated = burn(powered, hasRoom);
        updateHeat(powered);
        if (stage() == EngineStage.OVERHEAT) this.penaltyCooling = PENALTY_TICKS;
        return generated;
    }

    private long burn(boolean powered, boolean hasRoom) {
        this.currentOutput = 0;
        if (!powered || !hasRoom || this.penaltyCooling > 0) return 0;
        if (this.burnTime <= 0 && !consumeFuel()) return 0;
        BCFuels.Fuel fuel = BCFuels.fuel(this.burning);
        if (fuel == null) {
            this.burnTime = 0;
            return 0;
        }
        this.burnTime -= 1;
        this.currentOutput = fuel.powerCW();
        this.heat += fuel.powerCW() / 1000.0 * HEAT_PER_KCW;
        return fuel.powerCW();
    }

    /** Tira 1 CL do tanque; cada CL queima por {@code ticksPerBucket / 1000} ticks e soma o resíduo. */
    private boolean consumeFuel() {
        if (this.fuel.isEmpty()) return false;
        FluidVariant variant = this.fuel.variant;
        BCFuels.Fuel next = BCFuels.fuel(variant);
        if (next == null) return false;
        if (next.dirty()) {
            FluidVariant residueFluid = BCFuels.residue();
            if (!this.residue.isEmpty() && !this.residue.variant.equals(residueFluid)) return false;
            long pending = (long) Math.ceil(this.residueAmount + next.residueCL() / 1000.0);
            if (this.residue.amountCL() + pending > TANK_CL) return false;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            if (this.fuel.extract(variant, BCTank.fromCL(1), transaction) != BCTank.fromCL(1)) return false;
            transaction.commit();
        }
        this.burning = variant;
        this.burnTime += next.ticksPerBucket() / 1000.0;
        if (next.dirty()) {
            this.residueAmount += next.residueCL() / 1000.0;
            long whole = (long) this.residueAmount;
            if (whole > 0) {
                try (Transaction transaction = Transaction.openOuter()) {
                    long inserted = this.residue.insert(BCFuels.residue(), BCTank.fromCL(whole), transaction);
                    transaction.commit();
                    this.residueAmount -= BCTank.toCL(inserted);
                }
            }
        }
        return true;
    }

    /**
     * Sem sinal ou em penalidade, esfria 0,05 CCº por tick até 20. O que passa do ideal (100 CCº) é tirado
     * pelo refrigerante, até 40 CL por tick.
     */
    private void updateHeat(boolean powered) {
        if (this.heat > MIN_HEAT && (this.penaltyCooling > 0 || !powered)) this.heat -= PASSIVE_COOLING;
        double excess = this.heat - IDEAL_HEAT;
        if (excess > 0 && !this.coolant.isEmpty()) {
            FluidVariant variant = this.coolant.variant;
            double perCL = BCFuels.cooling(variant);
            if (perCL > 0) {
                long use = Math.min(Math.min(MAX_COOLANT_PER_TICK, this.coolant.amountCL()), (long) Math.ceil(excess / perCL));
                if (use > 0) {
                    try (Transaction transaction = Transaction.openOuter()) {
                        long drained = this.coolant.extract(variant, BCTank.fromCL(use), transaction);
                        transaction.commit();
                        this.heat = Math.max(IDEAL_HEAT, this.heat - BCTank.toCL(drained) * perCL);
                    }
                }
            }
        }
        if (this.heat <= MIN_HEAT && this.penaltyCooling > 0) this.penaltyCooling--;
        if (this.heat < MIN_HEAT) this.heat = MIN_HEAT;
    }

    /** Gelo por funis ou tubos: cada bloco vira água no tanque de refrigerante. */
    private long insertSolidCoolant(ItemVariant item, long maxAmount, TransactionContext transaction) {
        BCFuels.SolidCoolant solid = BCFuels.solidCoolant(item.getItem());
        if (solid == null) return 0;
        FluidVariant fluid = FluidVariant.of(solid.fluid());
        if (!this.coolant.isEmpty() && !this.coolant.variant.equals(fluid)) return 0;
        long each = BCTank.fromCL(solid.amountCL());
        long count = Math.min(maxAmount, (BCTank.fromCL(TANK_CL) - this.coolant.amount) / each);
        if (count <= 0) return 0;
        return this.coolant.insert(fluid, count * each, transaction) / each;
    }

    // ── consultas ─────────────────────────────────────────────────────────
    public BCTank fuelTank() {
        return this.fuel;
    }

    public BCTank coolantTank() {
        return this.coolant;
    }

    public BCTank residueTank() {
        return this.residue;
    }

    public Storage<FluidVariant> fluidStorage() {
        return this.fluidStorage;
    }

    public Storage<ItemVariant> itemStorage() {
        return this.itemStorage;
    }

    public double heat() {
        return this.heat;
    }

    public void setHeat(double heat) {
        this.heat = Math.clamp(heat, MIN_HEAT, MAX_HEAT);
    }

    /** Fração de 0 (20 CCº) a 1 (250 CCº). */
    public double heatLevel() {
        return (this.heat - MIN_HEAT) / (MAX_HEAT - MIN_HEAT);
    }

    public EngineStage stage() {
        return EngineStage.of(heatLevel());
    }

    public long currentOutput() {
        return this.currentOutput;
    }

    public boolean isPenalized() {
        return this.penaltyCooling > 0;
    }

    /** Se os tanques mudaram desde a última consulta (para sincronizar com o cliente). */
    public boolean consumeDirty() {
        boolean was = this.dirty;
        this.dirty = false;
        return was;
    }

    public void save(ValueOutput output) {
        this.fuel.save(output, "FuelTank");
        this.coolant.save(output, "CoolantTank");
        this.residue.save(output, "ResidueTank");
        output.putDouble("CombustionHeat", this.heat);
        output.putDouble("FuelBurnTime", this.burnTime);
        output.putDouble("ResidueAmount", this.residueAmount);
        output.putInt("PenaltyCooling", this.penaltyCooling);
        output.store("Burning", FluidVariant.CODEC, this.burning);
    }

    public void load(ValueInput input) {
        this.fuel.load(input, "FuelTank");
        this.coolant.load(input, "CoolantTank");
        this.residue.load(input, "ResidueTank");
        this.heat = Math.clamp(input.getDoubleOr("CombustionHeat", MIN_HEAT), MIN_HEAT, MAX_HEAT);
        this.burnTime = input.getDoubleOr("FuelBurnTime", 0.0);
        this.residueAmount = input.getDoubleOr("ResidueAmount", 0.0);
        this.penaltyCooling = input.getIntOr("PenaltyCooling", 0);
        this.burning = input.read("Burning", FluidVariant.CODEC).orElse(FluidVariant.blank());
    }
}
