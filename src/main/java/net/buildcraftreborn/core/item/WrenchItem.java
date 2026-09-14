package net.buildcraftreborn.core.item;

import net.buildcraftreborn.lib.block.BCDirectionalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;

/**
 * Chave inglesa do BuildCraft: gira motores, bombas e calhas para a próxima direção válida, e também
 * blocos do jogo que têm frente ou eixo (fornalhas, funis, observadores, toras). Agachado, o clique
 * passa direto para o bloco. Não gasta.
 */
public class WrenchItem extends Item {
    public WrenchItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (player != null && player.isSecondaryUseActive()) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof net.buildcraftreborn.lib.block.WrenchInteractable interactable
                    && interactable.onSneakWrench(level, pos, state, player)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (!rotate(level, pos, true)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            rotate(level, pos, false);
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 1.6F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Gira o bloco em {@code pos}; com {@code simulate} só diz se daria. */
    public static boolean rotate(Level level, BlockPos pos, boolean simulate) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof BCDirectionalBlock directional) {
            return simulate || directional.rotateToNext(level, pos, state);
        }
        // blocos de duas partes ou já estendidos quebrariam ao girar
        if (state.hasProperty(BlockStateProperties.BED_PART) || state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                || (state.hasProperty(BlockStateProperties.EXTENDED) && state.getValue(BlockStateProperties.EXTENDED))
                || (state.hasProperty(BlockStateProperties.CHEST_TYPE) && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE)) {
            return false;
        }

        BlockState rotated = null;
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction current = state.getValue(BlockStateProperties.FACING);
            rotated = state.setValue(BlockStateProperties.FACING, Direction.from3DDataValue((current.get3DDataValue() + 1) % 6));
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            rotated = state.setValue(BlockStateProperties.HORIZONTAL_FACING, state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise());
        } else if (state.hasProperty(BlockStateProperties.FACING_HOPPER)) {
            Direction current = state.getValue(BlockStateProperties.FACING_HOPPER);
            Direction next = current == Direction.DOWN ? Direction.NORTH
                    : current == Direction.WEST ? Direction.DOWN : current.getClockWise();
            rotated = state.setValue(BlockStateProperties.FACING_HOPPER, next);
        } else if (state.hasProperty(BlockStateProperties.AXIS)) {
            Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);
            rotated = state.setValue(BlockStateProperties.AXIS, Direction.Axis.values()[(axis.ordinal() + 1) % 3]);
        }
        if (rotated == null || !rotated.canSurvive(level, pos)) return false;
        if (!simulate) level.setBlock(pos, rotated, Block.UPDATE_ALL);
        return true;
    }
}
