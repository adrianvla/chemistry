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
    public static BlockEntityType<ReactionStationBlockEntity> REACTION_STATION;
    public static BlockEntityType<FilterBlockEntity> FILTER;
    public static BlockEntityType<DistillationBlockEntity> DISTILLATION;
    public static BlockEntityType<EvaporationDishBlockEntity> EVAPORATION_DISH;
    public static BlockEntityType<PetriDishBlockEntity> PETRI_DISH;
    public static BlockEntityType<ChemistryCauldronBlockEntity> CHEMISTRY_CAULDRON;
    public static BlockEntityType<SedimentationBlockEntity> SEDIMENTATION;
    public static BlockEntityType<VibratingBlockEntity> VIBRATING;

    public static void registerAllBlockEntities() {
        BEAKER_LIQUID = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Chemistry.MOD_ID, "beaker_liquid"),
                FabricBlockEntityTypeBuilder.create(BeakerLiquidBlockEntity::new, ModBlocks.BEAKER_BASE).build(null)
        );
        REACTION_STATION = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Chemistry.MOD_ID, "reaction_station"),
                FabricBlockEntityTypeBuilder.create(ReactionStationBlockEntity::new, ModBlocks.REACTION_STATION).build(null)
        );
        FILTER = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Chemistry.MOD_ID, "filter"),
                FabricBlockEntityTypeBuilder.create(FilterBlockEntity::new, ModBlocks.FILTER).build(null)
        );
        DISTILLATION = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Chemistry.MOD_ID, "distillation"),
                FabricBlockEntityTypeBuilder.create(DistillationBlockEntity::new, ModBlocks.DISTILLATION).build(null)
        );
        EVAPORATION_DISH = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Chemistry.MOD_ID, "evaporation_dish"),
                FabricBlockEntityTypeBuilder.create(EvaporationDishBlockEntity::new, ModBlocks.EVAPORATION_DISH).build(null)
        );
        PETRI_DISH = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Chemistry.MOD_ID, "petri_dish"),
                FabricBlockEntityTypeBuilder.create(PetriDishBlockEntity::new, ModBlocks.PETRI_DISH).build(null)
        );
        CHEMISTRY_CAULDRON = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Chemistry.MOD_ID, "chemistry_cauldron"),
                FabricBlockEntityTypeBuilder.create(ChemistryCauldronBlockEntity::new, ModBlocks.CHEMISTRY_CAULDRON).build(null)
        );
        SEDIMENTATION = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Chemistry.MOD_ID, "sedimentation"),
                FabricBlockEntityTypeBuilder.create(SedimentationBlockEntity::new, ModBlocks.SEDIMENTATION_BLOCK).build(null)
        );
        VIBRATING = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(Chemistry.MOD_ID, "vibrating"),
                FabricBlockEntityTypeBuilder.create(VibratingBlockEntity::new, ModBlocks.VIBRATING_BLOCK).build(null)
        );
    }
}
