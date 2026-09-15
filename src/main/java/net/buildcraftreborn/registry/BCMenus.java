package net.buildcraftreborn.registry;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.core.list.ListMenu;
import net.craftenergy.registry.DeferredRegister;
import net.craftenergy.registry.RegistryObject;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.MenuType;

/** Tipos de menu (telas com inventário) do BuildCraft Reborn. */
public final class BCMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BuildCraftReborn.MODID);

    /** Lista; o cliente recebe a mão que está segurando o item. */
    public static final RegistryObject<ExtendedMenuType<ListMenu, InteractionHand>> LIST = MENUS.register("list",
            () -> new ExtendedMenuType<>(ListMenu::new, ListMenu.HAND_CODEC));

    /** Motor Stirling; o cliente recebe a posição do motor. */
    public static final RegistryObject<ExtendedMenuType<net.buildcraftreborn.energy.engine.StirlingEngineMenu, net.minecraft.core.BlockPos>> STIRLING_ENGINE =
            MENUS.register("stirling_engine", () -> new ExtendedMenuType<>(net.buildcraftreborn.energy.engine.StirlingEngineMenu::new,
                    net.minecraft.core.BlockPos.STREAM_CODEC));

    public static final RegistryObject<ExtendedMenuType<net.buildcraftreborn.factory.AutoWorkbenchMenu, net.minecraft.core.BlockPos>> AUTO_WORKBENCH =
            MENUS.register("auto_workbench", () -> new ExtendedMenuType<>(net.buildcraftreborn.factory.AutoWorkbenchMenu::new, net.minecraft.core.BlockPos.STREAM_CODEC));

    public static final RegistryObject<ExtendedMenuType<net.buildcraftreborn.transport.PipeFilterMenu, net.minecraft.core.BlockPos>> PIPE_FILTER =
            MENUS.register("pipe_filter", () -> new ExtendedMenuType<>(net.buildcraftreborn.transport.PipeFilterMenu::new, net.minecraft.core.BlockPos.STREAM_CODEC));

    private BCMenus() {}
}
