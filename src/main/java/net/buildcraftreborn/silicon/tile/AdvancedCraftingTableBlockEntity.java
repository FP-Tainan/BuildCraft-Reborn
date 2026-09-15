package net.buildcraftreborn.silicon.tile;

import net.buildcraftreborn.registry.BCBlockEntities;
import net.craftenergy.api.EnergyUnits;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mesa de trabalho avançada ({@code TileAdvancedCraftingTable}): molde 3×3 fantasma, 15 slots de material e 9
 * de resultado. Cada item fabricado custa 500 CWh de laser.
 */
public class AdvancedCraftingTableBlockEntity extends LaserTableBlockEntity {
    public static final int GRID = 9;
    public static final int MATERIALS = 15;
    public static final int RESULTS = 9;
    public static final long ENERGY_PER_CRAFT = EnergyUnits.fromCWh(500);
    private static final int RECIPE_CHECK_INTERVAL = 10;

    private final SimpleContainer blueprint = new SimpleContainer(GRID) {
        @Override
        public void setChanged() {
            super.setChanged();
            AdvancedCraftingTableBlockEntity.this.recipeDirty = true;
            AdvancedCraftingTableBlockEntity.this.setChanged();
        }
    };
    /** Materiais (0–14) e resultados (15–23). */
    private final SimpleContainer inventory = new SimpleContainer(MATERIALS + RESULTS) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot < MATERIALS;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            AdvancedCraftingTableBlockEntity.this.setChanged();
        }
    };
    private final SimpleContainer preview = new SimpleContainer(1);
    private Optional<RecipeHolder<CraftingRecipe>> recipe = Optional.empty();
    private boolean recipeDirty = true;
    private int timer;
    private boolean canCraft;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return progressPermille();
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public AdvancedCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.ADVANCED_CRAFTING_TABLE.get(), pos, state);
    }

    public SimpleContainer blueprint() {
        return this.blueprint;
    }

    public SimpleContainer inventory() {
        return this.inventory;
    }

    public SimpleContainer preview() {
        return this.preview;
    }

    public ContainerData data() {
        return this.data;
    }

    @Override
    public long target() {
        return this.canCraft ? ENERGY_PER_CRAFT : 0;
    }

    private CraftingInput input() {
        List<ItemStack> items = new ArrayList<>(GRID);
        for (int i = 0; i < GRID; i++) items.add(this.blueprint.getItem(i));
        return CraftingInput.ofPositioned(3, 3, items).input();
    }

    @Override
    protected void tickTable() {
        if (!(this.level instanceof ServerLevel server)) return;
        if (this.recipeDirty || ++this.timer >= RECIPE_CHECK_INTERVAL) {
            this.timer = 0;
            this.recipeDirty = false;
            CraftingInput input = input();
            this.recipe = input.isEmpty() ? Optional.empty() : server.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, server);
            ItemStack result = this.recipe.map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
            if (!ItemStack.matches(result, this.preview.getItem(0))) this.preview.setItem(0, result);
        }
        int[] taken = this.recipe.isPresent() ? plan() : null;
        this.canCraft = taken != null;
        if (this.canCraft && this.power >= ENERGY_PER_CRAFT) {
            craft(server, taken);
            this.power -= ENERGY_PER_CRAFT;
        }
    }

    /** Quantos itens tirar de cada material, ou nulo se falta algo ou os resultados estão cheios. */
    private int @Nullable [] plan() {
        ItemStack result = this.recipe.get().value().assemble(input());
        if (result.isEmpty() || !fitsInResults(result)) return null;
        int[] taken = new int[MATERIALS];
        for (int slot = 0; slot < GRID; slot++) {
            ItemStack ghost = this.blueprint.getItem(slot);
            if (ghost.isEmpty()) continue;
            boolean found = false;
            for (int material = 0; material < MATERIALS && !found; material++) {
                ItemStack stack = this.inventory.getItem(material);
                if (!stack.isEmpty() && stack.getCount() > taken[material] && ItemStack.isSameItemSameComponents(stack, ghost)) {
                    taken[material]++;
                    found = true;
                }
            }
            if (!found) return null;
        }
        return taken;
    }

    private boolean fitsInResults(ItemStack result) {
        int left = result.getCount();
        for (int slot = MATERIALS; slot < MATERIALS + RESULTS && left > 0; slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (stack.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(stack, result)) left -= stack.getMaxStackSize() - stack.getCount();
        }
        return left <= 0;
    }

    private void craft(ServerLevel server, int[] taken) {
        CraftingInput input = input();
        ItemStack result = this.recipe.get().value().assemble(input);
        NonNullList<ItemStack> remaining = this.recipe.get().value().getRemainingItems(input);
        for (int material = 0; material < MATERIALS; material++) {
            if (taken[material] > 0) this.inventory.removeItem(material, taken[material]);
        }
        insert(result, MATERIALS, MATERIALS + RESULTS);
        for (ItemStack leftover : remaining) {
            if (leftover.isEmpty()) continue;
            ItemStack rest = insert(leftover.copy(), 0, MATERIALS);
            if (!rest.isEmpty()) Block.popResource(server, this.worldPosition.above(), rest);
        }
    }

    /** Coloca nos slots {@code from..to} (junta com iguais primeiro); devolve o que não coube. */
    private ItemStack insert(ItemStack stack, int from, int to) {
        for (int slot = from; slot < to && !stack.isEmpty(); slot++) {
            ItemStack existing = this.inventory.getItem(slot);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < existing.getMaxStackSize()) {
                int moved = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(moved);
                stack.shrink(moved);
            }
        }
        for (int slot = from; slot < to && !stack.isEmpty(); slot++) {
            if (this.inventory.getItem(slot).isEmpty()) {
                this.inventory.setItem(slot, stack.copy());
                stack = ItemStack.EMPTY;
            }
        }
        this.inventory.setChanged();
        return stack;
    }

    /** Funis: material só entra, resultado só sai. */
    public Storage<ItemVariant> itemStorage(@Nullable Direction face) {
        ContainerStorage storage = ContainerStorage.of(this.inventory, face);
        List<Storage<ItemVariant>> parts = new ArrayList<>();
        for (int slot = 0; slot < MATERIALS; slot++) parts.add(FilteringStorage.insertOnlyOf(storage.getSlot(slot)));
        for (int slot = MATERIALS; slot < MATERIALS + RESULTS; slot++) parts.add(FilteringStorage.extractOnlyOf(storage.getSlot(slot)));
        return new CombinedStorage<>(parts);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) Containers.dropContents(this.level, pos, this.inventory);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int slot = 0; slot < GRID; slot++) output.store("Ghost" + slot, ItemStack.OPTIONAL_CODEC, this.blueprint.getItem(slot));
        for (int slot = 0; slot < MATERIALS + RESULTS; slot++) output.store("Slot" + slot, ItemStack.OPTIONAL_CODEC, this.inventory.getItem(slot));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int slot = 0; slot < GRID; slot++) this.blueprint.setItem(slot, input.read("Ghost" + slot, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        for (int slot = 0; slot < MATERIALS + RESULTS; slot++) {
            this.inventory.setItem(slot, input.read("Slot" + slot, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        }
        this.recipeDirty = true;
    }
}
