package net.buildcraftreborn.registry;

import net.buildcraftreborn.BuildCraftReborn;
import net.craftenergy.registry.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Tipos de block entity do BuildCraft Reborn. */
public final class BCBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BuildCraftReborn.MODID);

    private BCBlockEntities() {}
}
