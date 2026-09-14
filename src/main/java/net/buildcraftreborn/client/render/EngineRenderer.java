package net.buildcraftreborn.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.energy.engine.EngineBlock;
import net.buildcraftreborn.energy.engine.EngineBlockEntity;
import net.buildcraftreborn.energy.engine.EngineStage;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Motor do BuildCraft ({@code engine_base.json}): base fixa, placa que sobe e desce com o pistão,
 * tronco na cor do estágio de calor e a sanfona entre as duas. Virado para a frente do bloco.
 */
public class EngineRenderer implements BlockEntityRenderer<EngineBlockEntity, EngineRenderer.State> {
    private static final Identifier CHAMBER = texture("engine/chamber");

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.UP;
        EngineBlock.Kind kind = EngineBlock.Kind.REDSTONE;
        EngineStage stage = EngineStage.BLUE;
        /** Deslocamento da placa em pixels (0 a 8). */
        float offset;
    }

    public EngineRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static Identifier texture(String name) {
        return BuildCraftReborn.id("textures/block/" + name + ".png");
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EngineBlockEntity engine, State state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(engine, state, crumbling);
        state.facing = engine.facing();
        state.kind = engine.kind();
        state.stage = engine.stage();
        state.offset = 0.0F;
        Level level = engine.getLevel();
        if (level != null && engine.isActive()) {
            float speed = Math.max(0.02F, state.stage.pistonSpeed) * (state.kind == EngineBlock.Kind.REDSTONE ? 0.5F : 1.0F);
            float progress = ((level.getGameTime() % 100_000) + partialTick) * speed % 1.0F;
            state.offset = progress > 0.5F ? (1.0F - progress) * 15.99F : progress * 15.99F;
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        String kind = switch (state.kind) {
            case REDSTONE -> "redstone";
            case STIRLING -> "stirling";
            case CREATIVE -> "creative";
        };
        Identifier back = texture("engine/" + kind + "_back");
        Identifier side = texture("engine/" + kind + "_side");
        Identifier trunk = texture("engine/trunk_" + (state.kind == EngineBlock.Kind.CREATIVE ? "creative" : state.stage.name));
        int light = state.lightCoords;
        float offset = state.offset;

        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        switch (state.facing) {
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH -> pose.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> pose.mulPose(Axis.XP.rotationDegrees(90));
            case EAST -> pose.mulPose(Axis.ZP.rotationDegrees(-90));
            case WEST -> pose.mulPose(Axis.ZP.rotationDegrees(90));
            default -> {
            }
        }
        pose.translate(-0.5F, -0.5F, -0.5F);

        // placas: faces de cima e de baixo com a textura "back", laterais com a "side"
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(back), (matrix, consumer) -> {
            horizontal(matrix, consumer, light, 0, 16, 0, 16, 0, 4);
            horizontal(matrix, consumer, light, 0, 16, 0, 16, 4 + offset, 8 + offset);
        });
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(side), (matrix, consumer) -> {
            sides(matrix, consumer, light, 0, 16, 0, 16, 0, 4, 0, 0, 16, 4);
            sides(matrix, consumer, light, 0, 16, 0, 16, 4 + offset, 8 + offset, 0, 0, 16, 4);
        });
        // tronco: ponta 8×8 e laterais 8×12 da textura
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(trunk), (matrix, consumer) -> {
            horizontal(matrix, consumer, light, 4, 12, 4, 12, 4, 16, 0, 0, 8, 8);
            sides(matrix, consumer, light, 4, 12, 4, 12, 4, 16, 8, 0, 16, 12);
        });
        if (offset > 0.01F) {
            float height = offset;
            collector.submitCustomGeometry(pose, RenderTypes.entityCutout(CHAMBER), (matrix, consumer) ->
                    sides(matrix, consumer, light, 3, 13, 3, 13, 4, 4 + height, 3, 0, 13, height));
        }
        pose.popPose();
    }

    /** Faces de cima e de baixo de uma caixa (coordenadas em pixels). */
    private static void horizontal(PoseStack.Pose m, VertexConsumer c, int light, float x0, float x1, float z0, float z1, float y0, float y1) {
        horizontal(m, c, light, x0, x1, z0, z1, y0, y1, 0, 0, 16, 16);
    }

    private static void horizontal(PoseStack.Pose m, VertexConsumer c, int light, float x0, float x1, float z0, float z1, float y0, float y1,
                                   float u0, float v0, float u1, float v1) {
        quad(m, c, light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, u0, v0, u1, v1);
        quad(m, c, light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, u0, v0, u1, v1);
    }

    /** As quatro laterais de uma caixa, com o recorte {@code u0,v0 → u1,v1} da textura (em pixels). */
    private static void sides(PoseStack.Pose m, VertexConsumer c, int light, float x0, float x1, float z0, float z1, float y0, float y1,
                              float u0, float v0, float u1, float v1) {
        quad(m, c, light, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, u0, v1, u1, v0);
        quad(m, c, light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, u0, v1, u1, v0);
        quad(m, c, light, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, u0, v1, u1, v0);
        quad(m, c, light, 1, 0, 0, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, u0, v1, u1, v0);
    }

    /** Quadrilátero a→b→c→d; UV: a = (u0,v0) … c = (u1,v1). */
    private static void quad(PoseStack.Pose m, VertexConsumer c, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1) {
        vertex(m, c, light, nx, ny, nz, ax, ay, az, u0, v0);
        vertex(m, c, light, nx, ny, nz, bx, by, bz, u1, v0);
        vertex(m, c, light, nx, ny, nz, cx, cy, cz, u1, v1);
        vertex(m, c, light, nx, ny, nz, dx, dy, dz, u0, v1);
    }

    private static void vertex(PoseStack.Pose m, VertexConsumer c, int light, float nx, float ny, float nz,
                               float x, float y, float z, float u, float v) {
        c.addVertex(m, x / 16.0F, y / 16.0F, z / 16.0F).setColor(-1).setUv(u / 16.0F, v / 16.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(m, nx, ny, nz);
    }
}
