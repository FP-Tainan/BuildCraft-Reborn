package net.buildcraftreborn.transport.gate;

import net.buildcraftreborn.BuildCraftReborn;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Gatilho ou ação de porta lógica ({@code IStatement}). Internos olham o próprio tubo; externos olham o bloco
 * vizinho numa face ({@code face} é a direção do tubo até ele).
 */
public record Statement(String id, Kind kind, Scope scope, ParamType paramType, String icon, String langKey, Object[] langArgs,
                        Availability availability, Test test, Run run) {
    public enum Kind {
        TRIGGER,
        ACTION
    }

    public enum Scope {
        INTERNAL,
        EXTERNAL
    }

    /** O primeiro parâmetro do slot: item de filtro ou "só o lado da porta". */
    public enum ParamType {
        NONE,
        ITEM,
        GATE_SIDE
    }

    @FunctionalInterface
    public interface Availability {
        boolean available(GateContext context, @Nullable Direction face);
    }

    @FunctionalInterface
    public interface Test {
        boolean test(GateContext context, @Nullable Direction face, GateLogic.Param[] params);
    }

    @FunctionalInterface
    public interface Run {
        void run(GateContext context, @Nullable Direction face, GateLogic.Param[] params);
    }

    public Component name() {
        return Component.translatable(this.langKey, this.langArgs);
    }

    public Identifier iconTexture() {
        return BuildCraftReborn.id("textures/gui/statements/" + this.icon + ".png");
    }
}
