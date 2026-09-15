package net.buildcraftreborn.transport.gate;

import net.buildcraftreborn.transport.block.PipeBlock;
import net.buildcraftreborn.transport.plug.PlugHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * Copiador de portas lógicas ({@code ItemGateCopier}): agachado numa porta copia gatilhos, ações e grupos;
 * normal numa porta cola o que couber nela; agachado no ar esquece a cópia.
 */
public class GateCopierItem extends Item {
    public GateCopierItem(Properties properties) {
        super(properties);
    }

    public static CompoundTag copy(GateLogic gate, HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        gate.save(output, "");
        return output.buildResult();
    }

    /** Configuração copiada aplicada numa porta da variante {@code target}. */
    public static GateLogic paste(CompoundTag tag, GateVariant target, HolderLookup.Provider registries) {
        return GateLogic.load(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag), "", target);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(pos) instanceof PlugHolder holder)) return InteractionResult.PASS;
        Direction side = PipeBlock.plugAt(holder, pos, context.getClickLocation());
        GateLogic gate = side == null ? null : holder.plugs().gate(side);
        if (gate == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        ItemStack stack = context.getItemInHand();
        if (player.isShiftKeyDown()) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(copy(gate, level.registryAccess())));
            player.sendOverlayMessage(Component.translatable("item.buildcraftreborn.gate_copier.copied"));
        } else {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            if (data == null) {
                player.sendOverlayMessage(Component.translatable("item.buildcraftreborn.gate_copier.empty"));
                return InteractionResult.FAIL;
            }
            holder.plugs().setGate(side, paste(data.copyTag(), gate.variant(), level.registryAccess()));
            player.sendOverlayMessage(Component.translatable("item.buildcraftreborn.gate_copier.pasted"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !stack.has(DataComponents.CUSTOM_DATA)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            stack.remove(DataComponents.CUSTOM_DATA);
            player.sendOverlayMessage(Component.translatable("item.buildcraftreborn.gate_copier.cleared"));
        }
        return InteractionResult.SUCCESS;
    }
}
