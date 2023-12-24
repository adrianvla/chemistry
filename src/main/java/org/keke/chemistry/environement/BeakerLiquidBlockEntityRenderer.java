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
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.model.json.ModelTransformation;
import org.keke.chemistry.entity.BeakerLiquidBlockEntity;
import org.keke.chemistry.item.ModItems;

@Environment(EnvType.CLIENT)
public class BeakerLiquidBlockEntityRenderer implements BlockEntityRenderer<BeakerLiquidBlockEntity > {
    // A jukebox itemstack
    private static ItemStack stack = new ItemStack(ModItems.BEAKER_LIQUID_ITEM, 1);

    public BeakerLiquidBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(BeakerLiquidBlockEntity blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        NbtCompound nbt = blockEntity.getNbt();
        int color = nbt.getInt("color");
        double offset = Math.sin((blockEntity.getWorld().getTime() + tickDelta) / 8.0) / 4.0;
//        color += 0x010101*(int)((offset*25));
//        if (color>0xffffff) color=0xffffff;
//        if (color<0x0) color=0x0;

        stack.getOrCreateNbt().putInt("color",color);
        matrices.push();
        // Move the item
        matrices.translate(0.5, 0.4375, 0.5);

        // Render the item
        MinecraftClient.getInstance().getItemRenderer().renderItem(stack,ModelTransformationMode.GROUND, light, overlay, matrices, vertexConsumers, blockEntity.getWorld(),0);

        // Mandatory call after GL calls
        matrices.pop();

    }
}