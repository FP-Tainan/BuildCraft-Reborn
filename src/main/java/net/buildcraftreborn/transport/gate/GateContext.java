package net.buildcraftreborn.transport.gate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Onde a porta lógica está: o tubo, a face do tubo em que ela encaixa e a própria lógica. */
public record GateContext(Level level, BlockPos pos, Direction side, BlockEntity pipe, GateLogic gate) {
}
