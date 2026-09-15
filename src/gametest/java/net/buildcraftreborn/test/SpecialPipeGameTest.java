package net.buildcraftreborn.test;

import net.buildcraftreborn.registry.BCBlocks;
import net.buildcraftreborn.transport.PipeType;
import net.buildcraftreborn.transport.block.ColoredPipeBlock;
import net.buildcraftreborn.transport.block.DirectionalPipeBlock;
import net.buildcraftreborn.transport.plug.PipePlugs;
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
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Sessão 14 (parte A): cores nos itens, tubos lápis, daizuli, madeira-diamante e emzuli, lentes e filtros. */
public class SpecialPipeGameTest {
    private static PipeBlockEntity pipe(GameTestHelper helper, BlockPos pos, PipeType type) {
        helper.setBlock(pos, BCBlocks.PIPES.get(type).get());
        return (PipeBlockEntity) helper.getBlockEntity(pos, PipeBlockEntity.class);
    }

    private static int count(GameTestHelper helper, BlockPos pos, Item item) {
        return ((Container) helper.getBlockEntity(pos, BlockEntity.class)).countItem(item);
    }

    private static Container chest(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Blocks.CHEST);
        return (Container) helper.getBlockEntity(pos, BlockEntity.class);
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
    public void lapisPaintsAndDaizuliRoutesByColour(GameTestHelper helper) {
        BlockPos lapis = new BlockPos(1, 1, 2);
        BlockPos daizuli = new BlockPos(2, 1, 2);
        chest(helper, daizuli.north());
        chest(helper, daizuli.east());
        pipe(helper, lapis, PipeType.LAPIS);
        helper.setBlock(lapis, helper.getBlockState(lapis).setValue(ColoredPipeBlock.COLOR, DyeColor.RED));
        pipe(helper, daizuli, PipeType.DAIZULI);
        helper.setBlock(daizuli, helper.getBlockState(daizuli).setValue(ColoredPipeBlock.COLOR, DyeColor.RED)
                .setValue(DirectionalPipeBlock.SPECIAL, Direction.NORTH));
        insert(helper, lapis, Direction.WEST, new ItemStack(Items.DIRT, 3));
        insert(helper, daizuli, Direction.WEST, new ItemStack(Items.COBBLESTONE, 2));
        helper.succeedWhen(() -> {
            if (count(helper, daizuli.north(), Items.DIRT) != 3) helper.fail("a terra pintada de vermelho deveria sair só pela face especial (norte)");
            if (count(helper, daizuli.east(), Items.COBBLESTONE) != 2) helper.fail("o pedregulho sem cor deveria evitar a face especial");
            if (count(helper, daizuli.east(), Items.DIRT) != 0 || count(helper, daizuli.north(), Items.COBBLESTONE) != 0) helper.fail("rota errada");
        });
    }

    @GameTest(maxTicks = 200)
    public void diamondWoodExtractsOnlyFiltered(GameTestHelper helper) {
        Container source = chest(helper, new BlockPos(0, 1, 1));
        chest(helper, new BlockPos(2, 1, 1));
        source.setItem(0, new ItemStack(Items.COBBLESTONE, 3));
        source.setItem(1, new ItemStack(Items.DIRT, 3));
        PipeBlockEntity pipe = pipe(helper, new BlockPos(1, 1, 1), PipeType.DIAMOND_WOOD);
        pipe.filters().setItem(0, new ItemStack(Items.DIRT));
        helper.onEachTick(() -> pipe.energy().receivePower(4_000, 220));
        helper.succeedWhen(() -> {
            if (count(helper, new BlockPos(2, 1, 1), Items.DIRT) != 3) helper.fail("a terra (na lista branca) deveria chegar");
            if (count(helper, new BlockPos(2, 1, 1), Items.COBBLESTONE) != 0) helper.fail("o pedregulho não está na lista");
        });
    }

    @GameTest(maxTicks = 200)
    public void emzuliExtractsActivePreset(GameTestHelper helper) {
        Container source = chest(helper, new BlockPos(0, 1, 1));
        chest(helper, new BlockPos(2, 1, 1));
        source.setItem(0, new ItemStack(Items.DIRT, 3));
        source.setItem(1, new ItemStack(Items.COBBLESTONE, 3));
        PipeBlockEntity pipe = pipe(helper, new BlockPos(1, 1, 1), PipeType.EMZULI);
        pipe.filters().setItem(0, new ItemStack(Items.COBBLESTONE));
        pipe.handlePresetButton(0);
        helper.onEachTick(() -> {
            pipe.energy().receivePower(4_000, 220);
            pipe.activatePreset(0);
        });
        helper.succeedWhen(() -> {
            if (pipe.presetColour(0) != DyeColor.WHITE) helper.fail("o botão deveria pintar de branco");
            if (count(helper, new BlockPos(2, 1, 1), Items.COBBLESTONE) != 3) helper.fail("o preset vermelho (pedregulho) deveria extrair");
            if (count(helper, new BlockPos(2, 1, 1), Items.DIRT) != 0) helper.fail("a terra não está em nenhum preset");
        });
    }

    @GameTest(maxTicks = 200)
    public void lensPaintsLeavingItems(GameTestHelper helper) {
        BlockPos plain = new BlockPos(1, 1, 3);
        BlockPos daizuli = new BlockPos(2, 1, 3);
        chest(helper, daizuli.north());
        chest(helper, daizuli.east());
        PipeBlockEntity first = pipe(helper, plain, PipeType.COBBLESTONE);
        first.plugs().attachLens(Direction.EAST, new PipePlugs.Lens(DyeColor.BLUE, false));
        pipe(helper, daizuli, PipeType.DAIZULI);
        helper.setBlock(daizuli, helper.getBlockState(daizuli).setValue(ColoredPipeBlock.COLOR, DyeColor.BLUE)
                .setValue(DirectionalPipeBlock.SPECIAL, Direction.NORTH));
        insert(helper, plain, Direction.WEST, new ItemStack(Items.DIRT, 2));
        helper.succeedWhen(() -> {
            if (count(helper, daizuli.north(), Items.DIRT) != 2) helper.fail("a lente azul deveria pintar e o daizuli azul mandar para o norte");
        });
    }

    @GameTest(maxTicks = 200)
    public void filterPrefersItsColourOverUnpainted(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        chest(helper, pos.east());
        chest(helper, pos.north());
        PipeBlockEntity pipe = pipe(helper, pos, PipeType.COBBLESTONE);
        pipe.plugs().attachLens(Direction.EAST, new PipePlugs.Lens(DyeColor.GREEN, true));
        insert(helper, pos, Direction.WEST, new ItemStack(Items.DIRT, 4));
        helper.succeedWhen(() -> {
            if (count(helper, pos.north(), Items.DIRT) != 4) helper.fail("itens sem cor deveriam preferir o lado sem filtro");
            if (count(helper, pos.east(), Items.DIRT) != 0) helper.fail("o filtro verde deixa item sem cor por último");
        });
    }
}
