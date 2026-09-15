package net.buildcraftreborn.transport.gate;

import net.buildcraftreborn.energy.engine.EngineBlockEntity;
import net.buildcraftreborn.energy.engine.EngineStage;
import net.buildcraftreborn.lib.energy.MachineEnergy;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.transport.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.buildcraftreborn.transport.plug.PipePlugs;
import net.buildcraftreborn.transport.plug.PlugHolder;
import net.buildcraftreborn.transport.plug.PlugKind;
import net.buildcraftreborn.transport.plug.WireNetwork;
import net.buildcraftreborn.transport.tile.FluidPipeBlockEntity;
import net.buildcraftreborn.transport.tile.PipeBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Gatilhos e ações da Sessão 11 (parte A): sempre verdadeiro, redstone, inventário, tanque, estágio do motor e
 * conteúdo do tubo. Fios, pulsar, sensor de luz e temporizador entram na parte B.
 */
public final class Statements {
    private static final Map<String, Statement> BY_ID = new LinkedHashMap<>();
    private static final Object[] NO_ARGS = new Object[0];

    static {
        trigger("true", Statement.Scope.INTERNAL, Statement.ParamType.NONE, "trigger_true", (c, f) -> true, (c, f, p) -> true);
        trigger("redstone.input.active", Statement.Scope.INTERNAL, Statement.ParamType.GATE_SIDE, "trigger_redstoneinput_active",
                (c, f) -> true, (c, f, p) -> redstone(c, p));
        trigger("redstone.input.inactive", Statement.Scope.INTERNAL, Statement.ParamType.GATE_SIDE, "trigger_redstoneinput_inactive",
                (c, f) -> true, (c, f, p) -> !redstone(c, p));
        action("redstone.output", Statement.Scope.INTERNAL, Statement.ParamType.GATE_SIDE, "action_redstoneoutput", (c, f) -> true,
                (c, f, p) -> c.gate().emitRedstone(flag(p) ? 1 << c.side().ordinal() : 0b111111));

        trigger("pipe.contains_items", Statement.Scope.INTERNAL, Statement.ParamType.ITEM, "trigger_pipecontents_containsitems",
                (c, f) -> c.pipe() instanceof PipeBlockEntity, (c, f, p) -> c.pipe() instanceof PipeBlockEntity pipe
                        && pipe.items().stream().anyMatch(item -> matches(item.stack, p)));
        trigger("pipe.contains_fluids", Statement.Scope.INTERNAL, Statement.ParamType.NONE, "trigger_pipecontents_containsfluids",
                (c, f) -> c.pipe() instanceof FluidPipeBlockEntity, (c, f, p) -> c.pipe() instanceof FluidPipeBlockEntity pipe && !pipe.tank().isEmpty());

        trigger("inventory.empty", Statement.Scope.EXTERNAL, Statement.ParamType.NONE, "trigger_inventory_empty",
                (c, f) -> items(c, f) != null, (c, f, p) -> !anyItem(items(c, f), view -> true));
        trigger("inventory.contains", Statement.Scope.EXTERNAL, Statement.ParamType.ITEM, "trigger_inventory_contains",
                (c, f) -> items(c, f) != null, (c, f, p) -> anyItem(items(c, f), view -> matches(view.getResource(), p)));
        trigger("inventory.space", Statement.Scope.EXTERNAL, Statement.ParamType.ITEM, "trigger_inventory_space",
                (c, f) -> items(c, f) != null, (c, f, p) -> itemSpace(items(c, f), p));
        trigger("inventory.full", Statement.Scope.EXTERNAL, Statement.ParamType.NONE, "trigger_inventory_full",
                (c, f) -> items(c, f) != null, (c, f, p) -> !itemSpace(items(c, f), p));
        for (int level : new int[]{25, 50, 75}) {
            trigger("inventory.below" + level, Statement.Scope.EXTERNAL, Statement.ParamType.ITEM, "trigger_inventory_below" + level,
                    (c, f) -> items(c, f) != null, (c, f, p) -> itemRatio(items(c, f), p) < level / 100.0,
                    "gate.buildcraftreborn.statement.inventory.below", level);
        }

        trigger("fluid.empty", Statement.Scope.EXTERNAL, Statement.ParamType.NONE, "trigger_liquidcontainer_empty",
                (c, f) -> fluids(c, f) != null, (c, f, p) -> !anyFluid(fluids(c, f), null));
        trigger("fluid.contains", Statement.Scope.EXTERNAL, Statement.ParamType.ITEM, "trigger_liquidcontainer_contains",
                (c, f) -> fluids(c, f) != null, (c, f, p) -> anyFluid(fluids(c, f), fluidOf(p)));
        trigger("fluid.space", Statement.Scope.EXTERNAL, Statement.ParamType.ITEM, "trigger_liquidcontainer_space",
                (c, f) -> fluids(c, f) != null, (c, f, p) -> fluidSpace(fluids(c, f), fluidOf(p)));
        trigger("fluid.full", Statement.Scope.EXTERNAL, Statement.ParamType.NONE, "trigger_liquidcontainer_full",
                (c, f) -> fluids(c, f) != null, (c, f, p) -> !fluidSpace(fluids(c, f), null));
        for (int level : new int[]{25, 50, 75}) {
            trigger("fluid.below" + level, Statement.Scope.EXTERNAL, Statement.ParamType.ITEM, "trigger_liquidcontainer_below" + level,
                    (c, f) -> fluids(c, f) != null, (c, f, p) -> fluidRatio(fluids(c, f), fluidOf(p)) < level / 100.0,
                    "gate.buildcraftreborn.statement.fluid.below", level);
        }

        for (EngineStage stage : EngineStage.values()) {
            trigger("engine." + stage.name, Statement.Scope.EXTERNAL, Statement.ParamType.NONE, "trigger_engineheat_" + stage.name,
                    (c, f) -> f != null && c.level().getBlockEntity(c.pos().relative(f)) instanceof EngineBlockEntity,
                    (c, f, p) -> f != null && c.level().getBlockEntity(c.pos().relative(f)) instanceof EngineBlockEntity engine && engine.stage() == stage);
        }

        // ── fios (parte B) ────────────────────────────────────────────────
        for (DyeColor color : DyeColor.values()) {
            Object[] colorName = {Component.translatable("color.minecraft." + color.getName())};
            String name = color.getName();
            register(new Statement("pipe.wire.input." + name + ".active", Statement.Kind.TRIGGER, Statement.Scope.INTERNAL,
                    Statement.ParamType.NONE, "trigger_pipesignal_" + name + "_active", "gate.buildcraftreborn.statement.pipe.wire.input.active",
                    colorName, (c, f) -> plugs(c).hasWire(color), (c, f, p) -> wireOn(c, color), (c, f, p) -> {}));
            register(new Statement("pipe.wire.input." + name + ".inactive", Statement.Kind.TRIGGER, Statement.Scope.INTERNAL,
                    Statement.ParamType.NONE, "trigger_pipesignal_" + name + "_inactive", "gate.buildcraftreborn.statement.pipe.wire.input.inactive",
                    colorName, (c, f) -> plugs(c).hasWire(color), (c, f, p) -> !wireOn(c, color), (c, f, p) -> {}));
            register(new Statement("pipe.wire.output." + name, Statement.Kind.ACTION, Statement.Scope.INTERNAL,
                    Statement.ParamType.NONE, "trigger_pipesignal_" + name + "_active", "gate.buildcraftreborn.statement.pipe.wire.output",
                    colorName, (c, f) -> plugs(c).hasWire(color), (c, f, p) -> false, (c, f, p) -> plugs(c).emitWire(color)));
        }

        // ── plugues do silicon ────────────────────────────────────────────
        trigger("light.dark", Statement.Scope.EXTERNAL, Statement.ParamType.NONE, "trigger_light_dark",
                (c, f) -> f != null && plugs(c).kind(f) == PlugKind.LIGHT_SENSOR,
                (c, f, p) -> f != null && c.level().getMaxLocalRawBrightness(c.pos().relative(f)) < 8);
        trigger("light.bright", Statement.Scope.EXTERNAL, Statement.ParamType.NONE, "trigger_light_bright",
                (c, f) -> f != null && plugs(c).kind(f) == PlugKind.LIGHT_SENSOR,
                (c, f, p) -> f != null && c.level().getMaxLocalRawBrightness(c.pos().relative(f)) >= 8);
        String[] timers = {"short", "medium", "long"};
        for (int i = 0; i < timers.length; i++) {
            long period = 20L * 5 * (i + 1);
            trigger("timer." + timers[i], Statement.Scope.INTERNAL, Statement.ParamType.NONE, "trigger_timer_" + timers[i],
                    (c, f) -> plugs(c).hasPlug(PlugKind.TIMER), (c, f, p) -> c.level().getGameTime() % period == 0);
        }
        action("pulsar.constant", Statement.Scope.EXTERNAL, Statement.ParamType.NONE, "action_pulsar_on",
                (c, f) -> f != null && plugs(c).kind(f) == PlugKind.PULSAR, (c, f, p) -> {
                    if (f != null) plugs(c).pulsarConstant(f);
                });
        action("pulsar.single", Statement.Scope.EXTERNAL, Statement.ParamType.NONE, "action_pulsar_single",
                (c, f) -> f != null && plugs(c).kind(f) == PlugKind.PULSAR, (c, f, p) -> {
                    if (f != null) plugs(c).pulsarSingle(f);
                });

        // ── energia e controle de máquinas ────────────────────────────────
        trigger("energy.high", Statement.Scope.EXTERNAL, Statement.ParamType.NONE, "trigger_energy_storage_high",
                (c, f) -> energyRatio(c, f) >= 0, (c, f, p) -> energyRatio(c, f) > 0.95);
        trigger("energy.low", Statement.Scope.EXTERNAL, Statement.ParamType.NONE, "trigger_energy_storage_low",
                (c, f) -> energyRatio(c, f) >= 0, (c, f, p) -> {
                    double ratio = energyRatio(c, f);
                    return ratio >= 0 && ratio < 0.05;
                });
        action("machine.off", Statement.Scope.EXTERNAL, Statement.ParamType.NONE, "action_machinecontrol_off",
                (c, f) -> machine(c, f) != null, (c, f, p) -> {
                    BCBlockEntity machine = machine(c, f);
                    if (machine != null) machine.disableFromGate();
                });

        // ── tubos especiais: pintar (lápis, daizuli) e presets do emzuli ──
        for (DyeColor color : DyeColor.values()) {
            register(new Statement("pipe.colour." + color.getName(), Statement.Kind.ACTION, Statement.Scope.INTERNAL, Statement.ParamType.NONE,
                    "action_pipe_colour_" + color.getName(), "gate.buildcraftreborn.statement.pipe.colour",
                    new Object[]{Component.translatable("color.minecraft." + color.getName())},
                    (c, f) -> c.level().getBlockState(c.pos()).hasProperty(net.buildcraftreborn.transport.block.ColoredPipeBlock.COLOR),
                    (c, f, p) -> false, (c, f, p) -> {
                        if (c.pipe() instanceof PipeBlockEntity pipe) pipe.setPipeColour(color);
                    }));
        }
        String[] presets = {"red", "green", "blue", "yellow"};
        for (int i = 0; i < presets.length; i++) {
            int slot = i;
            register(new Statement("pipe.extraction_preset." + presets[i], Statement.Kind.ACTION, Statement.Scope.INTERNAL, Statement.ParamType.NONE,
                    "extraction_preset_" + presets[i], "gate.buildcraftreborn.statement.pipe.extraction_preset." + presets[i], NO_ARGS,
                    (c, f) -> c.pipe() instanceof PipeBlockEntity pipe && pipe.type() == net.buildcraftreborn.transport.PipeType.EMZULI,
                    (c, f, p) -> false, (c, f, p) -> {
                        if (c.pipe() instanceof PipeBlockEntity pipe) pipe.activatePreset(slot);
                    }));
        }
    }

