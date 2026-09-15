package net.buildcraftreborn.transport.gate;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Lógica de uma porta ({@code GateLogic}). Cada slot tem um gatilho e uma ação; slots ligados formam um grupo.
 * A cada tick: num grupo AND todos os gatilhos (slot vazio conta como falso) precisam valer; num OR, basta um.
 * Grupo verdadeiro ativa as ações de todos os seus slots.
 */
public final class GateLogic {
    public static final int MAX_PARAMS = 3;
    public static final int FIELD_TRIGGER = 0;
    public static final int FIELD_ACTION = 1;
    public static final int FIELD_TRIGGER_PARAM = 2;
    public static final int FIELD_ACTION_PARAM = 5;
    public static final int FIELD_CONNECTION = 8;
    public static final int OP_NEXT = 0;
    public static final int OP_PREVIOUS = 1;
    public static final int OP_CLEAR = 2;

    /** Gatilho ou ação escolhido: id e, nos externos, a face do vizinho. */
    public record Choice(String id, @Nullable Direction face) {
        public static final Choice EMPTY = new Choice("", null);

        public boolean isEmpty() {
            return this.id.isEmpty();
        }
    }

    /** Parâmetro: item de filtro ou marcação (por exemplo, "só o lado da porta"). */
    public record Param(ItemStack item, boolean flag) {
        public static final Param EMPTY = new Param(ItemStack.EMPTY, false);
    }

    private final GateVariant variant;
    private final Choice[] triggers;
    private final Choice[] actions;
    private final Param[][] triggerParams;
    private final Param[][] actionParams;
    private final boolean[] connections;
    private final boolean[] triggerOn;
    private final boolean[] actionOn;
    private boolean on;
    private int redstoneMask;

    public GateLogic(GateVariant variant) {
        this.variant = variant;
        int slots = variant.slots();
        this.triggers = new Choice[slots];
        this.actions = new Choice[slots];
        this.triggerParams = new Param[slots][MAX_PARAMS];
        this.actionParams = new Param[slots][MAX_PARAMS];
        Arrays.fill(this.triggers, Choice.EMPTY);
        Arrays.fill(this.actions, Choice.EMPTY);
        for (int i = 0; i < slots; i++) {
            Arrays.fill(this.triggerParams[i], Param.EMPTY);
            Arrays.fill(this.actionParams[i], Param.EMPTY);
        }
        this.connections = new boolean[Math.max(0, slots - 1)];
        this.triggerOn = new boolean[slots];
        this.actionOn = new boolean[slots];
    }

    public GateVariant variant() {
        return this.variant;
    }

    public int slots() {
        return this.triggers.length;
    }

    public Choice trigger(int slot) {
        return this.triggers[slot];
    }

    public Choice action(int slot) {
        return this.actions[slot];
    }

    public Param triggerParam(int slot, int index) {
        return this.triggerParams[slot][index];
    }

    public Param actionParam(int slot, int index) {
        return this.actionParams[slot][index];
    }

    public boolean connected(int slot) {
        return slot >= 0 && slot < this.connections.length && this.connections[slot];
    }

    public boolean triggerOn(int slot) {
        return this.triggerOn[slot];
    }

    public boolean actionOn(int slot) {
        return this.actionOn[slot];
    }

    public boolean isOn() {
        return this.on;
    }

    public int redstoneMask() {
        return this.redstoneMask;
    }

    public void setTrigger(int slot, Choice choice) {
        this.triggers[slot] = choice;
        Arrays.fill(this.triggerParams[slot], Param.EMPTY);
    }

    public void setAction(int slot, Choice choice) {
        this.actions[slot] = choice;
        Arrays.fill(this.actionParams[slot], Param.EMPTY);
    }

    public void setTriggerParam(int slot, int index, Param param) {
        this.triggerParams[slot][index] = param;
    }

    public void setActionParam(int slot, int index, Param param) {
        this.actionParams[slot][index] = param;
    }

    public void setConnected(int slot, boolean connected) {
        if (slot >= 0 && slot < this.connections.length) this.connections[slot] = connected;
    }

