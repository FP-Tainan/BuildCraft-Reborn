package net.buildcraftreborn.lib.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Base dos menus do BuildCraft Reborn. Os slots da máquina vêm primeiro e o inventário do jogador
 * por último ({@link #addPlayerInventory}); shift-clique troca entre os dois e ignora slots fantasmas.
 */
public abstract class BCMenu extends AbstractContainerMenu {
    /** Quantos slots da máquina existem antes do inventário do jogador. */
    private int machineSlots = -1;

    protected BCMenu(MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    /** Inventário (3 linhas) com o canto em {@code x, y} e a barra de atalhos 58 pixels abaixo. */
    protected void addPlayerInventory(Inventory inventory, int x, int y) {
        this.machineSlots = this.slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, x + col * 18, y + 58));
        }
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex >= 0 && slotIndex < this.slots.size() && this.slots.get(slotIndex) instanceof PhantomSlot phantom) {
            if (input == ContainerInput.PICKUP || input == ContainerInput.QUICK_MOVE) {
                phantom.setGhost(getCarried());
            }
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (this.machineSlots < 0 || !slot.hasItem() || slot instanceof PhantomSlot) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < this.machineSlots) {
            if (!moveItemStackTo(stack, this.machineSlots, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, this.machineSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }
}
