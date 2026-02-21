package org.keke.chemistry.utils;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.*;

/**
 * Determines the physiological effects of ingesting (drinking) chemical compounds.
 * Effects are based on real toxicology:
 *
 * - Strong acids/bases cause chemical burns (instant damage)
 * - Heavy metal compounds cause poisoning (poison effect + nausea)
 * - Oxidizing agents cause tissue damage
 * - Flammable organics cause intoxication (nausea + slowness)
 * - Water and inert salts are relatively harmless
 *
 * Damage scales with concentration (moles consumed).
 */
public class DrinkingEffects {

    /**
     * Chemical hazard classification.
     */
    public enum HazardType {
        SAFE,           // Water, dilute NaCl, etc.
        MILD_IRRITANT,  // Weak acids, mildly basic
        CORROSIVE_ACID, // Strong acids: HCl, H2SO4, HNO3
        CORROSIVE_BASE, // Strong bases: NaOH, KOH
        TOXIC_METAL,    // Heavy metal compounds: Cu, Pb, Hg, Ba, Cd, As, Cr(VI)
        OXIDIZER,       // KMnO4, H2O2, K2Cr2O7
        FLAMMABLE,      // Ethanol, methanol, organic solvents
        ASPHYXIANT_GAS, // CO, CO2, N2 (shouldn't be drinkable but handle gracefully)
        TOXIC_GAS       // Cl2, NO2, SO2, HCN
    }

    // Strong acids by formula
    private static final Set<String> STRONG_ACIDS = Set.of(
            "HCl", "HBr", "HI", "HNO3", "H2SO4", "HClO3", "HClO4"
    );

    // Strong bases by formula
    private static final Set<String> STRONG_BASES = Set.of(
            "NaOH", "KOH", "LiOH", "Ba(OH)2", "Ca(OH)2", "Sr(OH)2"
    );

    // Weak acids
    private static final Set<String> WEAK_ACIDS = Set.of(
            "H3PO4", "H2CO3", "CH3COOH", "HF", "H2S", "HCN", "HCOOH"
    );

    // Toxic heavy metal symbols
    private static final Set<String> HEAVY_METALS = Set.of(
            "Pb", "Hg", "Cd", "As", "Tl", "Ba"
    );

    // Transition metals that are toxic in soluble form
    private static final Set<String> TOXIC_TRANSITION_METALS = Set.of(
            "Cu", "Cr", "Co", "Ni", "Ag"
    );

    // Known oxidizers
    private static final Set<String> OXIDIZERS = Set.of(
            "KMnO4", "H2O2", "K2CrO4", "K2Cr2O7", "NaClO", "KClO3"
    );

    // Flammable organics
    private static final Set<String> FLAMMABLE_ORGANICS = Set.of(
            "CH3OH", "C2H5OH", "CH4", "C2H6", "C3H8", "C2H4", "C2H2"
    );

    // Toxic gases
    private static final Set<String> TOXIC_GASES = Set.of(
            "Cl2", "NO2", "SO2", "HCN", "CO", "H2S", "NH3"
    );

    // Safe compounds
    private static final Set<String> SAFE_COMPOUNDS = Set.of(
            "H2O", "NaCl", "KCl", "C6H12O6", "C12H22O11", "NaHCO3"
    );

