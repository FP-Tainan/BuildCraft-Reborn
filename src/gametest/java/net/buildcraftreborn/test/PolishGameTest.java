package net.buildcraftreborn.test;

import net.buildcraftreborn.BuildCraftReborn;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;

/** Sessão 15: conquistas e receitas novas carregam no servidor. */
public class PolishGameTest {
    private static final String[] ADVANCEMENTS = {"root", "wrenched", "gears", "engine", "pipe_dream", "pipe_logic", "logic_transportation",
            "fluid_storage", "laser_power", "precision_crafting", "shaping_the_world", "architect", "guide", "black_gold"};
    private static final String[] RECIPES = {"guide", "facade", "architect_table", "pipe_items_stripes", "filtered_buffer", "library"};

    @GameTest(maxTicks = 20)
    public void advancementsAndRecipesLoad(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        for (String id : ADVANCEMENTS) {
            if (server.getAdvancements().get(BuildCraftReborn.id(id)) == null) helper.fail("conquista não carregou: " + id);
        }
        for (String id : RECIPES) {
            if (server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, BuildCraftReborn.id(id))).isEmpty()) {
                helper.fail("receita não carregou: " + id);
            }
        }
        helper.succeed();
    }
}
