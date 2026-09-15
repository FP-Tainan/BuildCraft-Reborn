package net.buildcraftreborn.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.transport.PipeColours;
import net.buildcraftreborn.transport.gate.GateLogic;
import net.buildcraftreborn.transport.gate.GateVariant;
import net.buildcraftreborn.transport.plug.PipePlugs;
import net.buildcraftreborn.transport.plug.PlugHolder;
import net.buildcraftreborn.transport.plug.PlugKind;
import net.buildcraftreborn.transport.plug.WireNetwork;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Encaixes do tubo: portas lógicas ({@code gate.json}: placa do material, placa da lógica acesa quando a porta
 * está ligada e pinos do modificador), plugues (pulsar, sensor de luz, temporizador), lentes e filtros (moldura e
 * vidro colorido) e fios nos cantos, acesos quando a rede está ligada.
 */
public final class PipePlugRenderer {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float WIRE_OFFSET = 4.51F / 16.0F;
    private static final float WIRE_HALF = 0.5F / 16.0F;

    /** Um encaixe pronto para desenhar. */
    public interface PlugView {
        void submit(PoseStack pose, SubmitNodeCollector collector, int light);
    }

    private PipePlugRenderer() {}

    private static Identifier texture(String path) {
        return BuildCraftReborn.id("textures/entity/" + path + ".png");
    }

