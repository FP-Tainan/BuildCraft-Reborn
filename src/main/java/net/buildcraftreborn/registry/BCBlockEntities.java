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

    // ── factory ───────────────────────────────────────────────────────────
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.factory.tile.TankBlockEntity>> TANK = BLOCK_ENTITIES.register("tank",
            () -> new BlockEntityType<>(net.buildcraftreborn.factory.tile.TankBlockEntity::new, Set.of(BCBlocks.TANK.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.factory.tile.PumpBlockEntity>> PUMP = BLOCK_ENTITIES.register("pump",
            () -> new BlockEntityType<>(net.buildcraftreborn.factory.tile.PumpBlockEntity::new, Set.of(BCBlocks.PUMP.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.factory.tile.MiningWellBlockEntity>> MINING_WELL = BLOCK_ENTITIES.register("mining_well",
            () -> new BlockEntityType<>(net.buildcraftreborn.factory.tile.MiningWellBlockEntity::new, Set.of(BCBlocks.MINING_WELL.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.factory.tile.FloodGateBlockEntity>> FLOOD_GATE = BLOCK_ENTITIES.register("flood_gate",
            () -> new BlockEntityType<>(net.buildcraftreborn.factory.tile.FloodGateBlockEntity::new, Set.of(BCBlocks.FLOOD_GATE.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.factory.tile.ChuteBlockEntity>> CHUTE = BLOCK_ENTITIES.register("chute",
            () -> new BlockEntityType<>(net.buildcraftreborn.factory.tile.ChuteBlockEntity::new, Set.of(BCBlocks.CHUTE.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.factory.tile.AutoWorkbenchBlockEntity>> AUTO_WORKBENCH = BLOCK_ENTITIES.register("auto_workbench",
            () -> new BlockEntityType<>(net.buildcraftreborn.factory.tile.AutoWorkbenchBlockEntity::new, Set.of(BCBlocks.AUTO_WORKBENCH.get())));

    private BCBlockEntities() {}
}
