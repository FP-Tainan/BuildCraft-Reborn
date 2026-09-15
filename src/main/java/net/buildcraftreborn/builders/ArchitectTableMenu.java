package net.buildcraftreborn.builders;

import net.buildcraftreborn.builders.item.SnapshotItem;
import net.buildcraftreborn.builders.tile.ArchitectTableBlockEntity;
import net.buildcraftreborn.lib.menu.BCMenu;
import net.buildcraftreborn.registry.BCMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Tela da mesa do arquiteto (textura original 256×166): molde/planta em branco à esquerda, resultado à direita. */
public class ArchitectTableMenu extends BCMenu {
    private final BlockPos pos;
    private final ContainerData data;

    public ArchitectTableMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(2), new SimpleContainerData(ArchitectTableBlockEntity.DATA_COUNT), pos);
    }

    public ArchitectTableMenu(int containerId, Inventory inventory, ArchitectTableBlockEntity table) {
        this(containerId, inventory, table.inventory(), table.data(), table.getBlockPos());
    }

    private ArchitectTableMenu(int containerId, Inventory inventory, Container slots, ContainerData data, BlockPos pos) {
        super(BCMenus.ARCHITECT_TABLE.get(), containerId);
        this.pos = pos;
        this.data = data;
        addSlot(new Slot(slots, ArchitectTableBlockEntity.SLOT_IN, 135, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SnapshotItem;
            }
        });
        addSlot(new Slot(slots, ArchitectTableBlockEntity.SLOT_OUT, 194, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerInventory(inventory, 88, 84);
        addDataSlots(data);
    }

    /** Progresso em milésimos; -1 parado. */
    public int progress() {
        return this.data.get(ArchitectTableBlockEntity.DATA_PROGRESS);
    }

    public boolean hasBox() {
        return (this.data.get(ArchitectTableBlockEntity.DATA_FLAGS) & ArchitectTableBlockEntity.FLAG_BOX) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(this.pos) instanceof ArchitectTableBlockEntity
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
