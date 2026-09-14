package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.factory.ChuteMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Tela da calha (textura original do BuildCraft). */
public class ChuteScreen extends BCScreen<ChuteMenu> {
    public ChuteScreen(ChuteMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "chute.png", 176, 153);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, 8, 6, 0xFF404040, false);
    }
}
