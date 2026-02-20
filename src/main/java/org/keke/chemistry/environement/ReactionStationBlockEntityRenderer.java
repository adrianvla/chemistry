package org.keke.chemistry.environement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import org.keke.chemistry.entity.ReactionStationBlockEntity;

@Environment(EnvType.CLIENT)
public class ReactionStationBlockEntityRenderer implements BlockEntityRenderer<ReactionStationBlockEntity> {

    public ReactionStationBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(ReactionStationBlockEntity blockEntity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // Particle effects are handled server-side via spawnParticles
        // Visual rendering of reaction contents could be added here
    }
}
