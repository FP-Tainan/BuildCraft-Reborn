package net.buildcraftreborn.transport.plug;

import net.buildcraftreborn.lib.energy.MachineEnergy;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.registry.BCItems;
import net.buildcraftreborn.transport.gate.GateContext;
import net.buildcraftreborn.transport.gate.GateItem;
import net.buildcraftreborn.transport.gate.GateLogic;
import net.buildcraftreborn.transport.gate.GateVariant;
import net.buildcraftreborn.transport.tile.FluidPipeBlockEntity;
import net.buildcraftreborn.transport.tile.PipeBlockEntity;
import net.craftenergy.api.EnergyUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encaixes de um tubo: portas lógicas e plugues (pulsar, sensor de luz, temporizador) nas seis faces, e fios
 * nos oito cantos. Face com encaixe não liga com o vizinho. Também guarda a redstone emitida pelas portas,
 * as cores que elas mandam pelos fios e o estado dos pulsares.
 */
public final class PipePlugs {
    /** Pulsar: 1 CWh a cada segundo (BuildCraft: 1 MJ por pulso), o que puxa um item ou 1.000 CL. */
    public static final long PULSE_ENERGY = EnergyUnits.fromCWh(1);
    private static final int PULSE_INTERVAL = 20;
    private static final int CONSTANT_TICKS = 10;

    private static final class Pulsar {
        boolean manual;
        int constantTicks;
        boolean singleRequested;
        boolean singleWasActive;
        int queued;
        int stage;
        boolean pulsing;
    }

    /** Lente ({@code filter} falso: pinta) ou filtro (barra outras cores); cor nula = transparente. Não tapa a ligação. */
    public record Lens(@Nullable DyeColor colour, boolean filter) {}

    /** Fachada: visual de um bloco na face; a sólida tapa a ligação, a vazada não. */
    public record Facade(net.minecraft.world.level.block.state.BlockState state, boolean hollow) {}

    private final EnumMap<Direction, Facade> facades = new EnumMap<>(Direction.class);

    private final EnumMap<Direction, Lens> lenses = new EnumMap<>(Direction.class);
    private final BCBlockEntity owner;
    private final EnumMap<Direction, GateLogic> gates = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, PlugKind> plugs = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, Pulsar> pulsars = new EnumMap<>(Direction.class);
    private final DyeColor[] wires = new DyeColor[WireNetwork.CORNERS];
    private int redstoneMask;
    private int wirePowered;
    /** Cores emitidas neste tick e no anterior; a rede lê sempre o valor do tick anterior. */
    private int emitCurrent;
    private int emitPrevious;
    private long emitTick = Long.MIN_VALUE;

    public PipePlugs(BCBlockEntity owner) {
        this.owner = owner;
    }

    // ── faces ─────────────────────────────────────────────────────────────
    public boolean has(Direction side) {
        Facade facade = this.facades.get(side);
        return this.gates.containsKey(side) || this.plugs.containsKey(side) || facade != null && !facade.hollow();
    }

    public @Nullable GateLogic gate(Direction side) {
        return this.gates.get(side);
    }

    public Map<Direction, GateLogic> gates() {
        return Collections.unmodifiableMap(this.gates);
    }

    public @Nullable PlugKind kind(Direction side) {
        return this.plugs.get(side);
    }

    public boolean hasPlug(PlugKind kind) {
        return this.plugs.containsValue(kind);
    }

    public Set<Direction> sides() {
        EnumSet<Direction> sides = EnumSet.noneOf(Direction.class);
        sides.addAll(this.gates.keySet());
        sides.addAll(this.plugs.keySet());
        sides.addAll(this.lenses.keySet());
        sides.addAll(this.facades.keySet());
        return sides;
    }

    public @Nullable Lens lens(Direction side) {
        return this.lenses.get(side);
    }

    /** Face com qualquer encaixe (inclusive lente, que não tapa a ligação). */
    public boolean occupied(Direction side) {
        return has(side) || this.lenses.containsKey(side) || this.facades.containsKey(side);
    }

