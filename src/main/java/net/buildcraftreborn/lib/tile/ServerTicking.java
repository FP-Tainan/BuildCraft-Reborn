package net.buildcraftreborn.lib.tile;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;

/** Block entity que roda lógica no servidor a cada tick. */
public interface ServerTicking {
    void serverTick();

    /** Ticker para {@code getTicker} dos blocos: só no servidor. */
    @SuppressWarnings("unchecked")
    static <T extends BlockEntity> BlockEntityTicker<T> ticker(Level level) {
        if (level.isClientSide()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<BlockEntity>) (tickLevel, pos, state, blockEntity) -> {
            // porta lógica com "desligar máquina" apontada para cá: pula o tick
            if (blockEntity instanceof BCBlockEntity machine && machine.consumeGateDisable()) return;
            if (blockEntity instanceof ServerTicking ticking) ticking.serverTick();
        };
    }
}
