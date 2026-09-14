package net.buildcraftreborn.factory.tile;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.factory.FactoryUtil;
import net.buildcraftreborn.lib.energy.MachineEnergy;
import net.buildcraftreborn.lib.fluid.BCTank;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.buildcraftreborn.registry.BCBlocks;
import net.craftenergy.api.EnergyUnits;
import net.craftenergy.api.MultimeterReadable;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bomba do BuildCraft: desce um tubo até o fluido e bombeia as fontes ligadas, das mais distantes
 * para as mais próximas (para o tubo não perder o contato). 10 CWh por balde, tanque de 16.000 CL,
 * busca até 64 blocos. Água infinita não seca (a menos que a configuração mande).
 */
public class PumpBlockEntity extends BCBlockEntity implements ServerTicking, MultimeterReadable {
    public static final long ENERGY_PER_BUCKET = EnergyUnits.fromCWh(10);
    private static final int TUBE_INTERVAL = 5;
    private static final int REBUILD_INTERVAL = 600;
    private static final int SEARCH_LIMIT = 16_384;
    private static final Direction[] SEARCH_NORMAL = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static final Direction[] SEARCH_GAS = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final MachineEnergy energy = new MachineEnergy(this, 220, EnergyUnits.fromCWh(50), 10_000);
    private final BCTank tank = new BCTank(16_000, this::setChanged);
    private final Deque<BlockPos> queue = new ArrayDeque<>();
    private int timer;
    private int rebuildTimer;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.PUMP.get(), pos, state);
    }

    public MachineEnergy energy() {
        return this.energy;
    }

    public BCTank tank() {
        return this.tank;
    }

    @Override
    public void serverTick() {
        if (this.level == null) return;
        if (!this.tank.isEmpty()) FactoryUtil.pushFluid(this.level, this.worldPosition, this.tank, FluidConstants.BUCKET);
        this.timer++;

        BlockPos tip = FactoryUtil.tubeTip(this.level, this.worldPosition);
        BlockState atTip = this.level.getBlockState(tip);
        if (atTip.isAir()) {
            if (this.timer % TUBE_INTERVAL == 0 && tip.getY() > this.level.getMinY()) {
                this.level.setBlock(tip, BCBlocks.MINING_PIPE.get().defaultBlockState(), Block.UPDATE_ALL);
            }
            return;
        }
        FluidState fluid = atTip.getFluidState();
        if (fluid.isEmpty()) return;
        if (this.tank.capacityCL() - this.tank.amountCL() < 1_000 || this.energy.stored() < ENERGY_PER_BUCKET) return;

        if (this.queue.isEmpty() || --this.rebuildTimer <= 0) {
            rebuildQueue(tip, fluid.getType());
            this.rebuildTimer = REBUILD_INTERVAL;
        }
        while (!this.queue.isEmpty()) {
            BlockPos target = this.queue.pollLast();
            BlockState state = this.level.getBlockState(target);
            FluidState targetFluid = state.getFluidState();
            if (!targetFluid.isSource() || !targetFluid.getType().isSame(fluid.getType())) continue;
            if (pump(target, state, targetFluid)) return;
        }
    }

    /** Tira um balde da fonte em {@code target}; água infinita fica no lugar. */
    private boolean pump(BlockPos target, BlockState state, FluidState fluid) {
        Fluid still = fluid.getType();
        FluidVariant variant = FluidVariant.of(still);
        try (Transaction transaction = Transaction.openOuter()) {
            if (this.tank.insert(variant, FluidConstants.BUCKET, transaction) != FluidConstants.BUCKET) return false;
            if (!isInfiniteWater(target, still)) {
                if (state.getBlock() instanceof BucketPickup pickup) {
                    if (pickup.pickupBlock(null, this.level, target, state).isEmpty()) return false;
                } else {
                    this.level.setBlock(target, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            transaction.commit();
        }
        this.energy.use(ENERGY_PER_BUCKET);
        return true;
    }

    private boolean isInfiniteWater(BlockPos pos, Fluid fluid) {
        if (BuildCraftReborn.config.pumpsConsumeWater || !fluid.isSame(Fluids.WATER)) return false;
        int sources = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            FluidState neighbor = this.level.getFluidState(pos.relative(direction));
            if (neighbor.isSource() && neighbor.getType().isSame(Fluids.WATER)) sources++;
        }
        return sources >= 2;
    }

    /** Busca em largura das fontes ligadas à ponta do tubo; a fila termina nas mais distantes. */
    private void rebuildQueue(BlockPos start, Fluid fluid) {
        this.queue.clear();
        boolean gas = FluidVariantAttributes.isLighterThanAir(FluidVariant.of(fluid));
        Direction[] search = gas ? SEARCH_GAS : SEARCH_NORMAL;
        int range = BuildCraftReborn.config.pumpMaxDistance;
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>();
        List<BlockPos> sources = new ArrayList<>();
        open.add(start);
        seen.add(start);
        while (!open.isEmpty() && seen.size() < SEARCH_LIMIT) {
            BlockPos current = open.poll();
            FluidState state = this.level.getFluidState(current);
            if (state.isEmpty() || !state.getType().isSame(fluid)) continue;
            if (state.isSource()) sources.add(current);
            for (Direction direction : search) {
                BlockPos next = current.relative(direction);
                if (Math.abs(next.getX() - start.getX()) > range || Math.abs(next.getZ() - start.getZ()) > range
                        || Math.abs(next.getY() - start.getY()) > range || !this.level.isLoaded(next) || !seen.add(next)) {
                    continue;
                }
                open.add(next);
            }
        }
        this.queue.addAll(sources);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) FactoryUtil.removeTube(this.level, pos);
    }

    @Override
    public void multimeterReading(List<Double> values, List<String> units) {
        MultimeterReadable.electric(values, units, this.energy.voltage(), this.energy.lastReceived());
        values.add((double) this.tank.amountCL());
        units.add("CL");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.save(output);
        this.tank.save(output, "Tank");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.load(input);
        this.tank.load(input, "Tank");
    }
}
