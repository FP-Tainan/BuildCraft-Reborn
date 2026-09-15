package net.buildcraftreborn.transport.gate;

import net.buildcraftreborn.registry.BCComponents;
import net.buildcraftreborn.registry.BCItems;
import net.buildcraftreborn.transport.block.PipeBlock;
import net.buildcraftreborn.transport.plug.PlugHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Porta lógica ({@code ItemPluggableGate}): um item só, a variante fica num componente. Usada numa face de tubo,
 * encaixa ali; agachado no ar, troca AND/OR.
 */
public class GateItem extends Item {
    public GateItem(Properties properties) {
        super(properties);
    }

    public static GateVariant variant(ItemStack stack) {
        GateVariant variant = stack.get(BCComponents.GATE_VARIANT.get());
        return variant == null ? GateVariant.BASIC : variant;
    }

    public static ItemStack stack(GateVariant variant) {
        ItemStack stack = new ItemStack(BCItems.GATE.get());
        stack.set(BCComponents.GATE_VARIANT.get(), variant);
        return stack;
    }

    /** Aba criativa: as portas modificáveis em todas as lógicas e modificadores (a básica já vem do registro). */
    public static void addCreativeVariants(CreativeModeTab.Output output) {
        for (GateVariant.Material material : GateVariant.Material.values()) {
            if (!material.modifiable) continue;
            for (GateVariant.Logic logic : GateVariant.Logic.values()) {
                for (GateVariant.Modifier modifier : GateVariant.Modifier.values()) {
                    output.accept(stack(new GateVariant(logic, material, modifier)));
                }
            }
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return variant(stack).name();
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
        if (!level.isClientSide()) {
            holder.plugs().attachGate(face, variant(context.getItemInHand()));
            PipeBlock.refreshConnections(level, pos);
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
            level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        GateVariant variant = variant(stack);
        if (!player.isShiftKeyDown() || !variant.material().modifiable) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            stack.set(BCComponents.GATE_VARIANT.get(), variant.withLogic(variant.logic() == GateVariant.Logic.AND ? GateVariant.Logic.OR : GateVariant.Logic.AND));
        }
        return InteractionResult.SUCCESS;
    }
}
