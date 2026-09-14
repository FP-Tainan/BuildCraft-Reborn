package net.buildcraftreborn.core.marker;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.lib.tile.BCBlockEntity;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Ligações de um marcador com os outros (posições absolutas), salvas e enviadas ao cliente. */
public class MarkerBlockEntity extends BCBlockEntity {
    private final List<BlockPos> connections = new ArrayList<>();

    public MarkerBlockEntity(BlockPos pos, BlockState state) {
        super(BCBlockEntities.MARKER.get(), pos, state);
    }

    public MarkerBlock.Kind kind() {
        return getBlockState().getBlock() instanceof MarkerBlock marker ? marker.kind() : MarkerBlock.Kind.VOLUME;
    }

    public List<BlockPos> connections() {
        return List.copyOf(this.connections);
    }

    public static @Nullable MarkerBlockEntity at(BlockGetter level, BlockPos pos, MarkerBlock.Kind kind) {
        return level.getBlockEntity(pos) instanceof MarkerBlockEntity marker && marker.kind() == kind ? marker : null;
    }

    // ── ligação ───────────────────────────────────────────────────────────
    /** Clique no marcador: liga com os vizinhos possíveis. Devolve quantas ligações novas surgiram. */
    public int connectManually() {
        if (this.level == null) return 0;
        return kind() == MarkerBlock.Kind.VOLUME ? connectVolume() : connectPath();
    }

    /** Área: em cada eixo ainda livre, o marcador mais próximo (em qualquer sentido) que também tenha o eixo livre. */
    private int connectVolume() {
        int created = 0;
        for (Direction.Axis axis : Direction.Axis.values()) {
            if (hasAxis(axis)) continue;
            MarkerBlockEntity partner = findVolumePartner(this.level, this.worldPosition, axis);
            if (partner != null) {
                link(this, partner);
                created++;
            }
        }
        return created;
    }

    /** Primeiro marcador de área nos dois sentidos do eixo que ainda não usa esse eixo. */
    public static @Nullable MarkerBlockEntity findVolumePartner(Level level, BlockPos pos, Direction.Axis axis) {
        int range = BuildCraftReborn.config.markerMaxDistance;
        for (Direction.AxisDirection sign : Direction.AxisDirection.values()) {
            Direction direction = Direction.fromAxisAndDirection(axis, sign);
            for (int distance = 1; distance <= range; distance++) {
                BlockPos other = pos.relative(direction, distance);
                if (!level.isLoaded(other)) break;
                MarkerBlockEntity marker = at(level, other, MarkerBlock.Kind.VOLUME);
                if (marker != null) {
                    if (!marker.hasAxis(axis)) return marker;
                    break;
                }
            }
        }
        return null;
    }

    /** Caminho: o marcador mais próximo com ponta livre que não esteja já na mesma corrente. */
    private int connectPath() {
        if (this.connections.size() >= 2) return 0;
        int range = BuildCraftReborn.config.markerMaxDistance;
        Set<BlockPos> chain = group();
        MarkerBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        int minX = SectionPos.blockToSectionCoord(this.worldPosition.getX() - range);
        int maxX = SectionPos.blockToSectionCoord(this.worldPosition.getX() + range);
        int minZ = SectionPos.blockToSectionCoord(this.worldPosition.getZ() - range);
        int maxZ = SectionPos.blockToSectionCoord(this.worldPosition.getZ() + range);
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                if (!this.level.hasChunk(chunkX, chunkZ)) continue;
                for (BlockEntity blockEntity : this.level.getChunk(chunkX, chunkZ).getBlockEntities().values()) {
                    if (!(blockEntity instanceof MarkerBlockEntity marker) || marker.kind() != MarkerBlock.Kind.PATH) continue;
                    if (chain.contains(marker.worldPosition) || marker.connections.size() >= 2) continue;
                    double distance = Math.sqrt(marker.worldPosition.distSqr(this.worldPosition));
                    if (distance <= range && distance < bestDistance) {
                        best = marker;
                        bestDistance = distance;
                    }
                }
            }
        }
        if (best == null) return 0;
        link(this, best);
        return 1;
    }

    private boolean hasAxis(Direction.Axis axis) {
        for (BlockPos other : this.connections) {
            if (axis.choose(other.getX(), other.getY(), other.getZ()) != axis.choose(this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFreeAxis(Direction.Axis axis) {
        return !hasAxis(axis);
    }

    private static void link(MarkerBlockEntity a, MarkerBlockEntity b) {
        a.connections.add(b.worldPosition.immutable());
        b.connections.add(a.worldPosition.immutable());
        a.syncToClient();
        b.syncToClient();
    }

    /** Desfaz todas as ligações deste marcador. */
    public void disconnectAll() {
        if (this.level == null) return;
        for (BlockPos other : this.connections) {
            if (this.level.getBlockEntity(other) instanceof MarkerBlockEntity partner) {
                partner.connections.remove(this.worldPosition);
                partner.syncToClient();
            }
        }
        this.connections.clear();
        syncToClient();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null && !this.level.isClientSide()) disconnectAll();
    }

    // ── consultas ─────────────────────────────────────────────────────────
    /** Todos os marcadores ligados, direta ou indiretamente, a este (inclusive ele). */
    public Set<BlockPos> group() {
        Set<BlockPos> seen = new LinkedHashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(this.worldPosition);
        MarkerBlock.Kind kind = kind();
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            if (!seen.add(current) || this.level == null) continue;
            MarkerBlockEntity marker = at(this.level, current, kind);
            if (marker != null) queue.addAll(marker.connections);
        }
        return seen;
    }

    /** Caixa formada pelos marcadores de área ligados ao de {@code pos}, ou {@code null} se ele está sozinho. */
    public static @Nullable BoundingBox volumeAt(Level level, BlockPos pos) {
        MarkerBlockEntity marker = at(level, pos, MarkerBlock.Kind.VOLUME);
        if (marker == null) return null;
        Set<BlockPos> group = marker.group();
        if (group.size() < 2) return null;
        BoundingBox box = null;
        for (BlockPos member : group) {
            box = box == null ? new BoundingBox(member) : box.encapsulate(member);
        }
        return box;
    }

    /** Pontos do caminho em ordem, de uma ponta à outra. */
    public List<BlockPos> path() {
        Set<BlockPos> chain = group();
        BlockPos start = this.worldPosition;
        for (BlockPos member : chain) {
            MarkerBlockEntity marker = this.level == null ? null : at(this.level, member, MarkerBlock.Kind.PATH);
            if (marker != null && marker.connections.size() < 2) {
                start = member;
                break;
            }
        }
        List<BlockPos> ordered = new ArrayList<>();
        BlockPos previous = null;
        BlockPos current = start;
        while (current != null && !ordered.contains(current) && this.level != null) {
            ordered.add(current);
            MarkerBlockEntity marker = at(this.level, current, MarkerBlock.Kind.PATH);
            BlockPos next = null;
            if (marker != null) {
                for (BlockPos candidate : marker.connections) {
                    if (!candidate.equals(previous)) {
                        next = candidate;
                        break;
                    }
                }
            }
            previous = current;
            current = next;
        }
        return ordered;
    }

    // ── salvar ────────────────────────────────────────────────────────────
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Connections", BlockPos.CODEC.listOf(), List.copyOf(this.connections));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.connections.clear();
        this.connections.addAll(input.read("Connections", BlockPos.CODEC.listOf()).orElse(List.of()));
    }
}
