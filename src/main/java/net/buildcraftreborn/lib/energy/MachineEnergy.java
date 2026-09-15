package net.buildcraftreborn.lib.energy;

import net.craftenergy.api.EnergySink;
import net.craftenergy.fabric.CraftEnergyApi;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Entrada de energia das máquinas do BuildCraft Reborn: guarda até {@code capacity} CW·tick
 * (a "bateria MJ" do original, com 1 MJ = 1 CWh) e puxa até {@code maxInput} CW por tick da rede.
 * Sobretensão explode, como no resto do pack.
 */
public class MachineEnergy implements EnergySink {
    private final BlockEntity owner;
    private final int voltage;
    private final long capacity;
    private final long maxInput;
    private long stored;
    private long lastReceived;

    /** Menor tensão aceita; negativo usa a nominal menos a tolerância. */
    private final int minVoltage;

    public MachineEnergy(BlockEntity owner, int voltage, long capacity, long maxInput) {
        this(owner, voltage, capacity, maxInput, -1);
    }

    public MachineEnergy(BlockEntity owner, int voltage, long capacity, long maxInput, int minVoltage) {
        this.owner = owner;
        this.voltage = voltage;
        this.capacity = capacity;
        this.maxInput = maxInput;
        this.minVoltage = minVoltage;
    }

    @Override
    public int nominalVoltage() {
        return this.voltage;
    }

    @Override
    public int minimumVoltage() {
        return this.minVoltage > 0 ? this.minVoltage : EnergySink.super.minimumVoltage();
    }

    @Override
    public long powerDemand() {
        return Math.max(0, Math.min(this.maxInput, this.capacity - this.stored));
    }

    @Override
    public void receivePower(long power, int voltage) {
        this.lastReceived = Math.max(0, power);
        if (power <= 0) return;
        this.stored = Math.min(this.capacity, this.stored + power);
        this.owner.setChanged();
    }

    @Override
    public void onOvervoltage(int voltage) {
        if (this.owner.getLevel() != null) {
            CraftEnergyApi.explodeFromOvervoltage(this.owner.getLevel(), this.owner.getBlockPos(), this.voltage);
        }
    }

    /** Gasta {@code amount} CW·tick se houver; senão não gasta nada. */
    public boolean use(long amount) {
        if (this.stored < amount) return false;
        this.stored -= amount;
        this.owner.setChanged();
        return true;
    }

    /** Energia que a máquina gera sozinha (bancada automática). */
    public void addPassive(long amount) {
        this.stored = Math.min(this.capacity, this.stored + amount);
    }

    public long stored() {
        return this.stored;
    }

    public long capacity() {
        return this.capacity;
    }

    public long lastReceived() {
        return this.lastReceived;
    }

    public int voltage() {
        return this.voltage;
    }

    public void save(ValueOutput output) {
        output.putLong("Energy", this.stored);
    }

    public void load(ValueInput input) {
        this.stored = Math.clamp(input.getLongOr("Energy", 0L), 0L, this.capacity);
    }
}
