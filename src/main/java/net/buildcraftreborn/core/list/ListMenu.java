package net.buildcraftreborn.core.list;

import io.netty.buffer.ByteBuf;
import net.buildcraftreborn.lib.menu.BCMenu;
import net.buildcraftreborn.lib.menu.PhantomSlot;
import net.buildcraftreborn.registry.BCMenus;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Tela da lista: duas linhas de nove slots fantasmas (a lista guarda cópias, não os itens) e três
 * botões por linha. Tudo é gravado direto no item que está na mão.
 */
public class ListMenu extends BCMenu {
    public static final StreamCodec<ByteBuf, InteractionHand> HAND_CODEC =
            ByteBufCodecs.BOOL.map(off -> off ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND, hand -> hand == InteractionHand.OFF_HAND);
    public static final int SLOT_X = 8;
    public static final int SLOT_Y = 32;
    public static final int LINE_SPACING = 34;

    private final Player player;
    private final InteractionHand hand;
    /** Enquanto os slots são preenchidos a partir do item, nada é gravado de volta. */
    private boolean loading = true;
    private final SimpleContainer ghosts = new SimpleContainer(ListContents.LINES * ListContents.WIDTH) {
        @Override
        public void setChanged() {
            super.setChanged();
            if (!ListMenu.this.loading) save();
        }
    };

    public ListMenu(int containerId, Inventory inventory, InteractionHand hand) {
        super(BCMenus.LIST.get(), containerId);
        this.player = inventory.player;
        this.hand = hand;

        ListContents contents = ListItem.contents(list());
        for (int line = 0; line < ListContents.LINES; line++) {
            for (int col = 0; col < ListContents.WIDTH; col++) {
                int index = line * ListContents.WIDTH + col;
                this.ghosts.setItem(index, contents.line(line).stack(col).copy());
                addSlot(new PhantomSlot(this.ghosts, index, SLOT_X + col * 18, SLOT_Y + line * LINE_SPACING));
            }
        }
        this.loading = false;
        addPlayerInventory(inventory, 8, 103);
    }

    public ItemStack list() {
        return this.player.getItemInHand(this.hand);
    }

    public ListContents contents() {
        return ListItem.contents(list());
    }

    private void save() {
        if (this.player.level().isClientSide() || !(list().getItem() instanceof ListItem)) return;
        ListContents contents = contents();
        for (int line = 0; line < ListContents.LINES; line++) {
            ListContents.Line updated = contents.line(line);
            for (int col = 0; col < ListContents.WIDTH; col++) {
                updated = updated.withStack(col, this.ghosts.getItem(line * ListContents.WIDTH + col));
            }
            contents = contents.withLine(line, updated);
        }
        ListItem.setContents(list(), contents);
    }

    /** Botão {@code linha × 3 + opção}: liga ou desliga "exato", "mesmo tipo" ou "mesmo material". */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        int line = id / ListContents.OPTIONS;
        int option = id % ListContents.OPTIONS;
        if (id < 0 || line >= ListContents.LINES || !(list().getItem() instanceof ListItem)) return false;
        ListContents contents = contents();
        ListItem.setContents(list(), contents.withLine(line, contents.line(line).toggle(option)));
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return list().getItem() instanceof ListItem;
    }
}
