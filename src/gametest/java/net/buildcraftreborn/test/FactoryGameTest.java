package net.buildcraftreborn.test;

import net.buildcraftreborn.factory.tile.AutoWorkbenchBlockEntity;
import net.buildcraftreborn.factory.tile.FloodGateBlockEntity;
import net.buildcraftreborn.factory.tile.MiningWellBlockEntity;
import net.buildcraftreborn.factory.tile.PumpBlockEntity;
import net.buildcraftreborn.factory.tile.TankBlockEntity;
import net.buildcraftreborn.registry.BCBlocks;
import net.craftenergy.api.EnergyUnits;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;

/** Sessão 3: tanque, bomba, poço de mineração, comporta, calha e bancada automática. */
public class FactoryGameTest {
    private static Item item(String name) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(name)).orElseThrow();
    }

    private static void fill(FluidVariant variant, long droplets, net.fabricmc.fabric.api.transfer.v1.storage.Storage<FluidVariant> storage) {
        try (Transaction transaction = Transaction.openOuter()) {
            storage.insert(variant, droplets, transaction);
            transaction.commit();
        }
    }

    @GameTest(maxTicks = 40)
    public void tankFluidFallsIntoTankBelow(GameTestHelper helper) {
        BlockPos bottom = new BlockPos(1, 1, 1);
        helper.setBlock(bottom, BCBlocks.TANK.get());
        helper.setBlock(bottom.above(), BCBlocks.TANK.get());
        helper.assertBlockProperty(bottom.above(), net.buildcraftreborn.factory.block.TankBlock.JOINED_BELOW, true);
        TankBlockEntity top = (TankBlockEntity) helper.getBlockEntity(bottom.above(), TankBlockEntity.class);
        fill(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET * 3, top.tank());
        helper.succeedWhen(() -> {
            TankBlockEntity lower = (TankBlockEntity) helper.getBlockEntity(bottom, TankBlockEntity.class);
            if (lower.tank().amountCL() != 3_000 || !top.tank().isEmpty()) {
                helper.fail("a água deveria descer para o tanque de baixo: " + lower.tank().amountCL() + " / " + top.tank().amountCL());
            }
        });
    }

    @GameTest(maxTicks = 60)
    public void pumpDrainsWaterWithEnergy(GameTestHelper helper) {
        BlockPos water = new BlockPos(1, 1, 1);
        helper.setBlock(water.below(), Blocks.STONE);
        for (Direction direction : Direction.Plane.HORIZONTAL) helper.setBlock(water.relative(direction), Blocks.STONE);
        helper.setBlock(water, Blocks.WATER);
        BlockPos pumpPos = water.above(2);
        helper.setBlock(pumpPos, BCBlocks.PUMP.get());
        PumpBlockEntity pump = (PumpBlockEntity) helper.getBlockEntity(pumpPos, PumpBlockEntity.class);
        pump.energy().receivePower(EnergyUnits.fromCWh(50), 220);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(BCBlocks.MINING_PIPE.get(), water.above());
            if (pump.tank().amountCL() != 1_000) helper.fail("a bomba deveria ter 1.000 CL de água: " + pump.tank().amountCL());
            helper.assertBlockNotPresent(Blocks.WATER, water);
        });
    }

    @GameTest(maxTicks = 60)
    public void miningWellDigsIntoChest(GameTestHelper helper) {
        BlockPos wellPos = new BlockPos(1, 3, 1);
        helper.setBlock(wellPos.below(), Blocks.STONE);
        helper.setBlock(wellPos, BCBlocks.MINING_WELL.get());
        helper.setBlock(wellPos.east(), Blocks.CHEST);
        MiningWellBlockEntity well = (MiningWellBlockEntity) helper.getBlockEntity(wellPos, MiningWellBlockEntity.class);
        well.energy().receivePower(EnergyUnits.fromCWh(500), 220);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(BCBlocks.MINING_PIPE.get(), wellPos.below());
            Container chest = (Container) helper.getBlockEntity(wellPos.east(), net.minecraft.world.level.block.entity.BlockEntity.class);
            if (chest.countItem(Items.COBBLESTONE) != 1) helper.fail("o pedregulho deveria ir para o baú");
        });
    }

    @GameTest(maxTicks = 60)
    public void floodGatePlacesWater(GameTestHelper helper) {
        BlockPos gatePos = new BlockPos(1, 3, 1);
        helper.setBlock(gatePos, BCBlocks.FLOOD_GATE.get());
        FloodGateBlockEntity gate = (FloodGateBlockEntity) helper.getBlockEntity(gatePos, FloodGateBlockEntity.class);
        fill(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET, gate.tank());
        helper.succeedWhen(() -> {
            if (!gate.tank().isEmpty()) helper.fail("a comporta deveria ter usado o balde");
        });
    }

    @GameTest(maxTicks = 40)
    public void autoWorkbenchCraftsFromMaterials(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BCBlocks.AUTO_WORKBENCH.get());
        AutoWorkbenchBlockEntity workbench = (AutoWorkbenchBlockEntity) helper.getBlockEntity(pos, AutoWorkbenchBlockEntity.class);
        Item planks = item("oak_planks");
        for (int slot : new int[]{0, 1, 3, 4}) workbench.grid().setItem(slot, new ItemStack(planks));
        workbench.inventory().setItem(0, new ItemStack(planks, 4));
        // sem energia não fabrica; depois de 5 ticks a rede passa a alimentar
        int[] ticks = {0};
        helper.onEachTick(() -> {
            if (++ticks[0] == 5 && !workbench.inventory().getItem(AutoWorkbenchBlockEntity.OUTPUT).isEmpty()) {
                helper.fail("sem energia a bancada não deveria fabricar");
            }
            if (ticks[0] > 5) workbench.energy().receivePower(AutoWorkbenchBlockEntity.MAX_INPUT, 220);
        });
        helper.succeedWhen(() -> {
            ItemStack output = workbench.inventory().getItem(AutoWorkbenchBlockEntity.OUTPUT);
            if (!output.is(Items.CRAFTING_TABLE)) helper.fail("a bancada deveria ter feito uma mesa de trabalho: " + output);
            if (!workbench.inventory().getItem(0).isEmpty()) helper.fail("as tábuas deveriam ter sido usadas");
        });
    }
}
