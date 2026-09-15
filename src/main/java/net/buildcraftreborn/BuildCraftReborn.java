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
            .displayItems((params, output) -> {
                BCItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.get()));
                net.buildcraftreborn.transport.gate.GateItem.addCreativeVariants(output);
                net.buildcraftreborn.transport.plug.LensItem.addCreativeVariants(output);
                net.buildcraftreborn.transport.plug.FacadeItem.addCreativeVariants(output);
            })
            .build());

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    @Override
    public void onInitialize() {
        config = BCConfig.load(FabricLoader.getInstance().getConfigDir().resolve(MODID + ".json"));

        BCComponents.COMPONENTS.register();
        // fluidos antes de blocos e itens: a classe põe os blocos e baldes do petróleo nesses registros
        net.buildcraftreborn.energy.fluid.BCFluids.FLUIDS.register();
        BCBlocks.BLOCKS.register();
        BCItems.ITEMS.register();
        BCBlockEntities.BLOCK_ENTITIES.register();
        BCMenus.MENUS.register();
        BCFeatures.FEATURES.register();
        net.buildcraftreborn.registry.BCRecipes.RECIPE_SERIALIZERS.register();
        TABS.register();

        // motores: saída de energia pela frente e combustível do Stirling por funis e tubos
        net.craftenergy.fabric.CraftEnergyApi.NODE.registerForBlockEntity((engine, face) -> engine.energyNode(face), BCBlockEntities.ENGINE.get());
        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlockEntity(
                (engine, face) -> engine.kind() == net.buildcraftreborn.energy.engine.EngineBlock.Kind.STIRLING
                        ? net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage.of(engine.fuel(), face)
                        : engine.combustion() != null ? engine.combustion().itemStorage() : null,
                BCBlockEntities.ENGINE.get());

        registerFactoryStorages();
        registerFuels();
        net.buildcraftreborn.core.item.GuideBookItem.init();

        if (config.waterSprings) {
            BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_DECORATION,
                    ResourceKey.create(Registries.PLACED_FEATURE, id("water_spring")));
        }
        if (config.oilWorldgen) {
            // roda uma vez por chunk; a própria feature sorteia os poços e respeita a configuração
            BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.LAKES,
                    ResourceKey.create(Registries.PLACED_FEATURE, id("oil_well")));
        }

        LOGGER.info("BuildCraft Reborn carregado");
    }

    /** Petróleo: combustíveis e refrigerantes do motor a combustão; fluidos inflamáveis pegam fogo. */
    private static void registerFuels() {
        net.buildcraftreborn.energy.fluid.BCFuels.init();
        net.buildcraftreborn.factory.refinery.RefineryRecipes.init();
        var flammable = net.fabricmc.fabric.api.registry.FlammableBlockRegistry.getDefaultInstance();
        for (net.buildcraftreborn.energy.fluid.BCFluids.Entry entry : net.buildcraftreborn.energy.fluid.BCFluids.all()) {
            if (entry.kind().flammable) flammable.add(entry.block().get(), 30, 60);
        }
    }

    /** Máquinas do factory: energia por todas as faces, fluidos e itens pelo Transfer API. */
    private static void registerFactoryStorages() {
        var node = net.craftenergy.fabric.CraftEnergyApi.NODE;
        node.registerForBlockEntity((pump, face) -> pump.energy(), BCBlockEntities.PUMP.get());
        node.registerForBlockEntity((well, face) -> well.energy(), BCBlockEntities.MINING_WELL.get());
        node.registerForBlockEntity((workbench, face) -> workbench.energy(), BCBlockEntities.AUTO_WORKBENCH.get());
        node.registerForBlockEntity((quarry, face) -> quarry.energy(), BCBlockEntities.QUARRY.get());
        node.registerForBlockEntity((pipe, face) -> pipe.energy(), BCBlockEntities.PIPE.get());
        node.registerForBlockEntity((pipe, face) -> pipe.energy(), BCBlockEntities.FLUID_PIPE.get());
        node.registerForBlockEntity((distiller, face) -> distiller.energy(), BCBlockEntities.DISTILLER.get());
        node.registerForBlockEntity((laser, face) -> laser.energy(), BCBlockEntities.LASER.get());
        node.registerForBlockEntity((filler, face) -> filler.energy(), BCBlockEntities.FILLER.get());

        var fluids = net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED;

        fluids.registerForBlockEntity((pipe, face) -> pipe.storage(face), BCBlockEntities.FLUID_PIPE.get());
        fluids.registerForBlockEntity((engine, face) -> engine.combustion() != null ? engine.combustion().fluidStorage() : null,
                BCBlockEntities.ENGINE.get());
        fluids.registerForBlockEntity((tank, face) -> tank.tank(), BCBlockEntities.TANK.get());
        fluids.registerForBlockEntity((pump, face) -> pump.tank(), BCBlockEntities.PUMP.get());
        fluids.registerForBlockEntity((gate, face) -> gate.tank(), BCBlockEntities.FLOOD_GATE.get());
        fluids.registerForBlockEntity((distiller, face) -> distiller.storage(face), BCBlockEntities.DISTILLER.get());
        fluids.registerForBlockEntity((exchanger, face) -> exchanger.storage(face), BCBlockEntities.HEAT_EXCHANGER.get());

        var items = net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED;
        items.registerForBlockEntity((workbench, face) -> workbench.itemStorage(face), BCBlockEntities.AUTO_WORKBENCH.get());
        items.registerForBlockEntity((pipe, face) -> pipe.insertion(face), BCBlockEntities.PIPE.get());
        items.registerForBlockEntity((table, face) -> table.itemStorage(face), BCBlockEntities.ASSEMBLY_TABLE.get());
        items.registerForBlockEntity((table, face) -> table.itemStorage(face), BCBlockEntities.ADVANCED_CRAFTING_TABLE.get());
        items.registerForBlockEntity((filler, face) -> filler.itemStorage(face), BCBlockEntities.FILLER.get());
        items.registerForBlockEntity((table, face) -> table.itemStorage(face), BCBlockEntities.ARCHITECT_TABLE.get());
        items.registerForBlockEntity((builder, face) -> builder.itemStorage(face), BCBlockEntities.BUILDER.get());
        items.registerForBlockEntity((buffer, face) -> buffer.itemStorage(face), BCBlockEntities.FILTERED_BUFFER.get());
        node.registerForBlockEntity((builder, face) -> builder.energy(), BCBlockEntities.BUILDER.get());
        fluids.registerForBlockEntity((builder, face) -> builder.fluidStorage(), BCBlockEntities.BUILDER.get());
    }
}