    private Statements() {}

    /** Fração da energia guardada no vizinho (bateria ou máquina); -1 se ele não guarda energia. */
    private static double energyRatio(GateContext context, @Nullable Direction face) {
        if (face == null) return -1;
        BlockPos other = context.pos().relative(face);
        if (context.level().getBlockState(other).getBlock() instanceof PipeBlock) return -1;
        net.craftenergy.api.EnergyNode node = net.craftenergy.fabric.CraftEnergyApi.NODE.find(context.level(), other, face.getOpposite());
        long stored;
        long capacity;
        if (node instanceof net.craftenergy.api.EnergyBuffer buffer) {
            stored = buffer.storedEnergy();
            capacity = buffer.energyCapacity();
        } else if (node instanceof MachineEnergy machine) {
            stored = machine.stored();
            capacity = machine.capacity();
        } else {
            return -1;
        }
        return capacity <= 0 ? -1 : (double) stored / capacity;
    }

    /** Máquina do BuildCraft ao lado que tica no servidor (não outro tubo). */
    private static @Nullable BCBlockEntity machine(GateContext context, @Nullable Direction face) {
        if (face == null) return null;
        BlockEntity blockEntity = context.level().getBlockEntity(context.pos().relative(face));
        return blockEntity instanceof BCBlockEntity machine && blockEntity instanceof ServerTicking && !(blockEntity instanceof PlugHolder)
                ? machine : null;
    }

