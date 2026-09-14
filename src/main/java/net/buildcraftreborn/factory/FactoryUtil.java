package net.buildcraftreborn.factory;

import net.buildcraftreborn.registry.BCBlocks;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Ajudas das máquinas do factory: entregar itens e fluidos aos vizinhos e cuidar dos tubos. */
public final class FactoryUtil {
    private FactoryUtil() {}

    /**
     * Entrega itens aos inventários vizinhos (menos o de baixo, onde fica o tubo); o que sobrar cai
     * em cima da máquina.
     */
    public static void output(Level level, BlockPos pos, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (Direction direction : Direction.values()) {
            if (rest.isEmpty() || direction == Direction.DOWN) continue;
            Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos.relative(direction), direction.getOpposite());
            if (target == null) continue;
            try (Transaction transaction = Transaction.openOuter()) {
                long inserted = target.insert(ItemVariant.of(rest), rest.getCount(), transaction);
                transaction.commit();
                rest.shrink((int) inserted);
            }
        }
        if (!rest.isEmpty()) Block.popResource(level, pos.above(), rest);
    }

    /** Empurra até {@code maxDroplets} de fluido para os vizinhos (menos o de baixo). */
    public static void pushFluid(Level level, BlockPos pos, Storage<FluidVariant> source, long maxDroplets) {
        long left = maxDroplets;
        for (Direction direction : Direction.values()) {
            if (left <= 0 || direction == Direction.DOWN) continue;
            Storage<FluidVariant> target = FluidStorage.SIDED.find(level, pos.relative(direction), direction.getOpposite());
            if (target == null) continue;
            try (Transaction transaction = Transaction.openOuter()) {
                left -= StorageUtil.move(source, target, variant -> true, left, transaction);
                transaction.commit();
            }
        }
    }

    /** Ponta do tubo de mineração abaixo da máquina (o primeiro bloco que não é tubo). */
    public static BlockPos tubeTip(Level level, BlockPos pos) {
        BlockPos tip = pos.below();
        while (tip.getY() > level.getMinY() && level.getBlockState(tip).is(BCBlocks.MINING_PIPE.get())) tip = tip.below();
        return tip;
    }

    /** Recolhe o tubo quando a máquina sai. */
    public static void removeTube(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        while (below.getY() >= level.getMinY() && level.getBlockState(below).is(BCBlocks.MINING_PIPE.get())) {
            level.removeBlock(below, false);
            below = below.below();
        }
    }
}
