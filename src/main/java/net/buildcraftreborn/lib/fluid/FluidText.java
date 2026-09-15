package net.buildcraftreborn.lib.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.network.chat.Component;

/** Texto curto de um tanque para mensagens e tooltips: "Petróleo 1500 CL" ou "Vazio". */
public final class FluidText {
    private FluidText() {}

    public static Component of(BCTank tank) {
        if (tank.isEmpty()) return Component.translatable("gui.buildcraftreborn.tank.empty");
        return FluidVariantAttributes.getName(tank.variant).copy().append(" " + tank.amountCL() + " CL");
    }
}