    private static PipePlugs plugs(GateContext context) {
        return ((PlugHolder) context.pipe()).plugs();
    }

    private static boolean wireOn(GateContext context, DyeColor color) {
        PipePlugs plugs = plugs(context);
        for (int corner = 0; corner < WireNetwork.CORNERS; corner++) {
            if (plugs.wire(corner) == color && WireNetwork.powered(context.level(), context.pos(), corner, color)) return true;
        }
        return false;
    }

    private static void trigger(String id, Statement.Scope scope, Statement.ParamType param, String icon,
                                Statement.Availability availability, Statement.Test test) {
        trigger(id, scope, param, icon, availability, test, "gate.buildcraftreborn.statement." + id);
    }

    private static void trigger(String id, Statement.Scope scope, Statement.ParamType param, String icon,
                                Statement.Availability availability, Statement.Test test, String langKey, Object... args) {
        register(new Statement(id, Statement.Kind.TRIGGER, scope, param, icon, langKey, args.length == 0 ? NO_ARGS : args,
                availability, test, (c, f, p) -> {}));
    }

    private static void action(String id, Statement.Scope scope, Statement.ParamType param, String icon,
                               Statement.Availability availability, Statement.Run run) {
        register(new Statement(id, Statement.Kind.ACTION, scope, param, icon, "gate.buildcraftreborn.statement." + id, NO_ARGS,
                availability, (c, f, p) -> false, run));
    }

