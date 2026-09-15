package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.transport.FilteredBufferMenu;
import net.buildcraftreborn.transport.tile.FilteredBufferBlockEntity;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Tela do buffer filtrado: slot sem filtro fica riscado; com filtro, mostra o item apagado até chegar algum. */
public class FilteredBufferScreen extends BCScreen<FilteredBufferMenu> {
    private static final Identifier NOTHING = BuildCraftReborn.id("textures/gui/nothing_filtered_buffer_slot.png");
    private static final Identifier EMPTY_FILTER = BuildCraftReborn.id("textures/gui/empty_filtered_buffer_slot.png");

    public FilteredBufferScreen(FilteredBufferMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "filtered_buffer.png", 176, 169);
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 75;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        for (int i = 0; i < FilteredBufferBlockEntity.SLOTS; i++) {
            int x = this.leftPos + 8 + 18 * i;
            ItemStack filter = this.menu.filter(i);
            if (filter.isEmpty()) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, EMPTY_FILTER, x, this.topPos + 27, 0, 0, 16, 16, 16, 16);
                graphics.blit(RenderPipelines.GUI_TEXTURED, NOTHING, x, this.topPos + 61, 0, 0, 16, 16, 16, 16);
            } else if (this.menu.getSlot(FilteredBufferBlockEntity.SLOTS + i).getItem().isEmpty()) {
                graphics.item(filter, x, this.topPos + 61);
                graphics.fill(x, this.topPos + 61, x + 16, this.topPos + 77, 0xA08B8B8B);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 10, 0xFF404040, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }
}
