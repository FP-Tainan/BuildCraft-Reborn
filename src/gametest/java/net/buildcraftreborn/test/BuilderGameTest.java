package net.buildcraftreborn.test;

import net.buildcraftreborn.builders.block.BuilderBlock;
import net.buildcraftreborn.builders.item.SnapshotItem;
import net.buildcraftreborn.builders.snapshot.Snapshot;
import net.buildcraftreborn.builders.snapshot.SnapshotHeader;
import net.buildcraftreborn.builders.snapshot.SnapshotStore;
import net.buildcraftreborn.builders.tile.ArchitectTableBlockEntity;
import net.buildcraftreborn.builders.tile.BuilderBlockEntity;
import net.buildcraftreborn.registry.BCBlocks;
import net.buildcraftreborn.registry.BCItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Sessão 13 (parte A): moldes, plantas, mesa do arquiteto e construtor. */
public class BuilderGameTest {
    private static Snapshot twoBlockBlueprint() {
        Snapshot snapshot = new Snapshot(Snapshot.Type.BLUEPRINT, 2, 1, 1, Direction.NORTH, new BlockPos(0, 0, 1));
        snapshot.setState(snapshot.index(0, 0, 0), Blocks.STONE.defaultBlockState());
        snapshot.setState(snapshot.index(1, 0, 0), Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
        return snapshot;
    }

    private static BuilderBlockEntity builder(GameTestHelper helper, BlockPos pos, Direction facing, Snapshot snapshot) {
        helper.setBlock(pos, BCBlocks.BUILDER.get().defaultBlockState().setValue(BuilderBlock.FACING, facing));
        BuilderBlockEntity builder = (BuilderBlockEntity) helper.getBlockEntity(pos, BuilderBlockEntity.class);
        String hash = SnapshotStore.put(helper.getLevel(), snapshot);
        builder.snapshotSlot().setItem(0, SnapshotItem.used(new SnapshotHeader(hash, snapshot.type.id(), "teste", "")));
        helper.onEachTick(() -> builder.energy().receivePower(BuilderBlockEntity.MAX_INPUT, 1_000));
        return builder;
    }

    @GameTest(maxTicks = 20)
    public void snapshotSavesAndLoads(GameTestHelper helper) {
        Snapshot original = twoBlockBlueprint();
        Snapshot copy = Snapshot.load(original.save(), helper.getLevel().holderLookup(Registries.BLOCK));
        if (!copy.hash().equals(original.hash())) helper.fail("a planta recarregada deveria ter o mesmo hash");
        if (!copy.state(copy.index(1, 0, 0)).is(Blocks.OAK_STAIRS)) helper.fail("a escada deveria voltar da paleta");
        String hash = SnapshotStore.put(helper.getLevel(), original);
        Snapshot stored = SnapshotStore.get(helper.getLevel(), hash);
        if (stored == null || !stored.hash().equals(hash)) helper.fail("a planta deveria ser achada pelo hash");

        Snapshot template = new Snapshot(Snapshot.Type.TEMPLATE, 3, 2, 1, Direction.EAST, BlockPos.ZERO);
        template.setFilled(template.index(2, 1, 0), true);
        Snapshot templateCopy = Snapshot.load(template.save(), helper.getLevel().holderLookup(Registries.BLOCK));
        if (!templateCopy.filled(templateCopy.index(2, 1, 0)) || templateCopy.filled(0)) helper.fail("o molde deveria guardar só o canto cheio");
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void architectScansBlueprint(GameTestHelper helper) {
        BlockPos min = new BlockPos(1, 1, 3);
        BlockPos max = new BlockPos(2, 2, 3);
        helper.setBlock(min, Blocks.STONE);
        helper.setBlock(max, Blocks.OAK_PLANKS);
        helper.setBlock(new BlockPos(1, 1, 1), BCBlocks.ARCHITECT_TABLE.get());
        ArchitectTableBlockEntity table = (ArchitectTableBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1), ArchitectTableBlockEntity.class);
        table.setArea(helper.absolutePos(min), helper.absolutePos(max));
        ItemStack blank = new ItemStack(BCItems.BLUEPRINT.get());
        blank.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Casinha"));
        table.inventory().setItem(ArchitectTableBlockEntity.SLOT_IN, blank);
        helper.succeedWhen(() -> {
            SnapshotHeader header = SnapshotItem.header(table.inventory().getItem(ArchitectTableBlockEntity.SLOT_OUT));
            if (header == null) helper.fail("a planta ainda não saiu");
            if (!"Casinha".equals(header.name())) helper.fail("o nome deveria vir do item renomeado");
            Snapshot snapshot = SnapshotStore.get(helper.getLevel(), header.hash());
            if (snapshot == null || snapshot.volume() != 4) helper.fail("a planta deveria ter 2×2×1");
            if (!snapshot.state(snapshot.index(0, 0, 0)).is(Blocks.STONE) || !snapshot.state(snapshot.index(1, 1, 0)).is(Blocks.OAK_PLANKS)) {
                helper.fail("a planta deveria guardar a pedra e a tábua nas posições certas");
            }
            if (!table.inventory().getItem(ArchitectTableBlockEntity.SLOT_IN).isEmpty()) helper.fail("a planta em branco deveria ser gasta");
        });
    }

    @GameTest(maxTicks = 200)
    public void builderBuildsBlueprintBehind(GameTestHelper helper) {
        // virado para o norte: constrói atrás (sul) com o deslocamento da planta
        BuilderBlockEntity builder = builder(helper, new BlockPos(2, 1, 1), Direction.NORTH, twoBlockBlueprint());
        builder.resources().setItem(0, new ItemStack(Items.STONE));
        builder.resources().setItem(1, new ItemStack(Items.OAK_STAIRS));
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.DIRT);
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(2, 1, 3));
            BlockState stairs = helper.getBlockState(new BlockPos(3, 1, 3));
            if (!stairs.is(Blocks.OAK_STAIRS) || stairs.getValue(StairBlock.FACING) != Direction.NORTH) helper.fail("escada para o norte em (3,1,3)");
            if (builder.resources().countItem(Items.DIRT) != 1) helper.fail("a terra quebrada deveria ir para o inventário");
        });
    }

    @GameTest(maxTicks = 200)
    public void builderRotatesBlueprint(GameTestHelper helper) {
        // virado para o leste: a planta escaneada para o norte gira 90° no sentido horário
        BuilderBlockEntity builder = builder(helper, new BlockPos(3, 1, 1), Direction.EAST, twoBlockBlueprint());
        builder.resources().setItem(0, new ItemStack(Items.STONE));
        builder.resources().setItem(1, new ItemStack(Items.OAK_STAIRS));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(1, 1, 1));
            BlockState stairs = helper.getBlockState(new BlockPos(1, 1, 2));
            if (!stairs.is(Blocks.OAK_STAIRS) || stairs.getValue(StairBlock.FACING) != Direction.EAST) helper.fail("escada girada para o leste em (1,1,2)");
        });
    }

    @GameTest(maxTicks = 300)
    public void builderBuildsAtEveryPathPosition(GameTestHelper helper) {
        Snapshot pillar = new Snapshot(Snapshot.Type.TEMPLATE, 1, 1, 1, Direction.NORTH, BlockPos.ZERO);
        pillar.setFilled(0, true);
        BuilderBlockEntity builder = builder(helper, new BlockPos(0, 1, 0), Direction.NORTH, pillar);
        builder.setBases(java.util.List.of(helper.absolutePos(new BlockPos(2, 1, 2)), helper.absolutePos(new BlockPos(3, 1, 2))));
        builder.resources().setItem(0, new ItemStack(Items.COBBLESTONE, 4));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(2, 1, 2));
            helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(3, 1, 2));
        });
    }

    @GameTest(maxTicks = 40)
    public void replacerSwapsBlocksInBlueprint(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), BCBlocks.REPLACER.get());
        net.buildcraftreborn.builders.tile.ReplacerBlockEntity replacer = (net.buildcraftreborn.builders.tile.ReplacerBlockEntity)
                helper.getBlockEntity(new BlockPos(1, 1, 1), net.buildcraftreborn.builders.tile.ReplacerBlockEntity.class);
        Snapshot original = twoBlockBlueprint();
        String hash = SnapshotStore.put(helper.getLevel(), original);
        replacer.inventory().setItem(0, SnapshotItem.used(new SnapshotHeader(hash, "blueprint", "Casinha", "Alguém")));
        replacer.inventory().setItem(1, net.buildcraftreborn.builders.item.SchematicItem.withState(new ItemStack(BCItems.SCHEMATIC_SINGLE.get()),
                Blocks.OAK_STAIRS.defaultBlockState()));
        replacer.inventory().setItem(2, net.buildcraftreborn.builders.item.SchematicItem.withState(new ItemStack(BCItems.SCHEMATIC_SINGLE.get()),
                Blocks.STONE_STAIRS.defaultBlockState()));
        helper.succeedWhen(() -> {
            SnapshotHeader header = SnapshotItem.header(replacer.inventory().getItem(0));
            if (header == null || header.hash().equals(hash)) helper.fail("a planta deveria ganhar um hash novo");
            if (!"Casinha".equals(header.name())) helper.fail("o nome deveria continuar");
            Snapshot replaced = SnapshotStore.get(helper.getLevel(), header.hash());
            BlockState stairs = replaced == null ? null : replaced.state(replaced.index(1, 0, 0));
            if (stairs == null || !stairs.is(Blocks.STONE_STAIRS) || stairs.getValue(StairBlock.FACING) != Direction.NORTH) {
                helper.fail("a escada de carvalho deveria virar de pedra mantendo a direção");
            }
            if (!replaced.state(0).is(Blocks.STONE)) helper.fail("a pedra não deveria mudar");
            if (!replacer.inventory().getItem(1).isEmpty() || !replacer.inventory().getItem(2).isEmpty()) helper.fail("os esquemas deveriam ser gastos");
        });
    }

    @GameTest(maxTicks = 200)
    public void libraryUploadsAndDownloads(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), BCBlocks.LIBRARY.get());
        net.buildcraftreborn.builders.tile.LibraryBlockEntity library = (net.buildcraftreborn.builders.tile.LibraryBlockEntity)
                helper.getBlockEntity(new BlockPos(1, 1, 1), net.buildcraftreborn.builders.tile.LibraryBlockEntity.class);
        Snapshot snapshot = new Snapshot(Snapshot.Type.BLUEPRINT, 1, 1, 1, Direction.SOUTH, new BlockPos(7, 3, 5));
        snapshot.setState(0, Blocks.GOLD_BLOCK.defaultBlockState());
        String hash = SnapshotStore.put(helper.getLevel(), snapshot);
        library.inventory().setItem(net.buildcraftreborn.builders.tile.LibraryBlockEntity.SLOT_UP_IN,
                SnapshotItem.used(new SnapshotHeader(hash, "blueprint", "Ouro", "Teste")));
        library.inventory().setItem(net.buildcraftreborn.builders.tile.LibraryBlockEntity.SLOT_DOWN_IN, new ItemStack(BCItems.TEMPLATE.get()));
        helper.succeedWhen(() -> {
            if (net.buildcraftreborn.builders.snapshot.LibraryStore.entry(hash) == null) helper.fail("a planta ainda não chegou à biblioteca");
            if (library.inventory().getItem(net.buildcraftreborn.builders.tile.LibraryBlockEntity.SLOT_UP_OUT).isEmpty()) helper.fail("o item enviado deveria sair");
            SnapshotHeader downloaded = SnapshotItem.header(library.inventory().getItem(net.buildcraftreborn.builders.tile.LibraryBlockEntity.SLOT_DOWN_OUT));
            if (downloaded == null) helper.fail("ainda não baixou");
            if (!downloaded.hash().equals(hash) || !"Ouro".equals(downloaded.name()) || downloaded.snapshotType() != Snapshot.Type.BLUEPRINT) {
                helper.fail("o item baixado deveria ser a planta \"Ouro\" (o tipo segue a planta)");
            }
        });
    }

    @GameTest(maxTicks = 200)
    public void builderFollowsTemplateAndListsMissing(GameTestHelper helper) {
        Snapshot template = new Snapshot(Snapshot.Type.TEMPLATE, 1, 2, 1, Direction.NORTH, new BlockPos(0, 0, 1));
        template.setFilled(template.index(0, 0, 0), true);
        template.setFilled(template.index(0, 1, 0), true);
        BuilderBlockEntity builder = builder(helper, new BlockPos(1, 1, 1), Direction.NORTH, template);
        builder.resources().setItem(0, new ItemStack(Items.COBBLESTONE, 2));
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(1, 1, 3));
            helper.assertBlockPresent(Blocks.COBBLESTONE, new BlockPos(1, 2, 3));
        });
    }
}
