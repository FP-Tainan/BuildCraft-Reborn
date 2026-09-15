package net.buildcraftreborn.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.buildcraftreborn.transport.tile.FluidPipeBlockEntity;
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
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Fluido dentro do tubo: o núcleo e os braços ligados enchem conforme a quantidade. Nos braços
 * horizontais o fluido sobe; nos verticais a coluna engrossa.
 */
public class FluidPipeRenderer implements BlockEntityRenderer<FluidPipeBlockEntity, FluidPipeRenderer.State> {
    private static final float LOW = 4.6F / 16.0F;
    private static final float HIGH = 11.4F / 16.0F;
    private static final float SIZE = HIGH - LOW;

    public static final class State extends BlockEntityRenderState {
        FluidVariant variant = FluidVariant.blank();
        float level;
        final boolean[] arms = new boolean[6];
        final java.util.List<PipePlugRenderer.PlugView> plugs = new java.util.ArrayList<>();
    }

    public FluidPipeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(FluidPipeBlockEntity pipe, State state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(pipe, state, crumbling);
        PipePlugRenderer.extract(pipe, state.plugs);
        state.variant = pipe.shownVariant();
        state.level = (float) pipe.shownRatio();
        for (Direction direction : Direction.values()) state.arms[direction.ordinal()] = pipe.connected(direction);
        if (!state.variant.isBlank() && FluidVariantAttributes.getLuminance(state.variant) > 0) state.lightCoords = 0xF000F0;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        PipePlugRenderer.submit(state.plugs, pose, collector, state.lightCoords);
        if (state.variant.isBlank() || state.level <= 0.01F) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getFluidStateModelSet()
                .get(state.variant.getFluid().defaultFluidState()).stillMaterial().sprite();
        int color = 0xFF000000 | FluidVariantRendering.getColor(state.variant);
        int light = state.lightCoords;
        float top = LOW + SIZE * state.level;
        float half = SIZE * state.level / 2.0F;
        float min = 0.5F - half;
        float max = 0.5F + half;
        boolean[] arms = state.arms;
        collector.submitCustomGeometry(pose, RenderTypes.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (m, c) -> {
            box(m, c, sprite, color, light, LOW, LOW, LOW, HIGH, top, HIGH);
            if (arms[Direction.NORTH.ordinal()]) box(m, c, sprite, color, light, LOW, LOW, 0.0F, HIGH, top, LOW);
            if (arms[Direction.SOUTH.ordinal()]) box(m, c, sprite, color, light, LOW, LOW, HIGH, HIGH, top, 1.0F);
            if (arms[Direction.WEST.ordinal()]) box(m, c, sprite, color, light, 0.0F, LOW, LOW, LOW, top, HIGH);
            if (arms[Direction.EAST.ordinal()]) box(m, c, sprite, color, light, HIGH, LOW, LOW, 1.0F, top, HIGH);
            if (arms[Direction.UP.ordinal()]) box(m, c, sprite, color, light, min, HIGH, min, max, 1.0F, max);
            if (arms[Direction.DOWN.ordinal()]) box(m, c, sprite, color, light, min, 0.0F, min, max, LOW, max);
        });
    }

    private static void box(PoseStack.Pose m, VertexConsumer c, TextureAtlasSprite sprite, int color, int light,
                            float x0, float y0, float z0, float x1, float y1, float z1) {
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        float u = u0 + (u1 - u0) * Math.max(0.01F, Math.max(x1 - x0, z1 - z0));
        float v = v0 + (v1 - v0) * Math.max(0.01F, y1 - y0);
        quad(m, c, color, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, u0, v0, u, v);
        quad(m, c, color, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, u0, v0, u, v);
        quad(m, c, color, light, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, u0, v, u, v0);
        quad(m, c, color, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, u0, v, u, v0);
        quad(m, c, color, light, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, u0, v, u, v0);
        quad(m, c, color, light, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, u0, v, u, v0);
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
