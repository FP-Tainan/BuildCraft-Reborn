package net.buildcraftreborn.transport;

import net.buildcraftreborn.lib.menu.BCMenu;
import net.buildcraftreborn.lib.menu.PhantomSlot;
import net.buildcraftreborn.registry.BCMenus;
import net.buildcraftreborn.transport.tile.PipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * Filtros do tubo de diamante: uma linha de 9 itens fantasmas por lado, na ordem baixo, cima, norte,
 * sul, oeste e leste (as cores da tela original).
 */
public class PipeFilterMenu extends BCMenu {
    private final BlockPos pos;

    public PipeFilterMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(PipeBlockEntity.FILTER_SLOTS), pos);
    }

    public PipeFilterMenu(int containerId, Inventory inventory, Container filters, BlockPos pos) {
        super(BCMenus.PIPE_FILTER.get(), containerId);
        this.pos = pos;
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < PipeBlockEntity.FILTER_WIDTH; col++) {
                addSlot(new PhantomSlot(filters, row * PipeBlockEntity.FILTER_WIDTH + col, 8 + col * 18, 18 + row * 18));
            }
        }
        addPlayerInventory(inventory, 8, 140);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(this.pos).getBlock() instanceof net.buildcraftreborn.transport.block.PipeBlock
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
