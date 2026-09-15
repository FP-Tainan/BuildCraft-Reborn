package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.builders.ArchitectTableMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Tela da mesa do arquiteto (textura original): seta de progresso entre a entrada e o resultado. */
public class ArchitectTableScreen extends BCScreen<ArchitectTableMenu> {
    public ArchitectTableScreen(ArchitectTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "architect.png", 256, 166);
        this.inventoryLabelX = 88;
        this.inventoryLabelY = 73;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int progress = this.menu.progress();
        if (progress > 0) blitPart(graphics, 159, 34, 0, 166, Math.max(1, progress * 24 / 1000), 17);
        if (isHovering(135, 35, 16, 16, mouseX, mouseY) && this.menu.getSlot(0).getItem().isEmpty()) {
            graphics.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("gui.buildcraftreborn.architect.input"),
                    Component.translatable("gui.buildcraftreborn.architect.naming").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, 88, 8, 0xFF404040, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
        Component status;
        int color = 0xFF404040;
        if (!this.menu.hasBox()) {
            status = Component.translatable("gui.buildcraftreborn.architect.no_box");
            color = 0xFFA02020;
        } else if (this.menu.progress() >= 0) {
            status = Component.translatable("gui.buildcraftreborn.architect.scanning", this.menu.progress() / 10);
        } else {
            status = Component.translatable("gui.buildcraftreborn.architect.ready");
        }
        graphics.text(this.font, status, 90, 60, color, false);
    }
}
