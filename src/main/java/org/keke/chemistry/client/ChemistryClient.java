package org.keke.chemistry.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keke.chemistry.block.ModBlocks;
import org.keke.chemistry.environement.BeakerLiquidBlockEntityRenderer;

import static org.keke.chemistry.entity.ModBlockEntities.BEAKER_LIQUID;

@Environment(EnvType.CLIENT)
public class ChemistryClient implements ClientModInitializer {
    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BEAKER_BASE, RenderLayer.getTranslucent());
        BlockEntityRendererRegistry.register(BEAKER_LIQUID, BeakerLiquidBlockEntityRenderer::new);
    }
}
