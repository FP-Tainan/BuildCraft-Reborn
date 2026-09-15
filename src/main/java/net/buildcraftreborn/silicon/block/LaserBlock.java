package net.buildcraftreborn.silicon.block;

import net.buildcraftreborn.lib.block.BCDirectionalBlock;
import net.buildcraftreborn.lib.tile.ServerTicking;
import net.buildcraftreborn.silicon.tile.LaserBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Laser: aponta para longe da face onde foi colocado e alimenta mesas num cone de 6 blocos à frente. */
public class LaserBlock extends BCDirectionalBlock implements EntityBlock {
    public LaserBlock(Properties properties) {
        super(properties.noOcclusion());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaserBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ServerTicking.ticker(level);
    }
}
