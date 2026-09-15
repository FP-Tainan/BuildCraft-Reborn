package net.buildcraftreborn.transport;

import net.buildcraftreborn.lib.menu.BCMenu;
import net.buildcraftreborn.lib.menu.PhantomSlot;
import net.buildcraftreborn.registry.BCMenus;
import net.buildcraftreborn.transport.tile.FilteredBufferBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Tela do buffer filtrado (textura original 176×169): filtros em cima, itens embaixo. */
public class FilteredBufferMenu extends BCMenu {
    private final BlockPos pos;
    private final Container filters;

    public FilteredBufferMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(FilteredBufferBlockEntity.SLOTS), new SimpleContainer(FilteredBufferBlockEntity.SLOTS), pos);
    }

    public FilteredBufferMenu(int containerId, Inventory inventory, FilteredBufferBlockEntity buffer) {
        this(containerId, inventory, buffer.filters(), buffer.storage(), buffer.getBlockPos());
    }

    private FilteredBufferMenu(int containerId, Inventory inventory, Container filters, Container storage, BlockPos pos) {
        super(BCMenus.FILTERED_BUFFER.get(), containerId);
        this.pos = pos;
        this.filters = filters;
        for (int i = 0; i < FilteredBufferBlockEntity.SLOTS; i++) addSlot(new PhantomSlot(filters, i, 8 + 18 * i, 27));
        for (int i = 0; i < FilteredBufferBlockEntity.SLOTS; i++) {
            int slot = i;
            addSlot(new Slot(storage, i, 8 + 18 * i, 61) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    ItemStack filter = FilteredBufferMenu.this.filters.getItem(slot);
                    return !filter.isEmpty() && ItemStack.isSameItemSameComponents(filter, stack);
                }
            });
        }
        addPlayerInventory(inventory, 8, 86);
    }

    public ItemStack filter(int slot) {
        return this.filters.getItem(slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(this.pos) instanceof FilteredBufferBlockEntity
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
