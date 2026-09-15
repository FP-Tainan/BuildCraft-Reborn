package net.buildcraftreborn.client.screen;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.transport.gate.GateLogic;
import net.buildcraftreborn.transport.gate.GateMenu;
import net.buildcraftreborn.transport.gate.GateVariant;
import net.buildcraftreborn.transport.gate.Statement;
import net.buildcraftreborn.transport.gate.Statements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Tela da porta lógica (textura {@code gate_interface}): uma linha por slot (duas colunas acima de 4), cada
 * uma com gatilho, parâmetros, lâmpadas e ação. Clique esquerdo avança, direito volta, com shift limpa; nos
 * parâmetros de item o esquerdo usa o item do cursor. Entre duas linhas, o conector liga os slots num grupo.
 */
public class GateScreen extends AbstractContainerScreen<GateMenu> {
    private static final Identifier TEXTURE = BuildCraftReborn.id("textures/gui/gate.png");
    private static final Identifier SIDE_ONLY = BuildCraftReborn.id("textures/gui/statements/redstone_gate_side_only.png");

    private final int rows;
    private final int columns;
    private final int triggerParams;
    private final int actionParams;
    private final int pairWidth;
    private final int start;

    public GateScreen(GateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 117 + menu.rows() * 18);
        GateVariant variant = menu.variant();
        this.rows = menu.rows();
        this.columns = variant.twoColumns() ? 2 : 1;
        this.triggerParams = variant.triggerParams();
        this.actionParams = variant.actionParams();
        this.pairWidth = 18 * (3 + this.triggerParams + this.actionParams);
        this.start = (162 - (this.pairWidth + (this.columns == 2 ? this.pairWidth + 18 : 0))) / 2;
        this.inventoryLabelY = 16 + this.rows * 18 + 4;
        this.inventoryLabelX = 5;
    }

    private int pairX(int column) {
        return this.start + 7 + column * (18 + this.pairWidth);
    }

    private int connectorOffset() {
        return 18 * (1 + this.triggerParams);
    }

    private int actionOffset() {
        return 18 * (2 + this.triggerParams);
    }

    private void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos + x, this.topPos + y, u, v, width, height, 256, 256);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        blit(graphics, 0, 0, 0, 0, 176, 16);
        for (int row = 0; row < this.rows; row++) blit(graphics, 0, 16 + row * 18, 0, 23, 176, 18);
        blit(graphics, 0, 16 + this.rows * 18, 0, 48, 176, 101);

        GateLogic gate = this.menu.gate();
        if (gate == null) return;
        for (int slot = 0; slot < gate.slots(); slot++) {
            int column = slot / this.rows;
            int row = slot % this.rows;
            int x = pairX(column);
            int y = 16 + row * 18;
            statementSlot(graphics, x, y, gate.trigger(slot));
            for (int k = 0; k < this.triggerParams; k++) paramSlot(graphics, x + 18 * (k + 1), y, gate.trigger(slot), gate.triggerParam(slot, k));
            int cp = connectorOffset();
            boolean triggerOn = gate.triggerOn(slot);
            boolean actionOn = gate.actionOn(slot);
            blit(graphics, x + cp, y, 176 + (triggerOn ? 18 : 0), 18, 7, 18);
            blit(graphics, x + cp + 7, y, 180 + (actionOn ? 18 : 0), 18, 4, 18);
            blit(graphics, x + cp + 11, y, 187 + (actionOn ? 18 : 0), 18, 7, 18);
            int ax = x + actionOffset();
            statementSlot(graphics, ax, y, gate.action(slot));
            for (int k = 0; k < this.actionParams; k++) paramSlot(graphics, ax + 18 * (k + 1), y, gate.action(slot), gate.actionParam(slot, k));

            if (row < this.rows - 1 && slot + 1 < gate.slots()) {
                boolean connected = gate.connected(slot);
                int cy = 25 + row * 18;
                blit(graphics, x + cp, cy, 176 + (actionOn ? 18 : 0), 36 + (connected ? 18 : 0), 18, 9);
                blit(graphics, x + cp, cy + 9, 176 + (gate.actionOn(slot + 1) ? 18 : 0), 45 + (connected ? 18 : 0), 18, 9);
            }
        }
        tooltips(graphics, gate, mouseX, mouseY);
    }

    /** Moldura de slot no estilo do inventário, com o ícone do gatilho ou da ação. */
    private void frame(GuiGraphicsExtractor graphics, int x, int y) {
        int left = this.leftPos + x;
        int top = this.topPos + y;
        graphics.fill(left, top, left + 18, top + 18, 0xFF373737);
        graphics.fill(left + 1, top + 1, left + 18, top + 18, 0xFFFFFFFF);
        graphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFF8B8B8B);
    }

    private void statementSlot(GuiGraphicsExtractor graphics, int x, int y, GateLogic.Choice choice) {
        frame(graphics, x, y);
        Statement statement = Statements.get(choice.id());
        if (statement == null) return;
        graphics.blit(RenderPipelines.GUI_TEXTURED, statement.iconTexture(), this.leftPos + x + 1, this.topPos + y + 1, 0, 0, 16, 16, 16, 16);
    }

    private void paramSlot(GuiGraphicsExtractor graphics, int x, int y, GateLogic.Choice choice, GateLogic.Param param) {
        Statement statement = Statements.get(choice.id());
        boolean usable = statement != null && statement.paramType() != Statement.ParamType.NONE;
        frame(graphics, x, y);
        int left = this.leftPos + x;
        int top = this.topPos + y;
        if (!usable) {
            graphics.fill(left + 1, top + 1, left + 17, top + 17, 0xFF5A5A5A);
            return;
        }
        if (!param.item().isEmpty()) graphics.item(param.item(), left + 1, top + 1);
        if (param.flag()) graphics.blit(RenderPipelines.GUI_TEXTURED, SIDE_ONLY, left + 1, top + 1, 0, 0, 16, 16, 16, 16);
    }

    private void tooltips(GuiGraphicsExtractor graphics, GateLogic gate, int mouseX, int mouseY) {
        for (int slot = 0; slot < gate.slots(); slot++) {
            int x = pairX(slot / this.rows);
            int y = 16 + (slot % this.rows) * 18;
            if (isHovering(x, y, 18, 18, mouseX, mouseY)) statementTooltip(graphics, gate.trigger(slot), mouseX, mouseY);
            if (isHovering(x + actionOffset(), y, 18, 18, mouseX, mouseY)) statementTooltip(graphics, gate.action(slot), mouseX, mouseY);
            for (int k = 0; k < this.triggerParams; k++) {
                if (isHovering(x + 18 * (k + 1), y, 18, 18, mouseX, mouseY)) paramTooltip(graphics, gate.trigger(slot), gate.triggerParam(slot, k), mouseX, mouseY);
            }
            for (int k = 0; k < this.actionParams; k++) {
                if (isHovering(x + actionOffset() + 18 * (k + 1), y, 18, 18, mouseX, mouseY)) paramTooltip(graphics, gate.action(slot), gate.actionParam(slot, k), mouseX, mouseY);
            }
        }
    }

    private void statementTooltip(GuiGraphicsExtractor graphics, GateLogic.Choice choice, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        Statement statement = Statements.get(choice.id());
        if (statement == null) {
            lines.add(Component.translatable("gate.buildcraftreborn.empty").withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(statement.name());
            if (choice.face() != null) {
                lines.add(Component.translatable("gate.buildcraftreborn.face",
                        Component.translatable("direction.buildcraftreborn." + choice.face().getName())).withStyle(ChatFormatting.GRAY));
            }
        }
        lines.add(Component.translatable("gate.buildcraftreborn.click").withStyle(ChatFormatting.DARK_GRAY));
        graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
    }

    private void paramTooltip(GuiGraphicsExtractor graphics, GateLogic.Choice choice, GateLogic.Param param, int mouseX, int mouseY) {
        Statement statement = Statements.get(choice.id());
        if (statement == null || statement.paramType() == Statement.ParamType.NONE) return;
        List<Component> lines = new ArrayList<>();
        if (statement.paramType() == Statement.ParamType.GATE_SIDE) {
            lines.add(Component.translatable(param.flag() ? "gate.buildcraftreborn.param.side_only" : "gate.buildcraftreborn.param.all_sides"));
        } else {
            lines.add(param.item().isEmpty() ? Component.translatable("gate.buildcraftreborn.param.any") : param.item().getHoverName());
            lines.add(Component.translatable("gate.buildcraftreborn.param.click").withStyle(ChatFormatting.DARK_GRAY));
        }
        graphics.setComponentTooltipForNextFrame(this.font, lines, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 5, 0xFF404040, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int button = event.button();
        GateLogic gate = this.menu.gate();
        if (gate != null && (button == 0 || button == 1)) {
            int op = event.hasShiftDown() ? GateLogic.OP_CLEAR : button == 0 ? GateLogic.OP_NEXT : GateLogic.OP_PREVIOUS;
            int id = -1;
            for (int slot = 0; slot < gate.slots() && id < 0; slot++) {
                int x = pairX(slot / this.rows);
                int row = slot % this.rows;
                int y = 16 + row * 18;
                if (isHovering(x, y, 18, 18, event.x(), event.y())) id = GateMenu.buttonId(slot, GateLogic.FIELD_TRIGGER, op);
                if (isHovering(x + actionOffset(), y, 18, 18, event.x(), event.y())) id = GateMenu.buttonId(slot, GateLogic.FIELD_ACTION, op);
                for (int k = 0; k < this.triggerParams; k++) {
                    if (isHovering(x + 18 * (k + 1), y, 18, 18, event.x(), event.y())) id = GateMenu.buttonId(slot, GateLogic.FIELD_TRIGGER_PARAM + k, op);
                }
                for (int k = 0; k < this.actionParams; k++) {
                    if (isHovering(x + actionOffset() + 18 * (k + 1), y, 18, 18, event.x(), event.y())) {
                        id = GateMenu.buttonId(slot, GateLogic.FIELD_ACTION_PARAM + k, op);
                    }
                }
                if (row < this.rows - 1 && slot + 1 < gate.slots() && isHovering(x + connectorOffset(), 25 + row * 18, 18, 18, event.x(), event.y())) {
                    id = GateMenu.buttonId(slot, GateLogic.FIELD_CONNECTION, op);
                }
            }
            if (id >= 0) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
