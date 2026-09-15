package net.buildcraftreborn.transport;

import net.buildcraftreborn.lib.menu.BCMenu;
import net.buildcraftreborn.lib.menu.PhantomSlot;
import net.buildcraftreborn.registry.BCMenus;
import net.buildcraftreborn.transport.tile.FluidPipeBlockEntity;
import net.buildcraftreborn.transport.tile.PipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntConsumer;

/**
 * Tela do tubo de madeira-diamante (textura original 175×161): 9 filtros e os modos lista branca, negra e rodízio.
 * A versão de fluido aceita recipientes de fluido e não tem rodízio.
 */
public class DiamondWoodPipeMenu extends BCMenu {
    private final BlockPos pos;
    private final ContainerData data;
    private final @Nullable IntConsumer setMode;

    public DiamondWoodPipeMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(PipeBlockEntity.FILTER_SLOTS), new SimpleContainerData(PipeBlockEntity.DATA_COUNT), pos, null);
    }

    public DiamondWoodPipeMenu(int containerId, Inventory inventory, PipeBlockEntity pipe) {
        this(containerId, inventory, pipe.filters(), pipe.data(), pipe.getBlockPos(), pipe::setFilterMode);
    }

    public DiamondWoodPipeMenu(int containerId, Inventory inventory, FluidPipeBlockEntity pipe) {
        this(containerId, inventory, pipe.filters(), pipe.data(), pipe.getBlockPos(), pipe::setFilterMode);
    }

    private DiamondWoodPipeMenu(int containerId, Inventory inventory, Container filters, ContainerData data, BlockPos pos, @Nullable IntConsumer setMode) {
        super(BCMenus.DIAMOND_WOOD_PIPE.get(), containerId);
        this.pos = pos;
        this.data = data;
        this.setMode = setMode;
        for (int i = 0; i < PipeBlockEntity.FILTER_WIDTH; i++) addSlot(new PhantomSlot(filters, i, 8 + 18 * i, 18));
        addPlayerInventory(inventory, 8, 79);
        addDataSlots(data);
    }

    public BlockPos pos() {
        return this.pos;
    }

    public int mode() {
        return this.data.get(PipeBlockEntity.DATA_MODE);
    }

    public int currentFilter() {
        return this.data.get(PipeBlockEntity.DATA_CURRENT_FILTER);
    }

    public boolean filterValid() {
        return this.data.get(PipeBlockEntity.DATA_FILTER_VALID) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.setMode != null) this.setMode.accept(id);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(this.pos).getBlock() instanceof net.buildcraftreborn.transport.block.PipeBlock
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
