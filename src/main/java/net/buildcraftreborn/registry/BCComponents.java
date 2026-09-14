package net.buildcraftreborn.registry;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.core.list.ListContents;
import net.craftenergy.registry.DeferredRegister;
import net.craftenergy.registry.RegistryObject;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

/** Componentes de dados de item do BuildCraft Reborn. */
public final class BCComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BuildCraftReborn.MODID);

    /** Itens e opções anotados numa lista. */
    public static final RegistryObject<DataComponentType<ListContents>> LIST_CONTENTS = COMPONENTS.register("list_contents",
            () -> DataComponentType.<ListContents>builder()
                    .persistent(ListContents.CODEC)
                    .networkSynchronized(ListContents.STREAM_CODEC)
                    .build());

    private BCComponents() {}
}
