package org.keke.chemistry.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.keke.chemistry.Chemistry;
import org.keke.chemistry.block.ModBlocks;
import org.keke.chemistry.utils.BeakerSize;
import org.keke.chemistry.utils.BeakerMaterial;

public class ModItems {
    public static final Item BEAKER = registerItem("beaker", new BeakerItem(new FabricItemSettings().maxCount(1)));

    // Internal render-only item, not obtainable by players
    public static final Item BEAKER_LIQUID_ITEM = registerItem("beaker_liquid_item_block", new Item(new FabricItemSettings()));

    // Lab equipment block items
    public static final Item REACTION_STATION_ITEM = registerItem("reaction_station",
            new BlockItem(ModBlocks.REACTION_STATION, new FabricItemSettings()));
    public static final Item FILTER_ITEM = registerItem("filter",
            new BlockItem(ModBlocks.FILTER, new FabricItemSettings()));
    public static final Item DISTILLATION_ITEM = registerItem("distillation",
            new BlockItem(ModBlocks.DISTILLATION, new FabricItemSettings()));
    public static final Item EVAPORATION_DISH_ITEM = registerItem("evaporation_dish",
            new BlockItem(ModBlocks.EVAPORATION_DISH, new FabricItemSettings()));
    public static final Item BUNSEN_BURNER_ITEM = registerItem("bunsen_burner",
            new BlockItem(ModBlocks.BUNSEN_BURNER, new FabricItemSettings()));
    public static final Item COOLING_BLOCK_ITEM = registerItem("cooling_block",
            new BlockItem(ModBlocks.COOLING_BLOCK, new FabricItemSettings()));
    public static final Item COMPOSITION_BLOCK_ITEM = registerItem("composition_block",
            new BlockItem(ModBlocks.COMPOSITION_BLOCK, new FabricItemSettings()));
    public static final Item PIPETTE_BLOCK_ITEM = registerItem("pipette_block",
            new BlockItem(ModBlocks.PIPETTE_BLOCK, new FabricItemSettings()));
    public static final Item PETRI_DISH_ITEM = registerItem("petri_dish",
            new BlockItem(ModBlocks.PETRI_DISH, new FabricItemSettings()));
    public static final Item SEDIMENTATION_BLOCK_ITEM = registerItem("sedimentation_block",
            new BlockItem(ModBlocks.SEDIMENTATION_BLOCK, new FabricItemSettings()));
    public static final Item VIBRATING_BLOCK_ITEM = registerItem("vibrating_block",
            new BlockItem(ModBlocks.VIBRATING_BLOCK, new FabricItemSettings()));

    // New container items
    public static final Item TEST_TUBE = registerItem("test_tube",
            new TestTubeItem(new FabricItemSettings().maxCount(1)));
    public static final Item AMPULE = registerItem("ampule",
            new AmpuleItem(new FabricItemSettings().maxCount(1)));
    public static final Item BOTTLE_ITEM = registerItem("bottle",
            new BottleItem(new FabricItemSettings().maxCount(1)));

    // Lab tools
    public static final Item PLIERS = registerItem("pliers",
            new PliersItem(new FabricItemSettings().maxCount(1).maxDamage(64)));
    public static final Item WASH_BOTTLE = registerItem("wash_bottle",
            new WashBottleItem(new FabricItemSettings().maxCount(1)));

    // New lab equipment block items
    public static final Item CRUCIBLE_ITEM = registerItem("crucible",
            new BlockItem(ModBlocks.CRUCIBLE, new FabricItemSettings()));
    public static final Item GAS_COLLECTOR_ITEM = registerItem("gas_collector",
            new BlockItem(ModBlocks.GAS_COLLECTOR, new FabricItemSettings()));
    public static final Item CONDENSER_ITEM = registerItem("condenser",
            new BlockItem(ModBlocks.CONDENSER, new FabricItemSettings()));
    public static final Item MORTAR_PESTLE_ITEM = registerItem("mortar_pestle",
            new BlockItem(ModBlocks.MORTAR_PESTLE, new FabricItemSettings()));
    public static final Item FUME_HOOD_ITEM = registerItem("fume_hood",
            new BlockItem(ModBlocks.FUME_HOOD, new FabricItemSettings()));

