package net.buildcraftreborn.test;

import net.buildcraftreborn.core.item.PaintbrushItem;
import net.buildcraftreborn.core.item.WrenchItem;
import net.buildcraftreborn.core.list.ListContents;
import net.buildcraftreborn.core.marker.MarkerBlockEntity;
import net.buildcraftreborn.registry.BCBlocks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Sessão 1: chave inglesa, pincel, lista, marcadores e fonte de água. */
public class CoreGameTest {
    private static net.minecraft.world.level.block.Block block(String name) {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(net.minecraft.resources.Identifier.withDefaultNamespace(name)).orElseThrow();
    }

    private static net.minecraft.world.item.Item item(String name) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.Identifier.withDefaultNamespace(name)).orElseThrow();
    }

    @GameTest(maxTicks = 20)
    public void wrenchRotatesVanillaBlocks(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH));
        if (!WrenchItem.rotate(helper.getLevel(), helper.absolutePos(pos), false)) helper.fail("a fornalha deveria girar");
        helper.assertBlockProperty(pos, FurnaceBlock.FACING, Direction.EAST);
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void paintbrushPaintsAndCleans(GameTestHelper helper) {
        BlockPos wool = new BlockPos(0, 1, 0);
        BlockPos glass = new BlockPos(2, 1, 0);
        helper.setBlock(wool, block("white_wool"));
        helper.setBlock(glass, Blocks.GLASS);
        PaintbrushItem.paint(helper.getLevel(), helper.absolutePos(wool), DyeColor.RED, false);
        PaintbrushItem.paint(helper.getLevel(), helper.absolutePos(glass), DyeColor.BLUE, false);
        helper.assertBlockPresent(block("red_wool"), wool);
        helper.assertBlockPresent(block("blue_stained_glass"), glass);
        PaintbrushItem.paint(helper.getLevel(), helper.absolutePos(glass), null, false);
        helper.assertBlockPresent(Blocks.GLASS, glass);
        if (PaintbrushItem.paint(helper.getLevel(), helper.absolutePos(wool), DyeColor.RED, true)) {
            helper.fail("pintar da mesma cor não deveria fazer nada");
        }
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void listMatchesByItemAndByType(GameTestHelper helper) {
        ListContents.Line exact = ListContents.Line.EMPTY.withStack(0, new ItemStack(item("white_wool")));
        if (!exact.matches(new ItemStack(item("white_wool"))) || exact.matches(new ItemStack(item("red_wool")))) {
            helper.fail("sem opções a lista deveria aceitar só o mesmo item");
        }
        ListContents.Line byType = exact.toggle(ListContents.OPTION_TYPE);
        if (!byType.matches(new ItemStack(item("red_wool"))) || byType.matches(new ItemStack(Items.STONE))) {
            helper.fail("\"mesmo tipo\" deveria aceitar lã de outra cor e recusar pedra: famílias "
                    + ListContents.Line.family(new ItemStack(item("red_wool"))) + " e "
                    + ListContents.Line.family(new ItemStack(Items.STONE)));
        }
        ListContents.Line planks = ListContents.Line.EMPTY.withStack(0, new ItemStack(item("oak_planks"))).toggle(ListContents.OPTION_TYPE);
        if (!planks.matches(new ItemStack(item("dark_oak_planks"))) || planks.matches(new ItemStack(item("oak_log")))) {
            helper.fail("\"mesmo tipo\" deveria juntar tábuas de qualquer madeira, mas não toras");
        }
        ListContents.Line iron = ListContents.Line.EMPTY.withStack(0, new ItemStack(Items.IRON_INGOT)).toggle(ListContents.OPTION_MATERIAL);
        if (iron.matches(new ItemStack(Items.GOLD_INGOT))) {
            helper.fail("\"mesmo material\" não deveria juntar ferro e ouro: " + ListContents.Line.materialTags(new ItemStack(Items.IRON_INGOT)));
        }
        ListContents contents = ListContents.EMPTY.withLine(1, byType);
        if (!contents.matches(new ItemStack(item("blue_wool")))) helper.fail("a segunda linha deveria valer para a lista toda");
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void volumeMarkersFormBox(GameTestHelper helper) {
        BlockPos origin = new BlockPos(0, 1, 0);
        helper.setBlock(origin, BCBlocks.MARKER_VOLUME.get());
        helper.setBlock(new BlockPos(3, 1, 0), BCBlocks.MARKER_VOLUME.get());
        helper.setBlock(new BlockPos(0, 3, 0), BCBlocks.MARKER_VOLUME.get());
        helper.setBlock(new BlockPos(0, 1, 3), BCBlocks.MARKER_VOLUME.get());
        MarkerBlockEntity marker = (MarkerBlockEntity) helper.getBlockEntity(origin, MarkerBlockEntity.class);
        if (marker.connectManually() != 3) helper.fail("o marcador deveria ligar nos três eixos: " + marker.connections());
        BoundingBox box = MarkerBlockEntity.volumeAt(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 0)));
        BoundingBox expected = BoundingBox.fromCorners(helper.absolutePos(origin), helper.absolutePos(new BlockPos(3, 3, 3)));
        if (!expected.equals(box)) helper.fail("caixa errada: " + box + ", esperada " + expected);

        helper.destroyBlock(new BlockPos(0, 3, 0));
        if (marker.connections().size() != 2) helper.fail("quebrar um marcador deveria desfazer a ligação: " + marker.connections());
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void pathMarkersFormChain(GameTestHelper helper) {
        BlockPos a = new BlockPos(0, 1, 0);
        BlockPos b = new BlockPos(2, 1, 0);
        BlockPos c = new BlockPos(4, 1, 1);
        helper.setBlock(a, BCBlocks.MARKER_PATH.get());
        helper.setBlock(b, BCBlocks.MARKER_PATH.get());
        helper.setBlock(c, BCBlocks.MARKER_PATH.get());
        MarkerBlockEntity middle = (MarkerBlockEntity) helper.getBlockEntity(b, MarkerBlockEntity.class);
        middle.connectManually();
        middle.connectManually();
        if (middle.connectManually() != 0) helper.fail("o marcador de caminho aceita no máximo duas ligações");
        var path = middle.path();
        if (path.size() != 3 || !path.get(1).equals(helper.absolutePos(b))) helper.fail("caminho fora de ordem: " + path);
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void springRefillsWater(GameTestHelper helper) {
        BlockPos spring = new BlockPos(1, 1, 1);
        helper.setBlock(spring, BCBlocks.WATER_SPRING.get());
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.WATER, spring.above()));
    }
}
