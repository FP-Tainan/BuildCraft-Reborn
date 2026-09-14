package net.buildcraftreborn.factory.tile;

import net.buildcraftreborn.lib.block.BCDirectionalBlock;
import net.buildcraftreborn.lib.energy.MachineEnergy;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.craftenergy.api.EnergyUnits;
import net.craftenergy.api.MultimeterReadable;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Calha do BuildCraft: quatro slots, pega até 3 itens soltos logo atrás e empurra um item por vez
 * para o inventário da frente. Sozinha move um item a cada 8 ticks; com energia, a cada 2.
 */
public class ChuteBlockEntity extends BCBlockEntity implements ServerTicking, MultimeterReadable {
    public static final int SLOTS = 4;
    private static final int SLOW_INTERVAL = 8;
    private static final int FAST_INTERVAL = 2;
    private static final int PICKUP_MAX = 3;
    private static final long ENERGY_PER_ITEM = 50;

    private final MachineEnergy energy = new MachineEnergy(this, 220, EnergyUnits.fromCWh(1), 1_000);
    private final SimpleContainer inventory = new SimpleContainer(SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            ChuteBlockEntity.this.setChanged();
        }
    };
    private int cooldown;

    public ChuteBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.CHUTE.get(), pos, state);
    }

    public SimpleContainer inventory() {
        return this.inventory;
    }

    public MachineEnergy energy() {
        return this.energy;
    }

    @Override
    public void serverTick() {
        if (this.level == null || --this.cooldown > 0) return;
        boolean fast = this.energy.use(ENERGY_PER_ITEM);
        this.cooldown = fast ? FAST_INTERVAL : SLOW_INTERVAL;
        Direction facing = getBlockState().getValue(BCDirectionalBlock.FACING);
        pickUp(facing.getOpposite());
        push(facing);
    }

    private void pickUp(Direction back) {
        List<ItemEntity> items = this.level.getEntitiesOfClass(ItemEntity.class, new AABB(this.worldPosition.relative(back)),
                entity -> entity.isAlive() && !entity.getItem().isEmpty());
        int picked = 0;
        for (ItemEntity entity : items) {
            if (picked++ >= PICKUP_MAX) break;
            ItemStack rest = this.inventory.addItem(entity.getItem().copy());
            if (rest.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(rest);
            }
        }
    }

    private void push(Direction facing) {
        Storage<ItemVariant> target = ItemStorage.SIDED.find(this.level, this.worldPosition.relative(facing), facing.getOpposite());
        if (target == null) return;
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = this.inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            try (Transaction transaction = Transaction.openOuter()) {
                if (target.insert(ItemVariant.of(stack), 1, transaction) == 1) {
                    transaction.commit();
                    stack.shrink(1);
                    this.inventory.setItem(slot, stack);
                    return;
                }
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) Containers.dropContents(this.level, pos, this.inventory);
    }

    @Override
    public void multimeterReading(List<Double> values, List<String> units) {
        MultimeterReadable.electric(values, units, this.energy.voltage(), this.energy.lastReceived());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.save(output);
        for (int slot = 0; slot < SLOTS; slot++) {
            output.store("Slot" + slot, ItemStack.OPTIONAL_CODEC, this.inventory.getItem(slot));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.load(input);
        for (int slot = 0; slot < SLOTS; slot++) {
            this.inventory.setItem(slot, input.read("Slot" + slot, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        }
    }
}
