package org.keke.chemistry.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Set;

/**
 * Determines environmental interactions when chemical compounds contact
 * blocks in the Minecraft world. This handles splash effects from thrown
 * containers and powder placement.
 *
 * Interaction categories:
 *   - CORROSIVE: Strong acids/bases slowly destroy blocks (like stone, wood)
 *   - INCENDIARY: Thermite, phosphorus, alkali metals create fire
 *   - OXIDIZING: Strong oxidizers can ignite flammable blocks
 *   - EXTINGUISHING: Water, CO2 extinguish fire
 *   - FREEZING: Liquid nitrogen, dry ice can freeze water blocks
 *   - REACTIVE_WITH_WATER: Alkali metals, CaO explode/react with water blocks
 */
public class MaterialInteraction {

    private static final Set<String> STRONG_ACIDS = Set.of(
            "HCl", "HBr", "HI", "HNO3", "H2SO4", "HClO3", "HClO4", "HF"
    );

    private static final Set<String> STRONG_BASES = Set.of(
            "NaOH", "KOH", "LiOH", "Ba(OH)2", "Ca(OH)2"
    );

    private static final Set<String> WATER_REACTIVE_METALS = Set.of(
            "Na", "K", "Li", "Rb", "Cs", "Ca", "Ba"
    );

    private static final Set<String> INCENDIARY_COMPOUNDS = Set.of(
            "P"  // white phosphorus ignites in air
    );

    private static final Set<String> FIRE_EXTINGUISHERS = Set.of(
            "H2O", "CO2"
    );

    /**
     * Apply environmental effects when chemicals splash on blocks.
     * Called when a thrown container breaks on impact.
     *
     * @param world    the world
     * @param center   impact position
     * @param contents map of formula → moles
     * @param tempK    temperature of the contents
     * @param radius   splash radius in blocks
     */
    public static void applySplashEffects(World world, BlockPos center, Map<String, Double> contents,
                                           double tempK, int radius) {
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) return;
        if (contents.isEmpty()) return;

        for (var entry : contents.entrySet()) {
            String formula = entry.getKey();
            double moles = entry.getValue();

            // Fire extinguishing
            if (FIRE_EXTINGUISHERS.contains(formula)) {
                extinguishNearby(world, center, radius, moles);
            }

            // Corrosive effects on blocks
            if (STRONG_ACIDS.contains(formula) || STRONG_BASES.contains(formula)) {
                corrodeBlocks(world, center, radius, moles);
            }

            // Water-reactive metals
            if (WATER_REACTIVE_METALS.contains(formula)) {
                reactWithWaterBlocks(world, center, radius, formula, moles);
            }

            // Incendiary compounds
            if (INCENDIARY_COMPOUNDS.contains(formula) || isThermiteMixture(contents)) {
                igniteSurroundings(world, center, radius, moles);
            }
        }