    public @Nullable Facade facade(Direction side) {
        return this.facades.get(side);
    }

    public void attachFacade(Direction side, Facade facade) {
        if (occupied(side)) return;
        this.facades.put(side, facade);
        this.owner.syncToClient();
    }

    public void attachLens(Direction side, Lens lens) {
        if (has(side)) return;
        this.lenses.put(side, lens);
        this.owner.syncToClient();
    }

    public boolean isEmpty() {
        if (!this.gates.isEmpty() || !this.plugs.isEmpty()) return false;
        for (DyeColor wire : this.wires) {
            if (wire != null) return false;
        }
        return true;
    }

    public void attachGate(Direction side, GateVariant variant) {
        setGate(side, new GateLogic(variant));
    }

    public void setGate(Direction side, GateLogic gate) {
        this.plugs.remove(side);
        this.pulsars.remove(side);
        this.gates.put(side, gate);
        this.owner.syncToClient();
    }

    public void attachPlug(Direction side, PlugKind kind) {
        this.gates.remove(side);
        this.plugs.put(side, kind);
        if (kind == PlugKind.PULSAR) this.pulsars.put(side, new Pulsar());
        this.owner.syncToClient();
    }

    /** Tira o encaixe e devolve o item dele (vazio se não havia). */
    public ItemStack remove(Direction side) {
        GateLogic gate = this.gates.remove(side);
        PlugKind kind = this.plugs.remove(side);
        this.pulsars.remove(side);
        Lens lens = gate == null && kind == null ? this.lenses.remove(side) : null;
        Facade facade = gate == null && kind == null && lens == null ? this.facades.remove(side) : null;
        if (gate == null && kind == null && lens == null && facade == null) return ItemStack.EMPTY;
        this.owner.syncToClient();
        if (facade != null) return FacadeItem.stack(facade.state(), facade.hollow());
        if (lens != null) return LensItem.stack(lens.colour(), lens.filter());
        return gate != null ? GateItem.stack(gate.variant()) : kind.stack();
    }

    /** Pulsar só serve em tubo que usa energia (madeira, obsidiana). */
    public @Nullable MachineEnergy energy() {
        if (this.owner instanceof PipeBlockEntity pipe) return pipe.energy();
        if (this.owner instanceof FluidPipeBlockEntity pipe) return pipe.energy();
        return null;
    }

    // ── fios ──────────────────────────────────────────────────────────────
    public @Nullable DyeColor wire(int corner) {
        return corner >= 0 && corner < WireNetwork.CORNERS ? this.wires[corner] : null;
    }

    public boolean hasWire(DyeColor color) {
        for (DyeColor wire : this.wires) {
            if (wire == color) return true;
        }
        return false;
    }

    /** Estado sincronizado do canto (para desenhar o fio aceso). */
    public boolean wirePowered(int corner) {
        return (this.wirePowered >> corner & 1) != 0;
    }

    public boolean placeWire(int corner, DyeColor color) {
        if (corner < 0 || corner >= WireNetwork.CORNERS || this.wires[corner] != null) return false;
        this.wires[corner] = color;
        this.owner.syncToClient();
        return true;
    }

    public ItemStack removeWire(int corner) {
        DyeColor color = wire(corner);
        if (color == null) return ItemStack.EMPTY;
        this.wires[corner] = null;
        this.owner.syncToClient();
        return new ItemStack(BCItems.WIRES.get(color).get());
    }

    public void emitWire(DyeColor color) {
        this.emitCurrent |= 1 << color.ordinal();
    }

    public boolean emitting(DyeColor color, long now) {
        int mask = this.emitTick == now ? this.emitPrevious : this.emitCurrent;
        return (mask >> color.ordinal() & 1) != 0;
    }

