package net.buildcraftreborn;

import com.mojang.logging.LogUtils;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.buildcraftreborn.registry.BCBlocks;
import net.buildcraftreborn.registry.BCComponents;
import net.buildcraftreborn.registry.BCFeatures;
import net.buildcraftreborn.registry.BCItems;
import net.buildcraftreborn.registry.BCMenus;
import net.craftenergy.registry.DeferredRegister;
import net.craftenergy.registry.RegistryObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.GenerationStep;
import org.slf4j.Logger;

/**
 * BuildCraft Reborn: port do BuildCraft 8.0.0 (SpaceToad e BuildCraft Team) para Fabric, com energia do
 * Craft Energy no lugar do MJ (1 MJ = 1 CWh, 1 MJ/t = 1.000 CW).
 */
public final class BuildCraftReborn implements ModInitializer {
    public static final String MODID = "buildcraftreborn";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Configuração lida de {@code config/buildcraftreborn.json}; os padrões valem até o mod carregar. */
    public static BCConfig config = BCConfig.defaults();

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register(MODID, () -> FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.buildcraftreborn"))
            .icon(() -> new ItemStack(BCItems.WRENCH.get()))
            .displayItems((params, output) -> BCItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.get())))
            .build());

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    @Override
    public void onInitialize() {
        config = BCConfig.load(FabricLoader.getInstance().getConfigDir().resolve(MODID + ".json"));

        BCComponents.COMPONENTS.register();
        BCBlocks.BLOCKS.register();
        BCItems.ITEMS.register();
        BCBlockEntities.BLOCK_ENTITIES.register();
        BCMenus.MENUS.register();
        BCFeatures.FEATURES.register();
        TABS.register();

        // motores: saída de energia pela frente e combustível do Stirling por funis e tubos
        net.craftenergy.fabric.CraftEnergyApi.NODE.registerForBlockEntity((engine, face) -> engine.energyNode(face), BCBlockEntities.ENGINE.get());
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity(
                (engine, face) -> engine.kind() == net.buildcraftreborn.energy.engine.EngineBlock.Kind.STIRLING
                        ? net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(engine.fuel(), face) : null,
                BCBlockEntities.ENGINE.get());

        if (config.waterSprings) {
            BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_DECORATION,
                    ResourceKey.create(Registries.PLACED_FEATURE, id("water_spring")));
        }

        LOGGER.info("BuildCraft Reborn carregado");
    }
}
