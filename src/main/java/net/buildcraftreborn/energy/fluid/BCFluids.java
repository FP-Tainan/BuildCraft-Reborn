package net.buildcraftreborn.energy.fluid;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.registry.BCBlocks;
import net.buildcraftreborn.registry.BCItems;
import net.craftenergy.registry.DeferredRegister;
import net.craftenergy.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Os fluidos do petróleo ({@code BCEnergyFluids}): 10 tipos, cada um em 3 temperaturas (frio, quente e
 * escaldante), com bloco no mundo e balde. Precisa carregar antes de {@link BCBlocks#BLOCKS} e
 * {@link BCItems#ITEMS} registrarem, porque põe os blocos e baldes neles.
 */
public final class BCFluids {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, BuildCraftReborn.MODID);

    /** Densidade, viscosidade, espalhamento, cor clara, cor escura, pegajoso e inflamável (valores do BuildCraft). */
    public enum Kind {
        OIL("oil", 900, 2000, 6, 0x505050, 0x050505, true, true),
        OIL_RESIDUE("oil_residue", 1200, 4000, 4, 0x100F10, 0x421042, true, false),
        OIL_HEAVY("oil_heavy", 850, 1800, 6, 0xA08F1F, 0x423520, true, true),
        OIL_DENSE("oil_dense", 950, 1600, 5, 0x876E77, 0x422424, true, true),
        OIL_DISTILLED("oil_distilled", 750, 1400, 8, 0xE4AF78, 0xB47F00, false, true),
        FUEL_DENSE("fuel_dense", 600, 800, 7, 0xFFAF3F, 0xE07F00, false, true),
        FUEL_MIXED_HEAVY("fuel_mixed_heavy", 700, 1000, 7, 0xF2A700, 0xC48700, false, true),
        FUEL_LIGHT("fuel_light", 400, 600, 8, 0xFFFF30, 0xE4CF00, false, true),
        FUEL_MIXED_LIGHT("fuel_mixed_light", 650, 900, 9, 0xF6D700, 0xC4B700, false, true),
        FUEL_GASEOUS("fuel_gaseous", 300, 500, 10, 0xFAF630, 0xE0D900, false, true);

        public final String id;
        public final int density;
        public final int viscosity;
        public final int spread;
        public final int lightColor;
        public final int darkColor;
        public final boolean sticky;
        public final boolean flammable;

        Kind(String id, int density, int viscosity, int spread, int lightColor, int darkColor, boolean sticky, boolean flammable) {
            this.id = id;
            this.density = density;
            this.viscosity = viscosity;
            this.spread = spread;
            this.lightColor = lightColor;
            this.darkColor = darkColor;
            this.sticky = sticky;
            this.flammable = flammable;
        }
    }

    public enum Heat {
        COOL(""),
        HOT("_hot"),
        SEARING("_searing");

        public final String suffix;

        Heat(String suffix) {
            this.suffix = suffix;
        }
    }

    private static final List<Entry> ALL = new ArrayList<>();
    private static final Map<Kind, Entry[]> BY_KIND = new EnumMap<>(Kind.class);

    static {
        for (Kind kind : Kind.values()) {
            Entry[] entries = new Entry[Heat.values().length];
            for (Heat heat : Heat.values()) entries[heat.ordinal()] = register(kind, heat);
            BY_KIND.put(kind, entries);
        }
    }

    private BCFluids() {}

    private static Entry register(Kind kind, Heat heat) {
        String name = kind.id + heat.suffix;
        Entry entry = new Entry(kind, heat, name);
        entry.source = FLUIDS.register(name, () -> new BCFluid.Source(entry));
        entry.flowing = FLUIDS.register("flowing_" + name, () -> new BCFluid.Flowing(entry));
        entry.block = BCBlocks.BLOCKS.register(name, () -> new BCFluidBlock(entry,
                BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).setId(BCBlocks.BLOCKS.key(name)).noLootTable()));
        String bucket = name + "_bucket";
        entry.bucket = BCItems.ITEMS.register(bucket, () -> new BucketItem(entry.source.get(),
                new Item.Properties().setId(BCItems.ITEMS.key(bucket)).craftRemainder(Items.BUCKET).stacksTo(1)));
        ALL.add(entry);
        return entry;
    }

    public static List<Entry> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static Entry get(Kind kind, Heat heat) {
        return BY_KIND.get(kind)[heat.ordinal()];
    }

    /** A versão fria (a que se bombeia do mundo e queima no motor). */
    public static Entry get(Kind kind) {
        return get(kind, Heat.COOL);
    }

    public static @Nullable Entry of(Fluid fluid) {
        for (Entry entry : ALL) {
            if (entry.fluid() == fluid || entry.flowing() == fluid) return entry;
        }
        return null;
    }

    public static final class Entry {
        private final Kind kind;
        private final Heat heat;
        private final String name;
        private RegistryObject<BCFluid.Source> source;
        private RegistryObject<BCFluid.Flowing> flowing;
        private RegistryObject<BCFluidBlock> block;
        private RegistryObject<Item> bucket;

        private Entry(Kind kind, Heat heat, String name) {
            this.kind = kind;
            this.heat = heat;
            this.name = name;
        }

        public Kind kind() {
            return this.kind;
        }

        public Heat heat() {
            return this.heat;
        }

        public String name() {
            return this.name;
        }

        public BCFluid.Source fluid() {
            return this.source.get();
        }

        public BCFluid.Flowing flowing() {
            return this.flowing.get();
        }

        public RegistryObject<BCFluidBlock> block() {
            return this.block;
        }

        public RegistryObject<Item> bucket() {
            return this.bucket;
        }
    }
}
