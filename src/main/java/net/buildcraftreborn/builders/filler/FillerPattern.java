package net.buildcraftreborn.builders.filler;

import java.util.Locale;

/**
 * Padrões do preenchedor ({@code buildcraft.builders.snapshot.pattern}) com os parâmetros de cada um e os
 * valores iniciais. A forma de cada padrão fica em {@link FillerTemplates}.
 */
public enum FillerPattern {
    NONE,
    CLEAR,
    FILL,
    BOX,
    FRAME,
    PYRAMID(new ParamType[]{ParamType.Y_DIR, ParamType.CENTER}, new int[]{0, 4}),
    STAIRS(new ParamType[]{ParamType.XZ_DIR, ParamType.Y_DIR}, new int[]{1, 0}),
    SPHERE(new ParamType[]{ParamType.HOLLOW}, new int[]{0}),
    SPHERE_HALF(new ParamType[]{ParamType.HOLLOW, ParamType.FACING, ParamType.ROTATION}, new int[]{0, 0, 0}),
    SPHERE_QUARTER(new ParamType[]{ParamType.HOLLOW, ParamType.FACING, ParamType.ROTATION}, new int[]{0, 0, 0}),
    SPHERE_EIGHTH(new ParamType[]{ParamType.HOLLOW, ParamType.FACING, ParamType.ROTATION}, new int[]{0, 0, 0}),
    SQUARE_2D("2d_square"),
    CIRCLE_2D("2d_circle"),
    TRIANGLE_2D("2d_triangle"),
    PENTAGON_2D("2d_pentagon"),
    HEXAGON_2D("2d_hexagon"),
    OCTAGON_2D("2d_octagon"),
    SEMI_CIRCLE_2D("2d_semi_circle"),
    ARC_2D("2d_arc");

    /** Tipos de parâmetro, com os valores possíveis e um símbolo curto para o botão da tela. */
    public enum ParamType {
        HOLLOW(new String[]{"filled", "filled_outer", "hollow"}, new String[]{"█", "▣", "□"}),
        AXIS(new String[]{"x", "y", "z"}, new String[]{"X", "Y", "Z"}),
        ROTATION(new String[]{"0", "90", "180", "270"}, new String[]{"0°", "90°", "180", "270"}),
        CENTER(new String[]{"north_west", "north", "north_east", "west", "center", "east", "south_west", "south", "south_east"},
                new String[]{"↖", "↑", "↗", "←", "•", "→", "↙", "↓", "↘"}),
        XZ_DIR(new String[]{"west", "east", "north", "south"}, new String[]{"←", "→", "↑", "↓"}),
        Y_DIR(new String[]{"up", "down"}, new String[]{"▲", "▼"}),
        FACING(new String[]{"down", "up", "north", "south", "west", "east"}, new String[]{"D", "U", "N", "S", "W", "E"});

        public final String[] values;
        public final String[] symbols;

        ParamType(String[] values, String[] symbols) {
            this.values = values;
            this.symbols = symbols;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public final String id;
    public final ParamType[] params;
    public final int[] defaults;

    FillerPattern() {
        this.id = name().toLowerCase(Locale.ROOT);
        this.params = new ParamType[0];
        this.defaults = new int[0];
    }

    FillerPattern(ParamType[] params, int[] defaults) {
        this.id = name().toLowerCase(Locale.ROOT);
        this.params = params;
        this.defaults = defaults;
    }

    /** Formas 2D: eixo Y, contorno (oco) e sem rotação. */
    FillerPattern(String id) {
        this.id = id;
        this.params = new ParamType[]{ParamType.AXIS, ParamType.HOLLOW, ParamType.ROTATION};
        this.defaults = new int[]{1, 2, 0};
    }

    public static FillerPattern byIndex(int index) {
        FillerPattern[] values = values();
        return values[Math.floorMod(index, values.length)];
    }
}
