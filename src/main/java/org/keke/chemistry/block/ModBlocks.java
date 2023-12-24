package org.keke.chemistry.block;

import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.keke.chemistry.Chemistry;

public class ModBlocks {
    public static final Block BEAKER_BASE  = registerBlock("beaker_base",new BeakerBase(FabricBlockSettings.copy(Blocks.GLASS).strength(0.5f)));
    public static final Block BEAKER_LIQUID_ITEM_BLOCK = registerBlock("beaker_liquid_item_block",new BeakerLiquidItemBlock(FabricBlockSettings.copy(Blocks.GLASS).strength(0.5f)));

//    private static Block registerColoredBlock(String name, Block block){
//        ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> 0x3495eb, block);
//        return Registry.register(Registries.BLOCK, new Identifier(Chemistry.MOD_ID, name), block);
//    }
    private static Block registerBlock(String name, Block block) {
        return Registry.register(Registries.BLOCK, new Identifier(Chemistry.MOD_ID, name), block);
    }
}
