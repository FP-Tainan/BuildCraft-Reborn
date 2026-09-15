package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.builders.LibraryMenu;
import net.buildcraftreborn.builders.snapshot.LibraryStore;
import net.buildcraftreborn.builders.tile.LibraryBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/**
 * Tela da biblioteca eletrônica (textura original): lista das plantas guardadas (roda do mouse rola), setas de
 * baixar e enviar e o botão de apagar (só no criativo).
 */
public class LibraryScreen extends BCScreen<LibraryMenu> {
    private static final int LIST_X = 8;
    private static final int LIST_Y = 22;
    private static final int LIST_WIDTH = 154;
    private static final int ROW_HEIGHT = 9;
    private static final int ROWS = 11;

    private int scroll;
    private Button delete;

    public LibraryScreen(LibraryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "electronic_library.png", 244, 220);
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 127;
    }

    @Override
    protected void init() {
        super.init();
        this.delete = addRenderableWidget(Button.builder(Component.translatable("gui.buildcraftreborn.library.delete"),
                button -> click(LibraryBlockEntity.BUTTON_DELETE)).bounds(this.leftPos + 174, this.topPos + 106, 62, 18).build());
    }

    private List<LibraryStore.Entry> entries() {
        return this.minecraft != null && this.minecraft.level != null
                && this.minecraft.level.getBlockEntity(this.menu.pos()) instanceof LibraryBlockEntity library ? library.entries() : List.of();
    }

    private void click(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int down = this.menu.progressDown();
        if (down >= 0) {
            int width = Math.max(1, down * 22 / LibraryBlockEntity.DURATION);
            blitPart(graphics, 194 + 22 - width, 58, 234 + 22 - width, 240, width, 16);
        }
        int up = this.menu.progressUp();
        if (up >= 0) blitPart(graphics, 194, 79, 234, 224, Math.max(1, up * 22 / LibraryBlockEntity.DURATION), 16);

        List<LibraryStore.Entry> entries = entries();
        this.scroll = Math.clamp(this.scroll, 0, Math.max(0, entries.size() - ROWS));
        for (int row = 0; row < ROWS && this.scroll + row < entries.size(); row++) {
            int index = this.scroll + row;
            LibraryStore.Entry entry = entries.get(index);
            int left = this.leftPos + LIST_X;
            int top = this.topPos + LIST_Y + row * ROW_HEIGHT;
            boolean hovered = isHovering(LIST_X, LIST_Y + row * ROW_HEIGHT, LIST_WIDTH, ROW_HEIGHT, mouseX, mouseY);
            if (index == this.menu.selected()) graphics.fill(left, top, left + LIST_WIDTH, top + ROW_HEIGHT, 0xFF555555);
            String name = entry.name().isEmpty() ? entry.hash().substring(0, 12) : entry.name();
            String prefix = "blueprint".equals(entry.type()) ? "▣ " : "▢ ";
            graphics.text(this.font, this.font.plainSubstrByWidth(prefix + name, LIST_WIDTH - 4), left + 2, top + 1,
                    hovered ? 0xFFFFFFA0 : 0xFFE0E0E0, false);
            if (hovered) {
                graphics.setComponentTooltipForNextFrame(this.font, List.of(Component.literal(name),
                        Component.translatable("item.buildcraftreborn." + entry.type()).withStyle(ChatFormatting.GRAY),
                        Component.translatable("gui.buildcraftreborn.library.author", entry.author()).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            }
        }
        this.delete.active = this.menu.selected() >= 0 && this.minecraft != null && this.minecraft.player != null
                && this.minecraft.player.getAbilities().instabuild;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, 8, 8, 0xFF404040, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            List<LibraryStore.Entry> entries = entries();
            for (int row = 0; row < ROWS && this.scroll + row < entries.size(); row++) {
                if (isHovering(LIST_X, LIST_Y + row * ROW_HEIGHT, LIST_WIDTH, ROW_HEIGHT, event.x(), event.y())) {
                    click(this.scroll + row);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isHovering(LIST_X, LIST_Y, LIST_WIDTH, ROWS * ROW_HEIGHT, mouseX, mouseY)) {
            this.scroll -= (int) Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
