package net.buildcraftreborn.transport.plug;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.buildcraftreborn.registry.BCComponents;
import net.buildcraftreborn.registry.BCItems;
import net.buildcraftreborn.transport.PipeFlow;
import net.buildcraftreborn.transport.block.PipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Lente ou filtro ({@code ItemPluggableLens}): encaixa numa face de tubo de itens sem tapar a ligação. A lente
 * pinta os itens que cruzam a face (a transparente tira a cor); o filtro barra itens de outra cor e dá preferência
 * aos da sua.
 */
public class LensItem extends Item {
    /** Cor (-1 = transparente) e se é filtro. */
    public record Data(int colour, boolean filter) {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("colour").forGetter(Data::colour),
                Codec.BOOL.fieldOf("filter").forGetter(Data::filter)
        ).apply(instance, Data::new));
        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Data::colour, ByteBufCodecs.BOOL, Data::filter, Data::new);

        public @Nullable DyeColor dyeColour() {
            DyeColor[] values = DyeColor.values();
            return this.colour >= 0 && this.colour < values.length ? values[this.colour] : null;
        }
    }

    public LensItem(Properties properties) {
        super(properties);
    }

    public static ItemStack stack(@Nullable DyeColor colour, boolean filter) {
        ItemStack stack = new ItemStack(BCItems.LENS.get());
        stack.set(BCComponents.LENS.get(), new Data(colour == null ? -1 : colour.ordinal(), filter));
        return stack;
    }

    public static Data data(ItemStack stack) {
        Data data = stack.get(BCComponents.LENS.get());
        return data == null ? new Data(-1, false) : data;
    }

    public static void addCreativeVariants(CreativeModeTab.Output output) {
        for (boolean filter : new boolean[]{false, true}) {
            output.accept(stack(null, filter));
            for (DyeColor colour : DyeColor.values()) output.accept(stack(colour, filter));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        Data data = data(stack);
        DyeColor colour = data.dyeColour();
        Component colourName = colour == null ? Component.translatable("color.buildcraftreborn.clear")
                : Component.translatable("color.minecraft." + colour.getName());
        return Component.translatable(data.filter() ? "item.buildcraftreborn.lens.filter" : "item.buildcraftreborn.lens.lens", colourName);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof PipeBlock pipe) || pipe.flow() != PipeFlow.ITEM
                || !(level.getBlockEntity(pos) instanceof PlugHolder holder)) {
            return InteractionResult.PASS;
        }
        Direction face = context.getClickedFace();
        if (holder.plugs().occupied(face)) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            Data data = data(context.getItemInHand());
            holder.plugs().attachLens(face, new PipePlugs.Lens(data.dyeColour(), data.filter()));
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
            level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }
}
