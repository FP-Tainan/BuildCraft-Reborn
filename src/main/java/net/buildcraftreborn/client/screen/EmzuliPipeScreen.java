package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.registry.BCItems;
import net.buildcraftreborn.transport.EmzuliPipeMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Tela do tubo emzuli (textura original): em cada preset, o filtro e o botão de pintura (esquerdo próxima cor,
 * direito anterior, meio ou shift sem cor). Presets ativos por porta lógica ganham um marcador; o atual, o "sempre".
 */
public class EmzuliPipeScreen extends BCScreen<EmzuliPipeMenu> {
    private static final Identifier CURRENT = BuildCraftReborn.id("textures/gui/statements/trigger_true.png");
    private static final int[] PAINT_X = {49, 49, 106, 106};
    private static final int[] PAINT_Y = {19, 47, 19, 47};
    private static final String[] PRESETS = {"red", "green", "blue", "yellow"};

    public EmzuliPipeScreen(EmzuliPipeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "pipe_emzuli.png", 176, 166);
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        for (int i = 0; i < PAINT_X.length; i++) {
            boolean hovered = isHovering(PAINT_X[i], PAINT_Y[i], 20, 20, mouseX, mouseY);
            blitPart(graphics, PAINT_X[i], PAINT_Y[i], 176, hovered ? 20 : 0, 20, 20);
            DyeColor colour = this.menu.presetColour(i);
            if (colour != null) {
                graphics.item(new ItemStack(BCItems.PAINTBRUSHES.get(colour).get()), this.leftPos + PAINT_X[i] + 2, this.topPos + PAINT_Y[i] + 2);
            } else {
                blitPart(graphics, PAINT_X[i] + 2, PAINT_Y[i] + 2, 176, 40, 16, 16);
            }
            int indicatorX = this.leftPos + (i < 2 ? 4 : 155);
            int indicatorY = this.topPos + (i % 2 == 0 ? 21 : 49);
            if (this.menu.currentPreset() == i) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, CURRENT, indicatorX, indicatorY, 0, 0, 16, 16, 16, 16);
            } else if (this.menu.active(i)) {
                graphics.fill(indicatorX + 4, indicatorY + 4, indicatorX + 12, indicatorY + 12, 0xFF606060);
            }
            if (hovered) {
                Component paint = colour == null ? Component.translatable("gui.buildcraftreborn.pipe_emzuli.nopaint")
                        : Component.translatable("gui.buildcraftreborn.pipe_emzuli.paint", Component.translatable("color.minecraft." + colour.getName()));
                graphics.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.translatable("gui.buildcraftreborn.pipe_emzuli.preset." + PRESETS[i]), paint,
                        Component.translatable("gui.buildcraftreborn.pipe_emzuli.click").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Component title = Component.translatable("gui.buildcraftreborn.pipe_emzuli.title");
        graphics.text(this.font, title, (this.imageWidth - this.font.width(title)) / 2, 6, 0xFF404040, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (int i = 0; i < PAINT_X.length; i++) {
            if (!isHovering(PAINT_X[i], PAINT_Y[i], 20, 20, event.x(), event.y())) continue;
            int op = event.button() == 2 || event.hasShiftDown() ? 2 : event.button() == 1 ? 1 : 0;
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, i * 3 + op);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
