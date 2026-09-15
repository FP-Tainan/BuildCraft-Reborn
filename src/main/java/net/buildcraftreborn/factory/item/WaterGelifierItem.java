package net.buildcraftreborn.factory.item;

import net.buildcraftreborn.registry.BCBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Gelificador de água ({@code ItemWaterGel}): usado mirando uma fonte de água, começa um gel que se espalha. */
public class WaterGelifierItem extends Item {
    public WaterGelifierItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        if (!level.getBlockState(pos).is(Blocks.WATER) || !level.getFluidState(pos).isSource()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlock(pos, BCBlocks.WATER_GEL.get().defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            ItemStack stack = player.getItemInHand(hand);
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
