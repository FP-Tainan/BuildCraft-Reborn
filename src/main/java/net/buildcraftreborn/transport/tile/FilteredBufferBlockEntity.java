package net.buildcraftreborn.transport.tile;

import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Buffer filtrado ({@code TileFilteredBuffer}): 9 filtros fantasmas e 9 slots; cada slot só aceita o item do seu
 * filtro (filtro vazio não aceita nada). Tira-se qualquer coisa por qualquer lado.
 */
public class FilteredBufferBlockEntity extends BCBlockEntity {
    public static final int SLOTS = 9;

    private final SimpleContainer filters = new SimpleContainer(SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            FilteredBufferBlockEntity.this.setChanged();
        }
    };
    private final SimpleContainer storage = new SimpleContainer(SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            FilteredBufferBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return accepts(slot, stack);
        }
    };

    public FilteredBufferBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.FILTERED_BUFFER.get(), pos, state);
    }

    public SimpleContainer filters() {
        return this.filters;
    }

    public SimpleContainer storage() {
        return this.storage;
    }

    public boolean accepts(int slot, ItemStack stack) {
        ItemStack filter = this.filters.getItem(slot);
        return !filter.isEmpty() && ItemStack.isSameItemSameComponents(filter, stack);
    }

    public Storage<ItemVariant> itemStorage(@Nullable Direction face) {
        ContainerStorage container = ContainerStorage.of(this.storage, face);
        List<Storage<ItemVariant>> slots = new ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            int slot = i;
            slots.add(new FilteringStorage<>(container.getSlot(slot)) {
                @Override
                protected boolean canInsert(ItemVariant resource) {
                    ItemStack filter = FilteredBufferBlockEntity.this.filters.getItem(slot);
                    return !filter.isEmpty() && resource.matches(filter);
                }
            });
        }
        return new CombinedStorage<>(slots);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) Containers.dropContents(this.level, pos, this.storage);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int slot = 0; slot < SLOTS; slot++) {
            output.store("Filter" + slot, ItemStack.OPTIONAL_CODEC, this.filters.getItem(slot));
            output.store("Slot" + slot, ItemStack.OPTIONAL_CODEC, this.storage.getItem(slot));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int slot = 0; slot < SLOTS; slot++) {
            this.filters.setItem(slot, input.read("Filter" + slot, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
            this.storage.setItem(slot, input.read("Slot" + slot, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        }
    }
}
