package net.buildcraftreborn;

import com.mojang.logging.LogUtils;
import net.buildcraftreborn.registry.BCBlockEntities;
import net.buildcraftreborn.registry.BCBlocks;
import net.buildcraftreborn.registry.BCItems;
import net.buildcraftreborn.registry.BCMenus;
import net.craftenergy.registry.DeferredRegister;
import net.craftenergy.registry.RegistryObject;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

/**
 * BuildCraft Reborn: port do BuildCraft 8.0.0 (SpaceToad e BuildCraft Team) para Fabric, com energia do
 * Craft Energy no lugar do MJ (1 MJ = 1 CWh, 1 MJ/t = 1.000 CW).
 */
public final class BuildCraftReborn implements ModInitializer {
    public static final String MODID = "buildcraftreborn";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** Configuração lida de {@code config/buildcraftreborn.json}; os padrões valem até o mod carregar. */
    public static BCConfig config = BCConfig.defaults();

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register(MODID, () -> FabricCreativeModeTab.builder()
            .title(Component.translatable("itemGroup.buildcraftreborn"))
            .icon(() -> new ItemStack(Items.PISTON))
            .displayItems((params, output) -> BCItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.get())))
            .build());

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    @Override
    public void onInitialize() {
        config = BCConfig.load(FabricLoader.getInstance().getConfigDir().resolve(MODID + ".json"));

        BCBlocks.BLOCKS.register();
        BCItems.ITEMS.register();
        BCBlockEntities.BLOCK_ENTITIES.register();
        BCMenus.MENUS.register();
        TABS.register();

        LOGGER.info("BuildCraft Reborn carregado");
    }
}
