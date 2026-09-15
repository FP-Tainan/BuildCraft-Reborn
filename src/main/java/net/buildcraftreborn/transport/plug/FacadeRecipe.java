package net.buildcraftreborn.transport.plug;

import com.mojang.serialization.MapCodec;
import net.buildcraftreborn.registry.BCItems;
import net.buildcraftreborn.registry.BCRecipes;
import net.buildcraftreborn.transport.PipeType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Receita especial das fachadas: 3 tubos de estrutura e 1 bloco válido dão 6 fachadas sólidas; uma fachada
 * sozinha vira vazada (ou volta a ser sólida). No BuildCraft 8 era na mesa de montagem (64 MJ).
 */
public class FacadeRecipe extends CustomRecipe {
    public static final FacadeRecipe INSTANCE = new FacadeRecipe();
    public static final MapCodec<FacadeRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, FacadeRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static ItemStack result(CraftingInput input) {
        int pipes = 0;
        int facades = 0;
        int others = 0;
        BlockState block = null;
        ItemStack facade = ItemStack.EMPTY;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) continue;
            if (stack.is(BCItems.PIPES.get(PipeType.STRUCTURE).get())) {
                pipes++;
            } else if (stack.is(BCItems.FACADE.get())) {
                facades++;
                facade = stack;
            } else if (block == null && stack.getItem() instanceof BlockItem blockItem && FacadeItem.isValid(blockItem.getBlock().defaultBlockState())) {
                block = blockItem.getBlock().defaultBlockState();
            } else {
                others++;
            }
        }
        if (others > 0) return ItemStack.EMPTY;
        if (pipes == 3 && block != null && facades == 0) return FacadeItem.stack(block, false).copyWithCount(6);
        if (pipes == 0 && block == null && facades == 1) {
            FacadeItem.Data data = FacadeItem.data(facade);
            return FacadeItem.stack(data.state(), !data.hollow());
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !result(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return result(input);
    }

    @Override
    public RecipeSerializer<FacadeRecipe> getSerializer() {
        return BCRecipes.FACADE.get();
    }
}
