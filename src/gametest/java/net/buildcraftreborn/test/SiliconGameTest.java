package net.buildcraftreborn.test;

import net.buildcraftreborn.lib.block.BCDirectionalBlock;
import net.buildcraftreborn.registry.BCBlocks;
import net.buildcraftreborn.registry.BCItems;
import net.buildcraftreborn.silicon.tile.AdvancedCraftingTableBlockEntity;
import net.buildcraftreborn.silicon.tile.AssemblyTableBlockEntity;
import net.buildcraftreborn.silicon.tile.LaserBlockEntity;
import net.craftenergy.api.EnergyUnits;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Sessão 10: laser, mesa de montagem e mesa de trabalho avançada. */
public class SiliconGameTest {
    private static AssemblyTableBlockEntity assembly(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, BCBlocks.ASSEMBLY_TABLE.get());
        return (AssemblyTableBlockEntity) helper.getBlockEntity(pos, AssemblyTableBlockEntity.class);
    }

    @GameTest(maxTicks = 40)
    public void assemblyTableMakesSavedChipset(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        AssemblyTableBlockEntity table = assembly(helper, pos);
        helper.setBlock(pos.above(), Blocks.CHEST);
        table.inventory().setItem(0, new ItemStack(Items.REDSTONE, 2));
        if (table.requiredLaserPower() != 0) helper.fail("sem receita marcada a mesa não pede energia");
        table.toggle(0);
        if (table.requiredLaserPower() != EnergyUnits.fromCWh(10_000)) helper.fail("chipset de redstone custa 10.000 CWh: " + table.requiredLaserPower());
        table.receiveLaserPower(EnergyUnits.fromCWh(10_000));
        helper.succeedWhen(() -> {
            Container chest = (Container) helper.getBlockEntity(pos.above(), net.minecraft.world.level.block.entity.BlockEntity.class);
            if (chest.countItem(BCItems.REDSTONE_CHIPSET.get()) != 1) helper.fail("o chipset deveria ir para o baú em cima");
            if (table.inventory().countItem(Items.REDSTONE) != 1) helper.fail("deveria gastar uma redstone");
        });
    }

    @GameTest(maxTicks = 100)
    public void laserPowersTableInFront(GameTestHelper helper) {
        BlockPos laserPos = new BlockPos(0, 1, 1);
        helper.setBlock(laserPos, BCBlocks.LASER.get().defaultBlockState().setValue(BCDirectionalBlock.FACING, Direction.EAST));
        LaserBlockEntity laser = (LaserBlockEntity) helper.getBlockEntity(laserPos, LaserBlockEntity.class);
        helper.onEachTick(() -> laser.energy().receivePower(LaserBlockEntity.MAX_OUTPUT, 1_000));
        AssemblyTableBlockEntity table = assembly(helper, laserPos.east(3));
        table.inventory().setItem(0, new ItemStack(Items.REDSTONE));
        table.toggle(0);
        helper.succeedWhen(() -> {
            if (table.power() <= 0) helper.fail("o laser deveria mandar energia para a mesa: " + laser.target());
        });
    }

    @GameTest(maxTicks = 60)
    public void advancedCraftingTableCraftsBlueprint(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BCBlocks.ADVANCED_CRAFTING_TABLE.get());
        AdvancedCraftingTableBlockEntity table = (AdvancedCraftingTableBlockEntity) helper.getBlockEntity(pos, AdvancedCraftingTableBlockEntity.class);
        for (int slot : new int[]{0, 1, 3, 4}) table.blueprint().setItem(slot, new ItemStack(Items.OAK_PLANKS));
        table.inventory().setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
        helper.runAfterDelay(3, () -> {
            if (table.requiredLaserPower() != AdvancedCraftingTableBlockEntity.ENERGY_PER_CRAFT) {
                helper.fail("com molde e material deveria pedir 500 CWh: " + table.requiredLaserPower());
            }
            table.receiveLaserPower(AdvancedCraftingTableBlockEntity.ENERGY_PER_CRAFT);
        });
        helper.succeedWhen(() -> {
            if (table.inventory().getItem(AdvancedCraftingTableBlockEntity.MATERIALS).getItem() != Items.CRAFTING_TABLE) {
                helper.fail("deveria fabricar uma bancada nos resultados");
            }
        });
    }
}
