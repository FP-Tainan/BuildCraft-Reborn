package net.buildcraftreborn.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.buildcraftreborn.factory.block.TankBlock;
import net.buildcraftreborn.factory.tile.TankBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Fluido dentro do tanque. O nível anda suave; numa coluna de tanques cheios o fluido é contínuo (sem
 * tampas no meio) e, quando o tanque de cima está escorrendo, aparece um filete de fluido caindo.
 */
public class TankRenderer implements BlockEntityRenderer<TankBlockEntity, TankRenderer.State> {
    private static final float MIN = 2.05F / 16.0F;
    private static final float MAX = 13.95F / 16.0F;
    private static final float STREAM_MIN = 6.5F / 16.0F;
    private static final float STREAM_MAX = 9.5F / 16.0F;

    public static final class State extends BlockEntityRenderState {
        FluidVariant variant = FluidVariant.blank();
        float level;
        boolean continuesAbove;
        boolean continuesBelow;
        FluidVariant stream = FluidVariant.blank();
    }

    public TankRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(TankBlockEntity tank, State state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(tank, state, crumbling);
        state.variant = tank.tank().variant;
        state.level = (float) tank.shownRatio(partialTick);
        state.continuesAbove = false;
        state.continuesBelow = false;
        state.stream = FluidVariant.blank();
        Level level = tank.getLevel();
        BlockState block = tank.getBlockState();
        if (level != null && block.getBlock() instanceof TankBlock) {
            if (block.getValue(TankBlock.JOINED_ABOVE) && level.getBlockEntity(tank.getBlockPos().above()) instanceof TankBlockEntity above) {
                FluidVariant aboveFluid = above.tank().variant;
                if (!aboveFluid.isBlank() && above.isPouring() && !FluidVariantAttributes.isLighterThanAir(aboveFluid)) {
                    state.stream = aboveFluid;
                }
                state.continuesAbove = state.level >= 0.999F && aboveFluid.equals(state.variant) && above.tank().ratio() > 0;
            }
            if (block.getValue(TankBlock.JOINED_BELOW) && level.getBlockEntity(tank.getBlockPos().below()) instanceof TankBlockEntity below) {
                state.continuesBelow = below.tank().variant.equals(state.variant) && below.tank().ratio() >= 0.999;
            }
        }
        FluidVariant lit = state.variant.isBlank() ? state.stream : state.variant;
        if (!lit.isBlank() && FluidVariantAttributes.getLuminance(lit) > 0) state.lightCoords = 0xF000F0;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        boolean body = !state.variant.isBlank() && state.level > 0.001F;
        if (!body && state.stream.isBlank()) return;
        int light = state.lightCoords;
        float top = body ? (state.continuesAbove ? 1.0F : Math.max(0.02F, state.level)) : 0.0F;

        if (body) {
            FluidModel model = model(state.variant);
            TextureAtlasSprite still = model.stillMaterial().sprite();
            int color = color(state.variant);
            float bottom = state.continuesBelow ? 0.0F : 0.001F;
            boolean drawTop = !state.continuesAbove;
            boolean drawBottom = !state.continuesBelow;
            collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (matrix, consumer) ->
                    box(matrix, consumer, still, color, light, MIN, bottom, MIN, MAX, top, MAX, drawTop, drawBottom));
        }

        if (!state.stream.isBlank() && top < 0.999F) {
            TextureAtlasSprite flowing = model(state.stream).flowingMaterial().sprite();
            int color = color(state.stream);
            float streamBottom = top;
            collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (matrix, consumer) ->
                    box(matrix, consumer, flowing, color, light, STREAM_MIN, streamBottom, STREAM_MIN, STREAM_MAX, 1.0F, STREAM_MAX, false, false));
        }
    }

    private static FluidModel model(FluidVariant variant) {
        return Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(variant.getFluid().defaultFluidState());
    }

    private static int color(FluidVariant variant) {
        return 0xFF000000 | FluidVariantRendering.getColor(variant);
    }

    /** Caixa de fluido; a textura das laterais é cortada na altura, para não esticar. */
    private static void box(PoseStack.Pose m, VertexConsumer c, TextureAtlasSprite sprite, int color, int light,
                            float x0, float y0, float z0, float x1, float y1, float z1, boolean drawTop, boolean drawBottom) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        float vSide = v0 + (v1 - v0) * (y1 - y0);
        if (drawTop) quad(m, c, color, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, u0, v0, u1, v1);
        if (drawBottom) quad(m, c, color, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, u0, v0, u1, v1);
        quad(m, c, color, light, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, u0, vSide, u1, v0);
        quad(m, c, color, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, u0, vSide, u1, v0);
        quad(m, c, color, light, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, u0, vSide, u1, v0);
        quad(m, c, color, light, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, u0, vSide, u1, v0);
    }

    /** a→b→c→d; UV: a = (u0,v0), c = (u1,v1). */
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
