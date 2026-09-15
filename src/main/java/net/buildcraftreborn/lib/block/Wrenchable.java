package net.buildcraftreborn.lib.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Bloco com ação própria para o clique normal da chave inglesa (tubo de ferro troca a saída, por exemplo). */
public interface Wrenchable {
    /** Devolve {@code true} se algo mudou. Chamado só no servidor. */
    boolean onWrench(Level level, BlockPos pos, BlockState state, @Nullable Player player);

    /** Versão com a face e o ponto clicados (tubo daizuli); por padrão ignora onde foi o clique. */
    default boolean onWrenchAt(Level level, BlockPos pos, BlockState state, @Nullable Player player, Direction face, Vec3 hit) {
        return onWrench(level, pos, state, player);
    }

    /** Clique agachado com a chave (tubos coloridos voltam uma cor); {@code false} deixa o clique passar. */
    default boolean onWrenchBack(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        return false;
    }
}
