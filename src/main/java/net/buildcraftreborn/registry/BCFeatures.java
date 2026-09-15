package net.buildcraftreborn.registry;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.core.world.SpringFeature;
import net.craftenergy.registry.DeferredRegister;
import net.craftenergy.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Geração de mundo do BuildCraft Reborn (os posicionamentos ficam em data/buildcraftreborn/worldgen). */
public final class BCFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, BuildCraftReborn.MODID);

    public static final RegistryObject<SpringFeature> WATER_SPRING = FEATURES.register("water_spring", SpringFeature::new);
    public static final RegistryObject<net.buildcraftreborn.energy.world.OilWellFeature> OIL_WELL =
            FEATURES.register("oil_well", net.buildcraftreborn.energy.world.OilWellFeature::new);

    private BCFeatures() {}
}
