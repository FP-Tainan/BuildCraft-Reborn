package net.buildcraftreborn.transport.plug;

import net.buildcraftreborn.transport.block.PipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Fio de tubo ({@code ItemWire}): usado num tubo, ocupa o canto mais perto do clique. */
public class WireItem extends Item {
    private final DyeColor color;

    public WireItem(Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
    }

    public DyeColor color() {
        return this.color;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.buildcraftreborn.wire", Component.translatable("color.minecraft." + this.color.getName()));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof PipeBlock) || !(level.getBlockEntity(pos) instanceof PlugHolder holder)) {
            return InteractionResult.PASS;
        }
        int corner = WireNetwork.cornerAt(context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ()));
        if (holder.plugs().wire(corner) != null) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            holder.plugs().placeWire(corner, this.color);
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
            level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
        }
        return InteractionResult.SUCCESS;
    }
}
