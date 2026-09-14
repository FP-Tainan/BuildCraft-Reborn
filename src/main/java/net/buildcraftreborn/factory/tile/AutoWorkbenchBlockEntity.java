package net.buildcraftreborn.factory.tile;

import net.buildcraftreborn.lib.energy.MachineEnergy;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.craftenergy.api.EnergyUnits;
import net.craftenergy.api.MultimeterReadable;
import net.minecraft.core.BlockPos;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bancada automática do BuildCraft: a receita fica marcada em 9 slots fantasmas e a bancada fabrica
 * sozinha com os materiais dos 9 slots de baixo. Cada item custa 40 CWh; ela gera 200 CW por conta
 * própria (uma fabricação a cada 10 s) e vai mais rápido ligada na rede.
 */
public class AutoWorkbenchBlockEntity extends BCBlockEntity implements ServerTicking, MultimeterReadable {
    public static final int GRID = 9;
    public static final int MATERIALS = 9;
    public static final int OUTPUT = MATERIALS;
    public static final long ENERGY_PER_CRAFT = EnergyUnits.fromCWh(40);
    public static final long PASSIVE_POWER = 200;
    private static final int RECIPE_CHECK_INTERVAL = 10;

    private final MachineEnergy energy = new MachineEnergy(this, 220, ENERGY_PER_CRAFT, 1_000);
    private final SimpleContainer grid = new SimpleContainer(GRID) {
        @Override
        public void setChanged() {
            super.setChanged();
            AutoWorkbenchBlockEntity.this.recipeDirty = true;
            AutoWorkbenchBlockEntity.this.setChanged();
        }
    };
    /** Materiais (0–8) e saída (9). */
    private final SimpleContainer inventory = new SimpleContainer(MATERIALS + 1) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot < MATERIALS;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            AutoWorkbenchBlockEntity.this.setChanged();
        }
    };
    private final SimpleContainer preview = new SimpleContainer(1);
    private Optional<RecipeHolder<CraftingRecipe>> recipe = Optional.empty();
    private boolean recipeDirty = true;
    private int timer;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return (int) (AutoWorkbenchBlockEntity.this.energy.stored() * 1000 / ENERGY_PER_CRAFT);
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public AutoWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.AUTO_WORKBENCH.get(), pos, state);
    }

    public SimpleContainer grid() {
        return this.grid;
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

    public MachineEnergy energy() {
        return this.energy;
    }

    /** Funis e tubos: materiais só entram, a saída só sai. */
    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> itemStorage(
            @org.jetbrains.annotations.Nullable net.minecraft.core.Direction face) {
        var storage = net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(this.inventory, face);
        List<net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant>> parts = new ArrayList<>();
        for (int slot = 0; slot < MATERIALS; slot++) {
            parts.add(net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage.insertOnlyOf(storage.getSlot(slot)));
        }
        parts.add(net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage.extractOnlyOf(storage.getSlot(OUTPUT)));
        return new net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage<>(parts);
    }

    private CraftingInput.Positioned input() {
        List<ItemStack> items = new ArrayList<>(GRID);
        for (int i = 0; i < GRID; i++) items.add(this.grid.getItem(i));
        return CraftingInput.ofPositioned(3, 3, items);
    }

    @Override
    public void serverTick() {
        if (!(this.level instanceof ServerLevel server)) return;
        this.energy.addPassive(PASSIVE_POWER);
        if (this.recipeDirty || ++this.timer >= RECIPE_CHECK_INTERVAL) {
            this.timer = 0;
            this.recipeDirty = false;
            CraftingInput input = input().input();
            this.recipe = input.isEmpty() ? Optional.empty() : server.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, server);
            ItemStack result = this.recipe.map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
            if (!ItemStack.matches(result, this.preview.getItem(0))) this.preview.setItem(0, result);
        }
        if (this.recipe.isEmpty() || this.energy.stored() < ENERGY_PER_CRAFT) return;
        craft(server);
    }

    /** Fabrica uma vez se houver material para todos os slots da receita e espaço na saída. */
    private void craft(ServerLevel server) {
        CraftingInput.Positioned positioned = input();
        CraftingInput input = positioned.input();
        ItemStack result = this.recipe.get().value().assemble(input);
        ItemStack output = this.inventory.getItem(OUTPUT);
        if (result.isEmpty() || (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, result)
                || output.getCount() + result.getCount() > output.getMaxStackSize()))) {
            return;
        }

        // cada slot da receita precisa de um material igual ao item fantasma
        int[] taken = new int[MATERIALS];
        int[] sources = new int[GRID];
        for (int slot = 0; slot < GRID; slot++) {
            ItemStack ghost = this.grid.getItem(slot);
            sources[slot] = -1;
            if (ghost.isEmpty()) continue;
            for (int material = 0; material < MATERIALS; material++) {
                ItemStack stack = this.inventory.getItem(material);
                if (!stack.isEmpty() && stack.getCount() > taken[material] && ItemStack.isSameItemSameComponents(stack, ghost)) {
                    taken[material]++;
                    sources[slot] = material;
                    break;
                }
            }
            if (sources[slot] < 0) return;
        }

        NonNullList<ItemStack> remaining = this.recipe.get().value().getRemainingItems(input);
        for (int material = 0; material < MATERIALS; material++) {
            if (taken[material] > 0) this.inventory.removeItem(material, taken[material]);
        }
        for (ItemStack leftover : remaining) {
            if (leftover.isEmpty()) continue;
            ItemStack rest = leftover.copy();
            for (int material = 0; material < MATERIALS && !rest.isEmpty(); material++) {
                ItemStack stack = this.inventory.getItem(material);
                if (stack.isEmpty()) {
                    this.inventory.setItem(material, rest);
                    rest = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(stack, rest) && stack.getCount() < stack.getMaxStackSize()) {
                    int moved = Math.min(rest.getCount(), stack.getMaxStackSize() - stack.getCount());
                    stack.grow(moved);
                    rest.shrink(moved);
                }
            }
            if (!rest.isEmpty()) Block.popResource(server, this.worldPosition.above(), rest);
        }
        if (output.isEmpty()) {
            this.inventory.setItem(OUTPUT, result);
        } else {
            output.grow(result.getCount());
            this.inventory.setChanged();
        }
        this.energy.use(ENERGY_PER_CRAFT);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) Containers.dropContents(this.level, pos, this.inventory);
    }

    @Override
    public void multimeterReading(List<Double> values, List<String> units) {
        MultimeterReadable.electric(values, units, this.energy.voltage(), this.energy.lastReceived());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.save(output);
        for (int slot = 0; slot < GRID; slot++) output.store("Ghost" + slot, ItemStack.OPTIONAL_CODEC, this.grid.getItem(slot));
        for (int slot = 0; slot <= MATERIALS; slot++) output.store("Slot" + slot, ItemStack.OPTIONAL_CODEC, this.inventory.getItem(slot));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.load(input);
        for (int slot = 0; slot < GRID; slot++) this.grid.setItem(slot, input.read("Ghost" + slot, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        for (int slot = 0; slot <= MATERIALS; slot++) this.inventory.setItem(slot, input.read("Slot" + slot, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.recipeDirty = true;
    }
}
