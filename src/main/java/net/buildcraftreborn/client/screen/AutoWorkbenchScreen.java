package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.factory.AutoWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Tela da bancada automática (textura original do BuildCraft), com a barra de energia da próxima fabricação. */
public class AutoWorkbenchScreen extends BCScreen<AutoWorkbenchMenu> {
    public AutoWorkbenchScreen(AutoWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "auto_workbench.png", 176, 197);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int width = (int) Math.round(22 * this.menu.progress());
        graphics.fill(this.leftPos + 90, this.topPos + 50, this.leftPos + 112, this.topPos + 52, 0xFF3A3A3A);
        if (width > 0) graphics.fill(this.leftPos + 90, this.topPos + 50, this.leftPos + 90 + width, this.topPos + 52, 0xFF2FB02F);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, 8, 6, 0xFF404040, false);
    }
}
