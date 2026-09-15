package net.buildcraftreborn.core.item;

import net.buildcraftreborn.registry.BCItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Livro guia do BuildCraft Reborn: abre a tela com os capítulos. Todo jogador ganha um na primeira entrada. */
public class GuideBookItem extends Item {
    private static final String RECEIVED_TAG = "buildcraftreborn.received_guide";
    /** Definido pelo cliente; no servidor não faz nada. */
    public static Runnable opener = () -> { };

    public GuideBookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (player.addTag(RECEIVED_TAG)) {
                ItemStack book = new ItemStack(BCItems.GUIDE.get());
                if (!player.getInventory().add(book)) player.drop(book, false);
            }
        });
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) opener.run();
        return InteractionResult.SUCCESS;
    }
}