    /** Ações chamam isto para ligar a redstone nas faces da máscara (bit = ordinal da face). */
    public void emitRedstone(int mask) {
        this.redstoneMask |= mask;
    }

    /** Avalia os grupos e roda as ações; devolve se mudou algo que o cliente precisa ver. */
    public boolean resolve(GateContext context) {
        boolean[] oldTriggers = this.triggerOn.clone();
        boolean[] oldActions = this.actionOn.clone();
        boolean oldOn = this.on;
        int oldMask = this.redstoneMask;
        Arrays.fill(this.triggerOn, false);
        Arrays.fill(this.actionOn, false);
        this.on = false;
        this.redstoneMask = 0;

        int count = 0;
        int active = 0;
        int start = 0;
        int slots = slots();
        for (int i = 0; i < slots; i++) {
            count++;
            Statement trigger = Statements.get(this.triggers[i].id());
            if (trigger != null && trigger.kind() == Statement.Kind.TRIGGER
                    && trigger.test().test(context, this.triggers[i].face(), this.triggerParams[i])) {
                active++;
                this.triggerOn[i] = true;
            }
            if (i == slots - 1 || !this.connections[i]) {
                boolean result = this.variant.logic() == GateVariant.Logic.AND ? active == count : active > 0;
                for (int s = start; s <= i; s++) {
                    this.actionOn[s] = result;
                    Statement action = Statements.get(this.actions[s].id());
                    if (result && action != null && action.kind() == Statement.Kind.ACTION) {
                        this.on = true;
                        action.run().run(context, this.actions[s].face(), this.actionParams[s]);
                    }
                }
                count = 0;
                active = 0;
                start = i + 1;
            }
        }
        return this.on != oldOn || this.redstoneMask != oldMask || !Arrays.equals(oldTriggers, this.triggerOn)
                || !Arrays.equals(oldActions, this.actionOn);
    }

    /** Clique na tela: troca gatilho/ação, ajusta parâmetro ou liga dois slots. */
    public void click(GateContext context, int slot, int field, int op, ItemStack carried) {
        if (slot < 0 || slot >= slots()) return;
        if (field == FIELD_TRIGGER || field == FIELD_ACTION) {
            boolean trigger = field == FIELD_TRIGGER;
            Choice current = trigger ? this.triggers[slot] : this.actions[slot];
            Choice next = Choice.EMPTY;
            if (op != OP_CLEAR) {
                List<Choice> options = Statements.choices(context, trigger ? Statement.Kind.TRIGGER : Statement.Kind.ACTION);
                int index = options.indexOf(current);
                if (index < 0) index = 0;
                next = options.get(Math.floorMod(index + (op == OP_NEXT ? 1 : -1), options.size()));
            }
            if (trigger) {
                setTrigger(slot, next);
            } else {
                setAction(slot, next);
            }
        } else if (field >= FIELD_TRIGGER_PARAM && field < FIELD_TRIGGER_PARAM + MAX_PARAMS) {
            int index = field - FIELD_TRIGGER_PARAM;
            if (index < this.variant.triggerParams()) this.triggerParams[slot][index] = clickParam(this.triggers[slot], this.triggerParams[slot][index], op, carried);
        } else if (field >= FIELD_ACTION_PARAM && field < FIELD_ACTION_PARAM + MAX_PARAMS) {
            int index = field - FIELD_ACTION_PARAM;
            if (index < this.variant.actionParams()) this.actionParams[slot][index] = clickParam(this.actions[slot], this.actionParams[slot][index], op, carried);
        } else if (field == FIELD_CONNECTION) {
            setConnected(slot, !connected(slot));
        }
    }

    private static Param clickParam(Choice choice, Param current, int op, ItemStack carried) {
        Statement statement = Statements.get(choice.id());
        if (statement == null || op == OP_CLEAR) return Param.EMPTY;
        return switch (statement.paramType()) {
            case ITEM -> op == OP_NEXT && !carried.isEmpty() ? new Param(carried.copyWithCount(1), false) : Param.EMPTY;
            case GATE_SIDE -> new Param(ItemStack.EMPTY, !current.flag());
            case NONE -> Param.EMPTY;
        };
    }

