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
            () -> new SpringBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BEDROCK).setId(BLOCKS.key("water_spring"))));
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

    private BCBlocks() {}

    private static BlockBehaviour.Properties marker(String name) {
        return BlockBehaviour.Properties.of().setId(BLOCKS.key(name)).noCollision().instabreak().sound(SoundType.WOOD)
                .lightLevel(state -> 7).pushReaction(PushReaction.DESTROY);
    }

    private static RegistryObject<Block> decorated(String name) {
        return BLOCKS.register(name, () -> new Block(BlockBehaviour.Properties.of().setId(BLOCKS.key(name))
                .strength(1.5F, 6.0F).sound(SoundType.STONE)));
    }
}
