package net.buildcraftreborn.test;

import net.buildcraftreborn.energy.fluid.BCFluids;
import net.buildcraftreborn.factory.block.HeatExchangerBlock;
import net.buildcraftreborn.factory.block.WaterGelBlock;
import net.buildcraftreborn.factory.tile.DistillerBlockEntity;
import net.buildcraftreborn.factory.tile.HeatExchangerBlockEntity;
import net.buildcraftreborn.lib.fluid.BCTank;
import net.buildcraftreborn.registry.BCBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Sessão 9: destilador, trocador de calor e gel de água. */
public class RefineryGameTest {
    private static void fill(BCTank tank, Fluid fluid, long cl) {
        try (Transaction transaction = Transaction.openOuter()) {
            tank.insert(FluidVariant.of(fluid), BCTank.fromCL(cl), transaction);
            transaction.commit();
        }
    }

    private static Fluid fluid(BCFluids.Kind kind, BCFluids.Heat heat) {
        return BCFluids.get(kind, heat).fluid();
    }

    private static DistillerBlockEntity distiller(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, BCBlocks.DISTILLER.get());
        DistillerBlockEntity distiller = (DistillerBlockEntity) helper.getBlockEntity(pos, DistillerBlockEntity.class);
        helper.onEachTick(() -> distiller.energy().receivePower(DistillerBlockEntity.MAX_POWER, 1_000));
        return distiller;
    }

    @GameTest(maxTicks = 200)
    public void distillerSplitsCoolOil(GameTestHelper helper) {
        DistillerBlockEntity distiller = distiller(helper, new BlockPos(1, 2, 1));
        fill(distiller.tank(DistillerBlockEntity.TANK_INPUT), fluid(BCFluids.Kind.OIL, BCFluids.Heat.COOL), 1_000);
        helper.succeedWhen(() -> {
            BCTank gas = distiller.tank(DistillerBlockEntity.TANK_GAS);
            BCTank liquid = distiller.tank(DistillerBlockEntity.TANK_LIQUID);
            if (!gas.variant.isOf(fluid(BCFluids.Kind.FUEL_GASEOUS, BCFluids.Heat.COOL)) || gas.amountCL() < 32) {
                helper.fail("petróleo frio deveria dar combustível gasoso em cima: " + gas.amountCL());
            }
            if (!liquid.variant.isOf(fluid(BCFluids.Kind.OIL_HEAVY, BCFluids.Heat.COOL))) helper.fail("e petróleo pesado embaixo");
        });
    }

    @GameTest(maxTicks = 200)
    public void distillerFollowsTemperature(GameTestHelper helper) {
        DistillerBlockEntity distiller = distiller(helper, new BlockPos(1, 2, 1));
        fill(distiller.tank(DistillerBlockEntity.TANK_INPUT), fluid(BCFluids.Kind.OIL, BCFluids.Heat.HOT), 1_000);
        helper.succeedWhen(() -> {
            if (!distiller.tank(DistillerBlockEntity.TANK_GAS).variant.isOf(fluid(BCFluids.Kind.FUEL_MIXED_LIGHT, BCFluids.Heat.HOT))) {
                helper.fail("petróleo quente deveria dar combustíveis leves misturados quentes");
            }
            if (!distiller.tank(DistillerBlockEntity.TANK_LIQUID).variant.isOf(fluid(BCFluids.Kind.OIL_DENSE, BCFluids.Heat.HOT))) {
                helper.fail("e petróleo denso quente");
            }
        });
    }

    private static HeatExchangerBlockEntity[] exchangerLine(GameTestHelper helper) {
        BlockPos start = new BlockPos(1, 2, 1);
        for (int i = 0; i < 3; i++) {
            helper.setBlock(start.east(i), BCBlocks.HEAT_EXCHANGER.get().defaultBlockState().setValue(HeatExchangerBlock.FACING, Direction.EAST));
        }
        helper.assertBlockProperty(start, HeatExchangerBlock.PART, HeatExchangerBlock.Part.START);
        helper.assertBlockProperty(start.east(), HeatExchangerBlock.PART, HeatExchangerBlock.Part.MIDDLE);
        helper.assertBlockProperty(start.east(2), HeatExchangerBlock.PART, HeatExchangerBlock.Part.END);
        return new HeatExchangerBlockEntity[]{
                (HeatExchangerBlockEntity) helper.getBlockEntity(start, HeatExchangerBlockEntity.class),
                (HeatExchangerBlockEntity) helper.getBlockEntity(start.east(2), HeatExchangerBlockEntity.class)};
    }

    @GameTest(maxTicks = 300)
    public void heatExchangerHeatsOilWithLava(GameTestHelper helper) {
        HeatExchangerBlockEntity[] line = exchangerLine(helper);
        fill(line[0].input(), fluid(BCFluids.Kind.OIL, BCFluids.Heat.COOL), 1_000);
        fill(line[1].input(), Fluids.LAVA, 1_000);
        helper.succeedWhen(() -> {
            if (!line[0].output().variant.isOf(fluid(BCFluids.Kind.OIL, BCFluids.Heat.HOT)) || line[0].output().amountCL() < 20) {
                helper.fail("a lava deveria esquentar o petróleo: " + line[0].output().amountCL() + " " + line[0].progress());
            }
            if (line[1].input().amountCL() >= 1_000) helper.fail("deveria gastar lava");
        });
    }

    @GameTest(maxTicks = 200)
    public void heatExchangerNeedsHotterFluid(GameTestHelper helper) {
        HeatExchangerBlockEntity[] line = exchangerLine(helper);
        fill(line[0].input(), fluid(BCFluids.Kind.OIL, BCFluids.Heat.HOT), 1_000);
        fill(line[1].input(), fluid(BCFluids.Kind.OIL, BCFluids.Heat.HOT), 1_000);
        helper.runAfterDelay(150, () -> {
            if (!line[0].output().isEmpty()) helper.fail("fluidos na mesma temperatura não trocam calor");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 20)
    public void waterGelSpreadsIntoWater(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos.east(), Blocks.WATER);
        helper.setBlock(pos, BCBlocks.WATER_GEL.get());
        BlockPos absolute = helper.absolutePos(pos);
        WaterGelBlock.advance(helper.getLevel(), absolute, helper.getLevel().getBlockState(absolute), helper.getLevel().getRandom());
        helper.assertBlockProperty(pos, WaterGelBlock.STAGE, 1);
        helper.assertBlockPresent(BCBlocks.WATER_GEL.get(), pos.east());
        helper.succeed();
    }
}
