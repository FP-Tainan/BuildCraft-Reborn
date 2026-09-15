package net.buildcraftreborn.silicon;

import net.buildcraftreborn.lib.menu.BCMenu;
import net.buildcraftreborn.registry.BCMenus;
import net.buildcraftreborn.silicon.tile.AssemblyTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Tela da mesa de montagem (textura original): 12 slots de material à esquerda e as receitas à direita;
 * clicar numa receita marca ou desmarca.
 */
public class AssemblyTableMenu extends BCMenu {
    public static final int RECIPES_START = AssemblyTableBlockEntity.SLOTS;

    private final BlockPos pos;
    private final ContainerData data;
    private final @Nullable AssemblyTableBlockEntity table;

    public AssemblyTableMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(AssemblyTableBlockEntity.SLOTS), new SimpleContainer(AssemblyTableBlockEntity.RECIPE_SLOTS),
                new SimpleContainerData(AssemblyTableBlockEntity.DATA_COUNT), pos, null);
    }

    public AssemblyTableMenu(int containerId, Inventory inventory, AssemblyTableBlockEntity table) {
        this(containerId, inventory, table.inventory(), table.display(), table.data(), table.getBlockPos(), table);
    }

    private AssemblyTableMenu(int containerId, Inventory inventory, Container materials, Container display, ContainerData data,
                              BlockPos pos, @Nullable AssemblyTableBlockEntity table) {
        super(BCMenus.ASSEMBLY_TABLE.get(), containerId);
        this.pos = pos;
        this.data = data;
        this.table = table;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) addSlot(new Slot(materials, row * 3 + col, 8 + col * 18, 36 + row * 18));
        }
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(display, row * 3 + col, 116 + col * 18, 36 + row * 18) {
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
        addPlayerInventory(inventory, 8, 123);
        addDataSlots(data);
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (slotIndex >= RECIPES_START && slotIndex < RECIPES_START + AssemblyTableBlockEntity.RECIPE_SLOTS) {
            if (this.table != null && !player.level().isClientSide()) this.table.toggle(slotIndex - RECIPES_START);
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    public BlockPos pos() {
        return this.pos;
    }

    public double progress() {
        int value = this.data.get(AssemblyTableBlockEntity.DATA_PROGRESS);
        return value < 0 ? -1.0 : value / 1000.0;
    }

    public boolean isSaved(int index) {
        return (this.data.get(AssemblyTableBlockEntity.DATA_SAVED) & (1 << index)) != 0;
    }

    public boolean isCraftable(int index) {
        return (this.data.get(AssemblyTableBlockEntity.DATA_CRAFTABLE) & (1 << index)) != 0;
    }

    public int activeIndex() {
        return this.data.get(AssemblyTableBlockEntity.DATA_ACTIVE);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(this.pos) instanceof AssemblyTableBlockEntity
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
