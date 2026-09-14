package net.buildcraftreborn.registry;

import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.core.item.PaintbrushItem;
import net.buildcraftreborn.core.item.WrenchItem;
import net.buildcraftreborn.core.list.ListItem;
import net.buildcraftreborn.core.marker.MarkerConnectorItem;
import net.craftenergy.registry.DeferredRegister;
import net.craftenergy.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.EnumMap;
import java.util.Map;

/** Itens do BuildCraft Reborn; todos aparecem na aba criativa, na ordem de registro. */
public final class BCItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, BuildCraftReborn.MODID);

    // ── core: ferramentas ─────────────────────────────────────────────────
    public static final RegistryObject<WrenchItem> WRENCH = ITEMS.register("wrench", () -> new WrenchItem(props("wrench")));
    public static final RegistryObject<Item> GEAR_WOOD = simple("gear_wood");
    public static final RegistryObject<Item> GEAR_STONE = simple("gear_stone");
    public static final RegistryObject<Item> GEAR_IRON = simple("gear_iron");
    public static final RegistryObject<Item> GEAR_GOLD = simple("gear_gold");
    public static final RegistryObject<Item> GEAR_DIAMOND = simple("gear_diamond");
    public static final RegistryObject<PaintbrushItem> PAINTBRUSH = ITEMS.register("paintbrush",
            () -> new PaintbrushItem(props("paintbrush"), null));
    /** Pincéis coloridos, um por cor de corante. */
    public static final Map<DyeColor, RegistryObject<PaintbrushItem>> PAINTBRUSHES = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            String name = "paintbrush_" + color.getName();
            PAINTBRUSHES.put(color, ITEMS.register(name, () -> new PaintbrushItem(props(name), color)));
        }
    }

    public static final RegistryObject<ListItem> LIST = ITEMS.register("list", () -> new ListItem(props("list")));
    public static final RegistryObject<MarkerConnectorItem> MARKER_CONNECTOR = ITEMS.register("marker_connector",
            () -> new MarkerConnectorItem(props("marker_connector")));

    // ── core: blocos ──────────────────────────────────────────────────────
    public static final RegistryObject<BlockItem> MARKER_VOLUME = block("marker_volume", BCBlocks.MARKER_VOLUME);
    public static final RegistryObject<BlockItem> MARKER_PATH = block("marker_path", BCBlocks.MARKER_PATH);
    public static final RegistryObject<BlockItem> WATER_SPRING = block("water_spring", BCBlocks.WATER_SPRING);
    public static final RegistryObject<BlockItem> DECORATED_DESTROY = block("decorated_destroy", BCBlocks.DECORATED_DESTROY);
    public static final RegistryObject<BlockItem> DECORATED_BLUEPRINT = block("decorated_blueprint", BCBlocks.DECORATED_BLUEPRINT);
    public static final RegistryObject<BlockItem> DECORATED_TEMPLATE = block("decorated_template", BCBlocks.DECORATED_TEMPLATE);
    public static final RegistryObject<BlockItem> DECORATED_PAPER = block("decorated_paper", BCBlocks.DECORATED_PAPER);
    public static final RegistryObject<BlockItem> DECORATED_LEATHER = block("decorated_leather", BCBlocks.DECORATED_LEATHER);
    public static final RegistryObject<BlockItem> DECORATED_LASER_BACK = block("decorated_laser_back", BCBlocks.DECORATED_LASER_BACK);

    // ── energia: motores ──────────────────────────────────────────────────
    public static final RegistryObject<BlockItem> REDSTONE_ENGINE = described("redstone_engine", BCBlocks.REDSTONE_ENGINE);
    public static final RegistryObject<BlockItem> STIRLING_ENGINE = described("stirling_engine", BCBlocks.STIRLING_ENGINE);
    public static final RegistryObject<BlockItem> CREATIVE_ENGINE = described("creative_engine", BCBlocks.CREATIVE_ENGINE);

    // ── factory ───────────────────────────────────────────────────────────
    public static final RegistryObject<BlockItem> TANK = block("tank", BCBlocks.TANK);
    public static final RegistryObject<BlockItem> PUMP = described("pump", BCBlocks.PUMP);
    public static final RegistryObject<BlockItem> MINING_WELL = described("mining_well", BCBlocks.MINING_WELL);
    public static final RegistryObject<BlockItem> FLOOD_GATE = described("flood_gate", BCBlocks.FLOOD_GATE);
    public static final RegistryObject<BlockItem> AUTO_WORKBENCH = described("auto_workbench", BCBlocks.AUTO_WORKBENCH);

    private BCItems() {}

    private static RegistryObject<BlockItem> described(String name, RegistryObject<? extends Block> block) {
        return ITEMS.register(name, () -> new net.buildcraftreborn.lib.item.DescribedBlockItem(block.get(), props(name).useBlockDescriptionPrefix()));
    }

    private static Item.Properties props(String name) {
        return new Item.Properties().setId(ITEMS.key(name));
    }

    private static RegistryObject<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(props(name)));
    }

    private static RegistryObject<BlockItem> block(String name, RegistryObject<? extends Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), props(name).useBlockDescriptionPrefix()));
    }
}
