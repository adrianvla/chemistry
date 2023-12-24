package org.keke.chemistry.item;

import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.block.AbstractBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.GlassBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.keke.chemistry.Chemistry;
import net.minecraft.item.Item;
import org.keke.chemistry.block.ModBlocks;

import java.util.Objects;

import static org.keke.chemistry.utils.calculateColor.calculateColor;

public class ModItems {
    public static final Item BEAKER = registerColoredItem("beaker", new Item(new FabricItemSettings()));

    public static final Item BEAKER_LIQUID_ITEM = registerLiquidItem("beaker_liquid_item_block", new BlockItem(ModBlocks.BEAKER_LIQUID_ITEM_BLOCK,new FabricItemSettings()));
    private static final ItemGroup CONTAINERS = FabricItemGroup.builder()
            .icon(() -> new ItemStack(BEAKER))
            .displayName(Text.translatable("itemGroup.chemistry.containers"))
            .entries((context, entries) -> {
                entries.add(BEAKER);
                entries.add(BEAKER_LIQUID_ITEM);
            })
            .build();

    private static Item registerLiquidItem(String name, Item item){
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> {
//            return layer == 1 ? stack.getOrCreateNbt().getInt("color") : 0xFFFFFF;
            return stack.getOrCreateNbt().getInt("color");
//                return 0x3495eb;
        }, item);
        return Registry.register(Registries.ITEM, new Identifier(Chemistry.MOD_ID, name), item);
    }
    private static Item registerColoredItem(String name, Item item){
        ColorProviderRegistry.ITEM.register((stack, layer) -> {
//            return layer == 1 ? stack.getOrCreateNbt().getInt("color") : 0xFFFFFF;
            return layer == 0 ? calculateColor(stack.getOrCreateNbt().getString("formula")) : 0xFFFFFF;
        }, item);
        return Registry.register(Registries.ITEM, new Identifier(Chemistry.MOD_ID, name), item);
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(Chemistry.MOD_ID, name), item);
    }

    private static ItemGroup registerItemGroup(String name, ItemGroup group) {
        return Registry.register(Registries.ITEM_GROUP, new Identifier(Chemistry.MOD_ID, name), group);
    }
    public static void registerModItems() {
        Chemistry.LOGGER.info("Registering items for "+Chemistry.MOD_ID);
        registerItemGroup("beakers",CONTAINERS);
    }


}
