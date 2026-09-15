package net.buildcraftreborn.builders;

import net.buildcraftreborn.builders.item.SnapshotItem;
import net.buildcraftreborn.builders.tile.BuilderBlockEntity;
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

/**
 * Tela do construtor (texturas originais): planta em cima, 27 slots de material, lista de 24 itens que faltam
 * e os 4 tanques à direita.
 */
public class BuilderMenu extends BCMenu {
    private final BlockPos pos;
    private final ContainerData data;

    public BuilderMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(1), new SimpleContainer(BuilderBlockEntity.SLOTS),
                new SimpleContainer(BuilderBlockEntity.DISPLAY), new SimpleContainerData(BuilderBlockEntity.DATA_COUNT), pos);
    }

    public BuilderMenu(int containerId, Inventory inventory, BuilderBlockEntity builder) {
        this(containerId, inventory, builder.snapshotSlot(), builder.resources(), builder.display(), builder.data(), builder.getBlockPos());
    }

    private BuilderMenu(int containerId, Inventory inventory, Container snapshot, Container resources, Container display,
                        ContainerData data, BlockPos pos) {
        super(BCMenus.BUILDER.get(), containerId);
        this.pos = pos;
        this.data = data;
        addSlot(new Slot(snapshot, 0, 80, 27) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SnapshotItem.header(stack) != null;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(resources, row * 9 + col, 8 + col * 18, 72 + row * 18));
        }
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 4; col++) {
                addSlot(new Slot(display, row * 4 + col, 179 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }
                });
            }
        }
        addPlayerInventory(inventory, 8, 140);
        addDataSlots(data);
    }

    public BlockPos pos() {
        return this.pos;
    }

    public int toBreak() {
        return this.data.get(BuilderBlockEntity.DATA_TO_BREAK);
    }

    public int toPlace() {
        return this.data.get(BuilderBlockEntity.DATA_TO_PLACE);
    }

    public boolean hasSnapshot() {
        return (this.data.get(BuilderBlockEntity.DATA_FLAGS) & BuilderBlockEntity.FLAG_SNAPSHOT) != 0;
    }

    public boolean done() {
        return (this.data.get(BuilderBlockEntity.DATA_FLAGS) & BuilderBlockEntity.FLAG_DONE) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(this.pos) instanceof BuilderBlockEntity
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
