package net.buildcraftreborn.builders.snapshot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Molde ({@code Template}: só onde há bloco) ou planta ({@code Blueprint}: paleta de estados e um índice por
 * posição). Guarda o tamanho, a direção da mesa do arquiteto e o deslocamento da caixa em relação ao bloco atrás
 * da mesa. O hash SHA-256 do conteúdo identifica o arquivo no mundo.
 */
public final class Snapshot {
    public enum Type {
        TEMPLATE(900),
        BLUEPRINT(300);

        /** Blocos lidos por tick na mesa do arquiteto. */
        public final int scanPerTick;

        Type(int scanPerTick) {
            this.scanPerTick = scanPerTick;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Type byId(String id) {
            return "blueprint".equals(id) ? BLUEPRINT : TEMPLATE;
        }
    }

    public final Type type;
    public final int sizeX;
    public final int sizeY;
    public final int sizeZ;
    public final Direction facing;
    public final BlockPos offset;
    private final BitSet filled = new BitSet();
    private final List<BlockState> palette = new ArrayList<>();
    private final Map<BlockState, Integer> paletteIndex = new HashMap<>();
    private final int[] data;
    private @Nullable String hash;

    public Snapshot(Type type, int sizeX, int sizeY, int sizeZ, Direction facing, BlockPos offset) {
        this.type = type;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.facing = facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
        this.offset = offset.immutable();
        this.data = type == Type.BLUEPRINT ? new int[volume()] : new int[0];
        addToPalette(Blocks.AIR.defaultBlockState());
    }

    public int volume() {
        return this.sizeX * this.sizeY * this.sizeZ;
    }

    public int index(int x, int y, int z) {
        return (z * this.sizeY + y) * this.sizeX + x;
    }

    public BlockPos local(int index) {
        return new BlockPos(index % this.sizeX, (index / this.sizeX) % this.sizeY, index / (this.sizeX * this.sizeY));
    }

    /** Posição com bloco (molde) ou com estado diferente de ar (planta). */
    public boolean filled(int index) {
        return this.type == Type.TEMPLATE ? this.filled.get(index) : this.data[index] != 0;
    }

    /** Estado guardado na planta (ar no molde). */
    public BlockState state(int index) {
        return this.type == Type.BLUEPRINT ? this.palette.get(this.data[index]) : Blocks.AIR.defaultBlockState();
    }

    public void setFilled(int index, boolean value) {
        this.filled.set(index, value);
        this.hash = null;
    }

    public void setState(int index, BlockState state) {
        this.data[index] = state.isAir() ? 0 : addToPalette(state);
        this.hash = null;
    }

    private int addToPalette(BlockState state) {
        return this.paletteIndex.computeIfAbsent(state, key -> {
            this.palette.add(key);
            return this.palette.size() - 1;
        });
    }

    public List<BlockState> palette() {
        return List.copyOf(this.palette);
    }

    /** Cópia com todo estado do bloco {@code from} trocado por {@code to}; propriedades de mesmo nome são mantidas. */
    public Snapshot replaced(Block from, BlockState to) {
        Snapshot copy = new Snapshot(this.type, this.sizeX, this.sizeY, this.sizeZ, this.facing, this.offset);
        if (this.type == Type.TEMPLATE) {
            copy.filled.or(this.filled);
            return copy;
        }
        for (int i = 0; i < volume(); i++) {
            BlockState state = state(i);
            copy.setState(i, state.is(from) ? keepProperties(state, to) : state);
        }
        return copy;
    }

    private static BlockState keepProperties(BlockState source, BlockState target) {
        BlockState result = target;
        for (Property<?> property : source.getProperties()) {
            if (result.hasProperty(property)) result = copyValue(source, result, property);
        }
        return result;
    }

    private static <T extends Comparable<T>> BlockState copyValue(BlockState source, BlockState target, Property<T> property) {
        return target.setValue(property, source.getValue(property));
    }

    // ── salvar ────────────────────────────────────────────────────────────
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", this.type.id());
        tag.putIntArray("Size", new int[]{this.sizeX, this.sizeY, this.sizeZ});
        tag.putString("Facing", this.facing.getName());
        tag.putIntArray("Offset", new int[]{this.offset.getX(), this.offset.getY(), this.offset.getZ()});
        if (this.type == Type.TEMPLATE) {
            tag.putByteArray("Filled", this.filled.toByteArray());
        } else {
            ListTag palette = new ListTag();
            for (BlockState state : this.palette) palette.add(NbtUtils.writeBlockState(state));
            tag.put("Palette", palette);
            tag.putIntArray("Data", this.data);
        }
        return tag;
    }

    public static Snapshot load(CompoundTag tag, HolderGetter<Block> blocks) {
        int[] size = tag.getIntArray("Size").orElse(new int[]{1, 1, 1});
        int[] offset = tag.getIntArray("Offset").orElse(new int[3]);
        Direction facing = Direction.byName(tag.getStringOr("Facing", "north"));
        Snapshot snapshot = new Snapshot(Type.byId(tag.getStringOr("Type", "template")), Math.max(1, size[0]), Math.max(1, size[1]),
                Math.max(1, size[2]), facing == null ? Direction.NORTH : facing, new BlockPos(offset[0], offset[1], offset[2]));
        if (snapshot.type == Type.TEMPLATE) {
            BitSet bits = BitSet.valueOf(tag.getByteArray("Filled").orElse(new byte[0]));
            snapshot.filled.or(bits.get(0, snapshot.volume()));
        } else {
            ListTag palette = tag.getListOrEmpty("Palette");
            List<BlockState> states = new ArrayList<>();
            for (int i = 0; i < palette.size(); i++) states.add(NbtUtils.readBlockState(blocks, palette.getCompoundOrEmpty(i)));
            int[] data = tag.getIntArray("Data").orElse(new int[0]);
            for (int i = 0; i < Math.min(data.length, snapshot.volume()); i++) {
                int entry = data[i];
                snapshot.setState(i, entry >= 0 && entry < states.size() ? states.get(entry) : Blocks.AIR.defaultBlockState());
            }
        }
        return snapshot;
    }

    /** Hash (hex) do conteúdo salvo: igual para duas cópias idênticas. */
    public String hash() {
        if (this.hash == null) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                NbtIo.write(save(), new DataOutputStream(bytes));
                this.hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
            } catch (IOException | NoSuchAlgorithmException exception) {
                throw new IllegalStateException("não deu para calcular o hash da planta", exception);
            }
        }
        return this.hash;
    }
}
