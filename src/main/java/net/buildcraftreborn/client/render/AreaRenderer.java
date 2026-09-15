package net.buildcraftreborn.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.builders.tile.ShowsArea;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** As 12 arestas da área da máquina com a faixa amarela e preta (mesa do arquiteto e construtor). */
public class AreaRenderer<T extends BlockEntity & ShowsArea> implements BlockEntityRenderer<T, AreaRenderer.State> {
    private static final Identifier STRIPES = BuildCraftReborn.id("textures/entity/laser/stripes.png");
    private static final float WIDTH = 2.0F / 16.0F;

    public static final class State extends BlockEntityRenderState {
        boolean visible;
        double minX, maxX, minY, maxY, minZ, maxZ;
    }

    public AreaRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T blockEntity, State state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(blockEntity, state, crumbling);
        BoundingBox box = blockEntity.shownArea();
        state.visible = box != null;
        if (box == null) return;
        BlockPos origin = blockEntity.getBlockPos();
        state.minX = box.minX() - origin.getX() + 0.5;
        state.maxX = box.maxX() - origin.getX() + 0.5;
        state.minY = box.minY() - origin.getY() + 0.5;
        state.maxY = box.maxY() - origin.getY() + 0.5;
        state.minZ = box.minZ() - origin.getZ() + 0.5;
        state.maxZ = box.maxZ() - origin.getZ() + 0.5;
    }

    @Override
    public void submit(State s, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!s.visible) return;
        double[] xs = {s.minX, s.maxX};
        double[] ys = {s.minY, s.maxY};
        double[] zs = {s.minZ, s.maxZ};
        for (double y : ys) for (double z : zs) beam(pose, collector, new Vec3(s.minX, y, z), new Vec3(s.maxX, y, z));
        for (double x : xs) for (double z : zs) beam(pose, collector, new Vec3(x, s.minY, z), new Vec3(x, s.maxY, z));
        for (double x : xs) for (double y : ys) beam(pose, collector, new Vec3(x, y, s.minZ), new Vec3(x, y, s.maxZ));
    }

    private static void beam(PoseStack pose, SubmitNodeCollector collector, Vec3 from, Vec3 to) {
        if (from.distanceToSqr(to) < 1.0E-4) return;
        LaserRenderer.beam(pose, collector, STRIPES, from, to, WIDTH, WIDTH * 4, false);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
