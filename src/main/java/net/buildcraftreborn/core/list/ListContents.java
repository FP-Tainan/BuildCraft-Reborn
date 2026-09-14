package net.buildcraftreborn.core.list;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.WoodType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * O que está anotado numa lista do BuildCraft: duas linhas de nove itens, cada uma com as opções
 * "exato" (componentes iguais), "mesmo tipo" (mesma família, pelas tags do jogo) e "mesmo material"
 * (tags {@code c:}, como lingotes de ferro de qualquer mod).
 */
public record ListContents(List<Line> lines) {
    public static final int LINES = 2;
    public static final int WIDTH = 9;
    public static final int OPTION_PRECISE = 0;
    public static final int OPTION_TYPE = 1;
    public static final int OPTION_MATERIAL = 2;
    public static final int OPTIONS = 3;

    public static final ListContents EMPTY = new ListContents(List.of(Line.EMPTY, Line.EMPTY));

    public static final Codec<ListContents> CODEC = Line.CODEC.listOf().xmap(ListContents::new, ListContents::lines);
    public static final StreamCodec<RegistryFriendlyByteBuf, ListContents> STREAM_CODEC =
            Line.STREAM_CODEC.apply(ByteBufCodecs.list(LINES)).map(ListContents::new, ListContents::lines);

    public Line line(int index) {
        return index >= 0 && index < this.lines.size() ? this.lines.get(index) : Line.EMPTY;
    }

    public ListContents withLine(int index, Line line) {
        List<Line> copy = new ArrayList<>();
        for (int i = 0; i < LINES; i++) copy.add(i == index ? line : line(i));
        return new ListContents(List.copyOf(copy));
    }

    public boolean isEmpty() {
        return this.lines.stream().noneMatch(Line::hasItems);
    }

    /** Se o item passa em alguma das linhas. */
    public boolean matches(ItemStack target) {
        return this.lines.stream().anyMatch(line -> line.matches(target));
    }

    public record Line(List<ItemStack> stacks, boolean precise, boolean byType, boolean byMaterial) {
        public static final Line EMPTY = new Line(Collections.nCopies(WIDTH, ItemStack.EMPTY), false, false, false);

        public static final Codec<Line> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.OPTIONAL_CODEC.listOf().fieldOf("stacks").forGetter(Line::stacks),
                Codec.BOOL.optionalFieldOf("precise", false).forGetter(Line::precise),
                Codec.BOOL.optionalFieldOf("by_type", false).forGetter(Line::byType),
                Codec.BOOL.optionalFieldOf("by_material", false).forGetter(Line::byMaterial)
        ).apply(instance, Line::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Line> STREAM_CODEC = StreamCodec.composite(
                ItemStack.OPTIONAL_LIST_STREAM_CODEC, Line::stacks,
                ByteBufCodecs.BOOL, Line::precise,
                ByteBufCodecs.BOOL, Line::byType,
                ByteBufCodecs.BOOL, Line::byMaterial,
                Line::new);

        public ItemStack stack(int index) {
            return index >= 0 && index < this.stacks.size() ? this.stacks.get(index) : ItemStack.EMPTY;
        }

        public boolean hasItems() {
            return this.stacks.stream().anyMatch(stack -> !stack.isEmpty());
        }

        public boolean option(int option) {
            return switch (option) {
                case OPTION_PRECISE -> this.precise;
                case OPTION_TYPE -> this.byType;
                case OPTION_MATERIAL -> this.byMaterial;
                default -> false;
            };
        }

        public Line toggle(int option) {
            return new Line(this.stacks,
                    option == OPTION_PRECISE ? !this.precise : this.precise,
                    option == OPTION_TYPE ? !this.byType : this.byType,
                    option == OPTION_MATERIAL ? !this.byMaterial : this.byMaterial);
        }

        public Line withStack(int index, ItemStack stack) {
            List<ItemStack> copy = new ArrayList<>();
            for (int i = 0; i < WIDTH; i++) {
                ItemStack current = i == index ? stack : stack(i);
                copy.add(current.isEmpty() ? ItemStack.EMPTY : current.copyWithCount(1));
            }
            return new Line(List.copyOf(copy), this.precise, this.byType, this.byMaterial);
        }

        public boolean matches(ItemStack target) {
            if (target.isEmpty()) return false;
            for (ItemStack example : this.stacks) {
                if (example.isEmpty()) continue;
                if (this.precise ? ItemStack.isSameItemSameComponents(example, target) : example.getItem() == target.getItem()) {
                    return true;
                }
                if (this.byType && family(example).equals(family(target))) return true;
                if (this.byMaterial && sharesMaterial(example, target)) return true;
            }
            return false;
        }

        /** Mesmo material: alguma tag específica {@code c:} em comum (como {@code c:ingots/iron}). */
        private static boolean sharesMaterial(ItemStack a, ItemStack b) {
            Set<TagKey<Item>> tags = materialTags(a);
            return !tags.isEmpty() && materialTags(b).stream().anyMatch(tags::contains);
        }

        public static Set<TagKey<Item>> materialTags(ItemStack stack) {
            return BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem()).tags()
                    .filter(tag -> tag.location().getNamespace().equals("c") && tag.location().getPath().contains("/"))
                    .collect(Collectors.toSet());
        }

        /**
         * Família do item, como as variações por metadata do BuildCraft antigo: o nome sem a cor
         * ({@code red_wool} → {@code wool}) e sem o tipo de madeira ({@code spruce_planks} → {@code planks}).
         */
        public static String family(ItemStack stack) {
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String path = id.getPath();
            for (String prefix : VARIANT_PREFIXES) {
                if (path.startsWith(prefix)) {
                    path = path.substring(prefix.length());
                    break;
                }
            }
            return id.getNamespace() + ":" + path;
        }

        /** Cores e madeiras, das mais longas para as mais curtas ({@code light_gray_} antes de {@code gray_}). */
        private static final List<String> VARIANT_PREFIXES = Stream.concat(
                        Arrays.stream(DyeColor.values()).map(DyeColor::getName),
                        WoodType.values().map(WoodType::name))
                .map(name -> name + "_")
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }
}
