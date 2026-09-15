package net.buildcraftreborn.builders.filler;

import org.jetbrains.annotations.Nullable;

import java.util.BitSet;

/**
 * Formas dos padrões do preenchedor dentro de uma caixa {@code sx × sy × sz}. O bit ligado quer bloco e o
 * desligado quer ar; índice = {@code (y * sz + z) * sx + x}.
 */
public final class FillerTemplates {
    private FillerTemplates() {}

    @FunctionalInterface
    private interface Shape3 {
        boolean in(int x, int y, int z);
    }

    @FunctionalInterface
    private interface Shape2 {
        boolean in(int u, int v);
    }

    public static int index(int x, int y, int z, int sx, int sz) {
        return (y * sz + z) * sx + x;
    }

    /** Nulo para o padrão "nenhum" (não mexe em nada). */
    public static @Nullable BitSet build(FillerPattern pattern, int[] params, int sx, int sy, int sz) {
        if (pattern == FillerPattern.NONE) return null;
        Shape3 shape = shape(pattern, params, sx, sy, sz);
        BitSet bits = new BitSet(sx * sy * sz);
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++) {
                    if (shape.in(x, y, z)) bits.set(index(x, y, z, sx, sz));
                }
            }
        }
        return bits;
    }

    private static int param(int[] params, int index) {
        return index < params.length ? params[index] : 0;
    }

    private static Shape3 shape(FillerPattern pattern, int[] p, int sx, int sy, int sz) {
        return switch (pattern) {
            case NONE, CLEAR -> (x, y, z) -> false;
            case FILL -> (x, y, z) -> true;
            case BOX -> (x, y, z) -> x == 0 || y == 0 || z == 0 || x == sx - 1 || y == sy - 1 || z == sz - 1;
            case FRAME -> (x, y, z) -> (x == 0 || x == sx - 1 ? 1 : 0) + (y == 0 || y == sy - 1 ? 1 : 0) + (z == 0 || z == sz - 1 ? 1 : 0) >= 2;
            case PYRAMID -> pyramid(param(p, 0), param(p, 1), sx, sy, sz);
            case STAIRS -> stairs(param(p, 0), param(p, 1), sx, sy, sz);
            case SPHERE -> hollow3(param(p, 0), ellipsoid(new double[]{sx / 2.0, sy / 2.0, sz / 2.0},
                    new double[]{sx / 2.0, sy / 2.0, sz / 2.0}), sx, sy, sz);
            case SPHERE_HALF, SPHERE_QUARTER, SPHERE_EIGHTH -> spherePart(pattern, param(p, 0), param(p, 1), param(p, 2), sx, sy, sz);
            default -> shape2d(pattern, param(p, 0), param(p, 1), param(p, 2), sx, sy, sz);
        };
    }

    /** Camadas encolhendo até o topo; o centro escolhe para que lado fica a ponta. */
    private static Shape3 pyramid(int yDir, int center, int sx, int sy, int sz) {
        int col = center % 3;
        int row = center / 3;
        return (x, y, z) -> {
            int layer = yDir == 0 ? y : sy - 1 - y;
            int minX = col == 0 ? 0 : layer;
            int maxX = col == 2 ? sx - 1 : sx - 1 - layer;
            int minZ = row == 0 ? 0 : layer;
            int maxZ = row == 2 ? sz - 1 : sz - 1 - layer;
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        };
    }

    /** Degraus subindo (ou descendo) um bloco por coluna na direção escolhida. */
    private static Shape3 stairs(int xzDir, int yDir, int sx, int sy, int sz) {
        return (x, y, z) -> {
            int step = switch (xzDir) {
                case 0 -> sx - 1 - x;
                case 1 -> x;
                case 2 -> sz - 1 - z;
                default -> z;
            };
            return yDir == 0 ? y <= step : y >= sy - 1 - step;
        };
    }

    private static Shape3 ellipsoid(double[] center, double[] radius) {
        return (x, y, z) -> {
            double dx = (x + 0.5 - center[0]) / radius[0];
            double dy = (y + 0.5 - center[1]) / radius[1];
            double dz = (z + 0.5 - center[2]) / radius[2];
            return dx * dx + dy * dy + dz * dz <= 1.0;
        };
    }

    /**
     * Pedaço de esfera: o centro vai para a face da direção escolhida (meia), e também para uma aresta
     * (quarto) ou um canto (oitavo) conforme a rotação.
     */
    private static Shape3 spherePart(FillerPattern pattern, int hollow, int facing, int rotation, int sx, int sy, int sz) {
        int[] size = {sx, sy, sz};
        double[] center = {sx / 2.0, sy / 2.0, sz / 2.0};
        double[] radius = {sx / 2.0, sy / 2.0, sz / 2.0};
        int axis = switch (facing) {
            case 0, 1 -> 1;
            case 2, 3 -> 2;
            default -> 0;
        };
        boolean positive = facing == 1 || facing == 3 || facing == 5;
        center[axis] = positive ? size[axis] : 0;
        radius[axis] = size[axis];
        int[] perpendicular = axis == 1 ? new int[]{0, 2} : axis == 0 ? new int[]{1, 2} : new int[]{0, 1};
        if (pattern == FillerPattern.SPHERE_QUARTER) {
            int chosen = perpendicular[rotation < 2 ? 0 : 1];
            center[chosen] = rotation % 2 == 1 ? size[chosen] : 0;
            radius[chosen] = size[chosen];
        } else if (pattern == FillerPattern.SPHERE_EIGHTH) {
            int a = perpendicular[0];
            int b = perpendicular[1];
            center[a] = rotation == 1 || rotation == 2 ? size[a] : 0;
            center[b] = rotation >= 2 ? size[b] : 0;
            radius[a] = size[a];
            radius[b] = size[b];
        }
        return hollow3(hollow, ellipsoid(center, radius), sx, sy, sz);
    }

    /** Cheio, só por fora (cercado) ou só a casca. */
    private static Shape3 hollow3(int mode, Shape3 shape, int sx, int sy, int sz) {
        return switch (mode) {
            case 0 -> shape;
            case 1 -> (x, y, z) -> !shape.in(x, y, z);
            default -> (x, y, z) -> shape.in(x, y, z) && (outside3(shape, x - 1, y, z, sx, sy, sz) || outside3(shape, x + 1, y, z, sx, sy, sz)
                    || outside3(shape, x, y - 1, z, sx, sy, sz) || outside3(shape, x, y + 1, z, sx, sy, sz)
                    || outside3(shape, x, y, z - 1, sx, sy, sz) || outside3(shape, x, y, z + 1, sx, sy, sz));
        };
    }

    private static boolean outside3(Shape3 shape, int x, int y, int z, int sx, int sy, int sz) {
        return x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz || !shape.in(x, y, z);
    }

    /** Forma plana no plano perpendicular ao eixo, repetida em toda a profundidade da caixa. */
    private static Shape3 shape2d(FillerPattern pattern, int axis, int hollow, int rotation, int sx, int sy, int sz) {
        int width = axis == 0 ? sz : sx;
        int height = axis == 1 ? sz : sy;
        Shape2 base = shape2(pattern, rotation, width, height);
        Shape2 shape = switch (hollow) {
            case 0 -> base;
            case 1 -> (u, v) -> !base.in(u, v);
            default -> (u, v) -> base.in(u, v) && (outside2(base, u - 1, v, width, height) || outside2(base, u + 1, v, width, height)
                    || outside2(base, u, v - 1, width, height) || outside2(base, u, v + 1, width, height));
        };
        return switch (axis) {
            case 0 -> (x, y, z) -> shape.in(z, y);
            case 1 -> (x, y, z) -> shape.in(x, z);
            default -> (x, y, z) -> shape.in(x, y);
        };
    }

    private static boolean outside2(Shape2 shape, int u, int v, int width, int height) {
        return u < 0 || v < 0 || u >= width || v >= height || !shape.in(u, v);
    }

    private static Shape2 shape2(FillerPattern pattern, int rotation, int width, int height) {
        return (u, v) -> {
            double nu = (u + 0.5) / width * 2 - 1;
            double nv = (v + 0.5) / height * 2 - 1;
            for (int r = 0; r < rotation; r++) {
                double t = nu;
                nu = nv;
                nv = -t;
            }
            return switch (pattern) {
                case CIRCLE_2D -> nu * nu + nv * nv <= 1.0;
                case TRIANGLE_2D -> Math.abs(nu) <= (1.0 - nv) / 2.0 + 1.0E-9;
                case PENTAGON_2D -> polygon(5, nu, nv);
                case HEXAGON_2D -> polygon(6, nu, nv);
                case OCTAGON_2D -> polygon(8, nu, nv);
                case SEMI_CIRCLE_2D -> {
                    double ny = (nv + 1) / 2;
                    yield nu * nu + ny * ny <= 1.0;
                }
                case ARC_2D -> {
                    double a = (nu + 1) / 2;
                    double b = (nv + 1) / 2;
                    yield a * a + b * b <= 1.0;
                }
                default -> true;
            };
        };
    }

    /** Polígono regular de raio 1 com um vértice para cima. */
    private static boolean polygon(int sides, double x, double y) {
        for (int i = 0; i < sides; i++) {
            double a1 = Math.PI / 2 + 2 * Math.PI * i / sides;
            double a2 = Math.PI / 2 + 2 * Math.PI * (i + 1) / sides;
            double x1 = Math.cos(a1);
            double y1 = Math.sin(a1);
            double x2 = Math.cos(a2);
            double y2 = Math.sin(a2);
            if ((x2 - x1) * (y - y1) - (y2 - y1) * (x - x1) < -1.0E-9) return false;
        }
        return true;
    }
}
