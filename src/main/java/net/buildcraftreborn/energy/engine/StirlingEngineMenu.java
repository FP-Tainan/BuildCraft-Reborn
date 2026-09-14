package net.buildcraftreborn.energy.engine;

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

/** Tela do motor Stirling: um slot de combustível, a chama e a potência entregue. */
public class StirlingEngineMenu extends BCMenu {
    private final Container fuel;
    private final ContainerData data;
    private final BlockPos pos;

    /** Cliente: dados vazios, preenchidos pela sincronização. */
    public StirlingEngineMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainer(1), new SimpleContainerData(EngineBlockEntity.DATA_COUNT), pos);
    }

    public StirlingEngineMenu(int containerId, Inventory inventory, EngineBlockEntity engine) {
        this(containerId, inventory, engine.fuel(), engine.data(), engine.getBlockPos());
    }

    private StirlingEngineMenu(int containerId, Inventory inventory, Container fuel, ContainerData data, BlockPos pos) {
        super(BCMenus.STIRLING_ENGINE.get(), containerId);
        this.fuel = fuel;
        this.data = data;
        this.pos = pos;
        addSlot(new Slot(fuel, 0, 80, 41) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return inventory.player.level().fuelValues().isFuel(stack);
            }
        });
        addPlayerInventory(inventory, 8, 84);
        addDataSlots(data);
    }

    /** Fração do combustível atual que ainda falta queimar. */
    public double burnRatio() {
        return this.data.get(EngineBlockEntity.DATA_BURN) / 1000.0;
    }

    public double heatRatio() {
        return this.data.get(EngineBlockEntity.DATA_ENERGY) / 1000.0;
    }

    public EngineStage stage() {
        int index = this.data.get(EngineBlockEntity.DATA_STAGE);
        return EngineStage.values()[Math.clamp(index, 0, EngineStage.values().length - 1)];
    }

    public int power() {
        return this.data.get(EngineBlockEntity.DATA_POWER);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(this.pos) instanceof EngineBlockEntity
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