    // ── pulsar ────────────────────────────────────────────────────────────
    public void pulsarConstant(Direction side) {
        Pulsar pulsar = this.pulsars.get(side);
        if (pulsar != null) pulsar.constantTicks = CONSTANT_TICKS;
    }

    public void pulsarSingle(Direction side) {
        Pulsar pulsar = this.pulsars.get(side);
        if (pulsar != null) pulsar.singleRequested = true;
    }

    public boolean pulsing(Direction side) {
        Pulsar pulsar = this.pulsars.get(side);
        return pulsar != null && pulsar.pulsing;
    }

    /** Clique no pulsar: liga ou desliga o modo manual (pulsa sem porta lógica). */
    public boolean toggleManual(Direction side) {
        Pulsar pulsar = this.pulsars.get(side);
        if (pulsar == null) return false;
        pulsar.manual = !pulsar.manual;
        this.owner.syncToClient();
        return pulsar.manual;
    }

    private boolean tickPulsars() {
        boolean changed = false;
        MachineEnergy energy = energy();
        for (Pulsar pulsar : this.pulsars.values()) {
            if (pulsar.singleRequested && !pulsar.singleWasActive) pulsar.queued = Math.min(pulsar.queued + 1, 64);
            pulsar.singleWasActive = pulsar.singleRequested;
            pulsar.singleRequested = false;
            boolean active = pulsar.manual || pulsar.constantTicks > 0 || pulsar.queued > 0;
            if (pulsar.constantTicks > 0) pulsar.constantTicks--;
            if (!active) {
                pulsar.stage = 0;
            } else if (++pulsar.stage >= PULSE_INTERVAL) {
                pulsar.stage = 0;
                if (energy != null) {
                    energy.addPassive(PULSE_ENERGY);
                    this.owner.setChanged();
                }
                if (pulsar.queued > 0) pulsar.queued--;
            }
            if (active != pulsar.pulsing) {
                pulsar.pulsing = active;
                changed = true;
            }
        }
        return changed;
    }

    // ── tick ──────────────────────────────────────────────────────────────
    public List<ItemStack> drops() {
        List<ItemStack> drops = new ArrayList<>();
        for (GateLogic gate : this.gates.values()) drops.add(GateItem.stack(gate.variant()));
        for (PlugKind kind : this.plugs.values()) drops.add(kind.stack());
        for (Lens lens : this.lenses.values()) drops.add(LensItem.stack(lens.colour(), lens.filter()));
        for (Facade facade : this.facades.values()) drops.add(FacadeItem.stack(facade.state(), facade.hollow()));
        for (DyeColor wire : this.wires) {
            if (wire != null) drops.add(new ItemStack(BCItems.WIRES.get(wire).get()));
        }
        return drops;
    }

    /** Força do sinal que o tubo manda pela face {@code side}. */
    public int signal(Direction side) {
        return (this.redstoneMask >> side.ordinal() & 1) != 0 ? 15 : 0;
    }

