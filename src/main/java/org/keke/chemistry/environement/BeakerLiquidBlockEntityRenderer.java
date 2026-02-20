package org.keke.chemistry.environement;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.keke.chemistry.entity.BeakerLiquidBlockEntity;
import org.keke.chemistry.item.ModItems;

@Environment(EnvType.CLIENT)
public class BeakerLiquidBlockEntityRenderer implements BlockEntityRenderer<BeakerLiquidBlockEntity> {
    private final ItemStack stack = new ItemStack(ModItems.BEAKER_LIQUID_ITEM, 1);

    public BeakerLiquidBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(BeakerLiquidBlockEntity blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (blockEntity.isEmpty()) return;

        int color = blockEntity.getCachedColor();
        stack.getOrCreateNbt().putInt("cachedColor", color);

        matrices.push();
        matrices.translate(0.5, 0.4375, 0.5);
        MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.GROUND, light, overlay, matrices, vertexConsumers, blockEntity.getWorld(), 0);
        matrices.pop();
    }
}