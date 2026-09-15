package net.buildcraftreborn.energy.engine;

import net.buildcraftreborn.lib.menu.BCMenu;
import net.buildcraftreborn.registry.BCMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

/**
 * Tela do motor a combustão: três tanques (combustível, refrigerante e resíduo), lidos do block entity
 * no cliente, e calor/potência pela sincronização de dados.
 */
public class CombustionEngineMenu extends BCMenu {
    private final ContainerData data;
    private final BlockPos pos;

    /** Cliente: dados vazios, preenchidos pela sincronização. */
    public CombustionEngineMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, new SimpleContainerData(EngineBlockEntity.DATA_COUNT), pos);
    }

    public CombustionEngineMenu(int containerId, Inventory inventory, EngineBlockEntity engine) {
        this(containerId, inventory, engine.data(), engine.getBlockPos());
    }

    private CombustionEngineMenu(int containerId, Inventory inventory, ContainerData data, BlockPos pos) {
        super(BCMenus.COMBUSTION_ENGINE.get(), containerId);
        this.data = data;
        this.pos = pos;
        addPlayerInventory(inventory, 8, 95);
        addDataSlots(data);
    }

    public BlockPos pos() {
        return this.pos;
    }

    public EngineStage stage() {
        int index = this.data.get(EngineBlockEntity.DATA_STAGE);
        return EngineStage.values()[Math.clamp(index, 0, EngineStage.values().length - 1)];
    }

    public int power() {
        return this.data.get(EngineBlockEntity.DATA_POWER);
    }

    /** Calor em CCº. */
    public double heat() {
        return this.data.get(EngineBlockEntity.DATA_HEAT) / 10.0;
    }

    /** Sem slots na máquina: shift-clique não tem para onde mandar. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockEntity(this.pos) instanceof EngineBlockEntity
                && player.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) <= 64.0;
    }
}
