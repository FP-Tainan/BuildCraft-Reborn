package net.buildcraftreborn.transport.gate;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Variante da porta lógica ({@code GateVariant}): lógica, material (quantos slots) e modificador (parâmetros
 * por slot). Slots = slots do material ÷ divisor do modificador.
 */
public record GateVariant(Logic logic, Material material, Modifier modifier) {
    public enum Logic {
        AND,
        OR;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Material {
        CLAY_BRICK(1, false, "bricks"),
        IRON(2, true, "iron_block"),
        NETHER_BRICK(4, true, "nether_bricks"),
        GOLD(8, true, "gold_block");

        public final int slots;
        public final boolean modifiable;
        public final Identifier texture;

        Material(int slots, boolean modifiable, String block) {
            this.slots = slots;
            this.modifiable = modifiable;
            this.texture = Identifier.withDefaultNamespace("textures/block/" + block + ".png");
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public enum Modifier {
        NONE(0, 0, 1, null),
        LAPIS(1, 0, 1, "lapis_block"),
        QUARTZ(1, 1, 2, "quartz_block_top"),
        DIAMOND(3, 3, 2, "diamond_block");

        public final int triggerParams;
        public final int actionParams;
        public final int divisor;
        public final @Nullable Identifier texture;

        Modifier(int triggerParams, int actionParams, int divisor, @Nullable String block) {
            this.triggerParams = triggerParams;
            this.actionParams = actionParams;
            this.divisor = divisor;
            this.texture = block == null ? null : Identifier.withDefaultNamespace("textures/block/" + block + ".png");
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final GateVariant BASIC = new GateVariant(Logic.AND, Material.CLAY_BRICK, Modifier.NONE);
    public static final Codec<GateVariant> CODEC = Codec.INT.xmap(GateVariant::unpack, GateVariant::pack);
    public static final StreamCodec<ByteBuf, GateVariant> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(GateVariant::unpack, GateVariant::pack);

    public int slots() {
        return Math.max(1, this.material.slots / this.modifier.divisor);
    }

    public int triggerParams() {
        return this.modifier.triggerParams;
    }

    public int actionParams() {
        return this.modifier.actionParams;
    }

    /** Mais de 4 slots: a tela divide em duas colunas. */
    public boolean twoColumns() {
        return slots() > 4;
    }

    public GateVariant withLogic(Logic logic) {
        return new GateVariant(logic, this.material, this.modifier);
    }

    public int pack() {
        return this.logic.ordinal() * 16 + this.material.ordinal() * 4 + this.modifier.ordinal();
    }

    public static GateVariant unpack(int value) {
        Logic[] logics = Logic.values();
        Material[] materials = Material.values();
        Modifier[] modifiers = Modifier.values();
        GateVariant variant = new GateVariant(logics[Math.floorMod(value / 16, logics.length)],
                materials[Math.floorMod(value / 4, 4) % materials.length], modifiers[Math.floorMod(value, 4) % modifiers.length]);
        return variant.material.modifiable ? variant : BASIC;
    }

    public Component name() {
        if (this.material == Material.CLAY_BRICK) return Component.translatable("gate.buildcraftreborn.name.basic");
        Component material = Component.translatable("gate.buildcraftreborn.material." + this.material.id());
        Component logic = Component.translatable("gate.buildcraftreborn.logic." + this.logic.id());
        if (this.modifier == Modifier.NONE) return Component.translatable("gate.buildcraftreborn.name", material, logic);
        return Component.translatable("gate.buildcraftreborn.name.modified", material, logic,
                Component.translatable("gate.buildcraftreborn.modifier." + this.modifier.id()));
    }
}
