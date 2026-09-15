package net.buildcraftreborn.test;

import net.buildcraftreborn.registry.BCBlocks;
import net.craftenergy.content.item.MultimeterItem;
import net.craftenergy.network.MultimeterReadingPayload;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/** O multímetro do Craft Energy lê as máquinas e motores do BuildCraft Reborn. */
public class MultimeterGameTest {
    @GameTest(maxTicks = 20)
    public void craftEnergyMultimeterReadsBuildcraftMachines(GameTestHelper helper) {
        BlockPos distiller = new BlockPos(1, 1, 1);
        BlockPos engine = new BlockPos(3, 1, 1);
        helper.setBlock(distiller, BCBlocks.DISTILLER.get());
        helper.setBlock(engine, BCBlocks.STIRLING_ENGINE.get());
        for (BlockPos pos : new BlockPos[]{distiller, engine}) {
            MultimeterReadingPayload reading = MultimeterItem.measure(helper.getLevel(), helper.absolutePos(pos));
            if (!reading.units().contains("MV")) helper.fail("o multímetro deveria ler " + helper.getBlockState(pos).getBlock() + ": " + reading.units());
        }
        helper.succeed();
    }
}