    private static final ItemGroup CONTAINERS = FabricItemGroup.builder()
            .icon(() -> new ItemStack(BEAKER))
            .displayName(Text.translatable("itemGroup.chemistry.containers"))
            .entries((context, entries) -> {
                entries.add(new ItemStack(BEAKER));
                // All beaker sizes
                for (BeakerSize size : BeakerSize.values()) {
                    if (size != BeakerSize.MEDIUM) { // MEDIUM is the default "Beaker"
                        ItemStack sizedBeaker = new ItemStack(BEAKER);
                        BeakerItem.setBeakerSize(sizedBeaker, size);
                        entries.add(sizedBeaker);
                    }
                }
                // Metal beakers (all sizes)
                for (BeakerSize size : BeakerSize.values()) {
                    ItemStack metalBeaker = new ItemStack(BEAKER);
                    BeakerItem.setBeakerSize(metalBeaker, size);
                    BeakerItem.setBeakerMaterial(metalBeaker, BeakerMaterial.METAL);
                    entries.add(metalBeaker);
                }
                // Pre-filled example beakers
                entries.add(createFilledBeaker("H2O", 1.0));
                entries.add(createFilledBeaker("HCl", 1.0));
                entries.add(createFilledBeaker("NaOH", 1.0));
                entries.add(createFilledBeaker("CuSO4", 1.0));
                entries.add(createFilledBeaker("KMnO4", 0.5));
                entries.add(createFilledBeaker("FeCl3", 1.0));
                entries.add(createFilledBeaker("H2SO4", 1.0));
                entries.add(createFilledBeaker("NaCl", 1.0));
                entries.add(createFilledBeaker("AgNO3", 1.0));
                entries.add(createFilledBeaker("Ca(OH)2", 1.0));
                // Lab equipment
                entries.add(new ItemStack(REACTION_STATION_ITEM));
                entries.add(new ItemStack(FILTER_ITEM));
                entries.add(new ItemStack(DISTILLATION_ITEM));
                entries.add(new ItemStack(EVAPORATION_DISH_ITEM));
                entries.add(new ItemStack(BUNSEN_BURNER_ITEM));
                entries.add(new ItemStack(COOLING_BLOCK_ITEM));
                entries.add(new ItemStack(COMPOSITION_BLOCK_ITEM));
                entries.add(new ItemStack(PIPETTE_BLOCK_ITEM));
                entries.add(new ItemStack(PETRI_DISH_ITEM));
                entries.add(new ItemStack(SEDIMENTATION_BLOCK_ITEM));
                entries.add(new ItemStack(VIBRATING_BLOCK_ITEM));
                // New containers
                entries.add(new ItemStack(TEST_TUBE));
                entries.add(new ItemStack(AMPULE));
                entries.add(new ItemStack(BOTTLE_ITEM));
                // Lab tools
                entries.add(new ItemStack(PLIERS));
                entries.add(new ItemStack(WASH_BOTTLE));
                // New lab equipment
                entries.add(new ItemStack(CRUCIBLE_ITEM));
                entries.add(new ItemStack(GAS_COLLECTOR_ITEM));
                entries.add(new ItemStack(CONDENSER_ITEM));
                entries.add(new ItemStack(MORTAR_PESTLE_ITEM));
                entries.add(new ItemStack(FUME_HOOD_ITEM));
                // Pre-filled explosive compound examples
                entries.add(createFilledBeaker("C3H5N3O9", 0.5));
                entries.add(createFilledBeaker("C7H5N3O6", 1.0));
            })
            .build();

    public static ItemStack createFilledBeaker(String formula, double moles) {
        ItemStack stack = new ItemStack(BEAKER);
        NbtCompound contentsNbt = new NbtCompound();
        contentsNbt.putDouble(formula, moles);
        stack.getOrCreateNbt().put("contents", contentsNbt);
        return stack;
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(Chemistry.MOD_ID, name), item);
    }

    private static ItemGroup registerItemGroup(String name, ItemGroup group) {
        return Registry.register(Registries.ITEM_GROUP, new Identifier(Chemistry.MOD_ID, name), group);
    }

    public static void registerModItems() {
        Chemistry.LOGGER.info("Registering items for " + Chemistry.MOD_ID);
        registerItemGroup("beakers", CONTAINERS);
    }
}
