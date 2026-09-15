package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.silicon.AdvancedCraftingTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Tela da mesa de trabalho avançada; a barra da direita enche com a energia dos lasers. */
public class AdvancedCraftingTableScreen extends BCScreen<AdvancedCraftingTableMenu> {
    private static final int BAR_X = 164;
    private static final int BAR_Y = 7;
    private static final int BAR_HEIGHT = 70;

    public AdvancedCraftingTableScreen(AdvancedCraftingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "advanced_crafting_table.png", 176, 241);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        double progress = this.menu.progress();
        if (progress > 0) {
            int height = (int) Math.round(BAR_HEIGHT * Math.min(1.0, progress));
            blitPart(graphics, BAR_X, BAR_Y + BAR_HEIGHT - height, 176, BAR_HEIGHT - height, 4, height);
        }
        if (isHovering(BAR_X, BAR_Y, 4, BAR_HEIGHT, mouseX, mouseY)) {
            Component line = progress < 0 ? Component.translatable("gui.buildcraftreborn.laser_table.idle")
                    : Component.translatable("gui.buildcraftreborn.laser_table.progress", (int) Math.round(progress * 100));
            graphics.setComponentTooltipForNextFrame(this.font, List.of(line), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.playerInventoryTitle, 8, 142, 0xFF404040, false);
    }
}
