package net.buildcraftreborn.energy.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Combustíveis e refrigerantes do motor a combustão ({@code BCEnergyRecipes}). No BuildCraft cada fluido
 * dá P MJ/t por T ticks a cada balde; aqui P × 1.000 CW. Petróleo e óleos pesados são "sujos" e deixam
 * resíduo.
 */
public final class BCFuels {
    /** Potência em CW, ticks de queima por balde (1.000 CL) e resíduo em CL por balde. */
    public record Fuel(long powerCW, int ticksPerBucket, long residueCL) {
        public boolean dirty() {
            return this.residueCL > 0;
        }
    }

    /** Gelo vira refrigerante líquido ao entrar no motor. */
    public record SolidCoolant(Fluid fluid, long amountCL) {}

    /** BuildCraft: a água tira 0,0023 °C por mB. */
    public static final double WATER_COOLING = 0.0023;

    private static final Map<Fluid, Fuel> FUELS = new HashMap<>();
    private static final Map<Fluid, Double> COOLANTS = new HashMap<>();
    private static final Map<Item, SolidCoolant> SOLID_COOLANTS = new HashMap<>();

    private BCFuels() {}

    public static void init() {
        if (!FUELS.isEmpty()) return;
        addCoolant(Fluids.WATER, WATER_COOLING);
        addSolidCoolant(Items.ICE, Fluids.WATER, 1_500);
        addSolidCoolant(Items.PACKED_ICE, Fluids.WATER, 2_000);
        addSolidCoolant(Items.BLUE_ICE, Fluids.WATER, 4_000);

        addFuel(BCFluids.Kind.FUEL_GASEOUS, 8_000, 1_875, 0);
        addFuel(BCFluids.Kind.FUEL_LIGHT, 6_000, 15_000, 0);
        addFuel(BCFluids.Kind.FUEL_DENSE, 4_000, 90_000, 0);
        addFuel(BCFluids.Kind.FUEL_MIXED_LIGHT, 3_000, 10_000, 0);
        addFuel(BCFluids.Kind.FUEL_MIXED_HEAVY, 5_000, 19_200, 0);
        addFuel(BCFluids.Kind.OIL_DISTILLED, 1_000, 37_500, 0);
        addFuel(BCFluids.Kind.OIL_DENSE, 4_000, 30_000, 500);
        addFuel(BCFluids.Kind.OIL_HEAVY, 2_000, 40_000, 333);
        addFuel(BCFluids.Kind.OIL, 3_000, 10_000, 125);
    }

    private static void addFuel(BCFluids.Kind kind, long powerCW, int ticksPerBucket, long residueCL) {
        addFuel(BCFluids.get(kind).fluid(), powerCW, ticksPerBucket, residueCL);
    }

    public static void addFuel(Fluid fluid, long powerCW, int ticksPerBucket, long residueCL) {
        FUELS.put(fluid, new Fuel(powerCW, ticksPerBucket, residueCL));
    }

    public static void addCoolant(Fluid fluid, double degreesPerCL) {
        COOLANTS.put(fluid, degreesPerCL);
    }

    public static void addSolidCoolant(Item item, Fluid fluid, long amountCL) {
        SOLID_COOLANTS.put(item, new SolidCoolant(fluid, amountCL));
    }

    public static @Nullable Fuel fuel(FluidVariant variant) {
        return variant.isBlank() ? null : FUELS.get(variant.getFluid());
    }

    /** Graus que cada CL do fluido tira do motor; 0 se não é refrigerante. */
    public static double cooling(FluidVariant variant) {
        return variant.isBlank() ? 0.0 : COOLANTS.getOrDefault(variant.getFluid(), 0.0);
    }

    public static @Nullable SolidCoolant solidCoolant(Item item) {
        return SOLID_COOLANTS.get(item);
    }

    public static FluidVariant residue() {
        return FluidVariant.of(BCFluids.get(BCFluids.Kind.OIL_RESIDUE).fluid());
    }
}
