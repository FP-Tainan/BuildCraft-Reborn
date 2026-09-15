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
                    Set.of(BCBlocks.REDSTONE_ENGINE.get(), BCBlocks.STIRLING_ENGINE.get(), BCBlocks.CREATIVE_ENGINE.get(),
                            BCBlocks.COMBUSTION_ENGINE.get())));

    // ── factory ───────────────────────────────────────────────────────────
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.factory.tile.TankBlockEntity>> TANK = BLOCK_ENTITIES.register("tank",
            () -> new BlockEntityType<>(net.buildcraftreborn.factory.tile.TankBlockEntity::new, Set.of(BCBlocks.TANK.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.factory.tile.PumpBlockEntity>> PUMP = BLOCK_ENTITIES.register("pump",
            () -> new BlockEntityType<>(net.buildcraftreborn.factory.tile.PumpBlockEntity::new, Set.of(BCBlocks.PUMP.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.factory.tile.MiningWellBlockEntity>> MINING_WELL = BLOCK_ENTITIES.register("mining_well",
            () -> new BlockEntityType<>(net.buildcraftreborn.factory.tile.MiningWellBlockEntity::new, Set.of(BCBlocks.MINING_WELL.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.factory.tile.FloodGateBlockEntity>> FLOOD_GATE = BLOCK_ENTITIES.register("flood_gate",
            () -> new BlockEntityType<>(net.buildcraftreborn.factory.tile.FloodGateBlockEntity::new, Set.of(BCBlocks.FLOOD_GATE.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.factory.tile.AutoWorkbenchBlockEntity>> AUTO_WORKBENCH = BLOCK_ENTITIES.register("auto_workbench",
            () -> new BlockEntityType<>(net.buildcraftreborn.factory.tile.AutoWorkbenchBlockEntity::new, Set.of(BCBlocks.AUTO_WORKBENCH.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.factory.tile.DistillerBlockEntity>> DISTILLER = BLOCK_ENTITIES.register("distiller",
            () -> new BlockEntityType<>(net.buildcraftreborn.factory.tile.DistillerBlockEntity::new, Set.of(BCBlocks.DISTILLER.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.factory.tile.HeatExchangerBlockEntity>> HEAT_EXCHANGER = BLOCK_ENTITIES.register("heat_exchanger",
            () -> new BlockEntityType<>(net.buildcraftreborn.factory.tile.HeatExchangerBlockEntity::new, Set.of(BCBlocks.HEAT_EXCHANGER.get())));

    // ── builders ──────────────────────────────────────────────────────────
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.builders.tile.QuarryBlockEntity>> QUARRY = BLOCK_ENTITIES.register("quarry",
            () -> new BlockEntityType<>(net.buildcraftreborn.builders.tile.QuarryBlockEntity::new, Set.of(BCBlocks.QUARRY.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.builders.tile.FillerBlockEntity>> FILLER = BLOCK_ENTITIES.register("filler",
            () -> new BlockEntityType<>(net.buildcraftreborn.builders.tile.FillerBlockEntity::new, Set.of(BCBlocks.FILLER.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.builders.tile.ArchitectTableBlockEntity>> ARCHITECT_TABLE = BLOCK_ENTITIES.register("architect_table",
            () -> new BlockEntityType<>(net.buildcraftreborn.builders.tile.ArchitectTableBlockEntity::new, Set.of(BCBlocks.ARCHITECT_TABLE.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.builders.tile.BuilderBlockEntity>> BUILDER = BLOCK_ENTITIES.register("builder",
            () -> new BlockEntityType<>(net.buildcraftreborn.builders.tile.BuilderBlockEntity::new, Set.of(BCBlocks.BUILDER.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.transport.tile.FilteredBufferBlockEntity>> FILTERED_BUFFER = BLOCK_ENTITIES.register("filtered_buffer",
            () -> new BlockEntityType<>(net.buildcraftreborn.transport.tile.FilteredBufferBlockEntity::new, Set.of(BCBlocks.FILTERED_BUFFER.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.builders.tile.LibraryBlockEntity>> LIBRARY = BLOCK_ENTITIES.register("library",
            () -> new BlockEntityType<>(net.buildcraftreborn.builders.tile.LibraryBlockEntity::new, Set.of(BCBlocks.LIBRARY.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.builders.tile.ReplacerBlockEntity>> REPLACER = BLOCK_ENTITIES.register("replacer",
            () -> new BlockEntityType<>(net.buildcraftreborn.builders.tile.ReplacerBlockEntity::new, Set.of(BCBlocks.REPLACER.get())));

    // ── silicon ───────────────────────────────────────────────────────────
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.silicon.tile.LaserBlockEntity>> LASER = BLOCK_ENTITIES.register("laser",
            () -> new BlockEntityType<>(net.buildcraftreborn.silicon.tile.LaserBlockEntity::new, Set.of(BCBlocks.LASER.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.silicon.tile.AssemblyTableBlockEntity>> ASSEMBLY_TABLE = BLOCK_ENTITIES.register("assembly_table",
            () -> new BlockEntityType<>(net.buildcraftreborn.silicon.tile.AssemblyTableBlockEntity::new, Set.of(BCBlocks.ASSEMBLY_TABLE.get())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.silicon.tile.AdvancedCraftingTableBlockEntity>> ADVANCED_CRAFTING_TABLE = BLOCK_ENTITIES.register("advanced_crafting_table",
            () -> new BlockEntityType<>(net.buildcraftreborn.silicon.tile.AdvancedCraftingTableBlockEntity::new, Set.of(BCBlocks.ADVANCED_CRAFTING_TABLE.get())));

    // ── transport ─────────────────────────────────────────────────────────
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.transport.tile.PipeBlockEntity>> PIPE = BLOCK_ENTITIES.register("pipe",
            () -> new BlockEntityType<>(net.buildcraftreborn.transport.tile.PipeBlockEntity::new,
                    BCBlocks.PIPES.values().stream().<net.minecraft.world.level.block.Block>map(RegistryObject::get).collect(java.util.stream.Collectors.toSet())));
    public static final RegistryObject<BlockEntityType<net.buildcraftreborn.transport.tile.FluidPipeBlockEntity>> FLUID_PIPE = BLOCK_ENTITIES.register("fluid_pipe",
            () -> new BlockEntityType<>(net.buildcraftreborn.transport.tile.FluidPipeBlockEntity::new,
                    BCBlocks.FLUID_PIPES.values().stream().<net.minecraft.world.level.block.Block>map(RegistryObject::get).collect(java.util.stream.Collectors.toSet())));

    private BCBlockEntities() {}
}
