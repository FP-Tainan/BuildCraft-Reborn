package net.buildcraftreborn.builders.item;

import net.buildcraftreborn.builders.snapshot.BlueprintRules;
import net.buildcraftreborn.registry.BCComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Esquema de bloco único ({@code ItemSchematicSingle}): usado num bloco, guarda o estado dele; agachado no ar,
 * esquece. Serve de "de" e "para" no substituidor.
 */
public class SchematicItem extends Item {
    public SchematicItem(Properties properties) {
        super(properties);
    }

    public static @Nullable BlockState state(ItemStack stack) {
        return stack.getItem() instanceof SchematicItem ? stack.get(BCComponents.SCHEMATIC_STATE.get()) : null;
    }

    public static ItemStack withState(ItemStack stack, BlockState state) {
        stack.set(BCComponents.SCHEMATIC_STATE.get(), state);
        stack.set(DataComponents.MAX_STACK_SIZE, 1);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        BlockState state = state(stack);
        return state == null ? super.getName(stack) : Component.translatable("item.buildcraftreborn.schematic_single.named", state.getBlock().getName());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockState target = BlueprintRules.forScan(context.getLevel().getBlockState(context.getClickedPos()));
        if (state(stack) != null || target.isAir()) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide()) {
            if (stack.getCount() > 1 && player != null) {
                ItemStack one = withState(stack.split(1), target);
                if (!player.getInventory().add(one)) player.drop(one, false);
            } else {
                withState(stack, target);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || state(stack) == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            stack.remove(BCComponents.SCHEMATIC_STATE.get());
            stack.remove(DataComponents.MAX_STACK_SIZE);
        }
        return InteractionResult.SUCCESS;
    }
}
