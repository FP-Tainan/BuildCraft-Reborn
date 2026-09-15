package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.silicon.AssemblyTableMenu;
import net.buildcraftreborn.silicon.tile.AssemblyTableBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Tela da mesa de montagem: moldura cinza nas receitas marcadas, vermelha nas que têm material e cheia na que
 * está sendo montada; a barra do meio enche com a energia dos lasers.
 */
public class AssemblyTableScreen extends BCScreen<AssemblyTableMenu> {
    private static final int BAR_X = 86;
    private static final int BAR_Y = 36;
    private static final int BAR_HEIGHT = 70;

    public AssemblyTableScreen(AssemblyTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "assembly_table.png", 176, 220);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        for (int i = 0; i < AssemblyTableBlockEntity.RECIPE_SLOTS; i++) {
            if (!this.menu.isSaved(i)) continue;
            int x = 116 + (i % 3) * 18;
            int y = 36 + (i / 3) * 18;
            int v = this.menu.activeIndex() == i ? 32 : this.menu.isCraftable(i) ? 16 : 0;
            blitPart(graphics, x, y, 176, v, 16, 16);
        }
        double progress = this.menu.progress();
        if (progress > 0) {
            int height = (int) Math.round(BAR_HEIGHT * Math.min(1.0, progress));
            blitPart(graphics, BAR_X, BAR_Y + BAR_HEIGHT - height, 176, 48 + BAR_HEIGHT - height, 4, height);
        }
        if (isHovering(BAR_X, BAR_Y, 4, BAR_HEIGHT, mouseX, mouseY)) {
            Component line = progress < 0 ? Component.translatable("gui.buildcraftreborn.laser_table.idle")
                    : Component.translatable("gui.buildcraftreborn.laser_table.progress", (int) Math.round(progress * 100));
            graphics.setComponentTooltipForNextFrame(this.font, List.of(line), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, 8, 6, 0xFF404040, false);
        graphics.text(this.font, this.playerInventoryTitle, 8, 112, 0xFF404040, false);
    }
}
