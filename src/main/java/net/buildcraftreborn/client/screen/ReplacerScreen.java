package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.builders.ReplacerMenu;
import net.buildcraftreborn.builders.item.SchematicItem;
import net.buildcraftreborn.builders.item.SnapshotItem;
import net.buildcraftreborn.builders.snapshot.SnapshotHeader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;

/** Tela do substituidor (textura original): no quadro de cima, a planta e a troca que vai ser feita. */
public class ReplacerScreen extends BCScreen<ReplacerMenu> {
    public ReplacerScreen(ReplacerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "replacer.png", 176, 241);
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 149;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, 12, 13, 0xFFE0E0E0, false);
        SnapshotHeader header = SnapshotItem.header(this.menu.getSlot(0).getItem());
        BlockState from = SchematicItem.state(this.menu.getSlot(1).getItem());
        BlockState to = SchematicItem.state(this.menu.getSlot(2).getItem());
        int y = 28;
        if (header == null) {
            graphics.text(this.font, Component.translatable("gui.buildcraftreborn.replacer.hint"), 12, y, 0xFFB0B0B0, false);
        } else {
            graphics.text(this.font, Component.translatable("item.buildcraftreborn.blueprint.named", header.name()), 12, y, 0xFFFFFFFF, false);
            if (!header.author().isEmpty()) {
                graphics.text(this.font, Component.translatable("gui.buildcraftreborn.library.author", header.author()), 12, y + 11, 0xFFB0B0B0, false);
            }
        }
        if (from != null) graphics.text(this.font, Component.translatable("gui.buildcraftreborn.replacer.from", from.getBlock().getName()), 12, y + 33, 0xFFE0E0E0, false);
        if (to != null) graphics.text(this.font, Component.translatable("gui.buildcraftreborn.replacer.to", to.getBlock().getName()), 12, y + 44, 0xFFE0E0E0, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }
}
