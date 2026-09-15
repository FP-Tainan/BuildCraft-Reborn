package net.buildcraftreborn.builders;

import net.buildcraftreborn.builders.item.SnapshotItem;
import net.buildcraftreborn.builders.tile.LibraryBlockEntity;
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
import org.jetbrains.annotations.Nullable;

/** Tela da biblioteca eletrônica (textura original 244×220): lista à esquerda, baixar e enviar à direita. */
public class LibraryMenu extends BCMenu {
    private final BlockPos pos;
    private final ContainerData data;
    private final @Nullable LibraryBlockEntity library;

    public LibraryMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(4), new SimpleContainerData(LibraryBlockEntity.DATA_COUNT), pos, null);
    }

    public LibraryMenu(int containerId, Inventory inventory, LibraryBlockEntity library) {
        this(containerId, inventory, library.inventory(), library.data(), library.getBlockPos(), library);
    }

    private LibraryMenu(int containerId, Inventory inventory, Container slots, ContainerData data, BlockPos pos, @Nullable LibraryBlockEntity library) {
        super(BCMenus.LIBRARY.get(), containerId);
        this.pos = pos;
        this.data = data;
        this.library = library;
        addSlot(new Slot(slots, LibraryBlockEntity.SLOT_DOWN_IN, 219, 57) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof SnapshotItem;
            }
        });
        addSlot(output(slots, LibraryBlockEntity.SLOT_DOWN_OUT, 175, 57));
        addSlot(new Slot(slots, LibraryBlockEntity.SLOT_UP_IN, 175, 79) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SnapshotItem.header(stack) != null;
            }
        });
        addSlot(output(slots, LibraryBlockEntity.SLOT_UP_OUT, 219, 79));
        addPlayerInventory(inventory, 8, 138);
        addDataSlots(data);
    }

    private static Slot output(Container container, int index, int x, int y) {
        return new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }

    public BlockPos pos() {
        return this.pos;
    }

    public int progressDown() {
        return this.data.get(LibraryBlockEntity.DATA_DOWN);
    }

    public int progressUp() {
        return this.data.get(LibraryBlockEntity.DATA_UP);
    }

    public int selected() {
        return this.data.get(LibraryBlockEntity.DATA_SELECTED);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.library != null) this.library.handleButton(id, player.getAbilities().instabuild);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(this.pos) instanceof LibraryBlockEntity
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
