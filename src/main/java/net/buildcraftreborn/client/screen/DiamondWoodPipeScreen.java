package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.transport.DiamondWoodPipeMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Tela do tubo de madeira-diamante (texturas originais): filtros, três botões de modo e o marcador do rodízio. */
public class DiamondWoodPipeScreen extends BCScreen<DiamondWoodPipeMenu> {
    private static final Identifier BUTTONS = BuildCraftReborn.id("textures/gui/pipe_emerald_button.png");
    private static final int[] BUTTON_X = {7, 25, 43};
    private static final int BUTTON_Y = 41;
    private static final int[] ICON_U = {19, 37, 55};
    private static final String[] MODES = {"whitelist", "blacklist", "roundrobin"};

    public DiamondWoodPipeScreen(DiamondWoodPipeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "pipe_emerald.png", 175, 161);
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 68;
    }

    /** Tubo de fluido: sem rodízio. */
    private int buttons() {
        return this.minecraft != null && this.minecraft.level != null
                && this.minecraft.level.getBlockEntity(this.menu.pos()) instanceof net.buildcraftreborn.transport.tile.FluidPipeBlockEntity ? 2 : 3;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        for (int i = 0; i < buttons(); i++) {
            boolean hovered = isHovering(BUTTON_X[i], BUTTON_Y, 18, 18, mouseX, mouseY);
            boolean active = this.menu.mode() == i;
            int state = active ? (hovered ? 4 : 3) : (hovered ? 2 : 1);
            int x = this.leftPos + BUTTON_X[i];
            int y = this.topPos + BUTTON_Y;
            graphics.blit(RenderPipelines.GUI_TEXTURED, BUTTONS, x, y, state * 18, 0, 18, 18, 256, 256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, BUTTONS, x + 1, y + 1, ICON_U[i], 19, 16, 16, 256, 256);
            if (hovered) {
                graphics.setComponentTooltipForNextFrame(this.font, List.of(Component.translatable("gui.buildcraftreborn.pipe_emerald." + MODES[i])), mouseX, mouseY);
            }
        }
        if (this.menu.mode() == 2) {
            if (this.menu.filterValid()) {
                blitPart(graphics, 6 + 18 * this.menu.currentFilter(), 16, 176, 0, 20, 20);
            } else {
                blitPart(graphics, 6, 16, 176, 20, 20, 20);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component title = Component.translatable("gui.buildcraftreborn.pipe_emerald.title");
        graphics.text(this.font, title, (this.imageWidth - this.font.width(title)) / 2, 6, 0xFF404040, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            for (int i = 0; i < buttons(); i++) {
                if (isHovering(BUTTON_X[i], BUTTON_Y, 18, 18, event.x(), event.y())) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, i);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
