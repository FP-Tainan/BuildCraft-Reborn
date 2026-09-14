package net.buildcraftreborn.core.marker;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;

import java.util.function.Consumer;

/**
 * Conector de marcadores: na mão, mostra as ligações possíveis entre marcadores de área. Clique num
 * marcador liga; agachado, desliga tudo daquele marcador.
 */
public class MarkerConnectorItem extends Item {
    public MarkerConnectorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MarkerBlockEntity marker)) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide()) {
            if (context.getPlayer() != null && context.getPlayer().isSecondaryUseActive()) {
                marker.disconnectAll();
            } else {
                marker.connectManually();
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.buildcraftreborn.marker_connector").withStyle(ChatFormatting.GRAY));
    }
}
