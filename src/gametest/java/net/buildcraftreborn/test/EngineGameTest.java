package net.buildcraftreborn.test;

import net.buildcraftreborn.energy.engine.EngineBlock;
import net.buildcraftreborn.energy.engine.EngineBlockEntity;
import net.buildcraftreborn.energy.engine.EngineStage;
import net.buildcraftreborn.registry.BCBlocks;
import net.craftenergy.content.CEBlocks;
import net.craftenergy.fabric.CraftEnergyApi;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Sessão 2: motores de redstone, Stirling e criativo. */
public class EngineGameTest {
    private static EngineBlockEntity engine(GameTestHelper helper, BlockPos pos) {
        return (EngineBlockEntity) helper.getBlockEntity(pos, EngineBlockEntity.class);
    }

    @GameTest(maxTicks = 40)
    public void redstoneEngineNeedsSignal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BCBlocks.REDSTONE_ENGINE.get().defaultBlockState().setValue(EngineBlock.FACING, Direction.UP));
        helper.runAfterDelay(5, () -> {
            if (engine(helper, pos).source().availablePower() != 0) helper.fail("sem redstone o motor não deveria entregar energia");
            helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
        });
        helper.succeedWhen(() -> {
            long power = engine(helper, pos).source().availablePower();
            if (power != EngineBlockEntity.REDSTONE_OUTPUT) helper.fail("com redstone deveria entregar 50 CW: " + power);
        });
    }

    @GameTest(maxTicks = 40)
    public void stirlingBurnsFuelWithRedstone(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BCBlocks.STIRLING_ENGINE.get().defaultBlockState().setValue(EngineBlock.FACING, Direction.UP));
        engine(helper, pos).fuel().setItem(0, new ItemStack(Items.COAL, 2));
        helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            EngineBlockEntity engine = engine(helper, pos);
            if (engine.burnTime() <= 0 || engine.storedEnergy() < EngineBlockEntity.STIRLING_OUTPUT * 5) {
                helper.fail("o Stirling deveria estar queimando carvão: " + engine.burnTime() + " / " + engine.storedEnergy());
            }
            if (engine.fuel().getItem(0).getCount() != 1) helper.fail("deveria ter gastado um carvão");
        });
    }

    @GameTest(maxTicks = 20)
    public void stirlingStageFollowsBuffer(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BCBlocks.STIRLING_ENGINE.get());
        EngineBlockEntity engine = engine(helper, pos);
        engine.setStoredEnergy(EngineBlockEntity.STIRLING_CAPACITY / 10);
        if (engine.stage() != EngineStage.BLUE) helper.fail("buffer em 10% deveria ser azul: " + engine.stage());
        engine.setStoredEnergy(EngineBlockEntity.STIRLING_CAPACITY * 9 / 10);
        if (engine.stage() != EngineStage.OVERHEAT) helper.fail("buffer em 90% deveria superaquecer: " + engine.stage());
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void engineOutputsOnlyFromFront(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BCBlocks.CREATIVE_ENGINE.get().defaultBlockState().setValue(EngineBlock.FACING, Direction.NORTH));
        BlockPos absolute = helper.absolutePos(pos);
        if (CraftEnergyApi.NODE.find(helper.getLevel(), absolute, Direction.NORTH) == null) helper.fail("a frente deveria ter saída");
        if (CraftEnergyApi.NODE.find(helper.getLevel(), absolute, Direction.SOUTH) != null) helper.fail("a traseira não deveria ter saída");
        EngineBlockEntity engine = engine(helper, pos);
        long before = engine.maxOutput();
        engine.cycleCreativePower();
        if (engine.maxOutput() != before * 2) helper.fail("a chave agachado deveria dobrar a potência: " + engine.maxOutput());
        int voltage = engine.outputVoltage();
        engine.cycleCreativeVoltage();
        if (engine.outputVoltage() <= voltage) helper.fail("o clique deveria subir a tensão: " + engine.outputVoltage());
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void wrenchTurnsEngineTowardCable(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BCBlocks.REDSTONE_ENGINE.get().defaultBlockState().setValue(EngineBlock.FACING, Direction.UP));
        helper.setBlock(pos.south(), CEBlocks.CABLE_COPPER_INSULATED.get());
        BlockPos absolute = helper.absolutePos(pos);
        BCBlocks.REDSTONE_ENGINE.get().rotateToNext(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute));
        helper.assertBlockProperty(pos, EngineBlock.FACING, Direction.SOUTH);
        helper.succeed();
    }
}
