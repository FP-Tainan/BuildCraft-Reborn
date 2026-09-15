package net.buildcraftreborn.builders.block;

import net.buildcraftreborn.builders.LibraryMenu;
import net.buildcraftreborn.builders.tile.LibraryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Biblioteca eletrônica. */
public class LibraryBlock extends MenuMachineBlock {
    public LibraryBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LibraryBlockEntity(pos, state);
    }

    @Override
    protected void beforeOpen(BlockEntity blockEntity) {
        if (blockEntity instanceof LibraryBlockEntity library) library.refresh();
    }

    @Override
    protected @Nullable AbstractContainerMenu menu(int containerId, Inventory inventory, BlockEntity blockEntity) {
        return blockEntity instanceof LibraryBlockEntity library ? new LibraryMenu(containerId, inventory, library) : null;
    }
}
