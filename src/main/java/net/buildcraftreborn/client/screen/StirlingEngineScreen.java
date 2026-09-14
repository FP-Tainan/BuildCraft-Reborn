package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.energy.engine.EngineStage;
import net.buildcraftreborn.energy.engine.StirlingEngineMenu;
import net.craftenergy.api.EnergyUnits;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Tela do motor Stirling (textura original do BuildCraft): chama queimando, potência e calor. */
public class StirlingEngineScreen extends BCScreen<StirlingEngineMenu> {
    private static final int FLAME_X = 81;
    private static final int FLAME_Y = 25;
    private static final int FLAME_SIZE = 14;

    public StirlingEngineScreen(StirlingEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "stirling_engine.png", 176, 166);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int height = (int) Math.ceil(FLAME_SIZE * this.menu.burnRatio());
        if (height > 0) {
            blitPart(graphics, FLAME_X, FLAME_Y + FLAME_SIZE - height, 176, FLAME_SIZE - height, FLAME_SIZE, height);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, 8, 6, 0xFF404040, false);
        graphics.text(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0xFF404040, false);
        EngineStage stage = this.menu.stage();
        graphics.text(this.font, Component.translatable("gui.buildcraftreborn.engine.power", EnergyUnits.formatPower(this.menu.power())),
                104, 30, 0xFF404040, false);
        graphics.text(this.font, Component.translatable("gui.buildcraftreborn.engine.stage." + stage.name).withStyle(stage.color),
                104, 44, 0xFF404040, false);
    }
}
