package net.buildcraftreborn.registry;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.core.block.SpringBlock;
import net.buildcraftreborn.core.marker.MarkerBlock;
import net.buildcraftreborn.energy.engine.EngineBlock;
import net.craftenergy.registry.DeferredRegister;
import net.craftenergy.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

/** Blocos do BuildCraft Reborn. */
public final class BCBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, BuildCraftReborn.MODID);

    // ── core: marcadores ──────────────────────────────────────────────────
    public static final RegistryObject<MarkerBlock> MARKER_VOLUME = BLOCKS.register("marker_volume",
            () -> new MarkerBlock(marker("marker_volume"), MarkerBlock.Kind.VOLUME));
    public static final RegistryObject<MarkerBlock> MARKER_PATH = BLOCKS.register("marker_path",
            () -> new MarkerBlock(marker("marker_path"), MarkerBlock.Kind.PATH));

    // ── core: fonte e decoração ───────────────────────────────────────────
    public static final RegistryObject<SpringBlock> WATER_SPRING = BLOCKS.register("water_spring",
            () -> new SpringBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).setId(BLOCKS.key("water_spring")),
                    () -> net.minecraft.world.level.material.Fluids.WATER));
    /** Fonte de petróleo: fica embaixo dos poços grandes e repõe o petróleo que a bomba tira. */
    public static final RegistryObject<SpringBlock> OIL_SPRING = BLOCKS.register("oil_spring",
            () -> new SpringBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).setId(BLOCKS.key("oil_spring")),
                    () -> net.buildcraftreborn.energy.fluid.BCFluids.get(net.buildcraftreborn.energy.fluid.BCFluids.Kind.OIL).fluid()));
    public static final RegistryObject<Block> DECORATED_DESTROY = decorated("decorated_destroy");
    public static final RegistryObject<Block> DECORATED_BLUEPRINT = decorated("decorated_blueprint");
    public static final RegistryObject<Block> DECORATED_TEMPLATE = decorated("decorated_template");
    public static final RegistryObject<Block> DECORATED_PAPER = decorated("decorated_paper");
    public static final RegistryObject<Block> DECORATED_LEATHER = decorated("decorated_leather");
    public static final RegistryObject<Block> DECORATED_LASER_BACK = decorated("decorated_laser_back");

    // ── energia: motores ──────────────────────────────────────────────────
    public static final RegistryObject<EngineBlock> REDSTONE_ENGINE = BLOCKS.register("redstone_engine",
            () -> new EngineBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("redstone_engine"))
                    .strength(2.0F, 3.0F).sound(SoundType.WOOD), EngineBlock.Kind.REDSTONE));
    public static final RegistryObject<EngineBlock> STIRLING_ENGINE = BLOCKS.register("stirling_engine",
            () -> new EngineBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("stirling_engine"))
                    .strength(3.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops(), EngineBlock.Kind.STIRLING));
    public static final RegistryObject<EngineBlock> CREATIVE_ENGINE = BLOCKS.register("creative_engine",
            () -> new EngineBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("creative_engine"))
                    .strength(3.0F, 6.0F).sound(SoundType.METAL), EngineBlock.Kind.CREATIVE));
    public static final RegistryObject<EngineBlock> COMBUSTION_ENGINE = BLOCKS.register("combustion_engine",
            () -> new EngineBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("combustion_engine"))
                    .strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops(), EngineBlock.Kind.COMBUSTION));

    // ── factory ───────────────────────────────────────────────────────────
    public static final RegistryObject<net.buildcraftreborn.factory.block.TankBlock> TANK = BLOCKS.register("tank",
            () -> new net.buildcraftreborn.factory.block.TankBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("tank"))
                    .strength(1.0F).sound(SoundType.GLASS)));
    public static final RegistryObject<net.buildcraftreborn.factory.block.FactoryMachineBlock> PUMP = BLOCKS.register("pump",
            () -> new net.buildcraftreborn.factory.block.FactoryMachineBlock(machine("pump"), net.buildcraftreborn.factory.block.FactoryMachineBlock.Kind.PUMP));
    public static final RegistryObject<net.buildcraftreborn.factory.block.MiningWellBlock> MINING_WELL = BLOCKS.register("mining_well",
            () -> new net.buildcraftreborn.factory.block.MiningWellBlock(machine("mining_well")));
    public static final RegistryObject<net.buildcraftreborn.factory.block.FactoryMachineBlock> FLOOD_GATE = BLOCKS.register("flood_gate",
            () -> new net.buildcraftreborn.factory.block.FactoryMachineBlock(machine("flood_gate"), net.buildcraftreborn.factory.block.FactoryMachineBlock.Kind.FLOOD_GATE));
    public static final RegistryObject<net.buildcraftreborn.factory.block.FactoryMachineBlock> AUTO_WORKBENCH = BLOCKS.register("auto_workbench",
            () -> new net.buildcraftreborn.factory.block.FactoryMachineBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("auto_workbench"))
                    .strength(2.5F).sound(SoundType.WOOD), net.buildcraftreborn.factory.block.FactoryMachineBlock.Kind.AUTO_WORKBENCH));
    public static final RegistryObject<net.buildcraftreborn.factory.block.MiningPipeBlock> MINING_PIPE = BLOCKS.register("mining_pipe",
            () -> new net.buildcraftreborn.factory.block.MiningPipeBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("mining_pipe"))
                    .strength(1.0F, 6.0F).sound(SoundType.METAL).noLootTable()));
    public static final RegistryObject<net.buildcraftreborn.factory.block.DistillerBlock> DISTILLER = BLOCKS.register("distiller",
            () -> new net.buildcraftreborn.factory.block.DistillerBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("distiller"))
                    .strength(2.0F, 6.0F).sound(SoundType.METAL)));
    public static final RegistryObject<net.buildcraftreborn.factory.block.HeatExchangerBlock> HEAT_EXCHANGER = BLOCKS.register("heat_exchanger",
            () -> new net.buildcraftreborn.factory.block.HeatExchangerBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("heat_exchanger"))
                    .strength(2.0F, 6.0F).sound(SoundType.METAL)));
    public static final RegistryObject<net.buildcraftreborn.factory.block.WaterGelBlock> WATER_GEL = BLOCKS.register("water_gel",
            () -> new net.buildcraftreborn.factory.block.WaterGelBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("water_gel"))
                    .strength(0.4F).sound(SoundType.SLIME_BLOCK)));

    // ── builders ──────────────────────────────────────────────────────────
    public static final RegistryObject<net.buildcraftreborn.builders.block.QuarryBlock> QUARRY = BLOCKS.register("quarry",
            () -> new net.buildcraftreborn.builders.block.QuarryBlock(machine("quarry")));
    public static final RegistryObject<net.buildcraftreborn.builders.block.FillerBlock> FILLER = BLOCKS.register("filler",
            () -> new net.buildcraftreborn.builders.block.FillerBlock(machine("filler")));
    public static final RegistryObject<net.buildcraftreborn.builders.block.FrameBlock> FRAME = BLOCKS.register("frame",
            () -> new net.buildcraftreborn.builders.block.FrameBlock(BlockBehaviour.Properties.of().setId(BLOCKS.key("frame"))
                    .strength(0.5F, 6.0F).sound(SoundType.METAL).noLootTable()));

    // ── silicon ───────────────────────────────────────────────────────────
    public static final RegistryObject<net.buildcraftreborn.silicon.block.LaserBlock> LASER = BLOCKS.register("laser",
            () -> new net.buildcraftreborn.silicon.block.LaserBlock(machine("laser")));
    public static final RegistryObject<net.buildcraftreborn.silicon.block.LaserTableBlock> ASSEMBLY_TABLE = BLOCKS.register("assembly_table",
            () -> new net.buildcraftreborn.silicon.block.LaserTableBlock(machine("assembly_table"),
                    net.buildcraftreborn.silicon.block.LaserTableBlock.Kind.ASSEMBLY));
    public static final RegistryObject<net.buildcraftreborn.silicon.block.LaserTableBlock> ADVANCED_CRAFTING_TABLE = BLOCKS.register("advanced_crafting_table",
            () -> new net.buildcraftreborn.silicon.block.LaserTableBlock(machine("advanced_crafting_table"),
                    net.buildcraftreborn.silicon.block.LaserTableBlock.Kind.ADVANCED_CRAFTING));

    // ── builders: arquiteto e construtor ──────────────────────────────────
    public static final RegistryObject<net.buildcraftreborn.builders.block.ArchitectTableBlock> ARCHITECT_TABLE = BLOCKS.register("architect_table",
            () -> new net.buildcraftreborn.builders.block.ArchitectTableBlock(machine("architect_table")));
    public static final RegistryObject<net.buildcraftreborn.builders.block.BuilderBlock> BUILDER = BLOCKS.register("builder",
            () -> new net.buildcraftreborn.builders.block.BuilderBlock(machine("builder")));
    public static final RegistryObject<net.buildcraftreborn.builders.block.LibraryBlock> LIBRARY = BLOCKS.register("library",
            () -> new net.buildcraftreborn.builders.block.LibraryBlock(machine("library")));
    public static final RegistryObject<net.buildcraftreborn.builders.block.ReplacerBlock> REPLACER = BLOCKS.register("replacer",
            () -> new net.buildcraftreborn.builders.block.ReplacerBlock(machine("replacer")));

    public static final RegistryObject<net.buildcraftreborn.transport.block.FilteredBufferBlock> FILTERED_BUFFER = BLOCKS.register("filtered_buffer",
            () -> new net.buildcraftreborn.transport.block.FilteredBufferBlock(machine("filtered_buffer")));

    // ── transport: tubos ──────────────────────────────────────────────────
    public static final java.util.Map<net.buildcraftreborn.transport.PipeType, RegistryObject<net.buildcraftreborn.transport.block.PipeBlock>> PIPES =
            new java.util.EnumMap<>(net.buildcraftreborn.transport.PipeType.class);

    static {
        for (net.buildcraftreborn.transport.PipeType type : net.buildcraftreborn.transport.PipeType.values()) {
            String name = type.blockId();
            PIPES.put(type, BLOCKS.register(name, () -> type == net.buildcraftreborn.transport.PipeType.LAPIS
                    ? new net.buildcraftreborn.transport.block.ColoredPipeBlock(pipe(name), type, net.buildcraftreborn.transport.PipeFlow.ITEM)
                    : type == net.buildcraftreborn.transport.PipeType.DAIZULI
                    ? new net.buildcraftreborn.transport.block.DaizuliPipeBlock(pipe(name), type, net.buildcraftreborn.transport.PipeFlow.ITEM)
                    : type.directional()
                    ? new net.buildcraftreborn.transport.block.DirectionalPipeBlock(pipe(name), type, net.buildcraftreborn.transport.PipeFlow.ITEM)
                    : new net.buildcraftreborn.transport.block.PipeBlock(pipe(name), type, net.buildcraftreborn.transport.PipeFlow.ITEM)));
        }
    }

    /** Tubos de fluidos (sem obsidiana e estrutura). */
    public static final java.util.Map<net.buildcraftreborn.transport.PipeType, RegistryObject<net.buildcraftreborn.transport.block.PipeBlock>> FLUID_PIPES =
            new java.util.EnumMap<>(net.buildcraftreborn.transport.PipeType.class);

    static {
        for (net.buildcraftreborn.transport.PipeType type : net.buildcraftreborn.transport.PipeType.values()) {
            if (!type.hasFluidPipe()) continue;
            String name = type.fluidBlockId();
            FLUID_PIPES.put(type, BLOCKS.register(name, () -> type.directional()
                    ? new net.buildcraftreborn.transport.block.DirectionalPipeBlock(pipe(name), type, net.buildcraftreborn.transport.PipeFlow.FLUID)
                    : new net.buildcraftreborn.transport.block.PipeBlock(pipe(name), type, net.buildcraftreborn.transport.PipeFlow.FLUID)));
        }
    }

    private BCBlocks() {}

    private static BlockBehaviour.Properties pipe(String name) {
        return BlockBehaviour.Properties.of().setId(BLOCKS.key(name)).strength(0.25F).sound(SoundType.GLASS);
    }

    private static BlockBehaviour.Properties machine(String name) {
        return BlockBehaviour.Properties.of().setId(BLOCKS.key(name)).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties marker(String name) {
        return BlockBehaviour.Properties.of().setId(BLOCKS.key(name)).noCollision().instabreak().sound(SoundType.WOOD)
                .lightLevel(state -> 7).pushReaction(PushReaction.DESTROY);
    }

    private static RegistryObject<Block> decorated(String name) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of().setId(BLOCKS.key(name))
                .strength(1.5F, 6.0F).sound(SoundType.STONE)));
    }
}
