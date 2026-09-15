package net.buildcraftreborn.test;

import net.buildcraftreborn.registry.BCBlocks;
import net.buildcraftreborn.registry.BCItems;
import net.buildcraftreborn.transport.PipeType;
import net.buildcraftreborn.transport.block.PipeBlock;
import net.buildcraftreborn.transport.plug.FacadeItem;
import net.buildcraftreborn.transport.plug.FacadeRecipe;
import net.buildcraftreborn.transport.plug.PipePlugs;
import net.buildcraftreborn.transport.tile.PipeBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.List;

/** Sessão 14 (parte C): fachadas. */
public class FacadeGameTest {
    @GameTest(maxTicks = 20)
    public void solidFacadeBlocksAndHollowDoesNot(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 1, 1);
        helper.setBlock(a, BCBlocks.PIPES.get(PipeType.COBBLESTONE).get());
        helper.setBlock(a.east(), BCBlocks.PIPES.get(PipeType.COBBLESTONE).get());
        PipeBlockEntity pipe = (PipeBlockEntity) helper.getBlockEntity(a, PipeBlockEntity.class);
        pipe.plugs().attachFacade(Direction.EAST, new PipePlugs.Facade(Blocks.STONE.defaultBlockState(), true));
        PipeBlock.refreshConnections(helper.getLevel(), pipe.getBlockPos());
        helper.assertBlockProperty(a, BlockStateProperties.EAST, true);
        ItemStack removed = pipe.plugs().remove(Direction.EAST);
        if (!FacadeItem.data(removed).hollow()) helper.fail("a fachada tirada deveria voltar vazada");
        pipe.plugs().attachFacade(Direction.EAST, new PipePlugs.Facade(Blocks.STONE.defaultBlockState(), false));
        PipeBlock.refreshConnections(helper.getLevel(), pipe.getBlockPos());
        helper.assertBlockProperty(a, BlockStateProperties.EAST, false);
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void facadeRecipeAndValidBlocks(GameTestHelper helper) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) grid.add(ItemStack.EMPTY);
        ItemStack structure = new ItemStack(BCItems.PIPES.get(PipeType.STRUCTURE).get());
        grid.set(0, structure.copy());
        grid.set(1, structure.copy());
        grid.set(2, structure.copy());
        grid.set(4, new ItemStack(Items.STONE_BRICKS));
        ItemStack result = FacadeRecipe.result(CraftingInput.of(3, 3, grid));
        if (!result.is(BCItems.FACADE.get()) || result.getCount() != 6 || !FacadeItem.data(result).state().is(Blocks.STONE_BRICKS)) {
            helper.fail("3 tubos de estrutura + tijolos de pedra deveriam dar 6 fachadas: " + result);
        }
        List<ItemStack> single = new ArrayList<>();
        for (int i = 0; i < 9; i++) single.add(i == 4 ? result.copyWithCount(1) : ItemStack.EMPTY);
        if (!FacadeItem.data(FacadeRecipe.result(CraftingInput.of(3, 3, single))).hollow()) helper.fail("fachada sozinha deveria virar vazada");
        if (!FacadeItem.isValid(Blocks.GLASS.defaultBlockState())) helper.fail("vidro vale como fachada");
        if (FacadeItem.isValid(Blocks.CHEST.defaultBlockState()) || FacadeItem.isValid(Blocks.OAK_STAIRS.defaultBlockState())) {
            helper.fail("baú e escada não valem como fachada");
        }
        grid.set(4, new ItemStack(Items.CHEST));
        if (!FacadeRecipe.result(CraftingInput.of(3, 3, grid)).isEmpty()) helper.fail("baú não faz fachada");
        helper.succeed();
    }
}
