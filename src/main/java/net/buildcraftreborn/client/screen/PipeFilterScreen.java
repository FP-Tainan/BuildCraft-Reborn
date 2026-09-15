package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.transport.PipeFilterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Tela de filtros do tubo de diamante (textura original do BuildCraft, uma cor por lado). */
public class PipeFilterScreen extends BCScreen<PipeFilterMenu> {
    public PipeFilterScreen(PipeFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "pipe_filter.png", 175, 225);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, 8, 6, 0xFF404040, false);
    }
}
