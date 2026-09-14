package net.buildcraftreborn.lib.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

/**
 * Slot "fantasma" do BuildCraft (filtros, receita da bancada automática): guarda só a cópia de um
 * item, sem tirar nada do jogador. Clicar com um item copia; clicar com a mão vazia limpa.
 */
public class PhantomSlot extends Slot {
    public PhantomSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    /** Copia um item (ou limpa, com a mão vazia). */
    public void setGhost(ItemStack carried) {
        set(carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
    }
}
