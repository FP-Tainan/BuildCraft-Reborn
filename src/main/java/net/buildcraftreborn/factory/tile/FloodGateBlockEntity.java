package net.buildcraftreborn.factory.tile;

import net.buildcraftreborn.lib.fluid.BCTank;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.craftenergy.api.MultimeterReadable;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Comporta do BuildCraft: com 1.000 CL no tanque, põe uma fonte do fluido no lugar livre mais baixo
 * ao redor (para baixo e para os lados, até 64 blocos). Sem lugar, espera cada vez mais para tentar
 * de novo (16, 32, 64, 128, 256 ticks).
 */
public class FloodGateBlockEntity extends BCBlockEntity implements ServerTicking, MultimeterReadable {
    private static final int[] DELAYS = {16, 32, 64, 128, 256};
    private static final int RANGE = 64;
    private static final int SEARCH_LIMIT = 4_096;
    private static final Direction[] SEARCH = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final BCTank tank = new BCTank(2_000, FloodGateBlockEntity::placeable, this::setChanged);
    private int delayIndex;
    private int timer;

    public FloodGateBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.FLOOD_GATE.get(), pos, state);
    }

    public BCTank tank() {
        return this.tank;
    }

    /** Só fluidos que viram bloco no mundo. */
    private static boolean placeable(FluidVariant variant) {
        return !variant.isBlank() && !variant.getFluid().defaultFluidState().createLegacyBlock().isAir();
    }

    @Override
    public void serverTick() {
        if (this.level == null || ++this.timer < DELAYS[this.delayIndex]) return;
        this.timer = 0;
        if (this.tank.amount < FluidConstants.BUCKET) return;
        Fluid fluid = this.tank.variant.getFluid();
        BlockPos target = findTarget(fluid);
        if (target == null) {
            this.delayIndex = Math.min(DELAYS.length - 1, this.delayIndex + 1);
            return;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            if (this.tank.extract(this.tank.variant, FluidConstants.BUCKET, transaction) != FluidConstants.BUCKET) return;
            transaction.commit();
        }
        this.level.setBlock(target, fluid.defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL);
        this.delayIndex = 0;
    }

    /** Lugar livre (ar ou o fluido correndo) mais baixo e mais perto, alcançável sem subir. */
    private @Nullable BlockPos findTarget(Fluid fluid) {
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>();
        for (Direction direction : SEARCH) {
            BlockPos start = this.worldPosition.relative(direction);
            if (seen.add(start)) open.add(start);
        }
        BlockPos best = null;
        while (!open.isEmpty() && seen.size() < SEARCH_LIMIT) {
            BlockPos current = open.poll();
            BlockState state = this.level.getBlockState(current);
            FluidState fluidState = state.getFluidState();
            boolean sameFluid = !fluidState.isEmpty() && fluidState.getType().isSame(fluid);
            if (!state.isAir() && !sameFluid) continue;
            if (!sameFluid || !fluidState.isSource()) {
                if (best == null || current.getY() < best.getY()
                        || (current.getY() == best.getY() && current.distSqr(this.worldPosition) < best.distSqr(this.worldPosition))) {
                    best = current;
                }
            }
            for (Direction direction : SEARCH) {
                BlockPos next = current.relative(direction);
                if (next.distManhattan(this.worldPosition) > RANGE || !this.level.isLoaded(next) || !seen.add(next)) continue;
                open.add(next);
            }
        }
        return best;
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
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.load(input, "Tank");
    }
}
