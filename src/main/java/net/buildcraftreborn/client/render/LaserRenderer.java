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
 * Feixes de laser do BuildCraft (marcadores, pedreira, lasers do silício): um tubo quadrado fino,
 * aceso, com a textura repetida a cada bloco de comprimento.
 */
public final class LaserRenderer {
    public static final int FULL_BRIGHT = 0xF000F0;

    private LaserRenderer() {}

    /** Feixe de {@code from} até {@code to}, em coordenadas relativas ao pose atual. */
    public static void beam(PoseStack pose, SubmitNodeCollector collector, Identifier texture, Vec3 from, Vec3 to, float width) {
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
        collector.submitCustomGeometry(pose, RenderTypes.entityCutout(texture), (matrix, consumer) -> {
            for (float start = 0.0F; start < total; start += 1.0F) {
                float end = Math.min(total, start + 1.0F);
                float v = end - start;
                quad(matrix, consumer, 0, 1, 0, -half, half, start, half, half, start, half, half, end, -half, half, end, v);
                quad(matrix, consumer, 0, -1, 0, half, -half, start, -half, -half, start, -half, -half, end, half, -half, end, v);
                quad(matrix, consumer, 1, 0, 0, half, half, start, half, -half, start, half, -half, end, half, half, end, v);
                quad(matrix, consumer, -1, 0, 0, -half, -half, start, -half, half, start, -half, half, end, -half, -half, end, v);
            }
        });
        pose.popPose();
    }

    private static void quad(PoseStack.Pose matrix, VertexConsumer consumer, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float v) {
        vertex(matrix, consumer, nx, ny, nz, ax, ay, az, 0.0F, 0.0F);
        vertex(matrix, consumer, nx, ny, nz, bx, by, bz, 1.0F, 0.0F);
        vertex(matrix, consumer, nx, ny, nz, cx, cy, cz, 1.0F, v);
        vertex(matrix, consumer, nx, ny, nz, dx, dy, dz, 0.0F, v);
    }

    private static void vertex(PoseStack.Pose matrix, VertexConsumer consumer, float nx, float ny, float nz,
                               float x, float y, float z, float u, float v) {
        consumer.addVertex(matrix, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(FULL_BRIGHT).setNormal(matrix, nx, ny, nz);
    }
}
