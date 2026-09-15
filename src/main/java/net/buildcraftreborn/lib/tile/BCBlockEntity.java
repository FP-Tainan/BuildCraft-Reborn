package net.buildcraftreborn.lib.tile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base dos block entities do BuildCraft Reborn: o que é salvo também vai para o cliente, para
 * renderizadores (pistão do motor, nível do tanque, broca da pedreira) enxergarem o estado.
 */
public abstract class BCBlockEntity extends BlockEntity {
    /** Ticks que ainda faltam desligados por uma porta lógica ("desligar máquina"). */
    private int gateDisabledTicks;

    protected BCBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Porta lógica pedindo para a máquina parar; vale enquanto a ação continuar ativa. */
    public void disableFromGate() {
        this.gateDisabledTicks = 2;
    }

    public boolean gateDisabled() {
        return this.gateDisabledTicks > 0;
    }

    /** Chamado pelo ticker: se a porta desligou, gasta um tick e pula a lógica. */
    public boolean consumeGateDisable() {
        if (this.gateDisabledTicks <= 0) return false;
        this.gateDisabledTicks--;
        return true;
    }

    /** Marca para salvar e manda o estado atual para os jogadores que veem o bloco. */
    public void syncToClient() {
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
