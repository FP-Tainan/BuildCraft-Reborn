package net.buildcraftreborn.transport.gate;

import io.netty.buffer.ByteBuf;
import net.buildcraftreborn.lib.menu.BCMenu;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.registry.BCMenus;
import net.buildcraftreborn.transport.plug.PlugHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Tela da porta lógica: só o inventário do jogador como slots; os gatilhos e ações vêm do block entity do tubo
 * (sincronizado) e os cliques voltam como botões de menu.
 */
public class GateMenu extends BCMenu {
    public record Target(BlockPos pos, Direction side) {
        public static final StreamCodec<ByteBuf, Target> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Target::pos, Direction.STREAM_CODEC, Target::side, Target::new);
    }

    private final Target target;
    private final Level level;
    private final GateVariant variant;

    public GateMenu(int containerId, Inventory inventory, Target target) {
        super(BCMenus.GATE.get(), containerId);
        this.target = target;
        this.level = inventory.player.level();
        GateLogic gate = gate();
        this.variant = gate != null ? gate.variant() : GateVariant.BASIC;
        addPlayerInventory(inventory, 8, 33 + rows() * 18);
    }

    public static int buttonId(int slot, int field, int op) {
        return (slot << 8) | (field << 2) | op;
    }

    public Target target() {
        return this.target;
    }

    public GateVariant variant() {
        return this.variant;
    }

    public int rows() {
        return this.variant.slots() / (this.variant.twoColumns() ? 2 : 1);
    }

    public @Nullable GateLogic gate() {
        return this.level.getBlockEntity(this.target.pos()) instanceof PlugHolder holder ? holder.plugs().gate(this.target.side()) : null;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.level.isClientSide()) return false;
        GateLogic gate = gate();
        BlockEntity pipe = this.level.getBlockEntity(this.target.pos());
        if (gate == null || pipe == null) return false;
        gate.click(new GateContext(this.level, this.target.pos(), this.target.side(), pipe, gate), id >> 8, (id >> 2) & 63, id & 3, getCarried());
        if (pipe instanceof BCBlockEntity blockEntity) blockEntity.syncToClient();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        BlockPos pos = this.target.pos();
        return gate() != null && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}
