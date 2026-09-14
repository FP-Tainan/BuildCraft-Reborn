package net.buildcraftreborn.test;

import net.buildcraftreborn.BCConfig;
import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.lib.fluid.BCTank;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluids;

/** Base do mod: aba criativa, configuração e tanque em CL. */
public class FoundationGameTest {
    @GameTest(maxTicks = 20)
    public void creativeTabIsRegistered(GameTestHelper helper) {
        if (!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(BuildCraftReborn.id(BuildCraftReborn.MODID))) {
            helper.fail("a aba criativa do BuildCraft Reborn deveria existir");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void configDefaultsFollowBuildCraft(GameTestHelper helper) {
        BCConfig config = BCConfig.defaults();
        if (config.pumpMaxDistance != 64 || config.markerMaxDistance != 64 || config.quarryMaxTasksPerTick != 4) {
            helper.fail("padrões da configuração diferentes do BuildCraft");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void tankCountsInCraftLiters(GameTestHelper helper) {
        int[] changes = {0};
        BCTank tank = new BCTank(4_000, () -> changes[0]++);
        long inserted;
        try (Transaction transaction = Transaction.openOuter()) {
            inserted = tank.insert(FluidVariant.of(Fluids.WATER), FluidConstants.BUCKET * 10, transaction);
            transaction.commit();
        }
        if (inserted != FluidConstants.BUCKET * 4) helper.fail("tanque de 4.000 CL deveria aceitar 4 baldes, aceitou " + inserted);
        if (tank.amountCL() != 4_000 || tank.ratio() != 1.0) helper.fail("tanque deveria marcar 4.000 CL cheio: " + tank.amountCL());
        if (changes[0] != 1) helper.fail("a mudança deveria ser avisada uma vez: " + changes[0]);
        helper.succeed();
    }
}
