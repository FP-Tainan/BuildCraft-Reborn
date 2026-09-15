package net.buildcraftreborn.lib.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Bloco com ação própria para o clique normal da chave inglesa (tubo de ferro troca a saída, por exemplo). */
public interface Wrenchable {
    /** Devolve {@code true} se algo mudou. Chamado só no servidor. */
    boolean onWrench(Level level, BlockPos pos, BlockState state, @Nullable Player player);
}
