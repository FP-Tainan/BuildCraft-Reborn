package net.buildcraftreborn.test;

import net.buildcraftreborn.registry.BCBlocks;
import net.buildcraftreborn.transport.PipeType;
import net.buildcraftreborn.transport.block.DirectionalPipeBlock;
import net.buildcraftreborn.transport.tile.PipeBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Sessão 5: tubos de itens. */
public class TransportGameTest {
    private static void pipe(GameTestHelper helper, BlockPos pos, PipeType type) {
        helper.setBlock(pos, BCBlocks.PIPES.get(type).get());
    }

    private static String describe(PipeBlockEntity pipe) {
        StringBuilder text = new StringBuilder("[");
        for (PipeBlockEntity.TravellingItem item : pipe.items()) {
            text.append(item.stack).append(' ').append(item.from).append("->").append(item.to).append('@').append(item.progress).append(';');
        }
        return text.append(']').toString();
    }

    private static int count(GameTestHelper helper, BlockPos pos, Item item) {
        return ((Container) helper.getBlockEntity(pos, BlockEntity.class)).countItem(item);
    }

    private static void insert(GameTestHelper helper, BlockPos pos, Direction side, ItemStack stack) {
        Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), helper.absolutePos(pos), side);
        if (storage == null) {
            helper.fail("o tubo deveria aceitar itens pelo lado " + side);
            return;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            storage.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            transaction.commit();
        }
    }

    @GameTest(maxTicks = 200)
    public void woodenPipePullsIntoChest(GameTestHelper helper) {
        BlockPos source = new BlockPos(0, 1, 1);
        BlockPos target = new BlockPos(3, 1, 1);
        helper.setBlock(source, Blocks.CHEST);
        helper.setBlock(target, Blocks.CHEST);
        ((Container) helper.getBlockEntity(source, BlockEntity.class)).setItem(0, new ItemStack(Items.DIAMOND, 3));
        pipe(helper, new BlockPos(1, 1, 1), PipeType.WOOD);
        pipe(helper, new BlockPos(2, 1, 1), PipeType.COBBLESTONE);
        helper.assertBlockProperty(new BlockPos(1, 1, 1), DirectionalPipeBlock.SPECIAL, Direction.WEST);
        PipeBlockEntity wood = (PipeBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1), PipeBlockEntity.class);
        helper.onEachTick(() -> wood.energy().receivePower(4_000, 220));
        helper.succeedWhen(() -> {
            if (count(helper, target, Items.DIAMOND) != 3) {
                PipeBlockEntity cobble = (PipeBlockEntity) helper.getBlockEntity(new BlockPos(2, 1, 1), PipeBlockEntity.class);
                helper.fail("os 3 diamantes deveriam chegar no baú: " + count(helper, target, Items.DIAMOND)
                        + " | origem " + count(helper, source, Items.DIAMOND) + " | energia " + wood.energy().stored()
                        + " | madeira " + describe(wood) + " | pedregulho " + describe(cobble)
                        + " | estado " + helper.getBlockState(new BlockPos(1, 1, 1)) + " / " + helper.getBlockState(new BlockPos(2, 1, 1)));
            }
        });
    }

    @GameTest(maxTicks = 100)
    public void machineOutputEntersWoodenPipe(GameTestHelper helper) {
        BlockPos wellPos = new BlockPos(0, 2, 1);
        helper.setBlock(wellPos, BCBlocks.MINING_WELL.get());
        pipe(helper, new BlockPos(1, 2, 1), PipeType.WOOD);
        helper.setBlock(new BlockPos(2, 2, 1), Blocks.CHEST);
        net.buildcraftreborn.factory.FactoryUtil.output(helper.getLevel(), helper.absolutePos(wellPos), new ItemStack(Items.COBBLESTONE, 3));
        helper.succeedWhen(() -> {
            if (count(helper, new BlockPos(2, 2, 1), Items.COBBLESTONE) != 3) helper.fail("o pedregulho do poço deveria passar pelo tubo de madeira até o baú");
            helper.assertItemEntityNotPresent(Items.COBBLESTONE);
        });
    }

    @GameTest(maxTicks = 100)
    public void ironPipeUsesOnlyItsOutput(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.north(), Blocks.CHEST);
        helper.setBlock(pos.east(), Blocks.CHEST);
        pipe(helper, pos, PipeType.IRON);
        helper.setBlock(pos, helper.getBlockState(pos).setValue(DirectionalPipeBlock.SPECIAL, Direction.EAST));
        for (int i = 0; i < 4; i++) insert(helper, pos, Direction.WEST, new ItemStack(Items.DIAMOND));
        helper.succeedWhen(() -> {
            if (count(helper, pos.east(), Items.DIAMOND) != 4) helper.fail("tudo deveria sair pelo leste");
            if (count(helper, pos.north(), Items.DIAMOND) != 0) helper.fail("nada deveria sair pelo norte");
        });
    }

    @GameTest(maxTicks = 100)
    public void diamondPipeFiltersBySide(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos.north(), Blocks.CHEST);
        helper.setBlock(pos.south(), Blocks.CHEST);
        pipe(helper, pos, PipeType.DIAMOND);
        PipeBlockEntity diamond = (PipeBlockEntity) helper.getBlockEntity(pos, PipeBlockEntity.class);
        diamond.filters().setItem(Direction.NORTH.ordinal() * PipeBlockEntity.FILTER_WIDTH, new ItemStack(Items.DIRT));
        insert(helper, pos, Direction.WEST, new ItemStack(Items.DIRT, 2));
        insert(helper, pos, Direction.WEST, new ItemStack(Items.COBBLESTONE, 2));
        helper.succeedWhen(() -> {
            if (count(helper, pos.north(), Items.DIRT) != 2) {
                helper.fail("a terra deveria ir para o norte (filtro): norte " + count(helper, pos.north(), Items.DIRT)
                        + " sul " + count(helper, pos.south(), Items.DIRT) + "/" + count(helper, pos.south(), Items.COBBLESTONE)
                        + " | tubo " + describe(diamond) + " | estado " + helper.getBlockState(pos));
            }
            if (count(helper, pos.south(), Items.COBBLESTONE) != 2) helper.fail("o pedregulho deveria ir para o lado sem filtro");
        });
    }

    @GameTest(maxTicks = 60)
    public void voidPipeDestroysItems(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        pipe(helper, pos, PipeType.VOID);
        insert(helper, pos, Direction.WEST, new ItemStack(Items.DIRT, 5));
        PipeBlockEntity pipe = (PipeBlockEntity) helper.getBlockEntity(pos, PipeBlockEntity.class);
        helper.succeedWhen(() -> {
            if (!pipe.items().isEmpty()) helper.fail("os itens ainda estão no tubo");
            helper.assertItemEntityNotPresent(Items.DIRT);
        });
    }

    @GameTest(maxTicks = 20)
    public void pipeConnectionRules(GameTestHelper helper) {
        pipe(helper, new BlockPos(0, 1, 0), PipeType.COBBLESTONE);
        pipe(helper, new BlockPos(1, 1, 0), PipeType.STONE);
        pipe(helper, new BlockPos(2, 1, 0), PipeType.GOLD);
        helper.assertBlockProperty(new BlockPos(0, 1, 0), BlockStateProperties.EAST, false);
        helper.assertBlockProperty(new BlockPos(1, 1, 0), BlockStateProperties.EAST, true);

        helper.setBlock(new BlockPos(0, 1, 2), Blocks.CHEST);
        pipe(helper, new BlockPos(1, 1, 2), PipeType.SANDSTONE);
        pipe(helper, new BlockPos(2, 1, 2), PipeType.WOOD);
        pipe(helper, new BlockPos(3, 1, 2), PipeType.WOOD);
        helper.assertBlockProperty(new BlockPos(1, 1, 2), BlockStateProperties.WEST, false);
        helper.assertBlockProperty(new BlockPos(2, 1, 2), BlockStateProperties.EAST, false);
        helper.succeed();
    }
}