    // ── salvar (com prefixo, várias portas no mesmo tubo) ────────────────
    public void save(ValueOutput output, String prefix) {
        output.putInt(prefix + "Variant", this.variant.pack());
        int triggerMask = 0;
        int actionMask = 0;
        int connectionMask = 0;
        for (int i = 0; i < slots(); i++) {
            output.putString(prefix + "T" + i, this.triggers[i].id());
            output.putInt(prefix + "TF" + i, this.triggers[i].face() == null ? -1 : this.triggers[i].face().ordinal());
            output.putString(prefix + "A" + i, this.actions[i].id());
            output.putInt(prefix + "AF" + i, this.actions[i].face() == null ? -1 : this.actions[i].face().ordinal());
            for (int k = 0; k < MAX_PARAMS; k++) {
                saveParam(output, prefix + "TP" + i + "_" + k, this.triggerParams[i][k]);
                saveParam(output, prefix + "AP" + i + "_" + k, this.actionParams[i][k]);
            }
            if (this.triggerOn[i]) triggerMask |= 1 << i;
            if (this.actionOn[i]) actionMask |= 1 << i;
            if (i < this.connections.length && this.connections[i]) connectionMask |= 1 << i;
        }
        output.putInt(prefix + "Connections", connectionMask);
        output.putInt(prefix + "TriggerOn", triggerMask);
        output.putInt(prefix + "ActionOn", actionMask);
        output.putBoolean(prefix + "On", this.on);
        output.putInt(prefix + "Redstone", this.redstoneMask);
    }

    private static void saveParam(ValueOutput output, String key, Param param) {
        if (!param.item().isEmpty()) output.store(key, ItemStack.OPTIONAL_CODEC, param.item());
        if (param.flag()) output.putBoolean(key + "F", true);
    }

    public static GateLogic load(ValueInput input, String prefix) {
        return load(input, prefix, GateVariant.unpack(input.getIntOr(prefix + "Variant", 0)));
    }

    /** Carrega a configuração numa porta da variante dada (copiador): slots e parâmetros que não cabem ficam de fora. */
    public static GateLogic load(ValueInput input, String prefix, GateVariant variant) {
        GateLogic gate = new GateLogic(variant);
        int connectionMask = input.getIntOr(prefix + "Connections", 0);
        int triggerMask = input.getIntOr(prefix + "TriggerOn", 0);
        int actionMask = input.getIntOr(prefix + "ActionOn", 0);
        for (int i = 0; i < gate.slots(); i++) {
            gate.triggers[i] = new Choice(input.getStringOr(prefix + "T" + i, ""), face(input.getIntOr(prefix + "TF" + i, -1)));
            gate.actions[i] = new Choice(input.getStringOr(prefix + "A" + i, ""), face(input.getIntOr(prefix + "AF" + i, -1)));
            for (int k = 0; k < MAX_PARAMS; k++) {
                gate.triggerParams[i][k] = k < variant.triggerParams() ? loadParam(input, prefix + "TP" + i + "_" + k) : Param.EMPTY;
                gate.actionParams[i][k] = k < variant.actionParams() ? loadParam(input, prefix + "AP" + i + "_" + k) : Param.EMPTY;
            }
            gate.triggerOn[i] = (triggerMask & (1 << i)) != 0;
            gate.actionOn[i] = (actionMask & (1 << i)) != 0;
            if (i < gate.connections.length) gate.connections[i] = (connectionMask & (1 << i)) != 0;
        }
        gate.on = input.getBooleanOr(prefix + "On", false);
        gate.redstoneMask = input.getIntOr(prefix + "Redstone", 0);
        return gate;
    }

    private static Param loadParam(ValueInput input, String key) {
        ItemStack item = input.read(key, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        boolean flag = input.getBooleanOr(key + "F", false);
        return item.isEmpty() && !flag ? Param.EMPTY : new Param(item, flag);
    }

    private static @Nullable Direction face(int ordinal) {
        return ordinal < 0 || ordinal >= 6 ? null : Direction.values()[ordinal];
    }
}
