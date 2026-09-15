package net.buildcraftreborn.silicon.block;

import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.silicon.AdvancedCraftingTableMenu;
import net.buildcraftreborn.silicon.AssemblyTableMenu;
import net.buildcraftreborn.silicon.tile.AdvancedCraftingTableBlockEntity;
import net.buildcraftreborn.silicon.tile.AssemblyTableBlockEntity;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Mesas do silício alimentadas por laser (9 pixels de altura): montagem e trabalho avançada. */
public class LaserTableBlock extends Block implements EntityBlock {
    public enum Kind {
        ASSEMBLY,
        ADVANCED_CRAFTING
    }

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 9, 16);
    private final Kind kind;

    public LaserTableBlock(Properties properties, Kind kind) {
        super(properties.noOcclusion());
        this.kind = kind;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.kind == Kind.ASSEMBLY ? new AssemblyTableBlockEntity(pos, state) : new AdvancedCraftingTableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ServerTicking.ticker(level);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof AssemblyTableBlockEntity) && !(blockEntity instanceof AdvancedCraftingTableBlockEntity)) {
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
                    return blockEntity instanceof AssemblyTableBlockEntity assembly
                            ? new AssemblyTableMenu(containerId, inventory, assembly)
                            : new AdvancedCraftingTableMenu(containerId, inventory, (AdvancedCraftingTableBlockEntity) blockEntity);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}