    public static void extract(@Nullable PlugHolder holder, List<PlugView> out) {
        out.clear();
        if (holder == null) return;
        PipePlugs plugs = holder.plugs();
        for (Map.Entry<Direction, GateLogic> entry : plugs.gates().entrySet()) {
            out.add(new GateView(entry.getKey(), entry.getValue().variant(), entry.getValue().isOn()));
        }
        for (Direction side : Direction.values()) {
            PlugKind kind = plugs.kind(side);
            if (kind != null) out.add(new BoxPlugView(side, kind, plugs.pulsing(side)));
            PipePlugs.Lens lens = plugs.lens(side);
            if (lens != null) out.add(new LensView(side, lens.colour(), lens.filter()));
            PipePlugs.Facade facade = plugs.facade(side);
            if (facade != null) out.add(FacadeView.of(side, facade));
        }
        if (!(holder instanceof BlockEntity blockEntity) || blockEntity.getLevel() == null) return;
        for (int corner = 0; corner < WireNetwork.CORNERS; corner++) {
            DyeColor color = plugs.wire(corner);
            if (color == null) continue;
            int links = 0;
            for (int axis = 0; axis < 3; axis++) {
                int mirrored = corner ^ (1 << axis);
                if (plugs.wire(mirrored) == color) links |= 1 << axis;
                Direction direction = WireNetwork.face(corner, axis);
                if (!plugs.has(direction)
                        && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos().relative(direction)) instanceof PlugHolder neighbor
                        && !neighbor.plugs().has(direction.getOpposite()) && neighbor.plugs().wire(mirrored) == color) {
                    links |= 8 << axis;
                }
            }
            out.add(new WireView(corner, color, plugs.wirePowered(corner), links));
        }
    }

    public static void submit(List<PlugView> plugs, PoseStack pose, SubmitNodeCollector collector, int light) {
        for (PlugView plug : plugs) plug.submit(pose, collector, light);
    }

    /** Gira o espaço para a face do encaixe ficar em z = 0 (norte). */
    private static void faceTransform(PoseStack pose, Direction side) {
        pose.translate(0.5F, 0.5F, 0.5F);
        switch (side) {
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(-90));
            case UP -> pose.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(-90));
            default -> {
            }
        }
        pose.translate(-0.5F, -0.5F, -0.5F);
    }

    private record GateView(Direction side, GateVariant variant, boolean on) implements PlugView {
        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light) {
            pose.pushPose();
            faceTransform(pose, this.side);
            box(pose, collector, this.variant.material().texture, light, 3, 3, 1, 13, 13, 3);
            if (this.variant.material() == GateVariant.Material.CLAY_BRICK) {
                box(pose, collector, texture("gate/" + (this.on ? "gate_on" : "gate_off")), this.on ? FULL_BRIGHT : light, 6, 6, 0, 10, 10, 1);
            } else {
                String logic = "gate/gate_" + this.variant.logic().id() + (this.on ? "_lit" : "_dark");
                box(pose, collector, texture(logic), this.on ? FULL_BRIGHT : light, 5, 5, 0, 11, 11, 1);
            }
            Identifier modifier = this.variant.modifier().texture;
            if (modifier != null) {
                box(pose, collector, modifier, light, 3, 3, 0, 5, 5, 1);
                box(pose, collector, modifier, light, 11, 3, 0, 13, 5, 1);
                box(pose, collector, modifier, light, 3, 11, 0, 5, 13, 1);
                box(pose, collector, modifier, light, 11, 11, 0, 13, 13, 1);
            }
            pose.popPose();
        }
    }

    private record BoxPlugView(Direction side, PlugKind kind, boolean pulsing) implements PlugView {
        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light) {
            pose.pushPose();
            faceTransform(pose, this.side);
            box(pose, collector, this.kind.texture, light, 5, 5, 2, 11, 11, 4);
            if (this.kind == PlugKind.PULSAR) {
                Identifier core = texture("plug/" + (this.pulsing ? "pulsar_dynamic_on" : "pulsar_dynamic_off"));
                box(pose, collector, core, this.pulsing ? FULL_BRIGHT : light, 6, 6, 0, 10, 10, 2);
            }
            pose.popPose();
        }
    }

    /** Moldura (lente ou filtro) e o vidro no meio, com a cor da lente; filtro transparente não tem vidro. */
    private record LensView(Direction side, @Nullable DyeColor colour, boolean filter) implements PlugView {
        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light) {
            pose.pushPose();
            faceTransform(pose, this.side);
            box(pose, collector, texture("plug/" + (this.filter ? "filter" : "lens")), light, 3, 3, 0, 13, 13, 2);
            if (!this.filter || this.colour != null) {
                int tint = this.colour == null ? 0xB0FFFFFF : 0xB0000000 | PipeColours.rgb(this.colour);
                cube(pose, collector, texture("plug/overlay_lens"), light, tint, true, false,
                        4 / 16F, 4 / 16F, 0.5F / 16F, 12 / 16F, 12 / 16F, 1.5F / 16F);
            }
            pose.popPose();
        }
    }

    /**
     * Fachada: os quads do modelo do bloco achatados numa placa de 2 px na face; a vazada vira quatro tiras em
     * volta do tubo.
     */
    private record FacadeView(Direction side, boolean hollow, List<net.minecraft.client.resources.model.geometry.BakedQuad> quads,
                              int[] tints) implements PlugView {
        static FacadeView of(Direction side, PipePlugs.Facade facade) {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            net.minecraft.client.renderer.block.dispatch.BlockStateModel model =
                    minecraft.getModelManager().getBlockStateModelSet().get(facade.state());
            List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> parts = new java.util.ArrayList<>();
            model.collectParts(net.minecraft.util.RandomSource.create(42L), parts);
            List<net.minecraft.client.resources.model.geometry.BakedQuad> quads = new java.util.ArrayList<>();
            for (net.minecraft.client.renderer.block.dispatch.BlockStateModelPart part : parts) {
                quads.addAll(part.getQuads(null));
                for (Direction direction : Direction.values()) quads.addAll(part.getQuads(direction));
            }
            int[] tints = new int[quads.size()];
            for (int i = 0; i < tints.length; i++) {
                int tintIndex = quads.get(i).materialInfo().tintIndex();
                net.minecraft.client.color.block.BlockTintSource source = tintIndex < 0 ? null
                        : minecraft.getBlockColors().getTintSource(facade.state(), tintIndex);
                tints[i] = source == null ? -1 : 0xFF000000 | source.color(facade.state());
            }
            return new FacadeView(side, facade.hollow(), quads, tints);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light) {
            pose.pushPose();
            faceTransform(pose, this.side);
            if (!this.hollow) {
                slab(pose, collector, light, 0, 0, 1, 1);
            } else {
                slab(pose, collector, light, 0, 0, 1, 0.25F);
                slab(pose, collector, light, 0, 0.75F, 1, 1);
                slab(pose, collector, light, 0, 0.25F, 0.25F, 0.75F);
                slab(pose, collector, light, 0.75F, 0.25F, 1, 0.75F);
            }
            pose.popPose();
        }

        private void slab(PoseStack pose, SubmitNodeCollector collector, int light, float x0, float y0, float x1, float y1) {
            pose.pushPose();
            pose.translate(x0, y0, 0);
            pose.scale(x1 - x0, y1 - y0, 2.0F / 16.0F);
            for (int q = 0; q < this.quads.size(); q++) {
                net.minecraft.client.resources.model.geometry.BakedQuad quad = this.quads.get(q);
                Identifier atlas = quad.materialInfo().sprite().atlasLocation();
                boolean translucent = "TRANSLUCENT".equals(quad.materialInfo().layer().name());
                int tint = this.tints[q];
                Direction normal = quad.direction();
                collector.submitCustomGeometry(pose, translucent ? RenderTypes.entityTranslucent(atlas) : RenderTypes.entityCutout(atlas), (m, c) -> {
                    for (int i = 0; i < 4; i++) {
                        org.joml.Vector3fc position = quad.position(i);
                        long uv = quad.packedUV(i);
                        c.addVertex(m, position.x(), position.y(), position.z()).setColor(tint)
                                .setUv(net.minecraft.client.model.geom.builders.UVPair.unpackU(uv), net.minecraft.client.model.geom.builders.UVPair.unpackV(uv))
                                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                                .setNormal(m, normal.getStepX(), normal.getStepY(), normal.getStepZ());
                    }
                });
            }
            pose.popPose();
        }
    }

    private record WireView(int corner, DyeColor color, boolean powered, int links) implements PlugView {
        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light) {
            Identifier wireTexture = texture("wire/" + this.color.getName());
            int wireLight = this.powered ? FULL_BRIGHT : light;
            int tint = this.powered ? -1 : 0xFF8A8A8A;
            float[] point = new float[3];
            for (int axis = 0; axis < 3; axis++) point[axis] = 0.5F + ((this.corner >> axis & 1) != 0 ? WIRE_OFFSET : -WIRE_OFFSET);
            segment(pose, collector, wireTexture, wireLight, tint, point, -1, 0, 0);
            for (int axis = 0; axis < 3; axis++) {
                boolean positive = (this.corner >> axis & 1) != 0;
                if ((this.links & (1 << axis)) != 0) segment(pose, collector, wireTexture, wireLight, tint, point, axis, point[axis], 0.5F);
                if ((this.links & (8 << axis)) != 0) {
                    segment(pose, collector, wireTexture, wireLight, tint, point, axis, point[axis], positive ? 1.0F : 0.0F);
                }
            }
        }

        /** Cubinho no canto ({@code axis} = -1) ou trecho do fio ao longo de um eixo. */
        private static void segment(PoseStack pose, SubmitNodeCollector collector, Identifier texture, int light, int tint,
                                    float[] point, int axis, float from, float to) {
            float[] min = {point[0] - WIRE_HALF, point[1] - WIRE_HALF, point[2] - WIRE_HALF};
            float[] max = {point[0] + WIRE_HALF, point[1] + WIRE_HALF, point[2] + WIRE_HALF};
            if (axis >= 0) {
                min[axis] = Math.min(from, to);
                max[axis] = Math.max(from, to);
            }
            cube(pose, collector, texture, light, tint, false, false, min[0], min[1], min[2], max[0], max[1], max[2]);
        }
    }

    /** Caixa em pixels com a textura recortada no tamanho de cada face. */
    private static void box(PoseStack pose, SubmitNodeCollector collector, Identifier texture, int light,
                            float x0, float y0, float z0, float x1, float y1, float z1) {
        cube(pose, collector, texture, light, -1, false, false, x0 / 16F, y0 / 16F, z0 / 16F, x1 / 16F, y1 / 16F, z1 / 16F);
    }

    /**
     * Caixa em blocos. {@code translucent} usa mistura (vidro); {@code fullUv} estica a textura inteira em cada face
     * (senão recorta pela posição).
     */
    public static void cube(PoseStack pose, SubmitNodeCollector collector, Identifier texture, int light, int tint, boolean translucent,
                            boolean fullUv, float ax, float ay, float az, float bx, float by, float bz) {
        collector.submitCustomGeometry(pose, translucent ? RenderTypes.entityTranslucent(texture) : RenderTypes.entityCutout(texture), (m, c) -> {
            float uxa = fullUv ? 0 : ax, uxb = fullUv ? 1 : bx;
            float vya = fullUv ? 0 : ay, vyb = fullUv ? 1 : by;
            float uza = fullUv ? 0 : az, uzb = fullUv ? 1 : bz;
            quad(m, c, light, tint, 0, 0, -1, bx, ay, az, ax, ay, az, ax, by, az, bx, by, az, uxa, vya, uxb, vyb);
            quad(m, c, light, tint, 0, 0, 1, ax, ay, bz, bx, ay, bz, bx, by, bz, ax, by, bz, uxa, vya, uxb, vyb);
            quad(m, c, light, tint, -1, 0, 0, ax, ay, az, ax, ay, bz, ax, by, bz, ax, by, az, uza, vya, uzb, vyb);
            quad(m, c, light, tint, 1, 0, 0, bx, ay, bz, bx, ay, az, bx, by, az, bx, by, bz, uza, vya, uzb, vyb);
            quad(m, c, light, tint, 0, 1, 0, ax, by, bz, bx, by, bz, bx, by, az, ax, by, az, uxa, uza, uxb, uzb);
            quad(m, c, light, tint, 0, -1, 0, ax, ay, az, bx, ay, az, bx, ay, bz, ax, ay, bz, uxa, uza, uxb, uzb);
        });
    }

    private static void quad(PoseStack.Pose m, VertexConsumer c, int light, int tint, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float u0, float v0, float u1, float v1) {
        vertex(m, c, light, tint, nx, ny, nz, ax, ay, az, u0, v1);
        vertex(m, c, light, tint, nx, ny, nz, bx, by, bz, u1, v1);
        vertex(m, c, light, tint, nx, ny, nz, cx, cy, cz, u1, v0);
        vertex(m, c, light, tint, nx, ny, nz, dx, dy, dz, u0, v0);
    }

    private static void vertex(PoseStack.Pose m, VertexConsumer c, int light, int tint, float nx, float ny, float nz,
                               float x, float y, float z, float u, float v) {
        c.addVertex(m, x, y, z).setColor(tint).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(m, nx, ny, nz);
    }
}
