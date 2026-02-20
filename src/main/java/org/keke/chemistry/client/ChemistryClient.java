package org.keke.chemistry.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import org.keke.chemistry.block.ModBlocks;
import org.keke.chemistry.environement.BeakerLiquidBlockEntityRenderer;
import org.keke.chemistry.environement.ReactionStationBlockEntityRenderer;
import org.keke.chemistry.item.BeakerItem;
import org.keke.chemistry.item.ModItems;
import org.keke.chemistry.screen.ModScreenHandlers;
import org.keke.chemistry.utils.CalculateColor;

import java.util.Map;

import static org.keke.chemistry.entity.ModBlockEntities.BEAKER_LIQUID;
import static org.keke.chemistry.entity.ModBlockEntities.REACTION_STATION;

@Environment(EnvType.CLIENT)
public class ChemistryClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Render layers
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BEAKER_BASE, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.REACTION_STATION, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FILTER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.DISTILLATION, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.EVAPORATION_DISH, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BUNSEN_BURNER, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.COOLING_BLOCK, RenderLayer.getTranslucent());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.COMPOSITION_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PIPETTE_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.PETRI_DISH, RenderLayer.getTranslucent());

        // Block entity renderers
        BlockEntityRendererRegistry.register(BEAKER_LIQUID, BeakerLiquidBlockEntityRenderer::new);
        BlockEntityRendererRegistry.register(REACTION_STATION, ReactionStationBlockEntityRenderer::new);

        // Screen registrations
        HandledScreens.register(ModScreenHandlers.COMPOSITION, CompositionScreen::new);
        HandledScreens.register(ModScreenHandlers.PIPETTE, PipetteScreen::new);
        HandledScreens.register(ModScreenHandlers.POURING, PouringScreen::new);

        // Color providers — uses new multi-reactant contents map
        ColorProviderRegistry.ITEM.register((stack, layer) -> {
            if (layer == 0) {
                Map<String, Double> contents = BeakerItem.getContents(stack);
                if (contents.isEmpty()) return 0xFFFFFF;
                return CalculateColor.calculateBlendedColor(contents);
            }
            return 0xFFFFFF;
        }, ModItems.BEAKER);

        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
            return stack.getOrCreateNbt().getInt("cachedColor");
        }, ModItems.BEAKER_LIQUID_ITEM);
    }
}