    /** Outras partes do mod (fios, pulsar...) acrescentam os seus. */
    public static void register(Statement statement) {
        BY_ID.put(statement.id(), statement);
    }

    public static @Nullable Statement get(String id) {
        return id.isEmpty() ? null : BY_ID.get(id);
    }

    /** Opções para um slot, na ordem da tela: vazio, internos e depois os externos de cada face. */
    public static List<GateLogic.Choice> choices(GateContext context, Statement.Kind kind) {
        List<GateLogic.Choice> choices = new ArrayList<>();
        choices.add(GateLogic.Choice.EMPTY);
        for (Statement statement : BY_ID.values()) {
            if (statement.kind() == kind && statement.scope() == Statement.Scope.INTERNAL && statement.availability().available(context, null)) {
                choices.add(new GateLogic.Choice(statement.id(), null));
            }
        }
        for (Direction face : Direction.values()) {
            if (face == context.side()) continue;
            for (Statement statement : BY_ID.values()) {
                if (statement.kind() == kind && statement.scope() == Statement.Scope.EXTERNAL && statement.availability().available(context, face)) {
                    choices.add(new GateLogic.Choice(statement.id(), face));
                }
            }
        }
        return choices;
    }

    // ── ajudas ────────────────────────────────────────────────────────────
    private static boolean flag(GateLogic.Param[] params) {
        return params.length > 0 && params[0].flag();
    }

    private static ItemStack item(GateLogic.Param[] params) {
        return params.length > 0 ? params[0].item() : ItemStack.EMPTY;
    }

    private static boolean redstone(GateContext context, GateLogic.Param[] params) {
        if (flag(params)) {
            Direction side = context.side();
            return context.level().getSignal(context.pos().relative(side), side) > 0;
        }
        return context.level().hasNeighborSignal(context.pos());
    }

