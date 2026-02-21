package org.keke.chemistry.block;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.keke.chemistry.Chemistry;

public class ModBlocks {
    public static final Block BEAKER_BASE = registerBlock("beaker_base",
            new BeakerBase(FabricBlockSettings.copy(Blocks.GLASS).strength(0.5f).nonOpaque()));
    public static final Block BEAKER_LIQUID_ITEM_BLOCK = registerBlock("beaker_liquid_item_block",
            new BeakerLiquidItemBlock(FabricBlockSettings.copy(Blocks.GLASS).strength(0.5f).nonOpaque()));
    public static final Block REACTION_STATION = registerBlock("reaction_station",
            new ReactionStationBlock(FabricBlockSettings.copy(Blocks.IRON_BLOCK).strength(2.0f).nonOpaque()));
    public static final Block FILTER = registerBlock("filter",
            new FilterBlock(FabricBlockSettings.copy(Blocks.GLASS).strength(0.5f).nonOpaque()));
    public static final Block DISTILLATION = registerBlock("distillation",
            new DistillationBlock(FabricBlockSettings.copy(Blocks.GLASS).strength(1.0f).nonOpaque()));
    public static final Block EVAPORATION_DISH = registerBlock("evaporation_dish",
            new EvaporationDishBlock(FabricBlockSettings.copy(Blocks.TERRACOTTA).strength(0.5f).nonOpaque()));
    public static final Block BUNSEN_BURNER = registerBlock("bunsen_burner",
            new BunsenBurnerBlock(FabricBlockSettings.copy(Blocks.IRON_BLOCK).strength(1.0f).nonOpaque().luminance(state -> state.get(BunsenBurnerBlock.LIT) ? 12 : 0)));
    public static final Block COOLING_BLOCK = registerBlock("cooling_block",
            new CoolingBlock(FabricBlockSettings.copy(Blocks.PACKED_ICE).strength(0.5f).nonOpaque().slipperiness(0.98f)));
    public static final Block COMPOSITION_BLOCK = registerBlock("composition_block",
            new CompositionBlock(FabricBlockSettings.copy(Blocks.IRON_BLOCK).strength(3.0f).nonOpaque()));
    public static final Block PIPETTE_BLOCK = registerBlock("pipette_block",
            new PipetteBlock(FabricBlockSettings.copy(Blocks.IRON_BLOCK).strength(3.0f).nonOpaque()));
    public static final Block PETRI_DISH = registerBlock("petri_dish",
            new PetriDishBlock(FabricBlockSettings.copy(Blocks.GLASS).strength(0.3f).nonOpaque()));
    public static final Block CHEMISTRY_CAULDRON = registerBlock("chemistry_cauldron",
            new ChemistryCauldronBlock(FabricBlockSettings.copy(Blocks.CAULDRON)));
    public static final Block SEDIMENTATION_BLOCK = registerBlock("sedimentation_block",
            new SedimentationBlock(FabricBlockSettings.copy(Blocks.IRON_BLOCK).strength(2.0f).nonOpaque()));
    public static final Block VIBRATING_BLOCK = registerBlock("vibrating_block",
            new VibratingBlock(FabricBlockSettings.copy(Blocks.IRON_BLOCK).strength(2.0f).nonOpaque()));

    // New container blocks
    public static final Block TEST_TUBE = registerBlock("test_tube",
            new TestTubeBlock(FabricBlockSettings.copy(Blocks.GLASS).strength(0.3f).nonOpaque()));
    public static final Block AMPULE = registerBlock("ampule",
            new AmpuleBlock(FabricBlockSettings.copy(Blocks.GLASS).strength(0.2f).nonOpaque()));
    public static final Block BOTTLE = registerBlock("bottle",
            new BottleBlock(FabricBlockSettings.copy(Blocks.GLASS).strength(0.4f).nonOpaque()));

    // New lab equipment blocks
    public static final Block CRUCIBLE = registerBlock("crucible",
            new CrucibleBlock(FabricBlockSettings.copy(Blocks.TERRACOTTA).strength(1.5f).nonOpaque()));
    public static final Block GAS_COLLECTOR = registerBlock("gas_collector",
            new GasCollectorBlock(FabricBlockSettings.copy(Blocks.GLASS).strength(0.5f).nonOpaque()));
    public static final Block CONDENSER = registerBlock("condenser",
            new CondenserBlock(FabricBlockSettings.copy(Blocks.GLASS).strength(0.5f).nonOpaque()));
    public static final Block MORTAR_PESTLE = registerBlock("mortar_pestle",
            new MortarPestleBlock(FabricBlockSettings.copy(Blocks.STONE).strength(1.0f).nonOpaque()));
    public static final Block FUME_HOOD = registerBlock("fume_hood",
            new FumeHoodBlock(FabricBlockSettings.copy(Blocks.IRON_BLOCK).strength(2.0f).nonOpaque()));

    private static Block registerBlock(String name, Block block) {
        return Registry.register(Registries.BLOCK, new Identifier(Chemistry.MOD_ID, name), block);
    }
}
