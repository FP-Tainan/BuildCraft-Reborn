package net.buildcraftreborn.builders.block;

import net.buildcraftreborn.lib.tile.ServerTicking;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Máquina virada para o jogador que tica no servidor e abre uma tela ao clicar (biblioteca, substituidor). */
public abstract class MenuMachineBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected MenuMachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** Tela da máquina; {@code null} se o block entity não é o esperado. */
    protected abstract @Nullable AbstractContainerMenu menu(int containerId, Inventory inventory, BlockEntity blockEntity);

    /** Antes de abrir (por exemplo, atualizar a lista da biblioteca). */
    protected void beforeOpen(BlockEntity blockEntity) {
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ServerTicking.ticker(level);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) {
            beforeOpen(blockEntity);
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
                public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player opener) {
                    return menu(containerId, inventory, blockEntity);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}
