package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.core.list.ListContents;
import net.buildcraftreborn.core.list.ListMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Tela da lista (textura original do BuildCraft), com os botões de opção acima de cada linha. */
public class ListScreen extends BCScreen<ListMenu> {
    private static final String[] LETTERS = {"E", "T", "M"};
    private static final String[] OPTION_KEYS = {"precise", "type", "material"};
    private final Button[] buttons = new Button[ListContents.LINES * ListContents.OPTIONS];

    public ListScreen(ListMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "list.png", 176, 191);
    }

    @Override
    protected void init() {
        super.init();
        for (int line = 0; line < ListContents.LINES; line++) {
            for (int option = 0; option < ListContents.OPTIONS; option++) {
                int id = line * ListContents.OPTIONS + option;
                this.buttons[id] = addRenderableWidget(Button.builder(Component.literal(LETTERS[option]),
                                button -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id))
                        .bounds(this.leftPos + 120 + option * 16, this.topPos + 16 + line * ListMenu.LINE_SPACING, 14, 14)
                        .tooltip(Tooltip.create(Component.translatable("gui.buildcraftreborn.list." + OPTION_KEYS[option])))
                        .build());
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        ListContents contents = this.menu.contents();
        for (int line = 0; line < ListContents.LINES; line++) {
            for (int option = 0; option < ListContents.OPTIONS; option++) {
                boolean active = contents.line(line).option(option);
                this.buttons[line * ListContents.OPTIONS + option].setMessage(Component.literal(LETTERS[option])
                        .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, 8, 6, 0xFF404040, false);
    }
}
