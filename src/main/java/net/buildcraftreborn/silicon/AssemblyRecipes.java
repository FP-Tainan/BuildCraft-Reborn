package net.buildcraftreborn.silicon;

import net.buildcraftreborn.registry.BCItems;
import net.buildcraftreborn.transport.gate.GateItem;
import net.buildcraftreborn.transport.gate.GateVariant;
import net.craftenergy.api.EnergyUnits;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Receitas da mesa de montagem ({@code assembly_recipes}): ingredientes, energia de laser e resultado.
 * Energia em CWh igual aos MJ do BuildCraft. A lista é montada no primeiro uso: {@link ItemStack} só
 * pode ser criado depois que os componentes dos itens estão prontos.
 */
public final class AssemblyRecipes {
    /** Ingrediente com quantidade; {@code extra} confere componentes (por exemplo, a variante da porta). */
    public record Input(Ingredient ingredient, int count, Predicate<ItemStack> extra) {
        public Input(Ingredient ingredient, int count) {
            this(ingredient, count, stack -> true);
        }

        public boolean test(ItemStack stack) {
            return this.ingredient.test(stack) && this.extra.test(stack);
        }
    }

    public record Recipe(String id, long energy, List<Input> inputs, ItemStack result) {
        public ItemStack output() {
            return this.result;
        }
    }

    private static final List<Recipe> RECIPES = new ArrayList<>();

    private AssemblyRecipes() {}

    private static void build() {
        if (!RECIPES.isEmpty()) return;
        chipset("redstone_chipset", 10_000, BCItems.REDSTONE_CHIPSET.get());
        chipset("iron_chipset", 20_000, BCItems.IRON_CHIPSET.get(), Items.IRON_INGOT);
        chipset("gold_chipset", 40_000, BCItems.GOLD_CHIPSET.get(), Items.GOLD_INGOT);
        chipset("quartz_chipset", 60_000, BCItems.QUARTZ_CHIPSET.get(), Items.QUARTZ);
        chipset("diamond_chipset", 80_000, BCItems.DIAMOND_CHIPSET.get(), Items.DIAMOND);
        gates();
        plugs();
    }

