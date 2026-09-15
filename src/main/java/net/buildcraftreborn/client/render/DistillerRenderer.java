package net.buildcraftreborn.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.buildcraftreborn.factory.block.DistillerBlock;
import net.buildcraftreborn.factory.tile.DistillerBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Fluido nos três tanques do destilador ({@code distiller.json}, desenhado virado para oeste): tanque alto
 * de entrada, tanque de gás em cima e de líquido embaixo.
 */
public class DistillerRenderer implements BlockEntityRenderer<DistillerBlockEntity, DistillerRenderer.State> {
    /** Caixas em pixels: x0, y0, z0, x1, altura cheia, z1. */
    private static final float[][] TANKS = {
            {0.5F, 0.5F, 4.5F, 7.5F, 15.0F, 11.5F},
            {8.5F, 8.5F, 0.5F, 15.5F, 7.0F, 15.5F},
            {8.5F, 0.5F, 0.5F, 15.5F, 7.0F, 15.5F}
    };

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.WEST;
        final FluidVariant[] fluids = {FluidVariant.blank(), FluidVariant.blank(), FluidVariant.blank()};
        final float[] levels = new float[3];
    }

    public DistillerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DistillerBlockEntity distiller, State state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(distiller, state, crumbling);
        BlockState block = distiller.getLevel() != null ? distiller.getLevel().getBlockState(distiller.getBlockPos()) : distiller.getBlockState();
        state.facing = block.getBlock() instanceof DistillerBlock ? block.getValue(DistillerBlock.FACING) : Direction.WEST;
        boolean lit = false;
        for (int i = 0; i < 3; i++) {
            state.fluids[i] = distiller.tank(i).variant;
            state.levels[i] = (float) distiller.shownRatio(i);
            if (!state.fluids[i].isBlank() && FluidVariantAttributes.getLuminance(state.fluids[i]) > 0) lit = true;
        }
        if (lit) state.lightCoords = 0xF000F0;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = state.lightCoords;
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-rotation(state.facing)));
        pose.translate(-0.5F, -0.5F, -0.5F);
        for (int i = 0; i < 3; i++) {
            FluidVariant variant = state.fluids[i];
            float level = state.levels[i];
            if (variant.isBlank() || level <= 0.005F) continue;
            TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
                    .get(variant.getFluid().defaultFluidState()).stillMaterial().sprite();
            int color = 0xFF000000 | FluidVariantRendering.getColor(variant);
            float[] t = TANKS[i];
            float top = t[1] + t[4] * Math.min(1.0F, level);
            collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (m, c) ->
                    box(m, c, sprite, color, light, t[0] / 16F, t[1] / 16F, t[2] / 16F, t[3] / 16F, top / 16F, t[5] / 16F));
        }
        pose.popPose();
    }

    /** Mesma rotação do blockstate (o modelo é desenhado virado para oeste). */
    private static float rotation(Direction facing) {
        return switch (facing) {
            case NORTH -> 90;
            case EAST -> 180;
            case SOUTH -> 270;
            default -> 0;
        };
    }

    private static void box(PoseStack.Pose m, VertexConsumer c, TextureAtlasSprite sprite, int color, int light,
                            float x0, float y0, float z0, float x1, float y1, float z1) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        float vSide = v0 + (v1 - v0) * Math.min(1.0F, y1 - y0);
        quad(m, c, color, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, u0, v0, u1, v1);
        quad(m, c, color, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, u0, v0, u1, v1);
        quad(m, c, color, light, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, u0, vSide, u1, v0);
        quad(m, c, color, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, u0, vSide, u1, v0);
        quad(m, c, color, light, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, u0, vSide, u1, v0);
        quad(m, c, color, light, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, u0, vSide, u1, v0);
    }

    private static void quad(PoseStack.Pose m, VertexConsumer c, int color, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1) {
        vertex(m, c, color, light, nx, ny, nz, ax, ay, az, u0, v0);
        vertex(m, c, color, light, nx, ny, nz, bx, by, bz, u1, v0);
        vertex(m, c, color, light, nx, ny, nz, cx, cy, cz, u1, v1);
        vertex(m, c, color, light, nx, ny, nz, dx, dy, dz, u0, v1);
    }

    private static void vertex(PoseStack.Pose m, VertexConsumer c, int color, int light, float nx, float ny, float nz,
                               float x, float y, float z, float u, float v) {
        c.addVertex(m, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(m, nx, ny, nz);
    }
}
