package net.buildcraftreborn.test;

import net.buildcraftreborn.registry.BCBlocks;
import net.buildcraftreborn.transport.PipeType;
import net.buildcraftreborn.transport.block.PipeBlock;
import net.buildcraftreborn.transport.gate.GateCopierItem;
import net.buildcraftreborn.transport.gate.GateLogic;
import net.buildcraftreborn.transport.gate.GateVariant;
import net.buildcraftreborn.transport.plug.PipePlugs;
import net.buildcraftreborn.transport.plug.PlugKind;
import net.buildcraftreborn.transport.tile.PipeBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Sessão 11 (parte B): fios, plugues do silicon e copiador de portas. */
public class WireGameTest {
    private static PipeBlockEntity pipe(GameTestHelper helper, BlockPos pos, PipeType type) {
        helper.setBlock(pos, BCBlocks.PIPES.get(type).get());
        return (PipeBlockEntity) helper.getBlockEntity(pos, PipeBlockEntity.class);
    }

    private static GateLogic gate(PipeBlockEntity pipe, GateVariant variant, String trigger, String action, Direction actionFace) {
        pipe.plugs().attachGate(Direction.UP, variant);
        GateLogic gate = pipe.plugs().gate(Direction.UP);
        gate.setTrigger(0, new GateLogic.Choice(trigger, null));
        gate.setAction(0, new GateLogic.Choice(action, actionFace));
        return gate;
    }

    @GameTest(maxTicks = 40)
    public void wireCarriesSignalBetweenPipes(GameTestHelper helper) {
        PipeBlockEntity sender = pipe(helper, new BlockPos(1, 1, 1), PipeType.COBBLESTONE);
        PipeBlockEntity middle = pipe(helper, new BlockPos(2, 1, 1), PipeType.COBBLESTONE);
        PipeBlockEntity receiver = pipe(helper, new BlockPos(3, 1, 1), PipeType.COBBLESTONE);
        // canto leste-cima-sul (7) liga com o oeste-cima-sul (6) do tubo ao lado; dentro do tubo, 6 liga com 7
        sender.plugs().placeWire(7, DyeColor.RED);
        middle.plugs().placeWire(6, DyeColor.RED);
        middle.plugs().placeWire(7, DyeColor.RED);
        receiver.plugs().placeWire(6, DyeColor.RED);
        gate(sender, GateVariant.BASIC, "true", "pipe.wire.output.red", null);
        gate(receiver, GateVariant.BASIC, "pipe.wire.input.red.active", "redstone.output", null);
        helper.succeedWhen(() -> {
            if (receiver.plugs().signal(Direction.NORTH) != 15) helper.fail("o sinal do fio vermelho não chegou");
            if (!middle.plugs().wirePowered(6)) helper.fail("o fio do meio deveria estar aceso");
        });
    }

    @GameTest(maxTicks = 40)
    public void differentColorsDoNotMix(GameTestHelper helper) {
        PipeBlockEntity sender = pipe(helper, new BlockPos(1, 1, 1), PipeType.COBBLESTONE);
        PipeBlockEntity receiver = pipe(helper, new BlockPos(2, 1, 1), PipeType.COBBLESTONE);
        sender.plugs().placeWire(7, DyeColor.RED);
        receiver.plugs().placeWire(6, DyeColor.BLUE);
        gate(sender, GateVariant.BASIC, "true", "pipe.wire.output.red", null);
        gate(receiver, GateVariant.BASIC, "pipe.wire.input.blue.active", "redstone.output", null);
        helper.runAfterDelay(20, () -> {
            if (receiver.plugs().signal(Direction.NORTH) != 0) helper.fail("fio azul não pode receber o vermelho");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 130)
    public void timerTriggerFires(GameTestHelper helper) {
        PipeBlockEntity pipe = pipe(helper, new BlockPos(1, 1, 1), PipeType.COBBLESTONE);
        pipe.plugs().attachPlug(Direction.WEST, PlugKind.TIMER);
        gate(pipe, GateVariant.BASIC, "timer.short", "redstone.output", null);
        helper.succeedWhen(() -> {
            if (pipe.plugs().signal(Direction.NORTH) != 15) helper.fail("o temporizador de 5 s deveria disparar");
        });
    }

    @GameTest(maxTicks = 60)
    public void pulsarPowersWoodPipe(GameTestHelper helper) {
        PipeBlockEntity pipe = pipe(helper, new BlockPos(1, 1, 1), PipeType.WOOD);
        if (pipe.plugs().energy() == null) helper.fail("tubo de madeira deveria aceitar pulsar");
        pipe.plugs().attachPlug(Direction.NORTH, PlugKind.PULSAR);
        gate(pipe, GateVariant.BASIC, "true", "pulsar.constant", Direction.NORTH);
        helper.succeedWhen(() -> {
            if (pipe.energy().stored() < PipePlugs.PULSE_ENERGY) helper.fail("o pulsar deveria dar 1 CWh por segundo");
        });
    }

    @GameTest(maxTicks = 20)
    public void plugBlocksConnectionAndCopierCopies(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 1, 1);
        PipeBlockEntity first = pipe(helper, a, PipeType.COBBLESTONE);
        PipeBlockEntity second = pipe(helper, a.east(), PipeType.COBBLESTONE);
        first.plugs().attachPlug(Direction.EAST, PlugKind.LIGHT_SENSOR);
        PipeBlock.refreshConnections(helper.getLevel(), first.getBlockPos());
        helper.assertBlockProperty(a, BlockStateProperties.EAST, false);

        GateLogic source = gate(first, new GateVariant(GateVariant.Logic.AND, GateVariant.Material.IRON, GateVariant.Modifier.NONE),
                "true", "redstone.output", null);
        source.setConnected(0, true);
        CompoundTag copied = GateCopierItem.copy(source, helper.getLevel().registryAccess());
        GateVariant target = new GateVariant(GateVariant.Logic.OR, GateVariant.Material.GOLD, GateVariant.Modifier.NONE);
        GateLogic pasted = GateCopierItem.paste(copied, target, helper.getLevel().registryAccess());
        second.plugs().setGate(Direction.UP, pasted);
        GateLogic result = second.plugs().gate(Direction.UP);
        if (!result.variant().equals(target)) helper.fail("a porta colada deve manter a própria variante");
        if (!"true".equals(result.trigger(0).id()) || !"redstone.output".equals(result.action(0).id()) || !result.connected(0)) {
            helper.fail("o copiador deveria levar gatilho, ação e ligação");
        }
        helper.succeed();
    }
}
