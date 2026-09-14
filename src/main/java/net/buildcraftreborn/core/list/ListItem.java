package net.buildcraftreborn.core.list;

import net.buildcraftreborn.registry.BCComponents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Lista do BuildCraft: filtro de itens usado por tubos e máquinas. Clique direito abre a tela. */
public class ListItem extends Item {
    public ListItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ListContents contents(ItemStack stack) {
        return stack.getOrDefault(BCComponents.LIST_CONTENTS.get(), ListContents.EMPTY);
    }

    /** Se {@code target} passa no filtro da lista {@code list}. */
    public static boolean matches(ItemStack list, ItemStack target) {
        return list.getItem() instanceof ListItem && contents(list).matches(target);
    }

    public static void setContents(ItemStack stack, ListContents contents) {
        if (contents.isEmpty() && contents.lines().stream().noneMatch(line -> line.precise() || line.byType() || line.byMaterial())) {
            stack.remove(BCComponents.LIST_CONTENTS.get());
        } else {
            stack.set(BCComponents.LIST_CONTENTS.get(), contents);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack stack = player.getItemInHand(hand);
            serverPlayer.openMenu(new ExtendedMenuProvider<InteractionHand>() {
                @Override
                public InteractionHand getScreenOpeningData(ServerPlayer opener) {
                    return hand;
                }

                @Override
                public Component getDisplayName() {
                    return stack.getHoverName();
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
                    return new ListMenu(containerId, inventory, hand);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}
