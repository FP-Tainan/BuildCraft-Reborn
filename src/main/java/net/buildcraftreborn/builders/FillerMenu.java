package net.buildcraftreborn.builders;

import net.buildcraftreborn.builders.filler.FillerPattern;
import net.buildcraftreborn.builders.tile.FillerBlockEntity;
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
import org.jetbrains.annotations.Nullable;

/** Tela do preenchedor (textura original): padrão, 4 parâmetros, escavar/inverter e 27 slots de material. */
public class FillerMenu extends BCMenu {
    private final BlockPos pos;
    private final ContainerData data;
    private final @Nullable FillerBlockEntity filler;

    public FillerMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(FillerBlockEntity.SLOTS), new SimpleContainerData(FillerBlockEntity.DATA_COUNT), pos, null);
    }

    public FillerMenu(int containerId, Inventory inventory, FillerBlockEntity filler) {
        this(containerId, inventory, filler.resources(), filler.data(), filler.getBlockPos(), filler);
    }

    private FillerMenu(int containerId, Inventory inventory, Container resources, ContainerData data, BlockPos pos, @Nullable FillerBlockEntity filler) {
        super(BCMenus.FILLER.get(), containerId);
        this.pos = pos;
        this.data = data;
        this.filler = filler;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(resources, row * 9 + col, 8 + col * 18, 84 + row * 18));
        }
        addPlayerInventory(inventory, 8, 153);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.filler != null) this.filler.handleButton(id);
        return true;
    }

    public FillerPattern pattern() {
        return FillerPattern.byIndex(this.data.get(FillerBlockEntity.DATA_PATTERN));
    }

    public int param(int index) {
        return this.data.get(FillerBlockEntity.DATA_PARAM + index);
    }

    public boolean excavate() {
        return (this.data.get(FillerBlockEntity.DATA_FLAGS) & FillerBlockEntity.FLAG_EXCAVATE) != 0;
    }

    public boolean inverted() {
        return (this.data.get(FillerBlockEntity.DATA_FLAGS) & FillerBlockEntity.FLAG_INVERT) != 0;
    }

    public boolean hasBox() {
        return (this.data.get(FillerBlockEntity.DATA_FLAGS) & FillerBlockEntity.FLAG_BOX) != 0;
    }

    public int toBreak() {
        return this.data.get(FillerBlockEntity.DATA_TO_BREAK);
    }

    public int toPlace() {
        return this.data.get(FillerBlockEntity.DATA_TO_PLACE);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(this.pos) instanceof FillerBlockEntity
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