    /**
     * Classify the hazard type of a chemical formula.
     */
    public static HazardType classifyHazard(String formula) {
        if (formula == null || formula.isEmpty()) return HazardType.SAFE;

        // Check exact matches first
        if (SAFE_COMPOUNDS.contains(formula)) return HazardType.SAFE;
        if (STRONG_ACIDS.contains(formula)) return HazardType.CORROSIVE_ACID;
        if (STRONG_BASES.contains(formula)) return HazardType.CORROSIVE_BASE;
        if (OXIDIZERS.contains(formula)) return HazardType.OXIDIZER;
        if (FLAMMABLE_ORGANICS.contains(formula)) return HazardType.FLAMMABLE;
        if (TOXIC_GASES.contains(formula)) return HazardType.TOXIC_GAS;
        if (WEAK_ACIDS.contains(formula)) return HazardType.MILD_IRRITANT;

        // Dynamic classification by parsing formula
        Map<Element, Integer> composition = FormulaParser.parse(formula);

        // Check for heavy metals in composition
        for (Element elem : composition.keySet()) {
            if (HEAVY_METALS.contains(elem.getSymbol())) {
                return HazardType.TOXIC_METAL;
            }
        }

        // Check for toxic transition metals in soluble compounds
        for (Element elem : composition.keySet()) {
            if (TOXIC_TRANSITION_METALS.contains(elem.getSymbol())) {
                return HazardType.TOXIC_METAL;
            }
        }

        // Detect acids dynamically: starts with H, followed by non-metal anion
        if (formula.startsWith("H") && formula.length() > 1) {
            char second = formula.charAt(1);
            if (Character.isUpperCase(second) && !formula.equals("He")) {
                // Likely an acid if second char is uppercase (new element)
                // and it's not a hydrogen compound like H2
                if (!formula.matches("H2?$")) {
                    return HazardType.MILD_IRRITANT;
                }
            }
            if (Character.isDigit(second)) {
                // H2..., H3... check what follows
                String rest = formula.replaceFirst("H\\d*", "");
                if (!rest.isEmpty() && !rest.equals("O") && !rest.equals("O2")) {
                    return HazardType.MILD_IRRITANT;
                }
            }
        }

        // Detect bases: contains OH group
        if (formula.contains("OH") && !formula.equals("H2O") && !formula.startsWith("H")) {
            // Check if it's a metal hydroxide
            for (Element elem : composition.keySet()) {
                if (elem.getAtomicNumber() <= 20 || elem.getAtomicNumber() > 30) {
                    // Group 1 or 2 metals with OH → base
                    String sym = elem.getSymbol();
                    if (Set.of("Na", "K", "Li", "Ca", "Ba", "Sr").contains(sym)) {
                        return HazardType.CORROSIVE_BASE;
                    }
                }
            }
            return HazardType.MILD_IRRITANT;
        }

        return HazardType.SAFE;
    }

    /**
     * Apply drinking effects to a living entity based on what they consumed.
     *
     * @param entity   the entity that drank the chemical
     * @param contents map of formula → moles consumed
     */
    public static void applyEffects(LivingEntity entity, Map<String, Double> contents) {
        if (contents.isEmpty()) return;

        // Find the most hazardous component
        HazardType worstHazard = HazardType.SAFE;
        double totalMoles = 0;
        double hazardousMoles = 0;

        for (var entry : contents.entrySet()) {
            String formula = entry.getKey();
            double moles = entry.getValue();
            totalMoles += moles;

            HazardType hazard = classifyHazard(formula);
            if (hazard.ordinal() > worstHazard.ordinal()) {
                worstHazard = hazard;
            }
            if (hazard != HazardType.SAFE) {
                hazardousMoles += moles;
            }
        }

        // Scale intensity by concentration (moles consumed)
        // 0.1 mol = mild, 0.5 mol = moderate, 1.0+ mol = severe
        int intensityLevel = Math.min(3, (int) Math.ceil(hazardousMoles * 3.0));
        int durationTicks;

        switch (worstHazard) {
            case SAFE:
                // Safe compounds: extinguish fire if water is present, no other effects
                if (contents.containsKey("H2O")) {
                    entity.extinguish();
                }
                break;

            case MILD_IRRITANT:
                // Weak acids: mild nausea
                durationTicks = 60 + intensityLevel * 60; // 3-15 sec
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NAUSEA, durationTicks, 0));
                if (intensityLevel >= 2) {
                    entity.damage(ModDamageSources.labAccident(entity.getWorld()), 1.0f + intensityLevel);
                }
                break;

