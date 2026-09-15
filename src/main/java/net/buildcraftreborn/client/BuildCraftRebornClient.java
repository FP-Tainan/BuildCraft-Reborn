package net.buildcraftreborn.client;

import net.buildcraftreborn.client.render.EngineRenderer;
import net.buildcraftreborn.client.render.MarkerRenderer;
import net.buildcraftreborn.client.render.PipeItemRenderer;
import net.buildcraftreborn.client.render.QuarryRenderer;
import net.buildcraftreborn.client.render.TankRenderer;
import net.buildcraftreborn.client.screen.AutoWorkbenchScreen;
import net.buildcraftreborn.client.screen.ListScreen;
import net.buildcraftreborn.client.screen.PipeFilterScreen;
import net.buildcraftreborn.client.screen.StirlingEngineScreen;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.buildcraftreborn.registry.BCMenus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;

/** Registros do lado do cliente: telas, renderizadores e cores. */
public final class BuildCraftRebornClient implements ClientModInitializer {
    @Override
    @SuppressWarnings("deprecation")
    public void onInitializeClient() {
        net.buildcraftreborn.core.item.GuideBookItem.opener = () -> net.minecraft.client.Minecraft.getInstance()
                .setScreenAndShow(new net.buildcraftreborn.client.screen.GuideBookScreen());
        MenuScreens.register(BCMenus.LIST.get(), ListScreen::new);
        MenuScreens.register(BCMenus.STIRLING_ENGINE.get(), StirlingEngineScreen::new);
        MenuScreens.register(BCMenus.AUTO_WORKBENCH.get(), AutoWorkbenchScreen::new);
        MenuScreens.register(BCMenus.PIPE_FILTER.get(), PipeFilterScreen::new);
        MenuScreens.register(BCMenus.COMBUSTION_ENGINE.get(), net.buildcraftreborn.client.screen.CombustionEngineScreen::new);
        MenuScreens.register(BCMenus.ASSEMBLY_TABLE.get(), net.buildcraftreborn.client.screen.AssemblyTableScreen::new);
        MenuScreens.register(BCMenus.ADVANCED_CRAFTING_TABLE.get(), net.buildcraftreborn.client.screen.AdvancedCraftingTableScreen::new);
        MenuScreens.register(BCMenus.FILLER.get(), net.buildcraftreborn.client.screen.FillerScreen::new);
        MenuScreens.register(BCMenus.GATE.get(), net.buildcraftreborn.client.screen.GateScreen::new);
        MenuScreens.register(BCMenus.ARCHITECT_TABLE.get(), net.buildcraftreborn.client.screen.ArchitectTableScreen::new);
        MenuScreens.register(BCMenus.BUILDER.get(), net.buildcraftreborn.client.screen.BuilderScreen::new);
        MenuScreens.register(BCMenus.LIBRARY.get(), net.buildcraftreborn.client.screen.LibraryScreen::new);
        MenuScreens.register(BCMenus.DIAMOND_WOOD_PIPE.get(), net.buildcraftreborn.client.screen.DiamondWoodPipeScreen::new);
        MenuScreens.register(BCMenus.EMZULI_PIPE.get(), net.buildcraftreborn.client.screen.EmzuliPipeScreen::new);
        MenuScreens.register(BCMenus.FILTERED_BUFFER.get(), net.buildcraftreborn.client.screen.FilteredBufferScreen::new);
        MenuScreens.register(BCMenus.REPLACER.get(), net.buildcraftreborn.client.screen.ReplacerScreen::new);
        BlockEntityRendererRegistry.register(BCBlockEntities.ARCHITECT_TABLE.get(), net.buildcraftreborn.client.render.AreaRenderer::new);
        BlockEntityRendererRegistry.register(BCBlockEntities.BUILDER.get(), net.buildcraftreborn.client.render.AreaRenderer::new);
        BlockEntityRendererRegistry.register(BCBlockEntities.MARKER.get(), MarkerRenderer::new);
        BlockEntityRendererRegistry.register(BCBlockEntities.ENGINE.get(), EngineRenderer::new);
        BlockEntityRendererRegistry.register(BCBlockEntities.TANK.get(), TankRenderer::new);
        BlockEntityRendererRegistry.register(BCBlockEntities.QUARRY.get(), QuarryRenderer::new);
        BlockEntityRendererRegistry.register(BCBlockEntities.PIPE.get(), PipeItemRenderer::new);
        BlockEntityRendererRegistry.register(BCBlockEntities.FLUID_PIPE.get(), net.buildcraftreborn.client.render.FluidPipeRenderer::new);
        BlockEntityRendererRegistry.register(BCBlockEntities.DISTILLER.get(), net.buildcraftreborn.client.render.DistillerRenderer::new);
        BlockEntityRendererRegistry.register(BCBlockEntities.LASER.get(), net.buildcraftreborn.client.render.LaserBeamRenderer::new);

        // fluidos do petróleo no mundo: texturas geradas por cor (parada e escorrendo)
        for (net.buildcraftreborn.energy.fluid.BCFluids.Entry entry : net.buildcraftreborn.energy.fluid.BCFluids.all()) {
            net.minecraft.resources.Identifier still = net.buildcraftreborn.BuildCraftReborn.id("block/fluid/" + entry.name() + "_still");
            net.minecraft.resources.Identifier flow = net.buildcraftreborn.BuildCraftReborn.id("block/fluid/" + entry.name() + "_flow");
            net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry.register(entry.fluid(), entry.flowing(),
                    new net.minecraft.client.renderer.block.FluidModel.Unbaked(
                            new net.minecraft.client.resources.model.sprite.Material(still, true),
                            new net.minecraft.client.resources.model.sprite.Material(flow, true), null, null));
        }
    }
}
