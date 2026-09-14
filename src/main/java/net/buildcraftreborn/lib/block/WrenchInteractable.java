package net.buildcraftreborn.lib.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Bloco com ação própria para a chave inglesa usada agachado (o clique normal gira). */
public interface WrenchInteractable {
    /** Devolve {@code true} se tratou o clique. */
    boolean onSneakWrench(Level level, BlockPos pos, BlockState state, Player player);
}