    public void tick(Level level, BlockPos pos) {
        long now = level.getGameTime();
        if (this.emitTick != now) {
            this.emitPrevious = this.emitCurrent;
            this.emitCurrent = 0;
            this.emitTick = now;
        }
        boolean changed = false;
        int mask = 0;
        for (Map.Entry<Direction, GateLogic> entry : this.gates.entrySet()) {
            GateLogic gate = entry.getValue();
            changed |= gate.resolve(new GateContext(level, pos, entry.getKey(), this.owner, gate));
            mask |= gate.redstoneMask();
        }
        changed |= tickPulsars();
        int powered = 0;
        for (int corner = 0; corner < WireNetwork.CORNERS; corner++) {
            if (this.wires[corner] != null && WireNetwork.powered(level, pos, corner, this.wires[corner])) powered |= 1 << corner;
        }
        if (powered != this.wirePowered) {
            this.wirePowered = powered;
            changed = true;
        }
        if (mask != this.redstoneMask) {
            this.redstoneMask = mask;
            level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock(), null);
            changed = true;
        }
        if (changed) this.owner.syncToClient();
    }

    // ── salvar ────────────────────────────────────────────────────────────
    public void save(ValueOutput output) {
        output.putInt("PlugRedstone", this.redstoneMask);
        output.putInt("WirePowered", this.wirePowered);
        for (Direction side : Direction.values()) {
            String key = side.getName();
            GateLogic gate = this.gates.get(side);
            if (gate != null) {
                output.putBoolean("Gate_" + key, true);
                gate.save(output, "Gate_" + key + "_");
            }
            PlugKind kind = this.plugs.get(side);
            if (kind != null) output.putString("Plug_" + key, kind.id());
            Lens lens = this.lenses.get(side);
            if (lens != null) {
                output.putInt("Lens_" + key, lens.colour() == null ? 0 : lens.colour().ordinal() + 1);
                output.putBoolean("LensFilter_" + key, lens.filter());
            }
            Facade facade = this.facades.get(side);
            if (facade != null) {
                output.store("Facade_" + key, net.minecraft.world.level.block.state.BlockState.CODEC, facade.state());
                output.putBoolean("FacadeHollow_" + key, facade.hollow());
            }
            Pulsar pulsar = this.pulsars.get(side);
            if (pulsar != null) {
                output.putBoolean("Pulsar_" + key + "_Manual", pulsar.manual);
                output.putInt("Pulsar_" + key + "_Queued", pulsar.queued);
                output.putInt("Pulsar_" + key + "_Stage", pulsar.stage);
                output.putBoolean("Pulsar_" + key + "_Pulsing", pulsar.pulsing);
            }
        }
        for (int corner = 0; corner < WireNetwork.CORNERS; corner++) {
            if (this.wires[corner] != null) output.putString("Wire" + corner, this.wires[corner].getName());
        }
    }

    public void load(ValueInput input) {
        this.gates.clear();
        this.plugs.clear();
        this.pulsars.clear();
        this.lenses.clear();
        this.facades.clear();
        this.redstoneMask = input.getIntOr("PlugRedstone", 0);
        this.wirePowered = input.getIntOr("WirePowered", 0);
        for (Direction side : Direction.values()) {
            String key = side.getName();
            if (input.getBooleanOr("Gate_" + key, false)) this.gates.put(side, GateLogic.load(input, "Gate_" + key + "_"));
            int lensColour = input.getIntOr("Lens_" + key, -1);
            if (lensColour >= 0) {
                DyeColor colour = lensColour == 0 ? null : DyeColor.values()[Math.min(DyeColor.values().length - 1, lensColour - 1)];
                this.lenses.put(side, new Lens(colour, input.getBooleanOr("LensFilter_" + key, false)));
            }
            boolean hollow = input.getBooleanOr("FacadeHollow_" + key, false);
            input.read("Facade_" + key, net.minecraft.world.level.block.state.BlockState.CODEC)
                    .ifPresent(state -> this.facades.put(side, new Facade(state, hollow)));
            PlugKind kind = PlugKind.byId(input.getStringOr("Plug_" + key, ""));
            if (kind == null) continue;
            this.plugs.put(side, kind);
            if (kind == PlugKind.PULSAR) {
                Pulsar pulsar = new Pulsar();
                pulsar.manual = input.getBooleanOr("Pulsar_" + key + "_Manual", false);
                pulsar.queued = input.getIntOr("Pulsar_" + key + "_Queued", 0);
                pulsar.stage = input.getIntOr("Pulsar_" + key + "_Stage", 0);
                pulsar.pulsing = input.getBooleanOr("Pulsar_" + key + "_Pulsing", false);
                this.pulsars.put(side, pulsar);
            }
        }
        for (int corner = 0; corner < WireNetwork.CORNERS; corner++) {
            String name = input.getStringOr("Wire" + corner, "");
            this.wires[corner] = name.isEmpty() ? null : DyeColor.byName(name, null);
        }
    }
}
