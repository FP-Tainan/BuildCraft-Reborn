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

    public static final RegistryObject<ExtendedMenuType<net.buildcraftreborn.energy.engine.CombustionEngineMenu, net.minecraft.core.BlockPos>> COMBUSTION_ENGINE =
            MENUS.register("combustion_engine", () -> new ExtendedMenuType<>(net.buildcraftreborn.energy.engine.CombustionEngineMenu::new,
                    net.minecraft.core.BlockPos.STREAM_CODEC));

    public static final RegistryObject<ExtendedMenuType<net.buildcraftreborn.silicon.AssemblyTableMenu, net.minecraft.core.BlockPos>> ASSEMBLY_TABLE =
            MENUS.register("assembly_table", () -> new ExtendedMenuType<>(net.buildcraftreborn.silicon.AssemblyTableMenu::new,
                    net.minecraft.core.BlockPos.STREAM_CODEC));
    public static final RegistryObject<ExtendedMenuType<net.buildcraftreborn.silicon.AdvancedCraftingTableMenu, net.minecraft.core.BlockPos>> ADVANCED_CRAFTING_TABLE =
            MENUS.register("advanced_crafting_table", () -> new ExtendedMenuType<>(net.buildcraftreborn.silicon.AdvancedCraftingTableMenu::new,
                    net.minecraft.core.BlockPos.STREAM_CODEC));

    public static final RegistryObject<ExtendedMenuType<net.buildcraftreborn.builders.FillerMenu, net.minecraft.core.BlockPos>> FILLER =
            MENUS.register("filler", () -> new ExtendedMenuType<>(net.buildcraftreborn.builders.FillerMenu::new, net.minecraft.core.BlockPos.STREAM_CODEC));

    public static final RegistryObject<ExtendedMenuType<net.buildcraftreborn.builders.ArchitectTableMenu, net.minecraft.core.BlockPos>> ARCHITECT_TABLE =
            MENUS.register("architect_table", () -> new ExtendedMenuType<>(net.buildcraftreborn.builders.ArchitectTableMenu::new,
                    net.minecraft.core.BlockPos.STREAM_CODEC));
    public static final RegistryObject<ExtendedMenuType<net.buildcraftreborn.builders.BuilderMenu, net.minecraft.core.BlockPos>> BUILDER =
            MENUS.register("builder", () -> new ExtendedMenuType<>(net.buildcraftreborn.builders.BuilderMenu::new, net.minecraft.core.BlockPos.STREAM_CODEC));

    public static final RegistryObject<ExtendedMenuType<net.buildcraftreborn.builders.LibraryMenu, net.minecraft.core.BlockPos>> LIBRARY =
            MENUS.register("library", () -> new ExtendedMenuType<>(net.buildcraftreborn.builders.LibraryMenu::new, net.minecraft.core.BlockPos.STREAM_CODEC));
    public static final RegistryObject<ExtendedMenuType<net.buildcraftreborn.builders.ReplacerMenu, net.minecraft.core.BlockPos>> REPLACER =
            MENUS.register("replacer", () -> new ExtendedMenuType<>(net.buildcraftreborn.builders.ReplacerMenu::new, net.minecraft.core.BlockPos.STREAM_CODEC));

    public static final RegistryObject<ExtendedMenuType<net.buildcraftreborn.transport.DiamondWoodPipeMenu, net.minecraft.core.BlockPos>> DIAMOND_WOOD_PIPE =
            MENUS.register("diamond_wood_pipe", () -> new ExtendedMenuType<>(net.buildcraftreborn.transport.DiamondWoodPipeMenu::new,
                    net.minecraft.core.BlockPos.STREAM_CODEC));
    public static final RegistryObject<ExtendedMenuType<net.buildcraftreborn.transport.EmzuliPipeMenu, net.minecraft.core.BlockPos>> EMZULI_PIPE =
            MENUS.register("emzuli_pipe", () -> new ExtendedMenuType<>(net.buildcraftreborn.transport.EmzuliPipeMenu::new, net.minecraft.core.BlockPos.STREAM_CODEC));

    public static final RegistryObject<ExtendedMenuType<net.buildcraftreborn.transport.FilteredBufferMenu, net.minecraft.core.BlockPos>> FILTERED_BUFFER =
            MENUS.register("filtered_buffer", () -> new ExtendedMenuType<>(net.buildcraftreborn.transport.FilteredBufferMenu::new,
                    net.minecraft.core.BlockPos.STREAM_CODEC));

    /** Porta lógica; o cliente recebe o tubo e a face. */
    public static final RegistryObject<ExtendedMenuType<net.buildcraftreborn.transport.gate.GateMenu, net.buildcraftreborn.transport.gate.GateMenu.Target>> GATE =
            MENUS.register("gate", () -> new ExtendedMenuType<>(net.buildcraftreborn.transport.gate.GateMenu::new,
                    net.buildcraftreborn.transport.gate.GateMenu.Target.STREAM_CODEC));

    private BCMenus() {}
}
