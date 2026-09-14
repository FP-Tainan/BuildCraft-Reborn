package net.buildcraftreborn.factory.block;

import net.buildcraftreborn.factory.tile.AutoWorkbenchBlockEntity;
import net.buildcraftreborn.factory.tile.FloodGateBlockEntity;
import net.buildcraftreborn.factory.tile.MiningWellBlockEntity;
import net.buildcraftreborn.factory.tile.PumpBlockEntity;
import net.buildcraftreborn.factory.AutoWorkbenchMenu;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Máquinas do factory sem orientação: bomba, comporta e bancada automática (o poço gira: {@link MiningWellBlock}). */
public class FactoryMachineBlock extends Block implements EntityBlock {
    public enum Kind { PUMP, MINING_WELL, FLOOD_GATE, AUTO_WORKBENCH }

    private final Kind kind;

    public FactoryMachineBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (this.kind) {
            case PUMP -> new PumpBlockEntity(pos, state);
            case MINING_WELL -> new MiningWellBlockEntity(pos, state);
            case FLOOD_GATE -> new FloodGateBlockEntity(pos, state);
            case AUTO_WORKBENCH -> new AutoWorkbenchBlockEntity(pos, state);
        };
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ServerTicking.ticker(level);
    }

    /** Balde na bomba tira fluido; na comporta, põe. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PumpBlockEntity pump && FluidStorageUtil.interactWithFluidStorage(pump.tank(), player, hand)) {
            return InteractionResult.SUCCESS;
        }
        if (blockEntity instanceof FloodGateBlockEntity gate && FluidStorageUtil.interactWithFluidStorage(gate.tank(), player, hand)) {
            return InteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (this.kind != Kind.AUTO_WORKBENCH || !(level.getBlockEntity(pos) instanceof AutoWorkbenchBlockEntity workbench)) {
            return InteractionResult.PASS;
        }
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
                    return new AutoWorkbenchMenu(containerId, inventory, workbench);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}
