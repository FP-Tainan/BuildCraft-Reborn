package net.buildcraftreborn.builders.tile;

import net.buildcraftreborn.builders.item.SchematicItem;
import net.buildcraftreborn.builders.item.SnapshotItem;
import net.buildcraftreborn.builders.snapshot.Snapshot;
import net.buildcraftreborn.builders.snapshot.SnapshotHeader;
import net.buildcraftreborn.builders.snapshot.SnapshotStore;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Substituidor ({@code TileReplacer}): com uma planta e dois esquemas de bloco único, troca na planta todo bloco
 * do primeiro esquema pelo do segundo. A planta ganha um arquivo novo (mesmo nome e autor) e os esquemas são
 * gastos. Sem energia e sem automação.
 */
public class ReplacerBlockEntity extends BCBlockEntity implements ServerTicking {
    public static final int SLOT_SNAPSHOT = 0;
    public static final int SLOT_FROM = 1;
    public static final int SLOT_TO = 2;

    private final SimpleContainer inventory = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            ReplacerBlockEntity.this.setChanged();
        }
    };

    public ReplacerBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.REPLACER.get(), pos, state);
    }

    public SimpleContainer inventory() {
        return this.inventory;
    }

    @Override
    public void serverTick() {
        if (!(this.level instanceof ServerLevel server)) return;
        SnapshotHeader header = SnapshotItem.header(this.inventory.getItem(SLOT_SNAPSHOT));
        BlockState from = SchematicItem.state(this.inventory.getItem(SLOT_FROM));
        BlockState to = SchematicItem.state(this.inventory.getItem(SLOT_TO));
        if (header == null || header.snapshotType() != Snapshot.Type.BLUEPRINT || from == null || to == null) return;
        Snapshot snapshot = SnapshotStore.get(server, header.hash());
        if (snapshot == null) return;
        String hash = SnapshotStore.put(server, snapshot.replaced(from.getBlock(), to));
        this.inventory.setItem(SLOT_SNAPSHOT, SnapshotItem.used(new SnapshotHeader(hash, header.type(), header.name(), header.author())));
        this.inventory.setItem(SLOT_FROM, ItemStack.EMPTY);
        this.inventory.setItem(SLOT_TO, ItemStack.EMPTY);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) Containers.dropContents(this.level, pos, this.inventory);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int slot = 0; slot < 3; slot++) output.store("Slot" + slot, ItemStack.OPTIONAL_CODEC, this.inventory.getItem(slot));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int slot = 0; slot < 3; slot++) this.inventory.setItem(slot, input.read("Slot" + slot, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }
}
