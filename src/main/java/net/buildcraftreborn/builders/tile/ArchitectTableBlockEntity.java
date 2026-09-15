package net.buildcraftreborn.builders.tile;

import net.buildcraftreborn.builders.block.ArchitectTableBlock;
import net.buildcraftreborn.builders.item.SnapshotItem;
import net.buildcraftreborn.builders.snapshot.BlueprintRules;
import net.buildcraftreborn.builders.snapshot.Snapshot;
import net.buildcraftreborn.builders.snapshot.SnapshotHeader;
import net.buildcraftreborn.builders.snapshot.SnapshotStore;
import net.buildcraftreborn.core.marker.MarkerBlock;
import net.buildcraftreborn.core.marker.MarkerBlockEntity;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.CombinedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.FilteringStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Mesa do arquiteto ({@code TileArchitectTable}): pega a caixa dos marcadores de área encostados (atrás dela, de
 * preferência) e escaneia o que está dentro num molde (900 blocos por tick) ou numa planta (300 por tick). Não usa
 * energia. O resultado vai para o arquivo do mundo e o item sai no slot da direita; o nome é o do item em branco
 * renomeado na bigorna.
 */
public class ArchitectTableBlockEntity extends BCBlockEntity implements ServerTicking, ShowsArea {
    public static final int SLOT_IN = 0;
    public static final int SLOT_OUT = 1;
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_FLAGS = 1;
    public static final int DATA_COUNT = 2;
    public static final int FLAG_BOX = 1;

    private final SimpleContainer inventory = new SimpleContainer(2) {
        @Override
        public void setChanged() {
            super.setChanged();
            ArchitectTableBlockEntity.this.setChanged();
        }
    };
    private @Nullable BlockPos min;
    private @Nullable BlockPos max;
    private String author = "";
    private @Nullable Snapshot scanning;
    private int cursor;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progressPermille();
                case DATA_FLAGS -> hasBox() ? FLAG_BOX : 0;
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

    public ArchitectTableBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.ARCHITECT_TABLE.get(), pos, state);
    }

    public SimpleContainer inventory() {
        return this.inventory;
    }

    public ContainerData data() {
        return this.data;
    }

    public boolean hasBox() {
        return this.min != null && this.max != null;
    }

    public void setAuthor(String author) {
        this.author = author;
        setChanged();
    }

    private Direction facing() {
        BlockState state = this.level != null ? this.level.getBlockState(this.worldPosition) : getBlockState();
        return state.hasProperty(ArchitectTableBlock.FACING) ? state.getValue(ArchitectTableBlock.FACING) : Direction.NORTH;
    }

    private int progressPermille() {
        if (this.scanning == null) return -1;
        return (int) ((long) this.cursor * 1000 / Math.max(1, this.scanning.volume()));
    }

    // ── área ──────────────────────────────────────────────────────────────
    /** Caixa dos marcadores atrás da mesa (ou em qualquer lado); os marcadores são recolhidos. */
    public void initArea() {
        if (this.level == null) return;
        Direction behind = facing().getOpposite();
        List<Direction> order = new java.util.ArrayList<>(List.of(Direction.values()));
        order.remove(behind);
        order.addFirst(behind);
        for (Direction direction : order) {
            BlockPos markerPos = this.worldPosition.relative(direction);
            BoundingBox box = MarkerBlockEntity.volumeAt(this.level, markerPos);
            if (box == null) continue;
            MarkerBlockEntity marker = MarkerBlockEntity.at(this.level, markerPos, MarkerBlock.Kind.VOLUME);
            if (marker != null) {
                for (BlockPos member : marker.group()) this.level.destroyBlock(member, true);
            }
            setArea(new BlockPos(box.minX(), box.minY(), box.minZ()), new BlockPos(box.maxX(), box.maxY(), box.maxZ()));
            return;
        }
    }

    public void setArea(BlockPos first, BlockPos second) {
        BoundingBox box = BoundingBox.fromCorners(first, second);
        this.min = new BlockPos(box.minX(), box.minY(), box.minZ());
        this.max = new BlockPos(box.maxX(), box.maxY(), box.maxZ());
        this.scanning = null;
        this.cursor = 0;
        syncToClient();
    }

    @Override
    public @Nullable BoundingBox shownArea() {
        return hasBox() ? BoundingBox.fromCorners(this.min, this.max) : null;
    }

    // ── escaneamento ──────────────────────────────────────────────────────
    @Override
    public void serverTick() {
        if (!(this.level instanceof ServerLevel server) || !hasBox()) return;
        ItemStack input = this.inventory.getItem(SLOT_IN);
        if (!(input.getItem() instanceof SnapshotItem item) || !this.inventory.getItem(SLOT_OUT).isEmpty()) {
            this.scanning = null;
            this.cursor = 0;
            return;
        }
        if (this.scanning == null || this.scanning.type != item.type()) {
            BlockPos behind = this.worldPosition.relative(facing().getOpposite());
            this.scanning = new Snapshot(item.type(), this.max.getX() - this.min.getX() + 1, this.max.getY() - this.min.getY() + 1,
                    this.max.getZ() - this.min.getZ() + 1, facing(), this.min.subtract(behind));
            this.cursor = 0;
        }
        Snapshot snapshot = this.scanning;
        for (int n = 0; n < snapshot.type.scanPerTick && this.cursor < snapshot.volume(); n++, this.cursor++) {
            BlockState state = this.level.getBlockState(this.min.offset(snapshot.local(this.cursor)));
            if (snapshot.type == Snapshot.Type.TEMPLATE) {
                snapshot.setFilled(this.cursor, !BlueprintRules.forScan(state).isAir());
            } else {
                snapshot.setState(this.cursor, BlueprintRules.forScan(state));
            }
        }
        if (this.cursor >= snapshot.volume()) finish(server, input, snapshot);
    }

    private void finish(ServerLevel server, ItemStack input, Snapshot snapshot) {
        String hash = SnapshotStore.put(server, snapshot);
        String name = input.has(DataComponents.CUSTOM_NAME) ? input.getHoverName().getString() : "";
        this.inventory.setItem(SLOT_OUT, SnapshotItem.used(new SnapshotHeader(hash, snapshot.type.id(), name, this.author)));
        input.shrink(1);
        this.inventory.setChanged();
        this.scanning = null;
        this.cursor = 0;
    }

    /** Funis colocam moldes/plantas à esquerda e tiram o resultado da direita. */
    public Storage<ItemVariant> itemStorage(@Nullable Direction face) {
        ContainerStorage storage = ContainerStorage.of(this.inventory, face);
        return new CombinedStorage<>(List.of(
                FilteringStorage.insertOnlyOf(storage.getSlot(SLOT_IN)),
                FilteringStorage.extractOnlyOf(storage.getSlot(SLOT_OUT))));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) Containers.dropContents(this.level, pos, this.inventory);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int slot = 0; slot < 2; slot++) output.store("Slot" + slot, ItemStack.OPTIONAL_CODEC, this.inventory.getItem(slot));
        output.putBoolean("HasBox", hasBox());
        if (hasBox()) {
            output.putLong("Min", this.min.asLong());
            output.putLong("Max", this.max.asLong());
        }
        output.putString("Author", this.author);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int slot = 0; slot < 2; slot++) this.inventory.setItem(slot, input.read("Slot" + slot, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
        if (input.getBooleanOr("HasBox", false)) {
            this.min = BlockPos.of(input.getLongOr("Min", 0L));
            this.max = BlockPos.of(input.getLongOr("Max", 0L));
        } else {
            this.min = null;
            this.max = null;
        }
        this.author = input.getStringOr("Author", "");
    }
}
