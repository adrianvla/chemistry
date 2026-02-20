package org.keke.chemistry.utils;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Set;

/**
 * Spawns AreaEffectCloud entities when gases escape from chemistry containers.
 * The cloud type and effects depend on the chemical properties of the gas.
 *
 * Gas hazard categories:
 *   - TOXIC: Cl₂, NO₂, SO₂, HCN, H₂S, CO → poison / wither effects
 *   - CORROSIVE: HCl gas, HF gas → instant damage
 *   - ASPHYXIANT: CO₂, N₂, noble gases → mining fatigue / slowness
 *   - IRRITANT: NH₃, SO₃ → nausea / blindness
 *   - FLAMMABLE: H₂, CH₄, C₂H₆ → no direct effect cloud, but marked flammable
 *   - INERT: O₂, He, Ne, Ar → harmless, small visual cloud only
 */
public class GasCloudHelper {

    // ── Gas classification sets ────────────────────────────────────────

    private static final Set<String> TOXIC_GASES = Set.of(
            "Cl2", "NO2", "SO2", "HCN", "H2S", "CO"
    );

    private static final Set<String> CORROSIVE_GASES = Set.of(
            "HCl", "HF", "HBr"
    );

    private static final Set<String> IRRITANT_GASES = Set.of(
            "NH3", "SO3", "NO"
    );

    private static final Set<String> ASPHYXIANT_GASES = Set.of(
            "CO2", "N2", "He", "Ne", "Ar", "Kr", "Xe"
    );

    private static final Set<String> FLAMMABLE_GASES = Set.of(
            "H2", "CH4", "C2H6", "C2H4", "C2H2", "C3H8"
    );

    private static final Set<String> INERT_GASES = Set.of(
            "O2"
    );

    // ── Public API ─────────────────────────────────────────────────────

    /**
     * Spawn gas cloud entities for escaped gases above a container position.
     *
     * @param world  the server world
     * @param pos    the block position of the container
     * @param gases  map of gas formula → moles that escaped
     */
    public static void spawnGasClouds(World world, BlockPos pos, Map<String, Double> gases) {
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) return;
        if (gases.isEmpty()) return;

        // Determine the worst gas category and total moles
        GasCategory worst = GasCategory.INERT;
        double totalMoles = 0;
        boolean hasFlammable = false;

        for (var entry : gases.entrySet()) {
            String formula = entry.getKey();
            double moles = entry.getValue();
            totalMoles += moles;

            GasCategory cat = classifyGas(formula);
            if (cat.ordinal() > worst.ordinal()) {
                worst = cat;
            }
            if (FLAMMABLE_GASES.contains(formula)) {
                hasFlammable = true;
            }
        }

        // Scale cloud radius and duration by moles released
        float radius = Math.min(4.0f, 1.0f + (float) (totalMoles * 0.5));
        int durationTicks = Math.min(600, 60 + (int) (totalMoles * 40));

        // Spawn the appropriate cloud
        spawnCloud(serverWorld, pos, worst, radius, durationTicks, hasFlammable);
    }

    // ── Gas classification ─────────────────────────────────────────────

    private enum GasCategory {
        INERT,        // O₂, harmless
        FLAMMABLE,    // H₂, CH₄ — visual only (no lingering effect)
        ASPHYXIANT,   // CO₂, N₂ — mining fatigue
        IRRITANT,     // NH₃ — nausea
        CORROSIVE,    // HCl — damage
        TOXIC         // Cl₂, HCN — poison/wither
    }

    private static GasCategory classifyGas(String formula) {
        if (TOXIC_GASES.contains(formula)) return GasCategory.TOXIC;
        if (CORROSIVE_GASES.contains(formula)) return GasCategory.CORROSIVE;
        if (IRRITANT_GASES.contains(formula)) return GasCategory.IRRITANT;
        if (ASPHYXIANT_GASES.contains(formula)) return GasCategory.ASPHYXIANT;
        if (FLAMMABLE_GASES.contains(formula)) return GasCategory.FLAMMABLE;
        return GasCategory.INERT;
    }

    // ── Cloud spawning ─────────────────────────────────────────────────

    private static void spawnCloud(ServerWorld world, BlockPos pos,
                                   GasCategory category, float radius,
                                   int durationTicks, boolean flammable) {

        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(
                world, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5);

        cloud.setRadius(radius);
        cloud.setRadiusGrowth(-radius / durationTicks); // shrink over time
        cloud.setDuration(durationTicks);
        cloud.setWaitTime(10); // short delay before effects start

        switch (category) {
            case TOXIC -> {
                cloud.setParticleType(ParticleTypes.ENTITY_EFFECT);
                // Poison + wither for severe toxic gases
                cloud.addEffect(new StatusEffectInstance(
                        StatusEffects.POISON, 100, 1));
                cloud.addEffect(new StatusEffectInstance(
                        StatusEffects.NAUSEA, 80, 0));
                if (radius > 2.0f) {
                    cloud.addEffect(new StatusEffectInstance(
                            StatusEffects.WITHER, 60, 0));
                }
            }
            case CORROSIVE -> {
                cloud.setParticleType(ParticleTypes.ENTITY_EFFECT);
                cloud.addEffect(new StatusEffectInstance(
                        StatusEffects.INSTANT_DAMAGE, 1, 0));
                cloud.addEffect(new StatusEffectInstance(
                        StatusEffects.NAUSEA, 60, 0));
            }
            case IRRITANT -> {
                cloud.setParticleType(ParticleTypes.ENTITY_EFFECT);
                cloud.addEffect(new StatusEffectInstance(
                        StatusEffects.NAUSEA, 120, 1));
                cloud.addEffect(new StatusEffectInstance(
                        StatusEffects.BLINDNESS, 60, 0));
            }
            case ASPHYXIANT -> {
                cloud.setParticleType(ParticleTypes.CAMPFIRE_COSY_SMOKE);
                cloud.addEffect(new StatusEffectInstance(
                        StatusEffects.MINING_FATIGUE, 80, 1));
                cloud.addEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS, 60, 0));
            }
            case FLAMMABLE -> {
                // Flammable gas cloud: visual only, no direct effect
                // Future: could ignite if fire/lava is nearby
                cloud.setParticleType(ParticleTypes.SMOKE);
                cloud.setDuration(Math.min(durationTicks, 200));
            }
            case INERT -> {
                // Harmless cloud, just visual
                cloud.setParticleType(ParticleTypes.CLOUD);
                cloud.setDuration(Math.min(durationTicks, 100));
            }
        }

        world.spawnEntity(cloud);
    }
}
