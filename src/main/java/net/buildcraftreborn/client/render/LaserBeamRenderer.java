package net.buildcraftreborn.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.lib.block.BCDirectionalBlock;
import net.buildcraftreborn.silicon.tile.LaserBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** Feixe do laser até a mesa; a cor segue a potência (texturas {@code lasers/power_*} do BuildCraft). */
public class LaserBeamRenderer implements BlockEntityRenderer<LaserBlockEntity, LaserBeamRenderer.State> {
    private static final Identifier[] BEAMS = {
            texture("power_low"), texture("power_med"), texture("power_high"), texture("power_full")
    };

    public static final class State extends BlockEntityRenderState {
        @Nullable Vec3 from;
        @Nullable Vec3 to;
        int level;
    }

    public LaserBeamRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static Identifier texture(String name) {
        return BuildCraftReborn.id("textures/entity/laser/" + name + ".png");
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LaserBlockEntity laser, State state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(laser, state, crumbling);
        state.from = null;
        state.to = null;
        state.level = laser.beamLevel();
        if (laser.getLevel() == null || state.level <= 0 || !BuildCraftReborn.config.renderLaserBeams) return;
        Vec3 point = laser.beamPoint(laser.getLevel().getGameTime());
        if (point == null) return;
        BlockPos pos = laser.getBlockPos();
        BlockState block = laser.getLevel().getBlockState(pos);
        Direction facing = block.hasProperty(BCDirectionalBlock.FACING) ? block.getValue(BCDirectionalBlock.FACING) : Direction.UP;
        state.from = new Vec3(0.5 + facing.getStepX() * 0.3125, 0.5 + facing.getStepY() * 0.3125, 0.5 + facing.getStepZ() * 0.3125);
        state.to = point.subtract(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.from == null || state.to == null) return;
        LaserRenderer.beam(pose, collector, BEAMS[Math.clamp(state.level - 1, 0, BEAMS.length - 1)], state.from, state.to, 0.125F);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}
