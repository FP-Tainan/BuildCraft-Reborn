package net.buildcraftreborn.transport;

/**
 * Tubos de itens do BuildCraft e as regras de cada material: com quem ligam, se aceitam itens das
 * máquinas, e como mudam a velocidade dos itens.
 */
public enum PipeType {
    WOOD("wood", "Wooden"),
    COBBLESTONE("cobblestone", "Cobblestone"),
    STONE("stone", "Stone"),
    SANDSTONE("sandstone", "Sandstone"),
    QUARTZ("quartz", "Quartz"),
    GOLD("gold", "Golden"),
    IRON("iron", "Iron"),
    DIAMOND("diamond", "Diamond"),
    CLAY("clay", "Clay"),
    VOID("void", "Void"),
    OBSIDIAN("obsidian", "Obsidian"),
    STRUCTURE("structure", "Structure");

    /** Velocidade normal (blocos por tick) e a dos itens que acabaram de sair de um tubo de madeira. */
    public static final float BASE_SPEED = 0.05F;
    public static final float EXTRACT_SPEED = 0.08F;
    public static final float MAX_SPEED = 0.25F;

    public final String material;
    public final String englishName;

    PipeType(String material, String englishName) {
        this.material = material;
        this.englishName = englishName;
    }

    public String blockId() {
        return this == STRUCTURE ? "pipe_structure_cobblestone" : "pipe_items_" + this.material;
    }

    public boolean carriesItems() {
        return this != STRUCTURE;
    }

    /** Materiais que também existem como tubo de fluido (obsidiana e estrutura não). */
    public boolean hasFluidPipe() {
        return this != OBSIDIAN && this != STRUCTURE;
    }

    public String fluidBlockId() {
        return "pipe_fluids_" + this.material;
    }

    /** Vazão do tubo de fluido em CL por tick (BuildCraft: 1×, 2×, 4× e 8× a vazão base). */
    public long fluidRateCL() {
        return switch (this) {
            case WOOD, COBBLESTONE -> 40;
            case STONE, SANDSTONE -> 80;
            case QUARTZ, IRON, CLAY -> 160;
            case GOLD, DIAMOND, VOID -> 320;
            default -> 0;
        };
    }

    /** Ferro (saída escolhida) e madeira (inventário de onde puxa) têm uma direção especial. */
    public boolean directional() {
        return this == IRON || this == WOOD;
    }

    public boolean usesEnergy() {
        return this == WOOD || this == OBSIDIAN;
    }

    /** Pedregulho, pedra e quartzo não se ligam a um tubo diferente desse grupo. */
    public boolean separate() {
        return this == COBBLESTONE || this == STONE || this == QUARTZ;
    }

    public boolean connectsToInventories() {
        return this != SANDSTONE && this != STRUCTURE;
    }

    /** Itens empurrados por máquinas (pedreira, poço), funis e baús; o arenito não encosta em máquina. */
    public boolean acceptsInsertion() {
        return carriesItems() && this != SANDSTONE;
    }

    public static boolean canPipesConnect(PipeType a, PipeType b) {
        if (a == WOOD && b == WOOD) return false;
        return !(a.separate() && b.separate() && a != b);
    }

    /** No centro do tubo: ouro acelera; pedregulho, pedra, arenito e quartzo freiam de leve. */
    public float modifySpeed(float speed) {
        return switch (this) {
            case GOLD -> Math.min(MAX_SPEED, speed + 0.07F);
            case COBBLESTONE -> Math.max(BASE_SPEED, speed - 0.02F);
            case STONE, SANDSTONE -> Math.max(BASE_SPEED, speed - 0.008F);
            case QUARTZ -> Math.max(BASE_SPEED, speed - 0.002F);
            default -> speed;
        };
    }
}
