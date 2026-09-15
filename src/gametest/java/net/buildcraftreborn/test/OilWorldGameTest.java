package net.buildcraftreborn.test;

import net.buildcraftreborn.energy.fluid.BCFluids;
import net.buildcraftreborn.energy.world.OilWellFeature;
import net.buildcraftreborn.registry.BCBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;

/** Sessão 8: petróleo no mundo. */
public class OilWorldGameTest {
    private static boolean isOil(FluidState state) {
        return state.isSource() && state.getType().isSame(BCFluids.get(BCFluids.Kind.OIL).fluid());
    }

    @GameTest(maxTicks = 40)
    public void oilSpringRefillsOilAbove(GameTestHelper helper) {
        BlockPos spring = new BlockPos(1, 1, 1);
        helper.setBlock(spring, BCBlocks.OIL_SPRING.get());
        helper.succeedWhen(() -> {
            if (!isOil(helper.getBlockState(spring.above()).getFluidState())) helper.fail("a fonte deveria encher petróleo acima");
        });
    }

    @GameTest(maxTicks = 20)
    public void sphereFillsOnlyInsideRadiusAndBox(GameTestHelper helper) {
        BlockPos center = helper.absolutePos(new BlockPos(2, 2, 2));
        BoundingBox box = new BoundingBox(center.getX() - 1, center.getY() - 1, center.getZ() - 1,
                center.getX() + 1, center.getY() + 1, center.getZ() + 1);
        OilWellFeature.fillSphere(helper.getLevel(), box, center, 1);
        if (!isOil(helper.getLevel().getFluidState(center))) helper.fail("o centro da esfera deveria ter petróleo");
        if (!isOil(helper.getLevel().getFluidState(center.east()))) helper.fail("raio 1 deveria alcançar o vizinho");
        if (isOil(helper.getLevel().getFluidState(center.offset(1, 1, 1)))) helper.fail("a quina fica fora do raio");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void mediumWellHasSphereTubeSpoutAndPool(GameTestHelper helper) {
        BlockPos base = helper.absolutePos(new BlockPos(3, 1, 3));
        BoundingBox box = new BoundingBox(base.getX() - 3, base.getY(), base.getZ() - 3, base.getX() + 3, base.getY() + 9, base.getZ() + 3);
        int surface = base.getY() + 4;
        // esfera em base+1 (raio 1), tubo até o chão em base+4, jorro de 3 blocos e poça de raio 1 em volta
        OilWellFeature.Plan plan = new OilWellFeature.Plan(OilWellFeature.Type.MEDIUM, base.getX(), base.getZ(), 1, 2,
                base.getY() + 1, 1, 3, 0, 42L);
        OilWellFeature.generate(helper.getLevel(), plan, box, (x, z, type) -> surface);
        if (!isOil(helper.getLevel().getFluidState(new BlockPos(base.getX(), base.getY() + 1, base.getZ())))) helper.fail("esfera sem petróleo");
        if (!isOil(helper.getLevel().getFluidState(new BlockPos(base.getX(), surface - 2, base.getZ())))) helper.fail("tubo sem petróleo");
        if (!isOil(helper.getLevel().getFluidState(new BlockPos(base.getX(), surface + 2, base.getZ())))) helper.fail("jorro sem petróleo");
        if (!isOil(helper.getLevel().getFluidState(new BlockPos(base.getX() + 1, surface - 1, base.getZ())))) helper.fail("poça sem petróleo");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void wellPlanIsDeterministic(GameTestHelper helper) {
        OilWellFeature.Plan first = OilWellFeature.plan(1234L, 5, -7, 10_000.0, 1.0, true, () -> OilWellFeature.Ground.NORMAL);
        OilWellFeature.Plan second = OilWellFeature.plan(1234L, 5, -7, 10_000.0, 1.0, true, () -> OilWellFeature.Ground.NORMAL);
        if (first == null || !first.equals(second)) helper.fail("o mesmo chunk deveria sortear o mesmo poço em qualquer vizinho");
        if (first != null && first.type() != OilWellFeature.Type.LARGE) helper.fail("com chance total deveria ser poço grande");
        if (OilWellFeature.plan(1234L, 5, -7, 0.0, 1.0, true, () -> OilWellFeature.Ground.OIL_FIELD) != null) {
            helper.fail("com taxa zero não deveria gerar nada");
        }
        OilWellFeature.Plan noSpout = OilWellFeature.plan(99L, 1, 1, 10_000.0, 1.0, false, () -> OilWellFeature.Ground.NORMAL);
        if (noSpout == null || noSpout.spoutHeight() != 0) helper.fail("sem jorros configurados a altura deveria ser zero");
        helper.succeed();
    }
}
