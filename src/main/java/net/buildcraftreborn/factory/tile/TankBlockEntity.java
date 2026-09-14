package net.buildcraftreborn.factory.tile;

import net.buildcraftreborn.lib.fluid.BCTank;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.craftenergy.api.MultimeterReadable;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/**
 * Tanque de 16.000 CL. O fluido escorre aos poucos para o tanque de baixo (gases sobem), 250 CL por
 * tick, para dar para ver a coluna enchendo. No cliente o nível anda suave até o valor recebido.
 */
public class TankBlockEntity extends BCBlockEntity implements ServerTicking, MultimeterReadable {
    public static final long CAPACITY = 16_000;
    /** Quanto escorre para o tanque vizinho por tick. */
    public static final long FLOW_PER_TICK = BCTank.fromCL(250);
    private static final int SYNC_INTERVAL = 4;

    private final BCTank tank = new BCTank(CAPACITY, this::onFluidChanged);
    private boolean dirty;
    private int syncTimer;
    /** Se neste momento há fluido escorrendo para o tanque de baixo (vai ao cliente). */
    private boolean pouring;
    // cliente: nível mostrado
    private double shownRatio = -1;

    public TankBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.TANK.get(), pos, state);
    }

    public BCTank tank() {
        return this.tank;
    }

    private void onFluidChanged() {
        this.dirty = true;
        setChanged();
    }

    @Override
    public void serverTick() {
        if (this.level == null) return;
        boolean poured = false;
        if (!this.tank.isEmpty()) {
            boolean gas = FluidVariantAttributes.isLighterThanAir(this.tank.variant);
            BlockPos other = gas ? this.worldPosition.above() : this.worldPosition.below();
            if (this.level.getBlockEntity(other) instanceof TankBlockEntity next) {
                try (Transaction transaction = Transaction.openOuter()) {
                    poured = StorageUtil.move(this.tank, next.tank, variant -> true, FLOW_PER_TICK, transaction) > 0;
                    transaction.commit();
                }
            }
        }
        if (poured != this.pouring) {
            this.pouring = poured;
            this.dirty = true;
        }
        if (this.dirty && ++this.syncTimer >= SYNC_INTERVAL) {
            this.syncTimer = 0;
            this.dirty = false;
            syncToClient();
            this.level.updateNeighbourForOutputSignal(this.worldPosition, getBlockState().getBlock());
        }
    }

    public boolean isPouring() {
        return this.pouring;
    }

    /** Cliente: aproxima o nível mostrado do real, para o fluido subir e descer sem pular. */
    public double shownRatio(float partialTick) {
        double target = this.tank.ratio();
        if (this.shownRatio < 0) this.shownRatio = target;
        this.shownRatio += (target - this.shownRatio) * Math.min(1.0, 0.08 + partialTick * 0.02);
        if (Math.abs(target - this.shownRatio) < 0.001) this.shownRatio = target;
        return this.shownRatio;
    }

    @Override
    public void multimeterReading(List<Double> values, List<String> units) {
        values.add((double) this.tank.amountCL());
        units.add("CL");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.tank.save(output, "Tank");
        output.putBoolean("Pouring", this.pouring);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.load(input, "Tank");
        this.pouring = input.getBooleanOr("Pouring", false);
    }
}
