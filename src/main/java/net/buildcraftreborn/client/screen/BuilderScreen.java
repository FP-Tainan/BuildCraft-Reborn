package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.builders.BuilderMenu;
import net.buildcraftreborn.builders.tile.BuilderBlockEntity;
import net.buildcraftreborn.lib.fluid.BCTank;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

/** Tela do construtor: textura base mais o painel lateral da planta (lista do que falta e tanques). */
public class BuilderScreen extends BCScreen<BuilderMenu> {
    private static final Identifier PANEL = BuildCraftReborn.id("textures/gui/builder_blueprint.png");

    public BuilderScreen(BuilderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "builder.png", 256, 222);
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 129;
    }

    private BuilderBlockEntity builder() {
        return this.minecraft != null && this.minecraft.level != null
                && this.minecraft.level.getBlockEntity(this.menu.pos()) instanceof BuilderBlockEntity builder ? builder : null;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL, this.leftPos + 169, this.topPos, 169, 0, 87, 222, 256, 256);
        BuilderBlockEntity builder = builder();
        for (int i = 0; i < BuilderBlockEntity.TANKS; i++) {
            int x = 179 + i * 18;
            if (builder != null) {
                BCTank tank = builder.tank(i);
                drawFluid(graphics, tank.variant, x, 145, 16, 47, tank.ratio());
                if (isHovering(x, 145, 16, 47, mouseX, mouseY)) {
                    Component name = tank.isEmpty() ? Component.translatable("gui.buildcraftreborn.tank.empty") : FluidVariantAttributes.getName(tank.variant);
                    graphics.setComponentTooltipForNextFrame(this.font, List.of(name,
                            Component.literal(tank.amountCL() + " / " + BuilderBlockEntity.TANK_CL + " CL").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
                }
            }
            graphics.blit(RenderPipelines.GUI_TEXTURED, PANEL, this.leftPos + x, this.topPos + 145, 0, 54, 16, 47, 256, 256);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, (176 - this.font.width(this.title)) / 2, 6, 0xFF404040, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
        Component status;
        if (!this.menu.hasSnapshot()) {
            status = Component.translatable("gui.buildcraftreborn.builder.no_snapshot");
        } else if (this.menu.done()) {
            status = Component.translatable("gui.buildcraftreborn.builder.done");
        } else {
            status = Component.translatable("gui.buildcraftreborn.filler.status", this.menu.toBreak(), this.menu.toPlace());
        }
        graphics.text(this.font, status, (176 - this.font.width(status)) / 2, 50, 0xFF404040, false);
        graphics.text(this.font, Component.translatable("gui.buildcraftreborn.builder.missing"), 179, 6, 0xFF404040, false);
    }
}
