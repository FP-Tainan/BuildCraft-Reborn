package net.buildcraftreborn.factory;

import net.buildcraftreborn.factory.tile.AutoWorkbenchBlockEntity;
import net.buildcraftreborn.lib.menu.BCMenu;
import net.buildcraftreborn.lib.menu.PhantomSlot;
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

/**
 * Tela da bancada automática: receita fantasma 3×3, prévia do resultado, saída e os 9 materiais.
 */
public class AutoWorkbenchMenu extends BCMenu {
    private final BlockPos pos;
    private final ContainerData data;

    public AutoWorkbenchMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(AutoWorkbenchBlockEntity.GRID),
                new SimpleContainer(AutoWorkbenchBlockEntity.MATERIALS + 1), new SimpleContainer(1), new SimpleContainerData(1), pos);
    }

    public AutoWorkbenchMenu(int containerId, Inventory inventory, AutoWorkbenchBlockEntity workbench) {
        this(containerId, inventory, workbench.grid(), workbench.inventory(), workbench.preview(), workbench.data(), workbench.getBlockPos());
    }

    private AutoWorkbenchMenu(int containerId, Inventory inventory, Container grid, Container items, Container preview,
                              ContainerData data, BlockPos pos) {
        super(BCMenus.AUTO_WORKBENCH.get(), containerId);
        this.pos = pos;
        this.data = data;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new PhantomSlot(grid, row * 3 + col, 30 + col * 18, 17 + row * 18));
            }
        }
        addSlot(new Slot(preview, 0, 93, 27) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });
        addSlot(new Slot(items, AutoWorkbenchBlockEntity.OUTPUT, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int col = 0; col < AutoWorkbenchBlockEntity.MATERIALS; col++) {
            addSlot(new Slot(items, col, 8 + col * 18, 84));
        }
        addPlayerInventory(inventory, 8, 115);
        addDataSlots(data);
    }

    /** Energia guardada para a próxima fabricação, de 0 a 1. */
    public double progress() {
        return Math.min(1.0, this.data.get(0) / 1000.0);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(this.pos) instanceof AutoWorkbenchBlockEntity
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
