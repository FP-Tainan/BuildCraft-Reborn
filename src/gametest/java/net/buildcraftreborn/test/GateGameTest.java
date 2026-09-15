package net.buildcraftreborn.test;

import net.buildcraftreborn.factory.tile.DistillerBlockEntity;
import net.buildcraftreborn.registry.BCBlocks;
import net.buildcraftreborn.transport.PipeType;
import net.buildcraftreborn.transport.block.PipeBlock;
import net.buildcraftreborn.transport.gate.GateLogic;
import net.buildcraftreborn.transport.gate.GateVariant;
import net.buildcraftreborn.transport.tile.PipeBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Sessão 11 (parte A): portas lógicas nos tubos. */
public class GateGameTest {
    private static PipeBlockEntity pipe(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, BCBlocks.PIPES.get(PipeType.COBBLESTONE).get());
        return (PipeBlockEntity) helper.getBlockEntity(pos, PipeBlockEntity.class);
    }

    private static GateLogic gate(GameTestHelper helper, PipeBlockEntity pipe, Direction side, GateVariant variant) {
        pipe.plugs().attachGate(side, variant);
        PipeBlock.refreshConnections(helper.getLevel(), pipe.getBlockPos());
        return pipe.plugs().gate(side);
    }

    @GameTest(maxTicks = 20)
    public void gateBlocksPipeConnection(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 1, 1);
        PipeBlockEntity first = pipe(helper, a);
        pipe(helper, a.east());
        helper.assertBlockProperty(a, BlockStateProperties.EAST, true);
        gate(helper, first, Direction.EAST, GateVariant.BASIC);
        helper.assertBlockProperty(a, BlockStateProperties.EAST, false);
        helper.assertBlockProperty(a.east(), BlockStateProperties.WEST, false);
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void redstoneInputDrivesRedstoneOutput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        PipeBlockEntity pipe = pipe(helper, pos);
        GateLogic gate = gate(helper, pipe, Direction.UP, new GateVariant(GateVariant.Logic.AND, GateVariant.Material.IRON, GateVariant.Modifier.NONE));
        gate.setTrigger(0, new GateLogic.Choice("redstone.input.active", null));
        gate.setAction(0, new GateLogic.Choice("redstone.output", null));
        helper.runAfterDelay(5, () -> {
            if (pipe.plugs().signal(Direction.NORTH) != 0) helper.fail("sem redstone a porta não deveria emitir");
            helper.setBlock(pos.west(), Blocks.REDSTONE_BLOCK);
        });
        helper.succeedWhen(() -> {
            if (pipe.plugs().signal(Direction.NORTH) != 15) helper.fail("com redstone a porta deveria emitir 15");
        });
    }

    @GameTest(maxTicks = 40)
    public void inventoryTriggerSeesChest(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.west(), Blocks.CHEST);
        PipeBlockEntity pipe = pipe(helper, pos);
        GateLogic gate = gate(helper, pipe, Direction.UP, GateVariant.BASIC);
        gate.setTrigger(0, new GateLogic.Choice("inventory.contains", Direction.WEST));
        gate.setAction(0, new GateLogic.Choice("redstone.output", null));
        ((Container) helper.getBlockEntity(pos.west(), BlockEntity.class)).setItem(0, new ItemStack(Items.DIAMOND));
        helper.succeedWhen(() -> {
            if (pipe.plugs().signal(Direction.EAST) != 15) helper.fail("baú com item deveria acionar a porta");
        });
    }

    @GameTest(maxTicks = 30)
    public void gateReadsEnergyAndTurnsMachineOff(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.west(), BCBlocks.DISTILLER.get());
        PipeBlockEntity pipe = pipe(helper, pos);
        GateLogic gate = gate(helper, pipe, Direction.UP, new GateVariant(GateVariant.Logic.AND, GateVariant.Material.IRON, GateVariant.Modifier.NONE));
        gate.setTrigger(0, new GateLogic.Choice("energy.low", Direction.WEST));
        gate.setAction(0, new GateLogic.Choice("machine.off", Direction.WEST));
        gate.setTrigger(1, new GateLogic.Choice("energy.low", Direction.WEST));
        gate.setAction(1, new GateLogic.Choice("redstone.output", null));
        DistillerBlockEntity distiller = (DistillerBlockEntity) helper.getBlockEntity(pos.west(), DistillerBlockEntity.class);
        helper.succeedWhen(() -> {
            if (pipe.plugs().signal(Direction.NORTH) != 15) helper.fail("destilador sem energia deveria acionar energia baixa");
            if (!distiller.gateDisabled()) helper.fail("a porta deveria desligar o destilador");
        });
    }

    @GameTest(maxTicks = 30)
    public void andNeedsEveryTriggerOrNeedsOne(GameTestHelper helper) {
        PipeBlockEntity andPipe = pipe(helper, new BlockPos(1, 1, 1));
        PipeBlockEntity orPipe = pipe(helper, new BlockPos(3, 1, 1));
        GateLogic and = gate(helper, andPipe, Direction.UP, new GateVariant(GateVariant.Logic.AND, GateVariant.Material.IRON, GateVariant.Modifier.NONE));
        GateLogic or = gate(helper, orPipe, Direction.UP, new GateVariant(GateVariant.Logic.OR, GateVariant.Material.IRON, GateVariant.Modifier.NONE));
        for (GateLogic gate : new GateLogic[]{and, or}) {
            gate.setConnected(0, true);
            gate.setTrigger(0, new GateLogic.Choice("true", null));
            gate.setAction(0, new GateLogic.Choice("redstone.output", null));
        }
        helper.runAfterDelay(10, () -> {
            if (andPipe.plugs().signal(Direction.NORTH) != 0) helper.fail("AND com um gatilho vazio no grupo não liga");
            if (orPipe.plugs().signal(Direction.NORTH) != 15) helper.fail("OR com um gatilho verdadeiro liga");
            helper.succeed();
        });
    }
}
