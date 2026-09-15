package net.buildcraftreborn.test;

import net.buildcraftreborn.builders.tile.QuarryBlockEntity;
import net.buildcraftreborn.core.marker.MarkerBlockEntity;
import net.buildcraftreborn.registry.BCBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Sessão 4: a pedreira monta a armação na caixa dos marcadores e minera por dentro. */
public class QuarryGameTest {
    @GameTest(maxTicks = 200)
    public void quarryBuildsFrameAndMines(GameTestHelper helper) {
        // caixa de marcadores: x 1..4, z 1..4 (a armação sobe até 4 de altura)
        BlockPos corner = new BlockPos(1, 2, 1);
        helper.setBlock(corner, BCBlocks.MARKER_VOLUME.get());
        helper.setBlock(new BlockPos(4, 2, 1), BCBlocks.MARKER_VOLUME.get());
        helper.setBlock(new BlockPos(1, 2, 4), BCBlocks.MARKER_VOLUME.get());
        ((MarkerBlockEntity) helper.getBlockEntity(corner, MarkerBlockEntity.class)).connectManually();

        BlockPos stone = new BlockPos(2, 1, 2);
        helper.setBlock(stone, Blocks.STONE);
        BlockPos quarryPos = new BlockPos(0, 2, 1);
        helper.setBlock(quarryPos.south(), Blocks.CHEST);
        helper.setBlock(quarryPos, BCBlocks.QUARRY.get());
        QuarryBlockEntity quarry = (QuarryBlockEntity) helper.getBlockEntity(quarryPos, QuarryBlockEntity.class);
        helper.onEachTick(() -> quarry.energy().receivePower(QuarryBlockEntity.MAX_INPUT, 220));

        helper.succeedWhen(() -> {
            helper.assertBlockPresent(BCBlocks.FRAME.get(), corner);
            helper.assertBlockPresent(BCBlocks.FRAME.get(), new BlockPos(4, 5, 4));
            helper.assertBlockNotPresent(Blocks.STONE, stone);
            Container chest = (Container) helper.getBlockEntity(quarryPos.south(), BlockEntity.class);
            if (chest.countItem(Items.COBBLESTONE) < 1) helper.fail("o pedregulho deveria ir para o baú");
            if (chest.countItem(BCBlocks.MARKER_VOLUME.get().asItem()) > 0) helper.fail("os marcadores caem no chão, não no baú");
        });
    }
}