        // Temperature-based interactions
        if (tempK > 573.15) { // > 300°C
            igniteSurroundings(world, center, Math.min(radius, 2), 0.5);
        }
        if (tempK < 200.0) { // very cold
            freezeWaterNearby(world, center, radius);
        }
    }

    /**
     * Detect if a mixture constitutes thermite (metal oxide + reactive metal).
     * Classic thermite: Fe2O3 + Al
     * Also: CuO + Al, Cr2O3 + Al, MnO2 + Al
     */
    public static boolean isThermiteMixture(Map<String, Double> contents) {
        boolean hasAluminum = contents.containsKey("Al");
        if (!hasAluminum) return false;

        Set<String> metalOxides = Set.of("Fe2O3", "Fe3O4", "CuO", "Cr2O3", "MnO2", "Co3O4", "NiO");
        for (String oxide : metalOxides) {
            if (contents.containsKey(oxide)) return true;
        }
        return false;
    }

    /**
     * Calculate thermite reaction power based on moles of limiting reagent.
     * Fe2O3 + 2Al → Al2O3 + 2Fe, ΔH = -851.5 kJ/mol
     */
    public static double getThermitePower(Map<String, Double> contents) {
        double alMoles = contents.getOrDefault("Al", 0.0);
        double fe2o3Moles = contents.getOrDefault("Fe2O3", 0.0);

        // Stoichiometry: 2 Al per 1 Fe2O3
        double limitingRatio = Math.min(alMoles / 2.0, fe2o3Moles);
        // ΔH = -851.5 kJ per mol Fe2O3 reacted
        return limitingRatio * 851.5;
    }

    private static void extinguishNearby(World world, BlockPos center, int radius, double moles) {
        int range = (int) Math.min(radius, 1 + moles * 2);
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    BlockState state = world.getBlockState(pos);
                    if (state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE)) {
                        world.removeBlock(pos, false);
                        if (world instanceof ServerWorld sw) {
                            sw.spawnParticles(ParticleTypes.SMOKE,
                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                    3, 0.2, 0.2, 0.2, 0.01);
                        }
                    }
                }
            }
        }
    }

    private static void corrodeBlocks(World world, BlockPos center, int radius, double moles) {
        // Strong acids/bases can destroy soft blocks (wood, wool, leaves)
        if (moles < 0.5) return;  // need significant amount
        int range = Math.min(radius, 1);
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    BlockState state = world.getBlockState(pos);
                    float hardness = state.getHardness(world, pos);
                    // Only corrode soft blocks (hardness < 1.0) and not air
                    if (hardness >= 0 && hardness < 1.0 && !state.isAir()) {
                        if (world.getRandom().nextFloat() < Math.min(0.5, moles * 0.2)) {
                            world.breakBlock(pos, false);
                            if (world instanceof ServerWorld sw) {
                                sw.spawnParticles(ParticleTypes.SMOKE,
                                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                        5, 0.3, 0.3, 0.3, 0.02);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void reactWithWaterBlocks(World world, BlockPos center, int radius, String metal, double moles) {
        int range = Math.min(radius, 2);
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    BlockState state = world.getBlockState(pos);
                    if (state.isOf(Blocks.WATER)) {
                        // Alkali metals react violently with water
                        // Na/K especially violent → small explosion + fire
                        float power = (float) Math.min(4.0, moles * 2.0);
                        if ("K".equals(metal) || "Rb".equals(metal) || "Cs".equals(metal)) {
                            power *= 1.5f; // more reactive
                        }
                        world.removeBlock(pos, false);
                        world.createExplosion(null, pos.getX() + 0.5, pos.getY() + 0.5,
                                pos.getZ() + 0.5, power, World.ExplosionSourceType.BLOCK);
                        // Spawn NaOH area effect cloud
                        GasCloudHelper.spawnGasClouds(world, pos, Map.of("H2", moles * 0.5));
                        return; // one reaction is enough
                    }
                }
            }
        }
    }

    private static void igniteSurroundings(World world, BlockPos center, int radius, double moles) {
        int range = Math.min(radius, (int) (1 + moles));
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    BlockPos above = pos.up();
                    if (world.getBlockState(above).isAir() && !world.getBlockState(pos).isAir()) {
                        if (world.getRandom().nextFloat() < Math.min(0.4, moles * 0.15)) {
                            world.setBlockState(above, Blocks.FIRE.getDefaultState());
                        }
                    }
                }
            }
        }
        // Spawn flame particles
        if (world instanceof ServerWorld sw) {
            sw.spawnParticles(ParticleTypes.FLAME,
                    center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                    20, radius * 0.5, 0.5, radius * 0.5, 0.05);
            sw.spawnParticles(ParticleTypes.LAVA,
                    center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                    8, radius * 0.3, 0.3, radius * 0.3, 0.01);
        }
        world.playSound(null, center, SoundEvents.ITEM_FIRECHARGE_USE,
                SoundCategory.BLOCKS, 1.5f, 0.8f);
    }

    private static void freezeWaterNearby(World world, BlockPos center, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.add(dx, dy, dz);
                    if (world.getBlockState(pos).isOf(Blocks.WATER)) {
                        world.setBlockState(pos, Blocks.ICE.getDefaultState());
                    }
                }
            }
        }
    }
}