            case CORROSIVE_ACID:
                // Strong acids: instant damage + nausea
                durationTicks = 100 + intensityLevel * 100; // 5-25 sec
                float acidDamage = 2.0f + intensityLevel * 3.0f; // 5-11 damage (2.5-5.5 hearts)
                entity.damage(ModDamageSources.labAccident(entity.getWorld()), acidDamage);
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NAUSEA, durationTicks, intensityLevel - 1));
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.POISON, durationTicks / 2, intensityLevel - 1));
                break;

            case CORROSIVE_BASE:
                // Strong bases: instant damage + blindness (saponification of tissue)
                durationTicks = 100 + intensityLevel * 80;
                float baseDamage = 2.0f + intensityLevel * 2.5f;
                entity.damage(ModDamageSources.labAccident(entity.getWorld()), baseDamage);
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NAUSEA, durationTicks, intensityLevel - 1));
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.BLINDNESS, durationTicks / 3, 0));
                break;

            case TOXIC_METAL:
                // Heavy metal poisoning: slow poison + nausea + mining fatigue
                durationTicks = 200 + intensityLevel * 200; // 10-50 sec
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.POISON, durationTicks, intensityLevel - 1));
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NAUSEA, durationTicks, intensityLevel - 1));
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.MINING_FATIGUE, durationTicks, intensityLevel - 1));
                if (intensityLevel >= 2) {
                    entity.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.WEAKNESS, durationTicks, intensityLevel - 2));
                }
                break;

            case OXIDIZER:
                // Oxidizers: instant damage + weakness
                durationTicks = 100 + intensityLevel * 100;
                float oxDamage = 3.0f + intensityLevel * 3.0f;
                entity.damage(ModDamageSources.labAccident(entity.getWorld()), oxDamage);
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WEAKNESS, durationTicks, intensityLevel - 1));
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NAUSEA, durationTicks / 2, 0));
                break;

            case FLAMMABLE:
                // Intoxication: nausea + slowness + hunger
                durationTicks = 200 + intensityLevel * 200;
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NAUSEA, durationTicks, intensityLevel - 1));
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS, durationTicks, 0));
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.HUNGER, durationTicks / 2, intensityLevel - 1));
                // Methanol specifically is much more toxic
                if (contents.containsKey("CH3OH")) {
                    entity.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.BLINDNESS, durationTicks, 0));
                    entity.damage(ModDamageSources.labAccident(entity.getWorld()), 4.0f * intensityLevel);
                }
                break;

            case ASPHYXIANT_GAS:
                // Suffocation effects
                durationTicks = 60 + intensityLevel * 60;
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.MINING_FATIGUE, durationTicks, intensityLevel));
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS, durationTicks, intensityLevel));
                entity.damage(ModDamageSources.labAccident(entity.getWorld()), intensityLevel * 2.0f);
                break;

            case TOXIC_GAS:
                // Severe respiratory damage
                durationTicks = 100 + intensityLevel * 150;
                entity.damage(ModDamageSources.labAccident(entity.getWorld()), 3.0f + intensityLevel * 4.0f);
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.POISON, durationTicks, intensityLevel));
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.NAUSEA, durationTicks, intensityLevel - 1));
                entity.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.WITHER, durationTicks / 4, intensityLevel - 1));
                break;
        }
    }

    /**
     * Apply temperature-based effects when drinking hot or cold liquids.
     * Called in addition to chemical hazard effects.
     *
     * @param entity       the entity that drank the liquid
     * @param temperatureK the temperature of the liquid in Kelvin
     * @param totalMoles   total moles consumed (affects severity)
     */
    public static void applyTemperatureEffects(LivingEntity entity, double temperatureK, double totalMoles) {
        if (temperatureK > 343.15) {
            // > 70 °C — scalding hot liquid
            // Damage scales: 1 at 70°C, up to 8 at 500°C
            float damage = Math.min(8.0f, 1.0f + (float) ((temperatureK - 343.15) / 50.0));
            damage *= (float) Math.min(2.0, totalMoles);
            entity.damage(entity.getDamageSources().hotFloor(), damage);

            int durationTicks = 40 + (int) ((temperatureK - 343.15) / 2.0);
            entity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NAUSEA, durationTicks, 0));

            if (temperatureK > 373.15) {
                // Boiling: also set on fire briefly
                entity.setOnFireFor(1 + (int) ((temperatureK - 373.15) / 100.0));
            }
        } else if (temperatureK < 273.15) {
            // < 0 °C — freezing liquid
            // Damage scales: 1 at 0°C, up to 6 at −200°C
            float damage = Math.min(6.0f, 1.0f + (float) ((273.15 - temperatureK) / 60.0));
            damage *= (float) Math.min(2.0, totalMoles);
            entity.damage(entity.getDamageSources().freeze(), damage);

            int durationTicks = 60 + (int) ((273.15 - temperatureK) / 2.0);
            int amplifier = Math.min(3, (int) ((273.15 - temperatureK) / 80.0));
            entity.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOWNESS, durationTicks, amplifier));
            entity.setFrozenTicks(Math.max(entity.getFrozenTicks(),
                    60 + (int) ((273.15 - temperatureK))));
        }
        // 0–70 °C: comfortable range, no thermal effects
    }
}
