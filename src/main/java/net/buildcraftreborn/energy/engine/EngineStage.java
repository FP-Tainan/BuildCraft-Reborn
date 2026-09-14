package net.buildcraftreborn.energy.engine;

import net.minecraft.ChatFormatting;

/**
 * Estágio de calor do motor, como no BuildCraft: a cor do tronco muda de azul a vermelho e o pistão
 * bate mais rápido. Superaquecido, o motor para de queimar até esfriar.
 */
public enum EngineStage {
    BLUE("blue", 0.02F, ChatFormatting.BLUE),
    GREEN("green", 0.04F, ChatFormatting.GREEN),
    YELLOW("yellow", 0.08F, ChatFormatting.YELLOW),
    RED("red", 0.12F, ChatFormatting.RED),
    OVERHEAT("overheat", 0.0F, ChatFormatting.DARK_RED);

    public final String name;
    /** Quanto do curso do pistão anda por tick. */
    public final float pistonSpeed;
    public final ChatFormatting color;

    EngineStage(String name, float pistonSpeed, ChatFormatting color) {
        this.name = name;
        this.pistonSpeed = pistonSpeed;
        this.color = color;
    }

    /** BuildCraft: abaixo de 25% azul, 50% verde, 75% amarelo, 85% vermelho, acima superaquecido. */
    public static EngineStage of(double heatLevel) {
        if (heatLevel < 0.25) return BLUE;
        if (heatLevel < 0.5) return GREEN;
        if (heatLevel < 0.75) return YELLOW;
        if (heatLevel < 0.85) return RED;
        return OVERHEAT;
    }
}
