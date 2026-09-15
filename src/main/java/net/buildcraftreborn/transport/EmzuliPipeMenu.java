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
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

/** Tela do tubo emzuli (textura original 176×166): 4 presets (vermelho, verde, azul, amarelo) com filtro e cor de pintura. */
public class EmzuliPipeMenu extends BCMenu {
    public static final int[] SLOT_X = {25, 25, 134, 134};
    public static final int[] SLOT_Y = {21, 49, 21, 49};

    private final BlockPos pos;
    private final ContainerData data;
    private final @Nullable PipeBlockEntity pipe;

    public EmzuliPipeMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(PipeBlockEntity.FILTER_SLOTS), new SimpleContainerData(PipeBlockEntity.DATA_COUNT), pos, null);
    }

    public EmzuliPipeMenu(int containerId, Inventory inventory, PipeBlockEntity pipe) {
        this(containerId, inventory, pipe.filters(), pipe.data(), pipe.getBlockPos(), pipe);
    }

    private EmzuliPipeMenu(int containerId, Inventory inventory, Container filters, ContainerData data, BlockPos pos, @Nullable PipeBlockEntity pipe) {
        super(BCMenus.EMZULI_PIPE.get(), containerId);
        this.pos = pos;
        this.data = data;
        this.pipe = pipe;
        for (int i = 0; i < PipeBlockEntity.PRESETS; i++) addSlot(new PhantomSlot(filters, i, SLOT_X[i], SLOT_Y[i]));
        addPlayerInventory(inventory, 8, 84);
        addDataSlots(data);
    }

    public @Nullable DyeColor presetColour(int slot) {
        int value = this.data.get(PipeBlockEntity.DATA_PRESET_COLOUR + slot);
        return value <= 0 || value > DyeColor.values().length ? null : DyeColor.values()[value - 1];
    }

    public boolean active(int slot) {
        return (this.data.get(PipeBlockEntity.DATA_ACTIVE_PRESETS) >> slot & 1) != 0;
    }

    public int currentPreset() {
        return this.data.get(PipeBlockEntity.DATA_CURRENT_PRESET);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.pipe != null) this.pipe.handlePresetButton(id);
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(this.pos) instanceof PipeBlockEntity
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
