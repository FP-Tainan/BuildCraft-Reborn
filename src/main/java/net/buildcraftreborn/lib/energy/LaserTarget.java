package net.buildcraftreborn.lib.energy;

/** Recebe energia de lasers ({@code ILaserTarget}): mesas de montagem, de trabalho avançada etc. */
public interface LaserTarget {
    /** Quanto ainda falta para a tarefa atual, em CW·tick; 0 se não precisa de nada. */
    long requiredLaserPower();

    /** Recebe até {@code power} CW·tick e devolve o que sobrou. */
    long receiveLaserPower(long power);
}
