package net.buildcraftreborn.test;

import net.buildcraftreborn.energy.engine.CombustionEngine;
import net.buildcraftreborn.energy.engine.EngineBlock;
import net.buildcraftreborn.energy.engine.EngineBlockEntity;
import net.buildcraftreborn.energy.engine.EngineStage;
import net.buildcraftreborn.energy.fluid.BCFluids;
import net.buildcraftreborn.energy.fluid.BCFuels;
import net.buildcraftreborn.lib.fluid.BCTank;
import net.buildcraftreborn.registry.BCBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Sessão 7: fluidos do petróleo e motor a combustão. */
public class CombustionGameTest {
    private static EngineBlockEntity place(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, BCBlocks.COMBUSTION_ENGINE.get().defaultBlockState().setValue(EngineBlock.FACING, Direction.UP));
        return (EngineBlockEntity) helper.getBlockEntity(pos, EngineBlockEntity.class);
    }

    private static void fill(BCTank tank, Fluid fluid, long cl) {
        try (Transaction transaction = Transaction.openOuter()) {
            tank.insert(FluidVariant.of(fluid), BCTank.fromCL(cl), transaction);
            transaction.commit();
        }
    }

    @GameTest(maxTicks = 20)
    public void oilFluidsAndFuelsAreRegistered(GameTestHelper helper) {
        if (BCFluids.all().size() != 30) helper.fail("deveriam existir 10 fluidos × 3 temperaturas: " + BCFluids.all().size());
        FluidVariant oil = FluidVariant.of(BCFluids.get(BCFluids.Kind.OIL).fluid());
        if (BCFuels.fuel(oil) == null || !BCFuels.fuel(oil).dirty()) helper.fail("petróleo deveria ser combustível sujo");
        if (BCFuels.fuel(FluidVariant.of(BCFluids.get(BCFluids.Kind.OIL, BCFluids.Heat.HOT).fluid())) != null) {
            helper.fail("petróleo quente não queima no motor");
        }
        if (BCFuels.cooling(FluidVariant.of(Fluids.WATER)) <= 0) helper.fail("água deveria ser refrigerante");
        helper.succeed();
    }

    @GameTest(maxTicks = 160)
    public void combustionEngineBurnsOilAndLeavesResidue(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        EngineBlockEntity engine = place(helper, pos);
        CombustionEngine combustion = engine.combustion();
        if (combustion == null) {
            helper.fail("motor a combustão sem tanques");
            return;
        }
        if (engine.outputVoltage() != CombustionEngine.VOLTAGE) helper.fail("deveria sair a 1.000 MV");
        fill(combustion.fuelTank(), BCFluids.get(BCFluids.Kind.OIL).fluid(), 1_000);
        helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            if (engine.storedEnergy() < 3_000L * 90) helper.fail("deveria gerar 3.000 CW por tick: " + engine.storedEnergy());
            if (combustion.fuelTank().amountCL() >= 1_000) helper.fail("deveria gastar petróleo");
            if (combustion.residueTank().amountCL() <= 0) helper.fail("petróleo deveria deixar resíduo");
        });
    }

    @GameTest(maxTicks = 100)
    public void waterHoldsHeatAtIdeal(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        EngineBlockEntity engine = place(helper, pos);
        CombustionEngine combustion = engine.combustion();
        fill(combustion.fuelTank(), BCFluids.get(BCFluids.Kind.FUEL_LIGHT).fluid(), 1_000);
        fill(combustion.coolantTank(), Fluids.WATER, 2_000);
        combustion.setHeat(101.0);
        helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            if (combustion.heat() > CombustionEngine.IDEAL_HEAT + 0.05) helper.fail("a água deveria segurar o calor em 100: " + combustion.heat());
            if (combustion.coolantTank().amountCL() >= 2_000) helper.fail("deveria gastar água");
            if (combustion.currentOutput() <= 0) helper.fail("deveria continuar queimando");
        });
    }

    @GameTest(maxTicks = 40)
    public void overheatStopsEngine(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        EngineBlockEntity engine = place(helper, pos);
        CombustionEngine combustion = engine.combustion();
        fill(combustion.fuelTank(), BCFluids.get(BCFluids.Kind.FUEL_LIGHT).fluid(), 1_000);
        combustion.setHeat(235.0);
        if (combustion.stage() != EngineStage.OVERHEAT) helper.fail("235 CCº deveria ser superaquecido: " + combustion.stage());
        helper.setBlock(pos.east(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(10, () -> {
            if (!combustion.isPenalized() || combustion.currentOutput() != 0) helper.fail("superaquecido deveria parar de queimar");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 20)
    public void iceBecomesCoolant(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        EngineBlockEntity engine = place(helper, pos);
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), Direction.NORTH);
        if (storage == null) {
            helper.fail("o motor deveria aceitar gelo por funis e tubos");
            return;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            if (storage.insert(ItemVariant.of(Items.ICE), 2, transaction) != 2) helper.fail("deveria aceitar 2 gelos");
            transaction.commit();
        }
        long water = engine.combustion().coolantTank().amountCL();
        if (water != 3_000) helper.fail("2 gelos deveriam virar 3.000 CL de água: " + water);
        helper.succeed();
    }
}
