package net.buildcraftreborn.factory;

import net.buildcraftreborn.factory.tile.ChuteBlockEntity;
import net.buildcraftreborn.lib.menu.BCMenu;
import net.buildcraftreborn.registry.BCMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

/** Tela da calha: quatro slots em funil. */
public class ChuteMenu extends BCMenu {
    private static final int[][] SLOTS = {{62, 18}, {80, 18}, {98, 18}, {80, 36}};
    private final BlockPos pos;

    public ChuteMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(ChuteBlockEntity.SLOTS), pos);
    }

    public ChuteMenu(int containerId, Inventory inventory, Container chute, BlockPos pos) {
        super(BCMenus.CHUTE.get(), containerId);
        this.pos = pos;
        for (int slot = 0; slot < ChuteBlockEntity.SLOTS; slot++) {
            addSlot(new Slot(chute, slot, SLOTS[slot][0], SLOTS[slot][1]));
        }
        addPlayerInventory(inventory, 8, 71);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(this.pos) instanceof ChuteBlockEntity
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
