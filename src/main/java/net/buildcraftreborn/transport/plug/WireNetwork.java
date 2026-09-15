package net.buildcraftreborn.transport.plug;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Fios de tubo ({@code WireSystem}): cada tubo tem 8 cantos (bit 0 = leste, 1 = cima, 2 = sul). Um pedaço
 * de fio liga com os cantos vizinhos do mesmo tubo e com o canto espelhado do tubo ao lado, se a cor for a
 * mesma e nenhum encaixe tapar a face. A rede está ligada se alguma porta nela emite a cor. O resultado de
 * cada rede fica guardado durante o tick, então todos os tubos dela veem o mesmo valor.
 */
public final class WireNetwork {
    public static final int CORNERS = 8;
    private static final int MAX_NODES = 4096;

    private record Node(BlockPos pos, int corner) {}

    private static final class Cache {
        long tick = Long.MIN_VALUE;
        final Map<Node, Boolean> powered = new HashMap<>();
    }

    private static final Map<Level, Cache> CACHES = new WeakHashMap<>();

    private WireNetwork() {}

    /** Face do tubo do lado do canto, no eixo dado. */
    public static Direction face(int corner, int axis) {
        boolean positive = (corner >> axis & 1) != 0;
        return switch (axis) {
            case 0 -> positive ? Direction.EAST : Direction.WEST;
            case 1 -> positive ? Direction.UP : Direction.DOWN;
            default -> positive ? Direction.SOUTH : Direction.NORTH;
        };
    }

    /** Canto mais perto de um ponto dentro do bloco (coordenadas locais 0..1). */
    public static int cornerAt(Vec3 local) {
        return (local.x >= 0.5 ? 1 : 0) | (local.y >= 0.5 ? 2 : 0) | (local.z >= 0.5 ? 4 : 0);
    }

    public static @Nullable PipePlugs plugsAt(Level level, BlockPos pos) {
        return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PlugHolder holder ? holder.plugs() : null;
    }

    public static boolean powered(Level level, BlockPos pos, int corner, DyeColor color) {
        long now = level.getGameTime();
        Cache cache = CACHES.computeIfAbsent(level, key -> new Cache());
        if (cache.tick != now) {
            cache.tick = now;
            cache.powered.clear();
        }
        Node start = new Node(pos.immutable(), corner);
        Boolean known = cache.powered.get(start);
        if (known != null) return known;

        List<Node> visited = new ArrayList<>();
        Set<Node> seen = new HashSet<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        boolean on = false;
        while (!queue.isEmpty() && visited.size() < MAX_NODES) {
            Node node = queue.poll();
            PipePlugs plugs = plugsAt(level, node.pos());
            if (plugs == null || plugs.wire(node.corner()) != color) continue;
            visited.add(node);
            if (plugs.emitting(color, now)) on = true;
            for (int axis = 0; axis < 3; axis++) {
                int mirrored = node.corner() ^ (1 << axis);
                Node inside = new Node(node.pos(), mirrored);
                if (seen.add(inside)) queue.add(inside);
                Direction direction = face(node.corner(), axis);
                if (plugs.has(direction)) continue;
                BlockPos next = node.pos().relative(direction);
                PipePlugs nextPlugs = plugsAt(level, next);
                if (nextPlugs == null || nextPlugs.has(direction.getOpposite())) continue;
                Node across = new Node(next, mirrored);
                if (seen.add(across)) queue.add(across);
            }
        }
        for (Node node : visited) cache.powered.put(node, on);
        cache.powered.putIfAbsent(start, false);
        return on;
    }
}
