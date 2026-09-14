package net.buildcraftreborn.lib.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.Predicate;

/**
 * Tanque de fluido das máquinas, com capacidade em CL (1 CL = 1 mB = 1/1000 de balde).
 * Por dentro usa as gotas do Fabric Transfer API, então funciona com canos e baldes de outros mods.
 */
public class BCTank extends SingleVariantStorage<FluidVariant> {
    private final long capacity;
    private final Predicate<FluidVariant> filter;
    private final Runnable onChange;

    public BCTank(long capacityCL, Predicate<FluidVariant> filter, Runnable onChange) {
        this.capacity = fromCL(capacityCL);
        this.filter = filter;
        this.onChange = onChange;
    }

    public BCTank(long capacityCL, Runnable onChange) {
        this(capacityCL, variant -> true, onChange);
    }

    /** CL → gotas do Fabric. */
    public static long fromCL(long cl) {
        return cl * FluidConstants.BUCKET / 1000;
    }

    /** Gotas do Fabric → CL. */
    public static long toCL(long droplets) {
        return droplets * 1000 / FluidConstants.BUCKET;
    }

    @Override
    protected FluidVariant getBlankVariant() {
        return FluidVariant.blank();
    }

    @Override
    protected long getCapacity(FluidVariant variant) {
        return this.capacity;
    }

    @Override
    protected boolean canInsert(FluidVariant variant) {
        return this.filter.test(variant);
    }

    @Override
    protected void onFinalCommit() {
        this.onChange.run();
    }

    public long amountCL() {
        return toCL(this.amount);
    }

    public long capacityCL() {
        return toCL(this.capacity);
    }

    public boolean isEmpty() {
        return this.variant.isBlank() || this.amount <= 0;
    }

    /** Fração cheia, de 0 a 1 (para barras e renderizadores). */
    public double ratio() {
        return this.capacity <= 0 ? 0.0 : Math.min(1.0, (double) this.amount / this.capacity);
    }

    public void save(ValueOutput output, String name) {
        output.store(name + "Fluid", FluidVariant.CODEC, this.variant);
        output.putLong(name + "Amount", this.amount);
    }

    public void load(ValueInput input, String name) {
        this.variant = input.read(name + "Fluid", FluidVariant.CODEC).orElse(FluidVariant.blank());
        this.amount = this.variant.isBlank() ? 0 : Math.clamp(input.getLongOr(name + "Amount", 0L), 0L, this.capacity);
    }
}
