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
    public static final RegistryObject<net.buildcraftreborn.core.item.GuideBookItem> GUIDE = ITEMS.register("guide",
            () -> new net.buildcraftreborn.core.item.GuideBookItem(props("guide")));
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
    public static final RegistryObject<BlockItem> OIL_SPRING = block("oil_spring", BCBlocks.OIL_SPRING);
    public static final RegistryObject<BlockItem> DECORATED_DESTROY = block("decorated_destroy", BCBlocks.DECORATED_DESTROY);
    public static final RegistryObject<BlockItem> DECORATED_BLUEPRINT = block("decorated_blueprint", BCBlocks.DECORATED_BLUEPRINT);
    public static final RegistryObject<BlockItem> DECORATED_TEMPLATE = block("decorated_template", BCBlocks.DECORATED_TEMPLATE);
    public static final RegistryObject<BlockItem> DECORATED_PAPER = block("decorated_paper", BCBlocks.DECORATED_PAPER);
    public static final RegistryObject<BlockItem> DECORATED_LEATHER = block("decorated_leather", BCBlocks.DECORATED_LEATHER);
    public static final RegistryObject<BlockItem> DECORATED_LASER_BACK = block("decorated_laser_back", BCBlocks.DECORATED_LASER_BACK);

    // ── energia: motores ──────────────────────────────────────────────────
    public static final RegistryObject<BlockItem> REDSTONE_ENGINE = described("redstone_engine", BCBlocks.REDSTONE_ENGINE);
    public static final RegistryObject<BlockItem> STIRLING_ENGINE = described("stirling_engine", BCBlocks.STIRLING_ENGINE);
    public static final RegistryObject<BlockItem> COMBUSTION_ENGINE = described("combustion_engine", BCBlocks.COMBUSTION_ENGINE);
    public static final RegistryObject<BlockItem> CREATIVE_ENGINE = described("creative_engine", BCBlocks.CREATIVE_ENGINE);

    // ── factory ───────────────────────────────────────────────────────────
    public static final RegistryObject<BlockItem> TANK = block("tank", BCBlocks.TANK);
    public static final RegistryObject<BlockItem> PUMP = described("pump", BCBlocks.PUMP);
    public static final RegistryObject<BlockItem> MINING_WELL = described("mining_well", BCBlocks.MINING_WELL);
    public static final RegistryObject<BlockItem> FLOOD_GATE = described("flood_gate", BCBlocks.FLOOD_GATE);
    public static final RegistryObject<BlockItem> AUTO_WORKBENCH = described("auto_workbench", BCBlocks.AUTO_WORKBENCH);
    public static final RegistryObject<BlockItem> DISTILLER = described("distiller", BCBlocks.DISTILLER);
    public static final RegistryObject<BlockItem> HEAT_EXCHANGER = described("heat_exchanger", BCBlocks.HEAT_EXCHANGER);
    public static final RegistryObject<net.buildcraftreborn.factory.item.WaterGelifierItem> WATER_GELIFIER = ITEMS.register("water_gelifier",
            () -> new net.buildcraftreborn.factory.item.WaterGelifierItem(props("water_gelifier")));
    public static final RegistryObject<Item> GELLED_WATER = simple("gelled_water");

    // ── builders ──────────────────────────────────────────────────────────
    public static final RegistryObject<BlockItem> QUARRY = described("quarry", BCBlocks.QUARRY);
    public static final RegistryObject<BlockItem> FILLER = described("filler", BCBlocks.FILLER);
    public static final RegistryObject<BlockItem> ARCHITECT_TABLE = described("architect_table", BCBlocks.ARCHITECT_TABLE);
    public static final RegistryObject<BlockItem> BUILDER = described("builder", BCBlocks.BUILDER);
    public static final RegistryObject<BlockItem> LIBRARY = described("library", BCBlocks.LIBRARY);
    public static final RegistryObject<BlockItem> REPLACER = described("replacer", BCBlocks.REPLACER);
    public static final RegistryObject<net.buildcraftreborn.builders.item.SchematicItem> SCHEMATIC_SINGLE = ITEMS.register("schematic_single",
            () -> new net.buildcraftreborn.builders.item.SchematicItem(props("schematic_single").stacksTo(16)));
    public static final RegistryObject<net.buildcraftreborn.builders.item.SnapshotItem> TEMPLATE = ITEMS.register("template",
            () -> new net.buildcraftreborn.builders.item.SnapshotItem(props("template").stacksTo(16),
                    net.buildcraftreborn.builders.snapshot.Snapshot.Type.TEMPLATE));
    public static final RegistryObject<net.buildcraftreborn.builders.item.SnapshotItem> BLUEPRINT = ITEMS.register("blueprint",
            () -> new net.buildcraftreborn.builders.item.SnapshotItem(props("blueprint").stacksTo(16),
                    net.buildcraftreborn.builders.snapshot.Snapshot.Type.BLUEPRINT));

    // ── silicon ───────────────────────────────────────────────────────────
    public static final RegistryObject<BlockItem> LASER = described("laser", BCBlocks.LASER);
    public static final RegistryObject<BlockItem> ASSEMBLY_TABLE = described("assembly_table", BCBlocks.ASSEMBLY_TABLE);
    public static final RegistryObject<BlockItem> ADVANCED_CRAFTING_TABLE = described("advanced_crafting_table", BCBlocks.ADVANCED_CRAFTING_TABLE);
    public static final RegistryObject<Item> REDSTONE_CHIPSET = simple("redstone_chipset");
    public static final RegistryObject<Item> IRON_CHIPSET = simple("iron_chipset");
    public static final RegistryObject<Item> GOLD_CHIPSET = simple("gold_chipset");
    public static final RegistryObject<Item> QUARTZ_CHIPSET = simple("quartz_chipset");
    public static final RegistryObject<Item> DIAMOND_CHIPSET = simple("diamond_chipset");

    // ── transport: tubos ──────────────────────────────────────────────────
    public static final Map<net.buildcraftreborn.transport.PipeType, RegistryObject<BlockItem>> PIPES =
            new EnumMap<>(net.buildcraftreborn.transport.PipeType.class);

    static {
        for (net.buildcraftreborn.transport.PipeType type : net.buildcraftreborn.transport.PipeType.values()) {
            PIPES.put(type, block(type.blockId(), BCBlocks.PIPES.get(type)));
        }
    }

    public static final RegistryObject<BlockItem> FILTERED_BUFFER = described("filtered_buffer", BCBlocks.FILTERED_BUFFER);

    /** Vedação: tubo de itens + vedação = tubo de fluidos. */
    public static final RegistryObject<Item> PIPE_SEALANT = simple("pipe_sealant");
    public static final Map<net.buildcraftreborn.transport.PipeType, RegistryObject<BlockItem>> FLUID_PIPES =
            new EnumMap<>(net.buildcraftreborn.transport.PipeType.class);

    static {
        BCBlocks.FLUID_PIPES.forEach((type, pipe) -> FLUID_PIPES.put(type, block(type.fluidBlockId(), pipe)));
    }

    /** Porta lógica (variante no componente {@code gate_variant}). */
    public static final RegistryObject<net.buildcraftreborn.transport.gate.GateItem> GATE = ITEMS.register("gate",
            () -> new net.buildcraftreborn.transport.gate.GateItem(props("gate")));
    public static final RegistryObject<net.buildcraftreborn.transport.gate.GateCopierItem> GATE_COPIER = ITEMS.register("gate_copier",
            () -> new net.buildcraftreborn.transport.gate.GateCopierItem(props("gate_copier").stacksTo(1)));
    public static final RegistryObject<net.buildcraftreborn.transport.plug.PlugItem> PLUG_PULSAR = plug("plug_pulsar",
            net.buildcraftreborn.transport.plug.PlugKind.PULSAR);
    public static final RegistryObject<net.buildcraftreborn.transport.plug.PlugItem> PLUG_LIGHT_SENSOR = plug("plug_light_sensor",
            net.buildcraftreborn.transport.plug.PlugKind.LIGHT_SENSOR);
    public static final RegistryObject<net.buildcraftreborn.transport.plug.PlugItem> PLUG_TIMER = plug("plug_timer",
            net.buildcraftreborn.transport.plug.PlugKind.TIMER);
    /** Lentes e filtros (cor no componente {@code lens}). */
    public static final RegistryObject<net.buildcraftreborn.transport.plug.LensItem> LENS = ITEMS.register("lens",
            () -> new net.buildcraftreborn.transport.plug.LensItem(props("lens")));
    /** Fachadas (bloco no componente {@code facade}). */
    public static final RegistryObject<net.buildcraftreborn.transport.plug.FacadeItem> FACADE = ITEMS.register("facade",
            () -> new net.buildcraftreborn.transport.plug.FacadeItem(props("facade")));
    /** Fios de tubo, um por cor. */
    public static final Map<DyeColor, RegistryObject<net.buildcraftreborn.transport.plug.WireItem>> WIRES = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            String name = "wire_" + color.getName();
            WIRES.put(color, ITEMS.register(name, () -> new net.buildcraftreborn.transport.plug.WireItem(props(name), color)));
        }
    }

    private static RegistryObject<net.buildcraftreborn.transport.plug.PlugItem> plug(String name, net.buildcraftreborn.transport.plug.PlugKind kind) {
        return ITEMS.register(name, () -> new net.buildcraftreborn.transport.plug.PlugItem(props(name), kind));
    }

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
