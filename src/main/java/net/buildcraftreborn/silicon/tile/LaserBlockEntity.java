package net.buildcraftreborn.silicon.tile;

import net.buildcraftreborn.lib.block.BCDirectionalBlock;
import net.buildcraftreborn.lib.energy.LaserTarget;
import net.buildcraftreborn.lib.energy.MachineEnergy;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.craftenergy.api.EnergyUnits;
import net.craftenergy.api.MultimeterReadable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Laser ({@code TileLaser}): guarda até 1.024 CWh da rede (1.000 MV, aceita a partir de 200 MV) e manda até
 * 4.000 CW por tick para uma mesa que precise, escolhida ao acaso entre as do cone de 6 blocos à frente.
 */
public class LaserBlockEntity extends BCBlockEntity implements ServerTicking, MultimeterReadable {
    public static final int RANGE = 6;
    public static final long MAX_OUTPUT = 4_000;
    private static final int SCAN_INTERVAL = 40;

    private final MachineEnergy energy = new MachineEnergy(this, 1_000, EnergyUnits.fromCWh(1_024), MAX_OUTPUT, 200);
    private final List<BlockPos> candidates = new ArrayList<>();
    private @Nullable BlockPos target;
    private int scanTimer;
    private int moveTimer;
    private double average;
    /** 0 = desligado, 1 a 4 = cor do feixe (baixa, média, alta, cheia). */
    private int beamLevel;
    // cliente: ponto do feixe na mesa, que pula de lugar
    private @Nullable Vec3 beamPoint;
    private long beamPointTime;

    public LaserBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.LASER.get(), pos, state);
    }

    public MachineEnergy energy() {
        return this.energy;
    }

    public @Nullable BlockPos target() {
        return this.target;
    }

    public int beamLevel() {
        return this.beamLevel;
    }

    private Direction facing() {
        BlockState state = this.level != null ? this.level.getBlockState(this.worldPosition) : getBlockState();
        return state.hasProperty(BCDirectionalBlock.FACING) ? state.getValue(BCDirectionalBlock.FACING) : Direction.UP;
    }

    @Override
    public void serverTick() {
        if (this.level == null) return;
        BlockPos oldTarget = this.target;
        int oldLevel = this.beamLevel;

        if (--this.scanTimer <= 0) {
            this.scanTimer = SCAN_INTERVAL;
            scan();
        }
        if (this.target != null && !needsPower(this.target)) this.target = null;
        if (--this.moveTimer <= 0) {
            this.moveTimer = 10 + this.level.getRandom().nextInt(11);
            if (this.target == null) chooseTarget();
        }

        long sent = 0;
        LaserTarget receiver = this.target != null && this.level.getBlockEntity(this.target) instanceof LaserTarget laserTarget ? laserTarget : null;
        if (receiver != null) {
            long amount = Math.min(Math.min(MAX_OUTPUT, this.energy.stored()), receiver.requiredLaserPower());
            if (amount > 0) {
                sent = amount - receiver.receiveLaserPower(amount);
                if (sent > 0) this.energy.use(sent);
            }
        }
        this.average = this.average * 0.9 + sent * 0.1;
        this.beamLevel = this.target == null || this.average < 1 ? 0 : 1 + Math.min(3, (int) (this.average * 4 / MAX_OUTPUT));
        if (oldLevel != this.beamLevel || !java.util.Objects.equals(oldTarget, this.target)) syncToClient();
    }

    /** Mesas que precisam de energia no cone à frente (a cada passo o cone abre um bloco para os lados). */
    private void scan() {
        this.candidates.clear();
        Direction facing = facing();
        Direction.Axis axis = facing.getAxis();
        for (int distance = 1; distance <= RANGE; distance++) {
            for (int a = -distance; a <= distance; a++) {
                for (int b = -distance; b <= distance; b++) {
                    BlockPos pos = this.worldPosition.relative(facing, distance);
                    pos = switch (axis) {
                        case Y -> pos.offset(a, 0, b);
                        case X -> pos.offset(0, a, b);
                        case Z -> pos.offset(a, b, 0);
                    };
                    if (this.level.getBlockEntity(pos) instanceof LaserTarget) this.candidates.add(pos);
                }
            }
        }
    }

    private boolean needsPower(BlockPos pos) {
        return this.level.getBlockEntity(pos) instanceof LaserTarget laserTarget && laserTarget.requiredLaserPower() > 0;
    }

    private void chooseTarget() {
        List<BlockPos> hungry = new ArrayList<>();
        for (BlockPos pos : this.candidates) {
            if (needsPower(pos)) hungry.add(pos);
        }
        this.target = hungry.isEmpty() ? null : hungry.get(this.level.getRandom().nextInt(hungry.size()));
    }

    /** Cliente: ponta do feixe no tampo da mesa, mudando de lugar a cada meio segundo. */
    public @Nullable Vec3 beamPoint(long gameTime) {
        if (this.target == null) return null;
        if (this.beamPoint == null || gameTime - this.beamPointTime >= 10 || gameTime < this.beamPointTime) {
            this.beamPointTime = gameTime;
            java.util.Random random = new java.util.Random(gameTime * 31 + this.worldPosition.asLong());
            this.beamPoint = new Vec3(this.target.getX() + (5 + random.nextInt(6) + 0.5) / 16.0, this.target.getY() + 0.5625,
                    this.target.getZ() + (5 + random.nextInt(6) + 0.5) / 16.0);
        }
        return this.beamPoint;
    }

    @Override
    public void multimeterReading(List<Double> values, List<String> units) {
        MultimeterReadable.electric(values, units, this.energy.voltage(), (long) this.average);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.save(output);
        output.putBoolean("HasTarget", this.target != null);
        if (this.target != null) output.putLong("Target", this.target.asLong());
        output.putInt("BeamLevel", this.beamLevel);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.load(input);
        BlockPos previous = this.target;
        this.target = input.getBooleanOr("HasTarget", false) ? BlockPos.of(input.getLongOr("Target", 0L)) : null;
        if (!java.util.Objects.equals(previous, this.target)) this.beamPoint = null;
        this.beamLevel = input.getIntOr("BeamLevel", 0);
    }
}
