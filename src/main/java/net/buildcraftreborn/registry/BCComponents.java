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

    /** Variante da porta lógica (lógica, material e modificador). */
    public static final RegistryObject<DataComponentType<net.buildcraftreborn.transport.gate.GateVariant>> GATE_VARIANT = COMPONENTS.register("gate_variant",
            () -> DataComponentType.<net.buildcraftreborn.transport.gate.GateVariant>builder()
                    .persistent(net.buildcraftreborn.transport.gate.GateVariant.CODEC)
                    .networkSynchronized(net.buildcraftreborn.transport.gate.GateVariant.STREAM_CODEC)
                    .build());

    /** Cabeçalho do molde/planta escaneado (hash do arquivo no mundo, nome, autor). */
    public static final RegistryObject<DataComponentType<net.buildcraftreborn.builders.snapshot.SnapshotHeader>> SNAPSHOT = COMPONENTS.register("snapshot",
            () -> DataComponentType.<net.buildcraftreborn.builders.snapshot.SnapshotHeader>builder()
                    .persistent(net.buildcraftreborn.builders.snapshot.SnapshotHeader.CODEC)
                    .networkSynchronized(net.buildcraftreborn.builders.snapshot.SnapshotHeader.STREAM_CODEC)
                    .build());

    /** Estado de bloco guardado no esquema de bloco único. */
    public static final RegistryObject<DataComponentType<net.minecraft.world.level.block.state.BlockState>> SCHEMATIC_STATE = COMPONENTS.register("schematic_state",
            () -> DataComponentType.<net.minecraft.world.level.block.state.BlockState>builder()
                    .persistent(net.minecraft.world.level.block.state.BlockState.CODEC)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.idMapper(net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY))
                    .build());

    /** Lente ou filtro: cor (-1 transparente) e se é filtro. */
    public static final RegistryObject<DataComponentType<net.buildcraftreborn.transport.plug.LensItem.Data>> LENS = COMPONENTS.register("lens",
            () -> DataComponentType.<net.buildcraftreborn.transport.plug.LensItem.Data>builder()
                    .persistent(net.buildcraftreborn.transport.plug.LensItem.Data.CODEC)
                    .networkSynchronized(net.buildcraftreborn.transport.plug.LensItem.Data.STREAM_CODEC)
                    .build());

    /** Fachada: estado do bloco e se é vazada. */
    public static final RegistryObject<DataComponentType<net.buildcraftreborn.transport.plug.FacadeItem.Data>> FACADE = COMPONENTS.register("facade",
            () -> DataComponentType.<net.buildcraftreborn.transport.plug.FacadeItem.Data>builder()
                    .persistent(net.buildcraftreborn.transport.plug.FacadeItem.Data.CODEC)
                    .networkSynchronized(net.buildcraftreborn.transport.plug.FacadeItem.Data.STREAM_CODEC)
                    .build());

    private BCComponents() {}
}
