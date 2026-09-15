package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.builders.FillerMenu;
import net.buildcraftreborn.builders.filler.FillerPattern;
import net.buildcraftreborn.builders.tile.FillerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Tela do preenchedor (textura original): ícone do padrão (clique esquerdo avança, direito volta), 4 botões
 * de parâmetro e os botões de escavar e inverter.
 */
public class FillerScreen extends BCScreen<FillerMenu> {
    private static final int PATTERN_X = 12;
    private static final int PATTERN_Y = 32;
    private static final int PARAM_X = 53;
    private static final int PARAM_Y = 39;
    private static final int EXCAVATE_X = 130;
    private static final int INVERT_X = 152;
    private static final int BUTTON_Y = 40;

    public FillerScreen(FillerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "filler.png", 176, 241);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        FillerPattern pattern = this.menu.pattern();
        graphics.blit(RenderPipelines.GUI_TEXTURED, BuildCraftReborn.id("textures/gui/filler_patterns/" + pattern.id + ".png"),
                this.leftPos + PATTERN_X + 8, this.topPos + PATTERN_Y + 8, 0, 0, 16, 16, 16, 16);
        button(graphics, EXCAVATE_X, 192, this.menu.excavate(), mouseX, mouseY);
        button(graphics, INVERT_X, 224, this.menu.inverted(), mouseX, mouseY);

        if (isHovering(PATTERN_X, PATTERN_Y, 32, 32, mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("gui.buildcraftreborn.filler.pattern." + pattern.id),
                    Component.translatable("gui.buildcraftreborn.filler.click").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
        for (int i = 0; i < pattern.params.length; i++) {
            if (!isHovering(PARAM_X + i * 18, PARAM_Y, 18, 18, mouseX, mouseY)) continue;
            FillerPattern.ParamType type = pattern.params[i];
            String value = type.values[Math.floorMod(this.menu.param(i), type.values.length)];
            graphics.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("gui.buildcraftreborn.filler.param." + type.id()),
                    Component.translatable("gui.buildcraftreborn.filler.param." + type.id() + "." + value).withStyle(ChatFormatting.YELLOW),
                    Component.translatable("gui.buildcraftreborn.filler.click").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
        if (isHovering(EXCAVATE_X, BUTTON_Y, 16, 16, mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(this.font, List.of(Component.translatable(
                    "gui.buildcraftreborn.filler.excavate." + (this.menu.excavate() ? "on" : "off"))), mouseX, mouseY);
        }
        if (isHovering(INVERT_X, BUTTON_Y, 16, 16, mouseX, mouseY)) {
            graphics.setComponentTooltipForNextFrame(this.font, List.of(Component.translatable(
                    "gui.buildcraftreborn.filler.invert." + (this.menu.inverted() ? "on" : "off"))), mouseX, mouseY);
        }
    }

    /** Botão de 4 estados da textura: normal, sob o mouse, ligado e ligado sob o mouse. */
    private void button(GuiGraphicsExtractor graphics, int x, int u, boolean active, int mouseX, int mouseY) {
        boolean hovered = isHovering(x, BUTTON_Y, 16, 16, mouseX, mouseY);
        blitPart(graphics, x, BUTTON_Y, u + (active ? 16 : 0), hovered ? 16 : 0, 16, 16);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 10, 0xFF404040, false);
        graphics.text(this.font, Component.translatable("gui.buildcraftreborn.filler.resources"), 7, 74, 0xFF404040, false);
        graphics.text(this.font, this.playerInventoryTitle, 7, 141, 0xFF404040, false);
        FillerPattern pattern = this.menu.pattern();
        for (int i = 0; i < pattern.params.length; i++) {
            FillerPattern.ParamType type = pattern.params[i];
            String symbol = type.symbols[Math.floorMod(this.menu.param(i), type.symbols.length)];
            graphics.text(this.font, symbol, PARAM_X + i * 18 + 9 - this.font.width(symbol) / 2, PARAM_Y + 5, 0xFF202020, false);
        }
        Component status = this.menu.hasBox()
                ? Component.translatable("gui.buildcraftreborn.filler.status", this.menu.toBreak(), this.menu.toPlace())
                : Component.translatable("gui.buildcraftreborn.filler.no_box");
        graphics.text(this.font, status, 52, 61, this.menu.hasBox() ? 0xFF404040 : 0xFFA02020, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int button = event.button();
        if (button == 0 || button == 1) {
            boolean left = button == 0;
            int id = -1;
            if (isHovering(PATTERN_X, PATTERN_Y, 32, 32, event.x(), event.y())) {
                id = left ? FillerBlockEntity.BUTTON_NEXT_PATTERN : FillerBlockEntity.BUTTON_PREVIOUS_PATTERN;
            }
            for (int i = 0; i < this.menu.pattern().params.length && id < 0; i++) {
                if (isHovering(PARAM_X + i * 18, PARAM_Y, 18, 18, event.x(), event.y())) {
                    id = (left ? FillerBlockEntity.BUTTON_NEXT_PARAM : FillerBlockEntity.BUTTON_PREVIOUS_PARAM) + i;
                }
            }
            if (left && isHovering(EXCAVATE_X, BUTTON_Y, 16, 16, event.x(), event.y())) id = FillerBlockEntity.BUTTON_EXCAVATE;
            if (left && isHovering(INVERT_X, BUTTON_Y, 16, 16, event.x(), event.y())) id = FillerBlockEntity.BUTTON_INVERT;
            if (id >= 0) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
