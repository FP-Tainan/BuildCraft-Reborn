package net.buildcraftreborn.silicon.tile;

import net.buildcraftreborn.lib.energy.LaserTarget;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.craftenergy.api.MultimeterReadable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/**
 * Base das mesas do silício ({@code TileLaserTableBase}): juntam energia dos lasers até a meta da tarefa
 * atual. Sem tarefa, a energia guardada se perde.
 */
public abstract class LaserTableBlockEntity extends BCBlockEntity implements ServerTicking, LaserTarget, MultimeterReadable {
    protected long power;
    private long receivedThisTick;
    private double average;

    protected LaserTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Energia que a tarefa atual precisa, em CW·tick; 0 se não há tarefa. */
    public abstract long target();

    protected abstract void tickTable();

    public long power() {
        return this.power;
    }

    public double averagePower() {
        return this.average;
    }

    @Override
    public long requiredLaserPower() {
        long target = target();
        return target <= 0 ? 0 : Math.max(0, target - this.power);
    }

    @Override
    public long receiveLaserPower(long amount) {
        long accepted = Math.min(amount, requiredLaserPower());
        if (accepted <= 0) return amount;
        this.power += accepted;
        this.receivedThisTick += accepted;
        setChanged();
        return amount - accepted;
    }

    @Override
    public void serverTick() {
        if (this.level == null) return;
        this.average = this.average * 0.95 + this.receivedThisTick * 0.05;
        this.receivedThisTick = 0;
        if (target() <= 0) this.power = 0;
        tickTable();
    }

    /** Progresso da tarefa em milésimos (para a tela); -1 sem tarefa. */
    public int progressPermille() {
        long target = target();
        return target <= 0 ? -1 : (int) Math.min(1000, this.power * 1000 / target);
    }

    @Override
    public void multimeterReading(List<Double> values, List<String> units) {
        values.add(this.average);
        units.add("CW");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("LaserPower", this.power);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.power = Math.max(0, input.getLongOr("LaserPower", 0L));
    }
}
