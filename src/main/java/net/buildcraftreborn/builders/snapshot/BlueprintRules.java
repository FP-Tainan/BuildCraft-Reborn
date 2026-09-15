package net.buildcraftreborn.builders.snapshot;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Regras de blocos da planta, refeitas para os estados do 26.2 (o BuildCraft usava arquivos com ids do 1.12):
 * o que não se copia, que propriedades não contam na comparação (acesa, aberta, idade, ligações...) e que itens
 * ou fluido cada bloco pede.
 */
public final class BlueprintRules {
    /** Propriedades que mudam sozinhas no mundo e não impedem de considerar o bloco pronto. */
    private static final Set<String> IGNORED = Set.of("powered", "power", "lit", "open", "triggered", "enabled", "extended", "age",
            "moisture", "shape", "waterlogged", "distance", "attached", "disarmed", "in_wall", "occupied", "unstable", "bottom", "stage",
            "locked", "short", "hatch", "can_summon", "shrieking", "sculk_sensor_phase", "tip", "thickness", "crafting", "instrument");
    private static final Set<String> CONNECTIONS = Set.of("north", "east", "south", "west", "up");
    private static final Set<Block> SKIPPED = Set.of(Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.MOVING_PISTON, Blocks.PISTON_HEAD,
            Blocks.BUBBLE_COLUMN, Blocks.NETHER_PORTAL, Blocks.END_PORTAL, Blocks.END_GATEWAY, Blocks.FROSTED_ICE);

    /** Itens e/ou fluido (em CL) para colocar o bloco; itens vazios = não pede nada (metade de cima da porta). */
    public record Requirement(List<ItemStack> items, @Nullable FluidVariant fluid, long fluidCL) {
        public static final Requirement NOTHING = new Requirement(List.of(), null, 0);
    }

    private BlueprintRules() {}

    /** Estado guardado na planta ao escanear: fogo, portais, fluido corrente e afins viram ar. */
    public static BlockState forScan(BlockState state) {
        if (state.isAir() || SKIPPED.contains(state.getBlock())) return Blocks.AIR.defaultBlockState();
        if (state.getBlock() instanceof LiquidBlock && !state.getFluidState().isSource()) return Blocks.AIR.defaultBlockState();
        return state;
    }

    /** O bloco no mundo já corresponde ao da planta? */
    public static boolean matches(BlockState wanted, BlockState current) {
        if (wanted.getBlock() != current.getBlock()) return false;
        boolean connecting = wanted.getBlock() instanceof CrossCollisionBlock || wanted.getBlock() instanceof WallBlock
                || wanted.getBlock() instanceof RedStoneWireBlock;
        for (Property<?> property : wanted.getProperties()) {
            String name = property.getName();
            if (IGNORED.contains(name) || (connecting && CONNECTIONS.contains(name))) continue;
            if (!current.hasProperty(property) || !wanted.getValue(property).equals(current.getValue(property))) return false;
        }
        return true;
    }

    /** O que o construtor precisa ter para colocar o estado; {@code null} se não dá para construir (sem item). */
    public static @Nullable Requirement required(BlockState state) {
        if (state.getBlock() instanceof LiquidBlock) {
            return state.getFluidState().isSource() ? new Requirement(List.of(), FluidVariant.of(state.getFluidState().getType()), 1_000) : null;
        }
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return Requirement.NOTHING;
        }
        if (state.hasProperty(BlockStateProperties.BED_PART) && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) {
            return Requirement.NOTHING;
        }
        Item item = state.getBlock().asItem();
        if (state.is(Blocks.REDSTONE_WIRE)) item = Items.REDSTONE;
        if (item == Items.AIR) return null;
        int count = 1;
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE) && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) count = 2;
        if (state.hasProperty(BlockStateProperties.LAYERS)) count = state.getValue(BlockStateProperties.LAYERS);
        if (state.hasProperty(BlockStateProperties.CANDLES)) count = state.getValue(BlockStateProperties.CANDLES);
        if (state.hasProperty(BlockStateProperties.PICKLES)) count = state.getValue(BlockStateProperties.PICKLES);
        return new Requirement(List.of(new ItemStack(item, count)), null, 0);
    }
}
