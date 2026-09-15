package net.buildcraftreborn.transport;

import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

/** Cores claras do BuildCraft ({@code ColourUtil.LIGHT_HEX}) para itens pintados e lentes. */
public final class PipeColours {
    private static final int[] LIGHT = {
            0xE4E4E4, 0xEA7835, 0xD943C6, 0x66AAFF, 0xFFD91C, 0x39D52E, 0xD97199, 0x7A7A7A,
            0xA0A7A7, 0x299799, 0x7E34BF, 0x253193, 0x89502D, 0x007F0E, 0xBE2B27, 0x181414
    };

    private PipeColours() {}

    public static int rgb(DyeColor colour) {
        return LIGHT[colour.ordinal()];
    }

    /** ARGB opaco; {@code 0} para sem cor. */
    public static int argb(@Nullable DyeColor colour) {
        return colour == null ? 0 : 0xFF000000 | rgb(colour);
    }

    public static DyeColor cycle(DyeColor colour, int step) {
        DyeColor[] values = DyeColor.values();
        return values[Math.floorMod(colour.ordinal() + step, values.length)];
    }
}
