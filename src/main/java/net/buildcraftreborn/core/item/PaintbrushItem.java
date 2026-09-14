package net.buildcraftreborn.core.item;

import net.buildcraftreborn.registry.BCItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Pincel do BuildCraft: pinta blocos coloridos (lã, carpete, vidro, terracota, concreto, velas) e,
 * mais adiante, tubos. Cada pincel colorido tem 64 usos e volta a ser um pincel limpo; o limpo tira
 * a cor de vidro, painel, terracota e vela.
 */
public class PaintbrushItem extends Item {
    public static final int USES = 64;
    private static final Set<String> FAMILIES = Set.of("wool", "carpet", "stained_glass", "stained_glass_pane",
            "terracotta", "glazed_terracotta", "concrete", "concrete_powder", "candle");
    /** Bloco sem cor → família colorida. */
    private static final Map<String, String> UNCOLORED = Map.of("glass", "stained_glass", "glass_pane", "stained_glass_pane",
            "terracotta", "terracotta", "candle", "candle");
    /** Família colorida → bloco sem cor (para o pincel limpo). */
    private static final Map<String, String> CLEAN = Map.of("stained_glass", "glass", "stained_glass_pane", "glass_pane",
            "terracotta", "terracotta", "candle", "candle");

    private final @Nullable DyeColor color;

    public PaintbrushItem(Properties properties, @Nullable DyeColor color) {
        super(color == null ? properties.stacksTo(1) : properties.durability(USES));
        this.color = color;
    }

    public @Nullable DyeColor color() {
        return this.color;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!paint(level, pos, this.color, true)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        paint(level, pos, this.color, false);
        level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (this.color != null && (player == null || !player.getAbilities().instabuild)) {
            int damage = stack.getDamageValue() + 1;
            if (damage >= stack.getMaxDamage()) {
                ItemStack clean = new ItemStack(BCItems.PAINTBRUSH.get());
                if (player != null) player.setItemInHand(context.getHand(), clean);
            } else {
                stack.setDamageValue(damage);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Pinta o bloco com {@code color} ({@code null} = limpar). Com {@code simulate} só diz se daria. */
    public static boolean paint(Level level, BlockPos pos, @Nullable DyeColor color, boolean simulate) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof Paintable paintable) {
            return paintable.paint(level, pos, state, color, simulate);
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!id.getNamespace().equals("minecraft")) return false;

        String path = id.getPath();
        String family = null;
        DyeColor current = null;
        for (DyeColor dye : DyeColor.values()) {
            String prefix = dye.getName() + "_";
            if (path.startsWith(prefix) && FAMILIES.contains(path.substring(prefix.length()))) {
                family = path.substring(prefix.length());
                current = dye;
                break;
            }
        }
        if (family == null) family = UNCOLORED.get(path);
        if (family == null || color == current) return false;

        String targetPath = color == null ? CLEAN.get(family) : color.getName() + "_" + family;
        if (targetPath == null || targetPath.equals(path)) return false;
        Block target = BuiltInRegistries.BLOCK.getOptional(Identifier.withDefaultNamespace(targetPath)).orElse(null);
        if (target == null) return false;
        if (!simulate) level.setBlock(pos, target.withPropertiesOf(state), Block.UPDATE_ALL);
        return true;
    }

    /** Blocos do mod que sabem ser pintados (tubos, por exemplo). */
    public interface Paintable {
        boolean paint(Level level, BlockPos pos, BlockState state, @Nullable DyeColor color, boolean simulate);
    }
}
