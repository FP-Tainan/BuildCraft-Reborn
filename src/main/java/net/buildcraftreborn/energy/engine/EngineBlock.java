package net.buildcraftreborn.energy.engine;

import net.buildcraftreborn.lib.block.BCDirectionalBlock;
import net.buildcraftreborn.lib.block.WrenchInteractable;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.craftenergy.fabric.CraftEnergyApi;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Motores do BuildCraft. A frente (o tronco) entrega energia no Craft Energy e só funciona com sinal
 * de redstone. A chave inglesa gira a frente para o próximo vizinho que aceita energia. O desenho
 * inteiro (placa, pistão, tronco colorido) é feito pelo renderizador.
 */
public class EngineBlock extends BCDirectionalBlock implements EntityBlock, WrenchInteractable {
    public enum Kind {
        /** Motor de redstone: 50 CW a 220 MV, de graça, enquanto tiver sinal. */
        REDSTONE,
        /** Motor Stirling: 1.000 CW a 220 MV queimando combustível sólido. */
        STIRLING,
        /** Motor criativo: potência e tensão escolhidas, sem combustível. */
        CREATIVE
    }

    private final Kind kind;

    public EngineBlock(Properties properties, Kind kind) {
        super(properties.noOcclusion());
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EngineBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != BCBlockEntities.ENGINE.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<EngineBlockEntity>) (tickLevel, pos, tickState, engine) -> engine.serverTick();
    }

    /** Prefere virar para quem aceita energia; sem ninguém em volta, gira normalmente. */
    @Override
    public boolean canFace(Level level, BlockPos pos, Direction direction) {
        return CraftEnergyApi.NODE.find(level, pos.relative(direction), direction.getOpposite()) != null;
    }

    @Override
    public boolean rotateToNext(Level level, BlockPos pos, BlockState state) {
        boolean anyReceiver = false;
        for (Direction direction : Direction.values()) {
            if (canFace(level, pos, direction)) anyReceiver = true;
        }
        if (anyReceiver) return super.rotateToNext(level, pos, state);
        Direction current = state.getValue(FACING);
        level.setBlock(pos, state.setValue(FACING, Direction.from3DDataValue((current.get3DDataValue() + 1) % 6)), Block.UPDATE_ALL);
        CraftEnergyApi.markChanged(level, pos);
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof EngineBlockEntity engine)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        switch (this.kind) {
            case STIRLING -> {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.openMenu(new ExtendedMenuProvider<BlockPos>() {
                        @Override
                        public BlockPos getScreenOpeningData(ServerPlayer opener) {
                            return pos;
                        }

                        @Override
                        public Component getDisplayName() {
                            return state.getBlock().getName();
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
                            return new StirlingEngineMenu(containerId, inventory, engine);
                        }
                    });
                }
            }
            case CREATIVE -> {
                engine.cycleCreativeVoltage();
                player.sendOverlayMessage(engine.creativeDescription());
            }
            case REDSTONE -> player.sendOverlayMessage(engine.statusDescription());
        }
        return InteractionResult.SUCCESS;
    }

    /** Agachado com a chave: no motor criativo, dobra a potência. */
    @Override
    public boolean onSneakWrench(Level level, BlockPos pos, BlockState state, Player player) {
        if (this.kind != Kind.CREATIVE || !(level.getBlockEntity(pos) instanceof EngineBlockEntity engine)) return false;
        if (!level.isClientSide()) {
            engine.cycleCreativePower();
            player.sendOverlayMessage(engine.creativeDescription());
        }
        return true;
    }
}
