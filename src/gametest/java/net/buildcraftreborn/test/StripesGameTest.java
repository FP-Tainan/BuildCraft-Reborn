package net.buildcraftreborn.test;

import net.buildcraftreborn.factory.tile.TankBlockEntity;
import net.buildcraftreborn.lib.fluid.BCTank;
import net.buildcraftreborn.registry.BCBlocks;
import net.buildcraftreborn.registry.BCItems;
import net.buildcraftreborn.transport.PipeType;
import net.buildcraftreborn.transport.tile.FilteredBufferBlockEntity;
import net.buildcraftreborn.transport.tile.FluidPipeBlockEntity;
import net.buildcraftreborn.transport.tile.PipeBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/** Sessão 14 (parte B): stripes, buffer filtrado e madeira-diamante de fluido. */
public class StripesGameTest {
    private static PipeBlockEntity pipe(GameTestHelper helper, BlockPos pos, PipeType type) {
        helper.setBlock(pos, BCBlocks.PIPES.get(type).get());
        return (PipeBlockEntity) helper.getBlockEntity(pos, PipeBlockEntity.class);
    }

    private static long insert(GameTestHelper helper, BlockPos pos, Direction side, ItemStack stack) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), side);
        if (storage == null) {
            helper.fail("deveria aceitar itens pelo lado " + side);
            return 0;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            transaction.commit();
            return inserted;
        }
    }

    @GameTest(maxTicks = 200)
    public void stripesBreaksBlockAndSendsDropBack(GameTestHelper helper) {
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.CHEST);
        pipe(helper, new BlockPos(1, 1, 1), PipeType.COBBLESTONE);
        PipeBlockEntity stripes = pipe(helper, new BlockPos(2, 1, 1), PipeType.STRIPES);
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.DIRT);
        helper.onEachTick(() -> stripes.energy().receivePower(4_000, 220));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 1, 1));
            if (((Container) helper.getBlockEntity(new BlockPos(0, 1, 1), BlockEntity.class)).countItem(Items.DIRT) != 1) {
                helper.fail("a terra quebrada deveria voltar pelo tubo até o baú");
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void stripesPlacesBlockAtItsTip(GameTestHelper helper) {
        pipe(helper, new BlockPos(1, 1, 1), PipeType.COBBLESTONE);
        pipe(helper, new BlockPos(2, 1, 1), PipeType.STRIPES);
        insert(helper, new BlockPos(1, 1, 1), Direction.WEST, new ItemStack(Items.COBBLESTONE));
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(3, 1, 1)));
    }

    @GameTest(maxTicks = 200)
    public void stripesExtendsWithPipeItem(GameTestHelper helper) {
        pipe(helper, new BlockPos(0, 1, 1), PipeType.COBBLESTONE);
        pipe(helper, new BlockPos(1, 1, 1), PipeType.STRIPES);
        insert(helper, new BlockPos(0, 1, 1), Direction.WEST, new ItemStack(BCItems.PIPES.get(PipeType.COBBLESTONE).get()));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(BCBlocks.PIPES.get(PipeType.COBBLESTONE).get(), new BlockPos(1, 1, 1));
            helper.assertBlockPresent(BCBlocks.PIPES.get(PipeType.STRIPES).get(), new BlockPos(2, 1, 1));
        });
    }

    @GameTest(maxTicks = 20)
    public void filteredBufferAcceptsOnlyFilteredItems(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BCBlocks.FILTERED_BUFFER.get());
        FilteredBufferBlockEntity buffer = (FilteredBufferBlockEntity) helper.getBlockEntity(pos, FilteredBufferBlockEntity.class);
        buffer.filters().setItem(0, new ItemStack(Items.DIRT));
        long dirt = insert(helper, pos, Direction.UP, new ItemStack(Items.DIRT, 5));
        long cobble = insert(helper, pos, Direction.UP, new ItemStack(Items.COBBLESTONE, 5));
        if (dirt != 5 || buffer.storage().countItem(Items.DIRT) != 5) helper.fail("a terra deveria entrar no slot do filtro");
        if (cobble != 0) helper.fail("sem filtro de pedregulho nada entra");
        helper.succeed();
    }

    private static TankBlockEntity tank(GameTestHelper helper, BlockPos pos, Fluid fluid) {
        helper.setBlock(pos, BCBlocks.TANK.get());
        TankBlockEntity tank = (TankBlockEntity) helper.getBlockEntity(pos, TankBlockEntity.class);
        if (fluid != Fluids.EMPTY) {
            try (Transaction transaction = Transaction.openOuter()) {
                tank.tank().insert(FluidVariant.of(fluid), BCTank.fromCL(2_000), transaction);
                transaction.commit();
            }
        }
        return tank;
    }

    @GameTest(maxTicks = 120)
    public void diamondWoodFluidPipeFiltersFluids(GameTestHelper helper) {
        tank(helper, new BlockPos(0, 1, 1), Fluids.LAVA);
        TankBlockEntity lavaTarget = tank(helper, new BlockPos(2, 1, 1), Fluids.EMPTY);
        tank(helper, new BlockPos(0, 1, 3), Fluids.WATER);
        TankBlockEntity waterTarget = tank(helper, new BlockPos(2, 1, 3), Fluids.EMPTY);
        for (BlockPos pos : new BlockPos[]{new BlockPos(1, 1, 1), new BlockPos(1, 1, 3)}) {
            helper.setBlock(pos, BCBlocks.FLUID_PIPES.get(PipeType.DIAMOND_WOOD).get());
            FluidPipeBlockEntity pipe = (FluidPipeBlockEntity) helper.getBlockEntity(pos, FluidPipeBlockEntity.class);
            pipe.filters().setItem(0, new ItemStack(Items.WATER_BUCKET));
            helper.onEachTick(() -> pipe.energy().receivePower(4_000, 220));
        }
        helper.runAfterDelay(100, () -> {
            if (!lavaTarget.tank().isEmpty()) helper.fail("a lava não está na lista branca");
            if (waterTarget.tank().amountCL() <= 0) helper.fail("a água (balde no filtro) deveria passar");
            helper.succeed();
        });
    }
}
