package net.buildcraftreborn.client;

import net.buildcraftreborn.client.render.EngineRenderer;
import net.buildcraftreborn.client.render.MarkerRenderer;
import net.buildcraftreborn.client.screen.ListScreen;
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
        MenuScreens.register(BCMenus.LIST.get(), ListScreen::new);
        MenuScreens.register(BCMenus.STIRLING_ENGINE.get(), StirlingEngineScreen::new);
        BlockEntityRendererRegistry.register(BCBlockEntities.MARKER.get(), MarkerRenderer::new);
        BlockEntityRendererRegistry.register(BCBlockEntities.ENGINE.get(), EngineRenderer::new);
    }
}
