package net.buildcraftreborn.energy.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Bloco do fluido no mundo; petróleo, resíduo e óleos pesados grudam e seguram quem entra. */
public class BCFluidBlock extends LiquidBlock {
    private static final Vec3 STICKY = new Vec3(0.5, 0.4, 0.5);
    private final BCFluids.Entry entry;

    public BCFluidBlock(BCFluids.Entry entry, Properties properties) {
        super(entry.fluid(), properties);
        this.entry = entry;
    }

    public BCFluids.Entry entry() {
        return this.entry;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
        super.entityInside(state, level, pos, entity, effects, intersects);
        if (this.entry.kind().sticky) entity.makeStuckInBlock(state, STICKY);
    }
}
