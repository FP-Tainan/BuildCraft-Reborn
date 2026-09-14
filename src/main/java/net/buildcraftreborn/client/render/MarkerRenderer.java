package net.buildcraftreborn.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.buildcraftreborn.BuildCraftReborn;
import net.buildcraftreborn.core.marker.MarkerBlock;
import net.buildcraftreborn.core.marker.MarkerBlockEntity;
import net.buildcraftreborn.core.marker.MarkerConnectorItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lasers dos marcadores: ligações feitas, contorno da caixa formada, eixos com sinal de redstone e,
 * com o conector na mão, as ligações possíveis.
 */
public class MarkerRenderer implements BlockEntityRenderer<MarkerBlockEntity, MarkerRenderer.State> {
    private static final Identifier VOLUME_CONNECTED = laser("marker_volume_connected");
    private static final Identifier VOLUME_POSSIBLE = laser("marker_volume_possible");
    private static final Identifier VOLUME_SIGNAL = laser("marker_volume_signal");
    private static final Identifier PATH_CONNECTED = laser("marker_path_connected");
    private static final float WIDTH = 2.0F / 16.0F;
    private static final Vec3 CENTER = new Vec3(0.5, 0.5, 0.5);

    record Beam(Vec3 from, Vec3 to, Identifier texture) {}

    public static final class State extends BlockEntityRenderState {
        final List<Beam> beams = new ArrayList<>();
    }

    public MarkerRenderer(BlockEntityRendererProvider.Context context) {
    }

    private static Identifier laser(String name) {
        return BuildCraftReborn.id("textures/entity/laser/" + name + ".png");
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MarkerBlockEntity marker, State state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(marker, state, crumbling);
        state.beams.clear();
        Level level = marker.getLevel();
        if (level == null) return;
        BlockPos pos = marker.getBlockPos();
        boolean volume = marker.kind() == MarkerBlock.Kind.VOLUME;

        // cada ligação é desenhada uma vez, pelo marcador de menor posição
        for (BlockPos other : marker.connections()) {
            if (other.asLong() > pos.asLong()) {
                state.beams.add(new Beam(CENTER, relative(pos, other), volume ? VOLUME_CONNECTED : PATH_CONNECTED));
            }
        }

        if (!volume) return;
        Set<BlockPos> group = marker.group();
        if (group.size() >= 3 && isLowest(pos, group)) addBox(state, pos, group);

        if (level.hasNeighborSignal(pos)) {
            int range = BuildCraftReborn.config.markerMaxDistance;
            for (Direction direction : Direction.values()) {
                if (marker.hasFreeAxis(direction.getAxis())) {
                    state.beams.add(new Beam(CENTER, CENTER.add(Vec3.atLowerCornerOf(direction.getUnitVec3i()).scale(range)), VOLUME_SIGNAL));
                }
            }
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && (player.getMainHandItem().getItem() instanceof MarkerConnectorItem
                || player.getOffhandItem().getItem() instanceof MarkerConnectorItem)) {
            for (Direction.Axis axis : Direction.Axis.values()) {
                if (!marker.hasFreeAxis(axis)) continue;
                MarkerBlockEntity partner = MarkerBlockEntity.findVolumePartner(level, pos, axis);
                if (partner != null && partner.getBlockPos().asLong() > pos.asLong()) {
                    state.beams.add(new Beam(CENTER, relative(pos, partner.getBlockPos()), VOLUME_POSSIBLE));
                }
            }
        }
    }

    private static boolean isLowest(BlockPos pos, Set<BlockPos> group) {
        for (BlockPos member : group) {
            if (member.asLong() < pos.asLong()) return false;
        }
        return true;
    }

    /** Contorno da caixa (12 arestas; as de comprimento zero somem). */
    private static void addBox(State state, BlockPos origin, Set<BlockPos> group) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos member : group) {
            minX = Math.min(minX, member.getX()); maxX = Math.max(maxX, member.getX());
            minY = Math.min(minY, member.getY()); maxY = Math.max(maxY, member.getY());
            minZ = Math.min(minZ, member.getZ()); maxZ = Math.max(maxZ, member.getZ());
        }
        int[] xs = {minX, maxX};
        int[] ys = {minY, maxY};
        int[] zs = {minZ, maxZ};
        for (int y : ys) for (int z : zs) edge(state, origin, new BlockPos(minX, y, z), new BlockPos(maxX, y, z));
        for (int x : xs) for (int z : zs) edge(state, origin, new BlockPos(x, minY, z), new BlockPos(x, maxY, z));
        for (int x : xs) for (int y : ys) edge(state, origin, new BlockPos(x, y, minZ), new BlockPos(x, y, maxZ));
    }

    private static void edge(State state, BlockPos origin, BlockPos a, BlockPos b) {
        if (!a.equals(b)) state.beams.add(new Beam(relative(origin, a), relative(origin, b), VOLUME_CONNECTED));
    }

    private static Vec3 relative(BlockPos origin, BlockPos target) {
        return new Vec3(target.getX() - origin.getX() + 0.5, target.getY() - origin.getY() + 0.5, target.getZ() - origin.getZ() + 0.5);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (Beam beam : state.beams) {
            LaserRenderer.beam(pose, collector, beam.texture(), beam.from(), beam.to(), WIDTH);
        }
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
