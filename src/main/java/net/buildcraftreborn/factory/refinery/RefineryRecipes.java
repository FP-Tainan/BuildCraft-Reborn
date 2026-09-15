package net.buildcraftreborn.factory.refinery;

import net.buildcraftreborn.energy.fluid.BCFluids;
import net.craftenergy.api.EnergyUnits;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Receitas do refino ({@code BCEnergyRecipes}): destilação de cada fluido numa temperatura e as trocas de
 * calor (cada fluido do petróleo esquenta ou esfria um nível; água só esquenta, lava só esfria).
 */
public final class RefineryRecipes {
    /** Entrada, gás (sai por cima) e líquido (sai por baixo) em CL, e a energia de cada lote. */
    public record Distillation(FluidVariant input, long inputCL, FluidVariant gas, long gasCL,
                               FluidVariant liquid, long liquidCL, long energy) {}

    /** Troca de calor: {@code output} nulo some (a água aquecida e a lava resfriada). */
    public record HeatRecipe(Fluid input, @Nullable Fluid output, int heatFrom, int heatTo) {}

    private static final Map<Fluid, Distillation> DISTILLATION = new HashMap<>();
    private static final Map<Fluid, HeatRecipe> HEATABLE = new HashMap<>();
    private static final Map<Fluid, HeatRecipe> COOLABLE = new HashMap<>();
    /** Quanto de cada fluido sai de 8 partes de petróleo (é daí que vêm as proporções). */
    private static final Map<BCFluids.Kind, Integer> PARTS = new EnumMap<>(BCFluids.Kind.class);

    private RefineryRecipes() {}

    public static void init() {
        if (!DISTILLATION.isEmpty()) return;
        PARTS.put(BCFluids.Kind.OIL, 8);
        PARTS.put(BCFluids.Kind.OIL_DISTILLED, 8);
        PARTS.put(BCFluids.Kind.FUEL_MIXED_LIGHT, 10);
        PARTS.put(BCFluids.Kind.FUEL_GASEOUS, 16);
        PARTS.put(BCFluids.Kind.OIL_HEAVY, 3);
        PARTS.put(BCFluids.Kind.FUEL_MIXED_HEAVY, 5);
        PARTS.put(BCFluids.Kind.FUEL_LIGHT, 4);
        PARTS.put(BCFluids.Kind.OIL_DENSE, 2);
        PARTS.put(BCFluids.Kind.FUEL_DENSE, 2);
        PARTS.put(BCFluids.Kind.OIL_RESIDUE, 1);

        distill(BCFluids.Kind.OIL, BCFluids.Kind.FUEL_GASEOUS, BCFluids.Kind.OIL_HEAVY, BCFluids.Heat.COOL, 32);
        distill(BCFluids.Kind.OIL, BCFluids.Kind.FUEL_MIXED_LIGHT, BCFluids.Kind.OIL_DENSE, BCFluids.Heat.HOT, 16);
        distill(BCFluids.Kind.OIL, BCFluids.Kind.OIL_DISTILLED, BCFluids.Kind.OIL_RESIDUE, BCFluids.Heat.SEARING, 12);
        distill(BCFluids.Kind.OIL_DISTILLED, BCFluids.Kind.FUEL_GASEOUS, BCFluids.Kind.FUEL_MIXED_HEAVY, BCFluids.Heat.COOL, 24);
        distill(BCFluids.Kind.OIL_DISTILLED, BCFluids.Kind.FUEL_MIXED_LIGHT, BCFluids.Kind.FUEL_DENSE, BCFluids.Heat.HOT, 16);
        distill(BCFluids.Kind.FUEL_MIXED_LIGHT, BCFluids.Kind.FUEL_GASEOUS, BCFluids.Kind.FUEL_LIGHT, BCFluids.Heat.COOL, 24);
        distill(BCFluids.Kind.OIL_HEAVY, BCFluids.Kind.FUEL_LIGHT, BCFluids.Kind.OIL_DENSE, BCFluids.Heat.HOT, 16);
        distill(BCFluids.Kind.OIL_HEAVY, BCFluids.Kind.FUEL_MIXED_HEAVY, BCFluids.Kind.OIL_RESIDUE, BCFluids.Heat.SEARING, 12);
        distill(BCFluids.Kind.FUEL_MIXED_HEAVY, BCFluids.Kind.FUEL_LIGHT, BCFluids.Kind.FUEL_DENSE, BCFluids.Heat.HOT, 16);
        distill(BCFluids.Kind.OIL_DENSE, BCFluids.Kind.FUEL_DENSE, BCFluids.Kind.OIL_RESIDUE, BCFluids.Heat.SEARING, 12);

        BCFluids.Heat[] heats = BCFluids.Heat.values();
        for (BCFluids.Kind kind : BCFluids.Kind.values()) {
            for (int heat = 0; heat < heats.length - 1; heat++) {
                Fluid cold = BCFluids.get(kind, heats[heat]).fluid();
                Fluid hot = BCFluids.get(kind, heats[heat + 1]).fluid();
                HEATABLE.put(cold, new HeatRecipe(cold, hot, heat, heat + 1));
                COOLABLE.put(hot, new HeatRecipe(hot, cold, heat + 1, heat));
            }
        }
        HEATABLE.put(Fluids.WATER, new HeatRecipe(Fluids.WATER, null, 0, 1));
        COOLABLE.put(Fluids.LAVA, new HeatRecipe(Fluids.LAVA, null, 4, 2));
    }

    /** As quantidades são reduzidas pelo máximo divisor comum, como no BuildCraft; a energia do lote não. */
    private static void distill(BCFluids.Kind input, BCFluids.Kind gas, BCFluids.Kind liquid, BCFluids.Heat heat, long energyCWh) {
        int in = PARTS.get(input);
        int out = PARTS.get(gas);
        int rest = PARTS.get(liquid);
        int divisor = gcd(gcd(in, out), rest);
        Fluid fluid = BCFluids.get(input, heat).fluid();
        DISTILLATION.put(fluid, new Distillation(FluidVariant.of(fluid), in / divisor,
                FluidVariant.of(BCFluids.get(gas, heat).fluid()), out / divisor,
                FluidVariant.of(BCFluids.get(liquid, heat).fluid()), rest / divisor, EnergyUnits.fromCWh(energyCWh)));
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    public static @Nullable Distillation distillation(FluidVariant variant) {
        return variant.isBlank() ? null : DISTILLATION.get(variant.getFluid());
    }

    public static @Nullable HeatRecipe heatable(FluidVariant variant) {
        return variant.isBlank() ? null : HEATABLE.get(variant.getFluid());
    }

    public static @Nullable HeatRecipe coolable(FluidVariant variant) {
        return variant.isBlank() ? null : COOLABLE.get(variant.getFluid());
    }

    public static Collection<Distillation> distillations() {
        return Collections.unmodifiableCollection(DISTILLATION.values());
    }
}
