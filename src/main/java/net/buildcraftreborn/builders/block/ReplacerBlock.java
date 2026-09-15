package net.buildcraftreborn.builders.block;

import net.buildcraftreborn.builders.ReplacerMenu;
import net.buildcraftreborn.builders.tile.ReplacerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Substituidor de blocos de planta. */
public class ReplacerBlock extends MenuMachineBlock {
    public ReplacerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReplacerBlockEntity(pos, state);
    }

    @Override
    protected @Nullable AbstractContainerMenu menu(int containerId, Inventory inventory, BlockEntity blockEntity) {
        return blockEntity instanceof ReplacerBlockEntity replacer ? new ReplacerMenu(containerId, inventory, replacer) : null;
    }
}
