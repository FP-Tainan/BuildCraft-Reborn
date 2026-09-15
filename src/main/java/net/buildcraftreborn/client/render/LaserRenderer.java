package net.buildcraftreborn.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Feixes de laser do BuildCraft (marcadores, pedreira, lasers do silício): uma caixa comprida e acesa,
 * com a textura repetida ao longo do comprimento.
 */
public final class LaserRenderer {
    public static final int FULL_BRIGHT = 0xF000F0;

    private LaserRenderer() {}

    /** Feixe fino dos marcadores: a textura se repete a cada bloco, sem tampas. */
    public static void beam(PoseStack pose, SubmitNodeCollector collector, Identifier texture, Vec3 from, Vec3 to, float width) {
        beam(pose, collector, texture, from, to, width, 1.0F, false);
    }

    /**
     * Feixe de {@code from} até {@code to} (coordenadas relativas ao pose atual).
     *
     * @param tileLength comprimento de cada repetição da textura; igual à largura deixa os ladrilhos quadrados
     * @param caps       fecha as duas pontas (vigas e cabeça da broca)
     */
    public static void beam(PoseStack pose, SubmitNodeCollector collector, Identifier texture, Vec3 from, Vec3 to,
                            float width, float tileLength, boolean caps) {
        beam(pose, collector, texture, from, to, width, tileLength, caps, FULL_BRIGHT);
    }

    /** Como acima, com a luz dada (peças sólidas, como o pórtico, usam a luz do mundo). */
    public static void beam(PoseStack pose, SubmitNodeCollector collector, Identifier texture, Vec3 from, Vec3 to,
                            float width, float tileLength, boolean caps, int light) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-3) return;
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        pose.pushPose();
        pose.translate(from.x, from.y, from.z);
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(delta.x, delta.z)));
        pose.mulPose(Axis.XP.rotation((float) Math.atan2(-delta.y, horizontal)));
        float half = width / 2.0F;
        float total = (float) length;
        float tile = Math.max(0.05F, tileLength);
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (matrix, consumer) -> {
            for (float start = 0.0F; start < total - 1.0E-4F; start += tile) {
                float end = Math.min(total, start + tile);
                float v = (end - start) / tile;
                quad(matrix, consumer, light, 0, 1, 0, -half, half, start, half, half, start, half, half, end, -half, half, end, v);
                quad(matrix, consumer, light, 0, -1, 0, half, -half, start, -half, -half, start, -half, -half, end, half, -half, end, v);
                quad(matrix, consumer, light, 1, 0, 0, half, half, start, half, -half, start, half, -half, end, half, half, end, v);
                quad(matrix, consumer, light, -1, 0, 0, -half, -half, start, -half, half, start, -half, half, end, -half, -half, end, v);
            }
            if (caps) {
                quad(matrix, consumer, light, 0, 0, -1, half, half, 0, -half, half, 0, -half, -half, 0, half, -half, 0, 1.0F);
                quad(matrix, consumer, light, 0, 0, 1, -half, half, total, half, half, total, half, -half, total, -half, -half, total, 1.0F);
            }
        });
        pose.popPose();
    }

    private static void quad(PoseStack.Pose matrix, VertexConsumer consumer, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float v) {
        vertex(matrix, consumer, light, nx, ny, nz, ax, ay, az, 0.0F, 0.0F);
        vertex(matrix, consumer, light, nx, ny, nz, bx, by, bz, 1.0F, 0.0F);
        vertex(matrix, consumer, light, nx, ny, nz, cx, cy, cz, 1.0F, v);
        vertex(matrix, consumer, light, nx, ny, nz, dx, dy, dz, 0.0F, v);
    }

    private static void vertex(PoseStack.Pose matrix, VertexConsumer consumer, int light, float nx, float ny, float nz,
                               float x, float y, float z, float u, float v) {
        consumer.addVertex(matrix, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(matrix, nx, ny, nz);
    }
}