    /** Plugues, copiador e fios (8 por receita). */
    private static void plugs() {
        simple("plug_pulsar", 1_000, new ItemStack(BCItems.PLUG_PULSAR.get()), BCItems.REDSTONE_ENGINE.get(), Items.IRON_INGOT, Items.IRON_INGOT);
        simple("plug_light_sensor", 500, new ItemStack(BCItems.PLUG_LIGHT_SENSOR.get()), Items.DAYLIGHT_DETECTOR);
        simple("plug_timer", 500, new ItemStack(BCItems.PLUG_TIMER.get()), Items.CLOCK);
        simple("gate_copier", 500, new ItemStack(BCItems.GATE_COPIER.get()), BCItems.WRENCH.get(), Items.STICK, Items.IRON_INGOT,
                BCItems.IRON_CHIPSET.get(), Items.REDSTONE, Items.GOLD_INGOT);
        // lentes e filtros (BuildCraft: 500 MJ; vidro colorido, com barras de ferro para o filtro)
        simple("lens_clear", 500, net.buildcraftreborn.transport.plug.LensItem.stack(null, false), Items.GLASS);
        simple("filter_clear", 500, net.buildcraftreborn.transport.plug.LensItem.stack(null, true), Items.GLASS, Items.IRON_BARS);
        for (net.minecraft.world.item.DyeColor color : net.minecraft.world.item.DyeColor.values()) {
            ItemLike glass = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                    net.minecraft.resources.Identifier.withDefaultNamespace(color.getName() + "_stained_glass"));
            simple("lens_" + color.getName(), 500, net.buildcraftreborn.transport.plug.LensItem.stack(color, false), glass);
            simple("filter_" + color.getName(), 500, net.buildcraftreborn.transport.plug.LensItem.stack(color, true), glass, Items.IRON_BARS);
        }
        for (net.minecraft.world.item.DyeColor color : net.minecraft.world.item.DyeColor.values()) {
            simple("wire_" + color.getName(), 10_000, new ItemStack(BCItems.WIRES.get(color).get(), 8), Items.REDSTONE,
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                            net.minecraft.resources.Identifier.withDefaultNamespace(color.getName() + "_dye")));
        }
    }

    /** Receita com um de cada ingrediente (repetir o item soma a quantidade). */
    private static void simple(String id, long energyCWh, ItemStack result, ItemLike... items) {
        java.util.LinkedHashMap<ItemLike, Integer> counts = new java.util.LinkedHashMap<>();
        for (ItemLike item : items) counts.merge(item, 1, Integer::sum);
        List<Input> inputs = new ArrayList<>();
        counts.forEach((item, count) -> inputs.add(new Input(Ingredient.of(item), count)));
        RECIPES.add(new Recipe(id, EnergyUnits.fromCWh(energyCWh), List.copyOf(inputs), result));
    }

    private static void chipset(String id, long energyCWh, ItemLike result, ItemLike... extra) {
        List<Input> inputs = new ArrayList<>();
        inputs.add(new Input(Ingredient.of(Items.REDSTONE), 1));
        for (ItemLike item : extra) inputs.add(new Input(Ingredient.of(item), 1));
        RECIPES.add(new Recipe(id, EnergyUnits.fromCWh(energyCWh), List.copyOf(inputs), new ItemStack(result)));
    }

    /** Portas lógicas (BuildCraft 8): chipsets viram portas; porta + lápis/chipset recebe modificador. */
    private static void gates() {
        for (GateVariant.Logic logic : GateVariant.Logic.values()) {
            gate(logic, GateVariant.Material.IRON, 20_000, new Input(Ingredient.of(BCItems.IRON_CHIPSET.get()), 1));
            gate(logic, GateVariant.Material.NETHER_BRICK, 40_000, new Input(Ingredient.of(BCItems.IRON_CHIPSET.get()), 1),
                    new Input(Ingredient.of(Items.NETHER_BRICKS), 1));
            gate(logic, GateVariant.Material.GOLD, 80_000, new Input(Ingredient.of(BCItems.GOLD_CHIPSET.get()), 1));
        }
        long[][] modifierEnergy = {{40_000, 60_000, 80_000}, {80_000, 100_000, 120_000}, {100_000, 140_000, 180_000}};
        GateVariant.Material[] materials = {GateVariant.Material.IRON, GateVariant.Material.NETHER_BRICK, GateVariant.Material.GOLD};
        ItemLike[] modifierItems = {Items.LAPIS_LAZULI, BCItems.QUARTZ_CHIPSET.get(), BCItems.DIAMOND_CHIPSET.get()};
        GateVariant.Modifier[] modifiers = {GateVariant.Modifier.LAPIS, GateVariant.Modifier.QUARTZ, GateVariant.Modifier.DIAMOND};
        for (int m = 0; m < materials.length; m++) {
            for (GateVariant.Logic logic : GateVariant.Logic.values()) {
                GateVariant plain = new GateVariant(logic, materials[m], GateVariant.Modifier.NONE);
                for (int k = 0; k < modifiers.length; k++) {
                    GateVariant result = new GateVariant(logic, materials[m], modifiers[k]);
                    RECIPES.add(new Recipe("gate_" + result.pack(), EnergyUnits.fromCWh(modifierEnergy[m][k]), List.of(
                            new Input(Ingredient.of(BCItems.GATE.get()), 1, stack -> GateItem.variant(stack).equals(plain)),
                            new Input(Ingredient.of(modifierItems[k]), 1)), GateItem.stack(result)));
                }
            }
        }
    }

    private static void gate(GateVariant.Logic logic, GateVariant.Material material, long energyCWh, Input... inputs) {
        GateVariant variant = new GateVariant(logic, material, GateVariant.Modifier.NONE);
        RECIPES.add(new Recipe("gate_" + variant.pack(), EnergyUnits.fromCWh(energyCWh), List.of(inputs), GateItem.stack(variant)));
    }

    public static List<Recipe> all() {
        build();
        return Collections.unmodifiableList(RECIPES);
    }
}
