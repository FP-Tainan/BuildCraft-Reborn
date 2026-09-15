package net.buildcraftreborn.builders.tile;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/** Máquina que mostra a própria área com a faixa listrada (arquiteto lendo, construtor escrevendo). */
public interface ShowsArea {
    @Nullable BoundingBox shownArea();
}
