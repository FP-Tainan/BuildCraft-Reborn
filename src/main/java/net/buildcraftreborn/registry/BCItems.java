package net.buildcraftreborn.registry;

import net.buildcraftreborn.BuildCraftReborn;
import net.craftenergy.registry.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/** Itens do BuildCraft Reborn; todos aparecem na aba criativa, na ordem de registro. */
public final class BCItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, BuildCraftReborn.MODID);

    private BCItems() {}
}
