package net.buildcraftreborn.transport.plug;

import net.buildcraftreborn.transport.block.PipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Pulsar, sensor de luz ou temporizador: usado numa face de tubo, encaixa ali. */
public class PlugItem extends Item {
    private final PlugKind kind;

    public PlugItem(Properties properties, PlugKind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof PipeBlock) || !(level.getBlockEntity(pos) instanceof PlugHolder holder)) {
            return InteractionResult.PASS;
        }
        Direction face = context.getClickedFace();
        if (holder.plugs().occupied(face)) return InteractionResult.FAIL;
        Player player = context.getPlayer();
        if (this.kind == PlugKind.PULSAR && holder.plugs().energy() == null) {
            if (player != null && !level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("item.buildcraftreborn.plug_pulsar.needs_energy"));
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            holder.plugs().attachPlug(face, this.kind);
            PipeBlock.refreshConnections(level, pos);
            if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
            level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }
}
