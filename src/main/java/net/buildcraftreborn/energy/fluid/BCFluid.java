package net.buildcraftreborn.energy.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import java.util.Optional;

/**
 * Fluido do petróleo no mundo. Viscosidade e espalhamento vêm do BuildCraft: o petróleo e o resíduo
 * andam devagar e pouco; os combustíveis leves correm como água. Não formam fonte infinita.
 */
public abstract class BCFluid extends FlowingFluid {
    protected final BCFluids.Entry entry;

    protected BCFluid(BCFluids.Entry entry) {
        this.entry = entry;
    }

    public BCFluids.Entry entry() {
        return this.entry;
    }

    @Override
    public Fluid getFlowing() {
        return this.entry.flowing();
    }

    @Override
    public Fluid getSource() {
        return this.entry.fluid();
    }

    @Override
    public Item getBucket() {
        return this.entry.bucket().get();
    }

    @Override
    protected boolean canConvertToSource(ServerLevel level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(state, level, pos, blockEntity);
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return this.entry.kind().spread >= 8 ? 4 : 2;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return this.entry.kind().spread >= 7 ? 1 : 2;
    }

    /** Água = 5 ticks; petróleo (viscosidade 2.000) = 10; resíduo (4.000) = 20. */
    @Override
    public int getTickDelay(LevelReader level) {
        return Math.clamp(this.entry.kind().viscosity / 200, 2, 30);
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !this.isSame(fluid);
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return this.entry.block().get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == this.getSource() || fluid == this.getFlowing();
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL);
    }

    public static class Source extends BCFluid {
        public Source(BCFluids.Entry entry) {
            super(entry);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends BCFluid {
        public Flowing(BCFluids.Entry entry) {
            super(entry);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
