package net.buildcraftreborn.registry;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.transport.plug.FacadeRecipe;
import net.craftenergy.registry.DeferredRegister;
import net.craftenergy.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** Tipos de receita especiais do BuildCraft Reborn. */
public final class BCRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BuildCraftReborn.MODID);

    /** Fachadas: 3 tubos de estrutura + bloco; fachada sozinha alterna vazada. */
    public static final RegistryObject<RecipeSerializer<FacadeRecipe>> FACADE = RECIPE_SERIALIZERS.register("facade",
            () -> new RecipeSerializer<>(FacadeRecipe.MAP_CODEC, FacadeRecipe.STREAM_CODEC));

    private BCRecipes() {}
}
