package net.buildcraftreborn.silicon;

import net.buildcraftreborn.lib.menu.BCMenu;
import net.buildcraftreborn.lib.menu.PhantomSlot;
import net.buildcraftreborn.registry.BCMenus;
import net.buildcraftreborn.silicon.tile.AdvancedCraftingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Tela da mesa de trabalho avançada (textura original): molde, prévia, 15 materiais e 9 resultados. */
public class AdvancedCraftingTableMenu extends BCMenu {
    private final BlockPos pos;
    private final ContainerData data;

    public AdvancedCraftingTableMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(AdvancedCraftingTableBlockEntity.GRID),
                new SimpleContainer(AdvancedCraftingTableBlockEntity.MATERIALS + AdvancedCraftingTableBlockEntity.RESULTS),
                new SimpleContainer(1), new SimpleContainerData(1), pos);
    }

    public AdvancedCraftingTableMenu(int containerId, Inventory inventory, AdvancedCraftingTableBlockEntity table) {
        this(containerId, inventory, table.blueprint(), table.inventory(), table.preview(), table.data(), table.getBlockPos());
    }

    private AdvancedCraftingTableMenu(int containerId, Inventory inventory, Container blueprint, Container items, Container preview,
                                      ContainerData data, BlockPos pos) {
        super(BCMenus.ADVANCED_CRAFTING_TABLE.get(), containerId);
        this.pos = pos;
        this.data = data;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) addSlot(new PhantomSlot(blueprint, row * 3 + col, 33 + col * 18, 16 + row * 18));
        }
        addSlot(new Slot(preview, 0, 127, 33) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 5; col++) addSlot(new Slot(items, row * 5 + col, 15 + col * 18, 85 + row * 18));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(items, AdvancedCraftingTableBlockEntity.MATERIALS + row * 3 + col, 109 + col * 18, 85 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        addPlayerInventory(inventory, 8, 153);
        addDataSlots(data);
    }

    public double progress() {
        int value = this.data.get(0);
        return value < 0 ? -1.0 : value / 1000.0;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(this.pos) instanceof AdvancedCraftingTableBlockEntity
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
