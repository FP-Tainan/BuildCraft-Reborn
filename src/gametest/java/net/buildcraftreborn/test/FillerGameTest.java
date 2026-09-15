package net.buildcraftreborn.test;

import net.buildcraftreborn.builders.filler.FillerPattern;
import net.buildcraftreborn.builders.filler.FillerTemplates;
import net.buildcraftreborn.builders.tile.FillerBlockEntity;
import net.buildcraftreborn.registry.BCBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.BitSet;

/** Sessão 12: preenchedor e padrões. */
public class FillerGameTest {
    private static FillerBlockEntity filler(GameTestHelper helper, BlockPos pos, BlockPos min, BlockPos max) {
        helper.setBlock(pos, BCBlocks.FILLER.get());
        FillerBlockEntity filler = (FillerBlockEntity) helper.getBlockEntity(pos, FillerBlockEntity.class);
        filler.setArea(helper.absolutePos(min), helper.absolutePos(max));
        helper.onEachTick(() -> filler.energy().receivePower(FillerBlockEntity.MAX_INPUT, 1_000));
        return filler;
    }

    @GameTest(maxTicks = 200)
    public void fillerFillsBoxFromResources(GameTestHelper helper) {
        BlockPos min = new BlockPos(2, 1, 1);
        BlockPos max = new BlockPos(3, 2, 2);
        FillerBlockEntity filler = filler(helper, new BlockPos(0, 1, 0), min, max);
        filler.resources().setItem(0, new ItemStack(Items.COBBLESTONE, 16));
        filler.setPattern(FillerPattern.FILL);
        helper.succeedWhen(() -> {
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) helper.assertBlockPresent(Blocks.COBBLESTONE, pos);
            if (filler.resources().countItem(Items.COBBLESTONE) != 8) helper.fail("deveria gastar 8 pedregulhos");
        });
    }

    @GameTest(maxTicks = 200)
    public void fillerClearsBoxIntoInventory(GameTestHelper helper) {
        BlockPos min = new BlockPos(2, 1, 1);
        BlockPos max = new BlockPos(3, 1, 2);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) helper.setBlock(pos, Blocks.DIRT);
        FillerBlockEntity filler = filler(helper, new BlockPos(0, 1, 0), min, max);
        filler.setPattern(FillerPattern.CLEAR);
        helper.succeedWhen(() -> {
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) helper.assertBlockPresent(Blocks.AIR, pos);
            if (filler.resources().countItem(Items.DIRT) != 4) helper.fail("a terra quebrada deveria ir para o inventário");
        });
    }

    @GameTest(maxTicks = 20)
    public void patternShapes(GameTestHelper helper) {
        BitSet box = FillerTemplates.build(FillerPattern.BOX, new int[0], 3, 3, 3);
        if (box == null || box.get(FillerTemplates.index(1, 1, 1, 3, 3)) || !box.get(FillerTemplates.index(0, 0, 0, 3, 3))) {
            helper.fail("caixa 3×3×3 deveria ter o centro vazio e as quinas cheias");
        }
        BitSet sphere = FillerTemplates.build(FillerPattern.SPHERE, FillerPattern.SPHERE.defaults, 5, 5, 5);
        if (sphere == null || !sphere.get(FillerTemplates.index(2, 2, 2, 5, 5)) || sphere.get(FillerTemplates.index(0, 0, 0, 5, 5))) {
            helper.fail("esfera cheia deveria ter o centro e não as quinas");
        }
        BitSet pyramid = FillerTemplates.build(FillerPattern.PYRAMID, FillerPattern.PYRAMID.defaults, 5, 3, 5);
        if (pyramid == null || !pyramid.get(FillerTemplates.index(2, 2, 2, 5, 5)) || pyramid.get(FillerTemplates.index(0, 2, 0, 5, 5))) {
            helper.fail("pirâmide deveria ter a ponta no meio do topo");
        }
        if (FillerTemplates.build(FillerPattern.NONE, new int[0], 2, 2, 2) != null) helper.fail("\"nenhum\" não mexe em nada");
        helper.succeed();
    }
}
