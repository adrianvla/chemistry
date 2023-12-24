package org.keke.chemistry.entity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.keke.chemistry.Chemistry;
import org.keke.chemistry.block.ModBlocks;

public class ModBlockEntities {
    public static BlockEntityType<BeakerLiquidBlockEntity> BEAKER_LIQUID;

    public static void registerAllBlockEntities(){
        BEAKER_LIQUID = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Chemistry.MOD_ID, "beaker_liquid"),
                FabricBlockEntityTypeBuilder.create(BeakerLiquidBlockEntity::new, ModBlocks.BEAKER_BASE).build(null)
        );
    }
}
