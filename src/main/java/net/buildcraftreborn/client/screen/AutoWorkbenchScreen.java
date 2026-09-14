package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.factory.AutoWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Tela da bancada automática (textura original do BuildCraft), com a seta enchendo enquanto fabrica. */
public class AutoWorkbenchScreen extends BCScreen<AutoWorkbenchMenu> {
    private static final int ARROW_X = 90;
    private static final int ARROW_Y = 47;
    private static final int ARROW_WIDTH = 23;
    private static final int ARROW_HEIGHT = 10;

    public AutoWorkbenchScreen(AutoWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "auto_workbench.png", 176, 197);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        double progress = this.menu.progress();
        if (progress < 0) return;
        // a seta branca do próprio BuildCraft (u=176, v=0) enche da esquerda para a direita
        int width = (int) Math.round(ARROW_WIDTH * progress);
        if (width > 0) blitPart(graphics, ARROW_X, ARROW_Y, 176, 0, width, ARROW_HEIGHT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, 8, 6, 0xFF404040, false);
    }
}
