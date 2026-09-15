package net.buildcraftreborn.builders.tile;

import net.buildcraftreborn.builders.item.SnapshotItem;
import net.buildcraftreborn.builders.snapshot.LibraryStore;
import net.buildcraftreborn.builders.snapshot.Snapshot;
import net.buildcraftreborn.builders.snapshot.SnapshotHeader;
import net.buildcraftreborn.builders.snapshot.SnapshotStore;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.List;

/**
 * Biblioteca eletrônica ({@code TileElectronicLibrary}): "enviar" (embaixo) copia a planta do item para a
 * biblioteca compartilhada entre mundos; "baixar" (em cima) grava a entrada escolhida num molde/planta. Cada
 * cópia leva 2,5 s. A lista vai para o cliente junto com os dados do bloco.
 */
public class LibraryBlockEntity extends BCBlockEntity implements ServerTicking {
    public static final int SLOT_DOWN_IN = 0;
    public static final int SLOT_DOWN_OUT = 1;
    public static final int SLOT_UP_IN = 2;
    public static final int SLOT_UP_OUT = 3;
    public static final int DURATION = 50;
    public static final int DATA_DOWN = 0;
    public static final int DATA_UP = 1;
    public static final int DATA_SELECTED = 2;
    public static final int DATA_COUNT = 3;
    public static final int BUTTON_DELETE = 1000;
    private static final int REFRESH_INTERVAL = 100;

    private final SimpleContainer inventory = new SimpleContainer(4) {
        @Override
        public void setChanged() {
            super.setChanged();
            LibraryBlockEntity.this.setChanged();
        }
    };
    private List<LibraryStore.Entry> entries = List.of();
    private String selected = "";
    private int progressDown = -1;
    private int progressUp = -1;
    private int refreshTimer;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_DOWN -> LibraryBlockEntity.this.progressDown;
                case DATA_UP -> LibraryBlockEntity.this.progressUp;
                case DATA_SELECTED -> selectedIndex();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public LibraryBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.LIBRARY.get(), pos, state);
    }

    public SimpleContainer inventory() {
        return this.inventory;
    }

    public ContainerData data() {
        return this.data;
    }

    /** Lista vista pelo cliente (ou a do servidor). */
    public List<LibraryStore.Entry> entries() {
        return this.entries;
    }

    private int selectedIndex() {
        for (int i = 0; i < this.entries.size(); i++) {
            if (this.entries.get(i).hash().equals(this.selected)) return i;
        }
        return -1;
    }

    public void select(String hash) {
        this.selected = hash;
        setChanged();
    }

    /** Botões da tela: índice da linha escolhida ou {@link #BUTTON_DELETE}. */
    public void handleButton(int id, boolean creative) {
        if (id == BUTTON_DELETE) {
            if (creative && LibraryStore.delete(this.selected)) {
                this.selected = "";
                refresh();
            }
        } else if (id >= 0 && id < this.entries.size()) {
            select(this.entries.get(id).hash());
        }
    }

    public void refresh() {
        List<LibraryStore.Entry> list = LibraryStore.list();
        if (!list.equals(this.entries)) {
            this.entries = list;
            syncToClient();
        }
    }

    @Override
    public void serverTick() {
        if (!(this.level instanceof ServerLevel server)) return;
        if (++this.refreshTimer >= REFRESH_INTERVAL) {
            this.refreshTimer = 0;
            refresh();
        }
        tickDownload(server);
        tickUpload(server);
    }

    private void tickDownload(ServerLevel server) {
        ItemStack input = this.inventory.getItem(SLOT_DOWN_IN);
        int index = selectedIndex();
        LibraryStore.Entry entry = index < 0 ? null : this.entries.get(index);
        if (!(input.getItem() instanceof SnapshotItem) || !this.inventory.getItem(SLOT_DOWN_OUT).isEmpty() || entry == null) {
            this.progressDown = -1;
            return;
        }
        if (++this.progressDown < DURATION) return;
        this.progressDown = -1;
        Snapshot snapshot = LibraryStore.load(server, entry.hash());
        if (snapshot == null) return;
        String hash = SnapshotStore.put(server, snapshot);
        this.inventory.setItem(SLOT_DOWN_OUT, SnapshotItem.used(new SnapshotHeader(hash, snapshot.type.id(), entry.name(), entry.author())));
        input.shrink(1);
        this.inventory.setChanged();
    }

    private void tickUpload(ServerLevel server) {
        ItemStack input = this.inventory.getItem(SLOT_UP_IN);
        SnapshotHeader header = SnapshotItem.header(input);
        if (header == null || !this.inventory.getItem(SLOT_UP_OUT).isEmpty()) {
            this.progressUp = -1;
            return;
        }
        if (++this.progressUp < DURATION) return;
        this.progressUp = -1;
        Snapshot snapshot = SnapshotStore.get(server, header.hash());
        if (snapshot != null) {
            LibraryStore.save(snapshot, header.name(), header.author());
            this.selected = snapshot.hash();
            refresh();
        }
        this.inventory.setItem(SLOT_UP_OUT, input.copy());
        this.inventory.setItem(SLOT_UP_IN, ItemStack.EMPTY);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) Containers.dropContents(this.level, pos, this.inventory);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.store("Library", LibraryStore.Entry.CODEC.listOf(), this.entries);
        return tag;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int slot = 0; slot < 4; slot++) output.store("Slot" + slot, ItemStack.OPTIONAL_CODEC, this.inventory.getItem(slot));
        output.putString("Selected", this.selected);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int slot = 0; slot < 4; slot++) this.inventory.setItem(slot, input.read("Slot" + slot, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        this.selected = input.getStringOr("Selected", "");
        input.read("Library", LibraryStore.Entry.CODEC.listOf()).ifPresent(list -> this.entries = list);
    }
}
