package net.buildcraftreborn.builders.block;

import net.buildcraftreborn.builders.ArchitectTableMenu;
import net.buildcraftreborn.builders.tile.ArchitectTableBlockEntity;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
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

/** Mesa do arquiteto: virada para o jogador, com a caixa dos marcadores atrás. */
public class ArchitectTableBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ArchitectTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
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
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ArchitectTableBlockEntity table) {
            if (placer instanceof Player player) table.setAuthor(player.getName().getString());
            table.initArea();
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ArchitectTableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ServerTicking.ticker(level);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ArchitectTableBlockEntity table)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer serverPlayer) {
            if (!table.hasBox()) table.initArea();
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
                    return new ArchitectTableMenu(containerId, inventory, table);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}
