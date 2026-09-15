package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.energy.engine.CombustionEngine;
import net.buildcraftreborn.energy.engine.CombustionEngineMenu;
import net.buildcraftreborn.energy.engine.EngineBlockEntity;
import net.buildcraftreborn.energy.engine.EngineStage;
import net.buildcraftreborn.lib.fluid.BCTank;
import net.craftenergy.api.EnergyUnits;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** Tela do motor a combustão (textura original): combustível + refrigerante → resíduo. */
public class CombustionEngineScreen extends BCScreen<CombustionEngineMenu> {
    private static final int[] TANK_X = {26, 80, 134};
    private static final String[] TANK_KEYS = {"fuel", "coolant", "residue"};
    private static final int TANK_Y = 18;
    private static final int TANK_WIDTH = 16;
    private static final int TANK_HEIGHT = 60;

    public CombustionEngineScreen(CombustionEngineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "combustion_engine.png", 176, 177);
    }

    private @Nullable CombustionEngine engine() {
        Level level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(this.menu.pos()) instanceof EngineBlockEntity engine ? engine.combustion() : null;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        CombustionEngine engine = engine();
        if (engine == null) return;
        BCTank[] tanks = {engine.fuelTank(), engine.coolantTank(), engine.residueTank()};
        for (int i = 0; i < tanks.length; i++) {
            BCTank tank = tanks[i];
            drawFluid(graphics, tank.variant, TANK_X[i], TANK_Y, TANK_WIDTH, TANK_HEIGHT, tank.ratio());
            blitPart(graphics, TANK_X[i], TANK_Y, 176, 0, TANK_WIDTH, TANK_HEIGHT);
            if (isHovering(TANK_X[i], TANK_Y, TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY)) {
                Component content = tank.isEmpty() ? Component.translatable("gui.buildcraftreborn.tank.empty")
                        : FluidVariantAttributes.getName(tank.variant).copy()
                        .append(": " + tank.amountCL() + " / " + tank.capacityCL() + " CL");
                graphics.setComponentTooltipForNextFrame(this.font, List.of(
                        Component.translatable("gui.buildcraftreborn.engine." + TANK_KEYS[i]), content), mouseX, mouseY);
            }
        }
        // entre os tanques (o "+" e a seta): potência e calor
        boolean overPlus = isHovering(TANK_X[0] + TANK_WIDTH, TANK_Y, TANK_X[1] - TANK_X[0] - TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY);
        boolean overArrow = isHovering(TANK_X[1] + TANK_WIDTH, TANK_Y, TANK_X[2] - TANK_X[1] - TANK_WIDTH, TANK_HEIGHT, mouseX, mouseY);
        if (overPlus || overArrow) {
            EngineStage stage = this.menu.stage();
            graphics.setComponentTooltipForNextFrame(this.font, List.of(
                    Component.translatable("gui.buildcraftreborn.engine.power", EnergyUnits.formatPower(this.menu.power())),
                    Component.translatable("gui.buildcraftreborn.engine.heat", heatText()),
                    Component.translatable("gui.buildcraftreborn.engine.stage." + stage.name).withStyle(stage.color)), mouseX, mouseY);
        }
    }

    private String heatText() {
        return String.format(Locale.ROOT, "%.1f CCº", this.menu.heat());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, 8, 6, 0xFF404040, false);
        graphics.text(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0xFF404040, false);
        EngineStage stage = this.menu.stage();
        Component heat = Component.literal(String.format(Locale.ROOT, "%.0f CCº", this.menu.heat())).withStyle(stage.color);
        graphics.text(this.font, heat, this.imageWidth - 8 - this.font.width(heat), 6, 0xFF404040, false);
    }
}
