package net.buildcraftreborn.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.buildcraftreborn.transport.tile.PipeBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Itens andando dentro do tubo: do lado de entrada até o centro e do centro até a saída. */
public class PipeItemRenderer implements BlockEntityRenderer<PipeBlockEntity, PipeItemRenderer.State> {
    private static final Vec3 CENTER = new Vec3(0.5, 0.5, 0.5);
    private static final float SCALE = 0.6F;
    private final ItemModelResolver resolver;

    public static final class State extends BlockEntityRenderState {
        final List<ItemStackRenderState> stacks = new ArrayList<>();
        final List<Vec3> positions = new ArrayList<>();
        final List<PipePlugRenderer.PlugView> plugs = new ArrayList<>();
        /** Cor (ARGB) da caixinha em volta de cada item pintado; 0 sem cor. */
        final List<Integer> colours = new ArrayList<>();
    }

    private static final net.minecraft.resources.Identifier COLOUR_BOX = net.buildcraftreborn.BuildCraftReborn.id("textures/entity/pipe/colour_item_box.png");

    public PipeItemRenderer(BlockEntityRendererProvider.Context context) {
        this.resolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PipeBlockEntity pipe, State state, float partialTick, Vec3 cameraPos,
                                   @Nullable ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(pipe, state, crumbling);
        state.stacks.clear();
        state.positions.clear();
        state.colours.clear();
        PipePlugRenderer.extract(pipe, state.plugs);
        int seed = 0;
        for (PipeBlockEntity.TravellingItem item : pipe.items()) {
            float progress = Math.min(item.to == null ? 0.5F : 1.0F, item.progress + item.speed * partialTick);
            ItemStackRenderState stack = new ItemStackRenderState();
            this.resolver.updateForTopItem(stack, item.stack, ItemDisplayContext.GROUND, pipe.getLevel(), null, seed++);
            if (stack.isEmpty()) continue;
            state.stacks.add(stack);
            state.positions.add(position(item.from, item.to, progress));
            state.colours.add(net.buildcraftreborn.transport.PipeColours.argb(item.colour));
        }
    }

    private static Vec3 position(@Nullable Direction from, @Nullable Direction to, float progress) {
        if (progress < 0.5F) return from == null ? CENTER : CENTER.add(unit(from).scale(0.5F - progress));
        return to == null ? CENTER : CENTER.add(unit(to).scale(progress - 0.5F));
    }

    private static Vec3 unit(Direction direction) {
        return new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        PipePlugRenderer.submit(state.plugs, pose, collector, state.lightCoords);
        for (int i = 0; i < state.stacks.size(); i++) {
            Vec3 position = state.positions.get(i);
            int colour = state.colours.get(i);
            if (colour != 0) {
                float x = (float) position.x, y = (float) position.y, z = (float) position.z;
                PipePlugRenderer.cube(pose, collector, COLOUR_BOX, state.lightCoords, colour, true, true, x - 0.2F, y - 0.2F, z - 0.2F,
                        x + 0.2F, y + 0.2F, z + 0.2F);
            }
            pose.pushPose();
            pose.translate(position.x, position.y - 0.15, position.z);
            pose.scale(SCALE, SCALE, SCALE);
            state.stacks.get(i).submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
    }
}
