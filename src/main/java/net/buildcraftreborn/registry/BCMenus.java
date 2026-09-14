package net.buildcraftreborn.registry;

import net.buildcraftreborn.BuildCraftReborn;
import net.craftenergy.registry.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

/** Tipos de menu (telas com inventário) do BuildCraft Reborn. */
public final class BCMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, BuildCraftReborn.MODID);

    private BCMenus() {}
}
