package net.buildcraftreborn.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.builders.tile.QuarryBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Pedreira como no BuildCraft: enquanto a armação não está pronta, a área fica marcada com a faixa
 * amarela e preta (acesa); minerando, aparecem as vigas do pórtico no topo da armação, a cabeça da
 * broca no cruzamento, a haste e a broca no bloco minerado, com a luz do mundo.
 */
public class QuarryRenderer implements BlockEntityRenderer<QuarryBlockEntity, QuarryRenderer.State> {
    private static final Identifier GANTRY = BuildCraftReborn.id("textures/block/builders/gantry.png");
    private static final Identifier DRILL_HEAD = BuildCraftReborn.id("textures/block/builders/drill_head.png");
    private static final Identifier DRILL_SHAFT = BuildCraftReborn.id("textures/block/builders/drill_shaft.png");
    private static final Identifier DRILL_TIP = BuildCraftReborn.id("textures/block/builders/drill_tip.png");
    private static final Identifier STRIPES = BuildCraftReborn.id("textures/entity/laser/stripes.png");
    private static final float STRIPES_WIDTH = 2.0F / 16.0F;
    private static final float BEAM_WIDTH = 0.5F;

    public enum Mode { NONE, AREA, MINING }

    public static final class State extends BlockEntityRenderState {
        Mode mode = Mode.NONE;
        double minX, maxX, minY, maxY, minZ, maxZ;
        Vec3 drill = Vec3.ZERO;
        int gantryLight = LaserRenderer.FULL_BRIGHT;
        int drillLight = LaserRenderer.FULL_BRIGHT;
    }

    public QuarryRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(QuarryBlockEntity quarry, State state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(quarry, state, crumbling);
        BlockPos min = quarry.min();
        BlockPos max = quarry.max();
        state.mode = Mode.NONE;
        Level level = quarry.getLevel();
        if (min == null || max == null || level == null) return;
        BlockPos origin = quarry.getBlockPos();
        state.minX = min.getX() - origin.getX() + 0.5;
        state.maxX = max.getX() - origin.getX() + 0.5;
        state.minY = min.getY() - origin.getY() + 0.5;
        state.maxY = max.getY() - origin.getY() + 0.5;
        state.minZ = min.getZ() - origin.getZ() + 0.5;
        state.maxZ = max.getZ() - origin.getZ() + 0.5;
        switch (quarry.stage()) {
            case FRAME -> state.mode = Mode.AREA;
            case MINE -> {
                Vec3 drill = quarry.shownDrill();
                if (drill != null) {
                    state.mode = Mode.MINING;
                    state.drill = drill.subtract(origin.getX(), origin.getY(), origin.getZ());
                    BlockPos head = BlockPos.containing(drill.x, max.getY() + 1, drill.z);
                    state.gantryLight = light(level, head);
                    state.drillLight = light(level, BlockPos.containing(drill.x, drill.y + 1, drill.z));
                }
            }
            default -> {
            }
        }
    }

    private static int light(Level level, BlockPos pos) {
        return level.getBrightness(LightLayer.BLOCK, pos) << 4 | level.getBrightness(LightLayer.SKY, pos) << 20;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        switch (state.mode) {
            case AREA -> area(state, pose, collector);
            case MINING -> gantry(state, pose, collector);
            default -> {
            }
        }
    }

    /** As 12 arestas da área com a faixa amarela e preta. */
    private static void area(State s, PoseStack pose, SubmitNodeCollector collector) {
        double[] xs = {s.minX, s.maxX};
        double[] ys = {s.minY, s.maxY};
        double[] zs = {s.minZ, s.maxZ};
        for (double y : ys) for (double z : zs) stripes(pose, collector, new Vec3(s.minX, y, z), new Vec3(s.maxX, y, z));
        for (double x : xs) for (double z : zs) stripes(pose, collector, new Vec3(x, s.minY, z), new Vec3(x, s.maxY, z));
        for (double x : xs) for (double y : ys) stripes(pose, collector, new Vec3(x, y, s.minZ), new Vec3(x, y, s.maxZ));
    }

    private static void stripes(PoseStack pose, SubmitNodeCollector collector, Vec3 from, Vec3 to) {
        LaserRenderer.beam(pose, collector, STRIPES, from, to, STRIPES_WIDTH, STRIPES_WIDTH * 4, false);
    }

    /** Vigas no topo, cabeça da broca no cruzamento, haste e broca até o bloco minerado. */
    private static void gantry(State s, PoseStack pose, SubmitNodeCollector collector) {
        double x = s.drill.x;
        double z = s.drill.z;
        double top = s.maxY;
        int light = s.gantryLight;
        LaserRenderer.beam(pose, collector, GANTRY, new Vec3(s.minX, top, z), new Vec3(s.maxX, top, z), BEAM_WIDTH, BEAM_WIDTH, true, light);
        LaserRenderer.beam(pose, collector, GANTRY, new Vec3(x, top, s.minZ), new Vec3(x, top, s.maxZ), BEAM_WIDTH, BEAM_WIDTH, true, light);
        // cabeça da broca: caixa de metal no cruzamento, um pouco maior que as vigas
        LaserRenderer.beam(pose, collector, DRILL_HEAD, new Vec3(x, top + 0.45, z), new Vec3(x, top - 0.45, z), 0.9F, 0.9F, true, light);

        // haste: cano ciano, um anel por bloco
        int drillLight = s.drillLight;
        double collarTop = s.drill.y + 0.55;
        if (top - 0.45 > collarTop) {
            LaserRenderer.beam(pose, collector, DRILL_SHAFT, new Vec3(x, top - 0.45, z), new Vec3(x, collarTop, z), 0.375F, 1.0F, false, drillLight);
        }
        // broca: colar largo e ponta afinando até dentro do bloco minerado
        LaserRenderer.beam(pose, collector, DRILL_HEAD, new Vec3(x, collarTop, z), new Vec3(x, collarTop - 0.15, z), 0.55F, 0.55F, true, drillLight);
        LaserRenderer.beam(pose, collector, DRILL_TIP, new Vec3(x, collarTop - 0.15, z), new Vec3(x, collarTop - 0.45, z), 0.4F, 0.4F, true, drillLight);
        LaserRenderer.beam(pose, collector, DRILL_TIP, new Vec3(x, collarTop - 0.45, z), new Vec3(x, collarTop - 0.7, z), 0.22F, 0.22F, true, drillLight);
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
