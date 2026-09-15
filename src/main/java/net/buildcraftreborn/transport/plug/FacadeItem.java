package net.buildcraftreborn.transport.plug;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.buildcraftreborn.registry.BCComponents;
import net.buildcraftreborn.registry.BCItems;
import net.buildcraftreborn.transport.block.PipeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fachada ({@code ItemPluggableFacade}): cobre a face do tubo com o visual de um bloco. A sólida tapa a ligação;
 * a vazada deixa o tubo ligar através dela. Feita na bancada: 3 tubos de estrutura + o bloco = 6 fachadas; uma
 * fachada sozinha alterna entre sólida e vazada.
 */
public class FacadeItem extends Item {
    public record Data(BlockState state, boolean hollow) {
        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockState.CODEC.fieldOf("state").forGetter(Data::state),
                Codec.BOOL.optionalFieldOf("hollow", false).forGetter(Data::hollow)
        ).apply(instance, Data::new));
        public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), Data::state, ByteBufCodecs.BOOL, Data::hollow, Data::new);
    }

    public FacadeItem(Properties properties) {
        super(properties);
    }

    /** Blocos inteiros comuns (sem block entity, com modelo normal) e vidros. */
    public static boolean isValid(BlockState state) {
        if (state.isAir() || state.hasBlockEntity() || state.getRenderShape() != RenderShape.MODEL || !state.getFluidState().isEmpty()) return false;
        if (state.getBlock() instanceof TransparentBlock || state.getBlock() instanceof StainedGlassBlock) return true;
        return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
    }

    public static ItemStack stack(BlockState state, boolean hollow) {
        ItemStack stack = new ItemStack(BCItems.FACADE.get());
        stack.set(BCComponents.FACADE.get(), new Data(state, hollow));
        return stack;
    }

    public static Data data(ItemStack stack) {
        Data data = stack.get(BCComponents.FACADE.get());
        return data == null ? new Data(Blocks.STONE.defaultBlockState(), false) : data;
    }

    public static void addCreativeVariants(CreativeModeTab.Output output) {
        for (Block block : new Block[]{Blocks.STONE, Blocks.COBBLESTONE, Blocks.STONE_BRICKS, Blocks.BRICKS, Blocks.OAK_PLANKS, Blocks.GLASS,
                Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK, Blocks.QUARTZ_BLOCK, Blocks.SMOOTH_STONE}) {
            output.accept(stack(block.defaultBlockState(), false));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        Data data = data(stack);
        return Component.translatable(data.hollow() ? "item.buildcraftreborn.facade.hollow" : "item.buildcraftreborn.facade.named",
                data.state().getBlock().getName());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof PipeBlock) || !(level.getBlockEntity(pos) instanceof PlugHolder holder)) {
            return InteractionResult.PASS;
        }
        Direction face = context.getClickedFace();
        if (holder.plugs().occupied(face)) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            Data data = data(context.getItemInHand());
            holder.plugs().attachFacade(face, new PipePlugs.Facade(data.state(), data.hollow()));
            PipeBlock.refreshConnections(level, pos);
            Player player = context.getPlayer();
            if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
            level.playSound(null, pos, data.state().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }
}
