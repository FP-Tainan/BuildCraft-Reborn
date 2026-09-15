package net.buildcraftreborn.test;

import net.buildcraftreborn.factory.tile.TankBlockEntity;
import net.buildcraftreborn.registry.BCBlocks;
import net.buildcraftreborn.transport.PipeType;
import net.buildcraftreborn.transport.block.DirectionalPipeBlock;
import net.buildcraftreborn.transport.tile.FluidPipeBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

/** Sessão 6: tubos de fluidos. */
public class FluidTransportGameTest {
    private static void fluidPipe(GameTestHelper helper, BlockPos pos, PipeType type) {
        helper.setBlock(pos, BCBlocks.FLUID_PIPES.get(type).get());
    }

    private static TankBlockEntity tank(GameTestHelper helper, BlockPos pos) {
        return (TankBlockEntity) helper.getBlockEntity(pos, TankBlockEntity.class);
    }

    private static void insert(GameTestHelper helper, BlockPos pos, Direction side, long droplets) {
        Storage<FluidVariant> storage = FluidStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), side);
        if (storage == null) {
            helper.fail("deveria aceitar fluido pelo lado " + side);
            return;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            storage.insert(FluidVariant.of(Fluids.WATER), droplets, transaction);
            transaction.commit();
        }
    }

    @GameTest(maxTicks = 200)
    public void woodenFluidPipeMovesWaterBetweenTanks(GameTestHelper helper) {
        BlockPos source = new BlockPos(0, 1, 1);
        BlockPos target = new BlockPos(3, 1, 1);
        helper.setBlock(source, BCBlocks.TANK.get());
        helper.setBlock(target, BCBlocks.TANK.get());
        insert(helper, source, Direction.UP, FluidConstants.BUCKET * 4);
        fluidPipe(helper, new BlockPos(1, 1, 1), PipeType.WOOD);
        fluidPipe(helper, new BlockPos(2, 1, 1), PipeType.COBBLESTONE);
        helper.assertBlockProperty(new BlockPos(1, 1, 1), DirectionalPipeBlock.SPECIAL, Direction.WEST);
        FluidPipeBlockEntity wood = (FluidPipeBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1), FluidPipeBlockEntity.class);
        helper.onEachTick(() -> wood.energy().receivePower(4_000, 220));
        helper.succeedWhen(() -> {
            long arrived = tank(helper, target).tank().amountCL();
            if (arrived < 1_000) helper.fail("pelo menos um balde deveria chegar no outro tanque: " + arrived + " CL");
            FluidPipeBlockEntity cobble = (FluidPipeBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 1), FluidPipeBlockEntity.class);
            if (cobble.flowCL() <= 0 || cobble.shownVariant().isBlank()) helper.fail("o tubo de passagem deveria registrar a vazão para desenhar o fluido");
        });
    }

    @GameTest(maxTicks = 100)
    public void ironFluidPipeUsesOnlyItsOutput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.north(), BCBlocks.TANK.get());
        helper.setBlock(pos.east(), BCBlocks.TANK.get());
        fluidPipe(helper, pos, PipeType.IRON);
        helper.setBlock(pos, helper.getBlockState(pos).setValue(DirectionalPipeBlock.SPECIAL, Direction.EAST));
        insert(helper, pos, Direction.WEST, FluidConstants.BUCKET / 2);
        helper.succeedWhen(() -> {
            if (tank(helper, pos.east()).tank().amountCL() != 500) helper.fail("os 500 CL deveriam ir para o leste");
            if (!tank(helper, pos.north()).tank().isEmpty()) helper.fail("nada deveria ir para o norte");
        });
    }

    @GameTest(maxTicks = 40)
    public void voidFluidPipeDestroysFluid(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        fluidPipe(helper, pos, PipeType.VOID);
        insert(helper, pos, Direction.WEST, FluidConstants.BUCKET);
        FluidPipeBlockEntity pipe = (FluidPipeBlockEntity) helper.getBlockEntity(pos, FluidPipeBlockEntity.class);
        helper.succeedWhen(() -> {
            if (!pipe.tank().isEmpty()) helper.fail("o tubo vazio deveria destruir o fluido");
        });
    }

    @GameTest(maxTicks = 20)
    public void itemAndFluidPipesStayApart(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 1, 0), BCBlocks.PIPES.get(PipeType.COBBLESTONE).get());
        fluidPipe(helper, new BlockPos(1, 1, 0), PipeType.COBBLESTONE);
        helper.setBlock(new BlockPos(2, 1, 0), BCBlocks.PIPES.get(PipeType.STRUCTURE).get());
        helper.assertBlockProperty(new BlockPos(0, 1, 0), BlockStateProperties.EAST, false);
        helper.assertBlockProperty(new BlockPos(1, 1, 0), BlockStateProperties.EAST, true);
        helper.succeed();
    }
}