    private static boolean matches(ItemStack stack, GateLogic.Param[] params) {
        ItemStack filter = item(params);
        return !stack.isEmpty() && (filter.isEmpty() || ItemStack.isSameItem(filter, stack));
    }

    private static boolean matches(ItemVariant variant, GateLogic.Param[] params) {
        ItemStack filter = item(params);
        return !variant.isBlank() && (filter.isEmpty() || variant.isOf(filter.getItem()));
    }

    private static @Nullable Storage<ItemVariant> items(GateContext context, @Nullable Direction face) {
        if (face == null) return null;
        BlockPos other = context.pos().relative(face);
        if (context.level().getBlockState(other).getBlock() instanceof PipeBlock) return null;
        return ItemStorage.SIDED.find(context.level(), other, face.getOpposite());
    }

    private static @Nullable Storage<FluidVariant> fluids(GateContext context, @Nullable Direction face) {
        if (face == null) return null;
        BlockPos other = context.pos().relative(face);
        if (context.level().getBlockState(other).getBlock() instanceof PipeBlock) return null;
        return FluidStorage.SIDED.find(context.level(), other, face.getOpposite());
    }

    private static boolean anyItem(@Nullable Storage<ItemVariant> storage, Predicate<StorageView<ItemVariant>> test) {
        if (storage == null) return false;
        for (StorageView<ItemVariant> view : storage) {
            if (!view.isResourceBlank() && view.getAmount() > 0 && test.test(view)) return true;
        }
        return false;
    }

    private static boolean itemSpace(@Nullable Storage<ItemVariant> storage, GateLogic.Param[] params) {
        if (storage == null) return false;
        ItemStack filter = item(params);
        if (!filter.isEmpty()) {
            try (Transaction transaction = Transaction.openOuter()) {
                return storage.insert(ItemVariant.of(filter), 1, transaction) > 0;
            }
        }
        for (StorageView<ItemVariant> view : storage) {
            if (view.isResourceBlank() || view.getAmount() < view.getCapacity()) return true;
        }
        return false;
    }

    private static double itemRatio(@Nullable Storage<ItemVariant> storage, GateLogic.Param[] params) {
        if (storage == null) return 1.0;
        long amount = 0;
        long capacity = 0;
        for (StorageView<ItemVariant> view : storage) {
            if (!view.isResourceBlank() && !matches(view.getResource(), params)) continue;
            amount += view.getAmount();
            capacity += view.getCapacity();
        }
        return capacity <= 0 ? 1.0 : (double) amount / capacity;
    }

    private static @Nullable FluidVariant fluidOf(GateLogic.Param[] params) {
        ItemStack stack = item(params);
        if (stack.isEmpty()) return null;
        Storage<FluidVariant> contents = ContainerItemContext.withConstant(stack).find(FluidStorage.ITEM);
        if (contents == null) return null;
        for (StorageView<FluidVariant> view : contents) {
            if (!view.isResourceBlank()) return view.getResource();
        }
        return null;
    }

    private static boolean anyFluid(@Nullable Storage<FluidVariant> storage, @Nullable FluidVariant filter) {
        if (storage == null) return false;
        for (StorageView<FluidVariant> view : storage) {
            if (!view.isResourceBlank() && view.getAmount() > 0 && (filter == null || view.getResource().equals(filter))) return true;
        }
        return false;
    }

    private static boolean fluidSpace(@Nullable Storage<FluidVariant> storage, @Nullable FluidVariant filter) {
        if (storage == null) return false;
        if (filter != null) {
            try (Transaction transaction = Transaction.openOuter()) {
                return storage.insert(filter, 1, transaction) > 0;
            }
        }
        for (StorageView<FluidVariant> view : storage) {
            if (view.isResourceBlank() || view.getAmount() < view.getCapacity()) return true;
        }
        return false;
    }

    private static double fluidRatio(@Nullable Storage<FluidVariant> storage, @Nullable FluidVariant filter) {
        if (storage == null) return 1.0;
        long amount = 0;
        long capacity = 0;
        for (StorageView<FluidVariant> view : storage) {
            if (filter != null && !view.isResourceBlank() && !view.getResource().equals(filter)) continue;
            amount += view.getAmount();
            capacity += view.getCapacity();
        }
        return capacity <= 0 ? 1.0 : (double) amount / capacity;
    }
}
