package net.buildcraftreborn.registry;

import net.buildcraftreborn.BuildCraftReborn;
import net.craftenergy.registry.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;

/** Blocos do BuildCraft Reborn. */
public final class BCBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, BuildCraftReborn.MODID);

    private BCBlocks() {}
}
