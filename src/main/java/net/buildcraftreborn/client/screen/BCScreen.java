package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.BuildCraftReborn;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Base das telas do BuildCraft Reborn: fundo com a textura original e tanques de fluido. */
public abstract class BCScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    private final Identifier texture;

    protected BCScreen(M menu, Inventory inventory, Component title, String texture, int width, int height) {
        super(menu, inventory, title, width, height);
        this.texture = BuildCraftReborn.id("textures/gui/" + texture);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight, 256, 256);
    }

    /** Pedaço da própria textura da tela (barras, chamas, setas de progresso). */
    protected void blitPart(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, this.leftPos + x, this.topPos + y, u, v, width, height, 256, 256);
    }

    /** Fluido subindo de baixo para cima, com a textura "still" repetida e a cor do fluido. */
    protected void drawFluid(GuiGraphicsExtractor graphics, FluidVariant variant, int x, int y, int width, int height, double ratio) {
        int fluidHeight = (int) Math.round(height * Math.clamp(ratio, 0.0, 1.0));
        if (variant.isBlank() || fluidHeight <= 0) return;

        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
                .get(variant.getFluid().defaultFluidState()).stillMaterial().sprite();
        int color = 0xFF000000 | FluidVariantRendering.getColor(variant);
        int left = this.leftPos + x;
        int bottom = this.topPos + y + height;
        int top = bottom - fluidHeight;
        graphics.enableScissor(left, top, left + width, bottom);
        for (int tileY = bottom - width; tileY > top - width; tileY -= width) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, left, tileY, width, width, color);
        }
        graphics.disableScissor();
    }
}
