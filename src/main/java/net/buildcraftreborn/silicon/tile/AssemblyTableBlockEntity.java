package net.buildcraftreborn.silicon.tile;

import net.buildcraftreborn.factory.FactoryUtil;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.buildcraftreborn.silicon.AssemblyRecipes;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Mesa de montagem ({@code TileAssemblyTable}): 12 slots de material e 12 receitas na tela. A tela mostra
 * primeiro as receitas marcadas ou que já têm material e completa com as demais, na ordem da lista. A mesa
 * monta, uma de cada vez e em rodízio, as receitas marcadas que têm material; cada uma espera a energia dos
 * lasers e o resultado sai para inventários vizinhos (ou cai em cima).
 */
public class AssemblyTableBlockEntity extends LaserTableBlockEntity {
    public static final int SLOTS = 12;
    public static final int RECIPE_SLOTS = 12;
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_SAVED = 1;
    public static final int DATA_ACTIVE = 2;
    public static final int DATA_CRAFTABLE = 3;
    public static final int DATA_COUNT = 4;

    private final SimpleContainer inventory = new SimpleContainer(SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            AssemblyTableBlockEntity.this.setChanged();
        }
    };
    /** Resultados das receitas visíveis, só para mostrar na tela. */
    private final SimpleContainer display = new SimpleContainer(RECIPE_SLOTS);
    /** Índice na lista de receitas de cada slot da tela (-1 = vazio). */
    private final int[] visible = new int[RECIPE_SLOTS];
    /** Receitas marcadas, por índice na lista inteira. */
    private final java.util.BitSet saved = new java.util.BitSet();
    /** Receita em andamento, por índice na lista inteira. */
    private int active = -1;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progressPermille();
                case DATA_SAVED -> displayMask(true);
                case DATA_ACTIVE -> displayIndex(AssemblyTableBlockEntity.this.active);
                case DATA_CRAFTABLE -> displayMask(false);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public AssemblyTableBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.ASSEMBLY_TABLE.get(), pos, state);
        Arrays.fill(this.visible, -1);
        refreshVisible();
    }

    public SimpleContainer inventory() {
        return this.inventory;
    }

    public SimpleContainer display() {
        return this.display;
    }

    public ContainerData data() {
        return this.data;
    }

    private boolean isSaved(int recipeIndex) {
        return recipeIndex >= 0 && this.saved.get(recipeIndex);
    }

    /** Clique no slot {@code displayIndex} da tela: marca ou desmarca a receita mostrada ali. */
    public void toggle(int displayIndex) {
        refreshVisible();
        if (displayIndex < 0 || displayIndex >= RECIPE_SLOTS) return;
        int index = this.visible[displayIndex];
        if (index < 0) return;
        this.saved.flip(index);
        if (this.active == index && !isSaved(index)) this.active = -1;
        setChanged();
    }

    /** Marcadas ou com material primeiro; o resto da tela com as outras receitas. */
    private void refreshVisible() {
        List<AssemblyRecipes.Recipe> recipes = AssemblyRecipes.all();
        int count = 0;
        boolean[] used = new boolean[recipes.size()];
        for (int i = 0; i < recipes.size() && count < RECIPE_SLOTS; i++) {
            if (isSaved(i) || hasMaterials(recipes.get(i))) {
                this.visible[count++] = i;
                used[i] = true;
            }
        }
        for (int i = 0; i < recipes.size() && count < RECIPE_SLOTS; i++) {
            if (!used[i]) this.visible[count++] = i;
        }
        for (int slot = 0; slot < RECIPE_SLOTS; slot++) {
            if (slot >= count) this.visible[slot] = -1;
            ItemStack wanted = this.visible[slot] < 0 ? ItemStack.EMPTY : recipes.get(this.visible[slot]).output();
            if (!ItemStack.matches(this.display.getItem(slot), wanted)) this.display.setItem(slot, wanted.copy());
        }
    }

    private int displayIndex(int recipeIndex) {
        if (recipeIndex < 0) return -1;
        for (int slot = 0; slot < RECIPE_SLOTS; slot++) {
            if (this.visible[slot] == recipeIndex) return slot;
        }
        return -1;
    }

    private int displayMask(boolean savedMask) {
        List<AssemblyRecipes.Recipe> recipes = AssemblyRecipes.all();
        int mask = 0;
        for (int slot = 0; slot < RECIPE_SLOTS; slot++) {
            int index = this.visible[slot];
            if (index < 0) continue;
            if (savedMask ? isSaved(index) : hasMaterials(recipes.get(index))) mask |= 1 << slot;
        }
        return mask;
    }

    private @Nullable AssemblyRecipes.Recipe recipe(int index) {
        List<AssemblyRecipes.Recipe> recipes = AssemblyRecipes.all();
        return index >= 0 && index < recipes.size() ? recipes.get(index) : null;
    }

    /** Receita em andamento; troca para a próxima marcada que tem material quando a atual não serve. */
    private @Nullable AssemblyRecipes.Recipe activeRecipe() {
        AssemblyRecipes.Recipe current = recipe(this.active);
        if (current != null && isSaved(this.active) && hasMaterials(current)) return current;
        selectNext();
        return recipe(this.active);
    }

    private void selectNext() {
        int total = AssemblyRecipes.all().size();
        for (int step = 1; step <= total; step++) {
            int index = Math.floorMod(this.active + step, total);
            AssemblyRecipes.Recipe recipe = recipe(index);
            if (recipe != null && isSaved(index) && hasMaterials(recipe)) {
                this.active = index;
                return;
            }
        }
        this.active = -1;
    }

    @Override
    public long target() {
        AssemblyRecipes.Recipe recipe = activeRecipe();
        return recipe == null ? 0 : recipe.energy();
    }

    @Override
    protected void tickTable() {
        refreshVisible();
        AssemblyRecipes.Recipe recipe = activeRecipe();
        if (recipe == null || this.power < recipe.energy()) return;
        for (AssemblyRecipes.Input input : recipe.inputs()) take(input);
        FactoryUtil.output(this.level, this.worldPosition, recipe.output().copy());
        this.power -= recipe.energy();
        selectNext();
        setChanged();
    }

    private boolean hasMaterials(AssemblyRecipes.Recipe recipe) {
        for (AssemblyRecipes.Input input : recipe.inputs()) {
            int found = 0;
            for (int slot = 0; slot < SLOTS; slot++) {
                ItemStack stack = this.inventory.getItem(slot);
                if (!stack.isEmpty() && input.test(stack)) found += stack.getCount();
            }
            if (found < input.count()) return false;
        }
        return true;
    }

    private void take(AssemblyRecipes.Input input) {
        int left = input.count();
        for (int slot = 0; slot < SLOTS && left > 0; slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (stack.isEmpty() || !input.test(stack)) continue;
            int taken = Math.min(left, stack.getCount());
            this.inventory.removeItem(slot, taken);
            left -= taken;
        }
    }

    /** Funis e tubos só colocam material. */
    public Storage<ItemVariant> itemStorage(@Nullable Direction face) {
        return FilteringStorage.insertOnlyOf(ContainerStorage.of(this.inventory, face));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) Containers.dropContents(this.level, pos, this.inventory);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int slot = 0; slot < SLOTS; slot++) output.store("Slot" + slot, ItemStack.OPTIONAL_CODEC, this.inventory.getItem(slot));
        output.store("SavedBits", com.mojang.serialization.Codec.LONG.listOf(), Arrays.stream(this.saved.toLongArray()).boxed().toList());
        output.putInt("Active", this.active);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int slot = 0; slot < SLOTS; slot++) this.inventory.setItem(slot, input.read("Slot" + slot, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.saved.clear();
        long[] bits = input.read("SavedBits", com.mojang.serialization.Codec.LONG.listOf())
                .map(list -> list.stream().mapToLong(Long::longValue).toArray())
                .orElseGet(() -> new long[]{input.getLongOr("SavedRecipes", input.getIntOr("Saved", 0))});
        this.saved.or(java.util.BitSet.valueOf(bits));
        this.active = input.getIntOr("Active", -1);
    }
}
