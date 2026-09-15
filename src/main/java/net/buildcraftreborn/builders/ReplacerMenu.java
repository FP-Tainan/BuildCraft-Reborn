package net.buildcraftreborn.builders;

import net.buildcraftreborn.builders.item.SchematicItem;
import net.buildcraftreborn.builders.item.SnapshotItem;
import net.buildcraftreborn.builders.snapshot.Snapshot;
import net.buildcraftreborn.builders.snapshot.SnapshotHeader;
import net.buildcraftreborn.builders.tile.ReplacerBlockEntity;
import net.buildcraftreborn.lib.menu.BCMenu;
import net.buildcraftreborn.registry.BCMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Tela do substituidor (textura original 176×241): planta, esquema "de" e esquema "para". */
public class ReplacerMenu extends BCMenu {
    private final BlockPos pos;

    public ReplacerMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(3), pos);
    }

    public ReplacerMenu(int containerId, Inventory inventory, ReplacerBlockEntity replacer) {
        this(containerId, inventory, replacer.inventory(), replacer.getBlockPos());
    }

    private ReplacerMenu(int containerId, Inventory inventory, Container slots, BlockPos pos) {
        super(BCMenus.REPLACER.get(), containerId);
        this.pos = pos;
        addSlot(new Slot(slots, ReplacerBlockEntity.SLOT_SNAPSHOT, 8, 115) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                SnapshotHeader header = SnapshotItem.header(stack);
                return header != null && header.snapshotType() == Snapshot.Type.BLUEPRINT;
            }
        });
        addSlot(schematic(slots, ReplacerBlockEntity.SLOT_FROM, 8, 137));
        addSlot(schematic(slots, ReplacerBlockEntity.SLOT_TO, 56, 137));
        addPlayerInventory(inventory, 8, 159);
    }

    private static Slot schematic(Container container, int index, int x, int y) {
        return new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SchematicItem.state(stack) != null;
            }
        };
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(this.pos) instanceof ReplacerBlockEntity
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
