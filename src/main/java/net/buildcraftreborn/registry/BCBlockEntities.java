package net.buildcraftreborn.registry;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.core.marker.MarkerBlockEntity;
import net.craftenergy.registry.DeferredRegister;
import net.craftenergy.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Set;

/** Tipos de block entity do BuildCraft Reborn. */
public final class BCBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BuildCraftReborn.MODID);

    /** Marcadores de área e de caminho. */
    public static final RegistryObject<BlockEntityType<MarkerBlockEntity>> MARKER = BLOCK_ENTITIES.register("marker",
            () -> new BlockEntityType<>(MarkerBlockEntity::new, Set.of(BCBlocks.MARKER_VOLUME.get(), BCBlocks.MARKER_PATH.get())));

    /** Motores de redstone, Stirling e criativo. */
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.energy.engine.EngineBlockEntity>> ENGINE = BLOCK_ENTITIES.register("engine",
            () -> new BlockEntityType<>(net.buildcraftreborn.energy.engine.EngineBlockEntity::new,
                    Set.of(BCBlocks.REDSTONE_ENGINE.get(), BCBlocks.STIRLING_ENGINE.get(), BCBlocks.CREATIVE_ENGINE.get())));

    private BCBlockEntities() {}
}
