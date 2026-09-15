package net.buildcraftreborn.factory.tile;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.factory.FactoryUtil;
import net.buildcraftreborn.lib.energy.MachineEnergy;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.buildcraftreborn.registry.BCBlocks;
import net.craftenergy.api.EnergyUnits;
import net.craftenergy.api.MultimeterReadable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/**
 * Poço de mineração do BuildCraft: cava direto para baixo, deixando o tubo no caminho. Cada bloco
 * custa dureza × 16 CWh (pedra: 24 CWh); os itens vão para os inventários ao lado.
 */
public class MiningWellBlockEntity extends BCBlockEntity implements ServerTicking, MultimeterReadable, net.buildcraftreborn.lib.tile.ItemPipeConnectable {
    private static final int INTERVAL = 5;

    private final MachineEnergy energy = new MachineEnergy(this, 220, EnergyUnits.fromCWh(500), 10_000);
    private int cooldown;

    public MiningWellBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.MINING_WELL.get(), pos, state);
    }

    public MachineEnergy energy() {
        return this.energy;
    }

    /** BuildCraft: dureza × 16 MJ × multiplicador, arredondado para baixo. */
    public static long breakEnergy(Level level, BlockPos pos, BlockState state) {
        float hardness = state.getDestroySpeed(level, pos);
        return EnergyUnits.fromCWh(Math.floor(hardness * 16.0 * BuildCraftReborn.config.miningMultiplier));
    }

    @Override
    public void serverTick() {
        if (!(this.level instanceof ServerLevel serverLevel)) return;
        if (--this.cooldown > 0) return;
        this.cooldown = INTERVAL;

        BlockPos tip = FactoryUtil.tubeTip(this.level, this.worldPosition);
        if (tip.getY() < this.level.getMinY() || this.worldPosition.getY() - tip.getY() > BuildCraftReborn.config.miningMaxDepth) return;
        BlockState target = this.level.getBlockState(tip);
        BlockState pipe = BCBlocks.MINING_PIPE.get().defaultBlockState();
        if (target.isAir() || (!target.getFluidState().isEmpty() && target.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)) {
            this.level.setBlock(tip, pipe, Block.UPDATE_ALL);
            return;
        }
        if (target.getDestroySpeed(this.level, tip) < 0) return;
        if (!this.energy.use(breakEnergy(this.level, tip, target))) return;

        List<ItemStack> drops = Block.getDrops(target, serverLevel, tip, this.level.getBlockEntity(tip), null, new ItemStack(Items.IRON_PICKAXE));
        this.level.destroyBlock(tip, false);
        this.level.setBlock(tip, pipe, Block.UPDATE_ALL);
        for (ItemStack drop : drops) FactoryUtil.output(this.level, this.worldPosition, drop);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) FactoryUtil.removeTube(this.level, pos);
    }

    @Override
    public void multimeterReading(List<Double> values, List<String> units) {
        MultimeterReadable.electric(values, units, this.energy.voltage(), this.energy.lastReceived());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.energy.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy.load(input);
    }
}
