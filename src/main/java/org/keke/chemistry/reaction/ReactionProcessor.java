package org.keke.chemistry.reaction;

import org.keke.chemistry.utils.ThermodynamicData;

import java.util.*;

/**
 * Handles executing a chemical reaction: validates stoichiometry, computes product moles,
 * handles limiting reagent calculation, and calculates thermodynamic data.
 *
 * Stoichiometry:
 *   n_product = min(n_i / coeff_i) × coeff_product
 *
 * Thermodynamics (Hess's law):
 *   ΔH°_rxn = Σ(ν_product × ΔHf°_product) - Σ(ν_reactant × ΔHf°_reactant)
 *   q = ξ × ΔH°_rxn  (total heat in kJ)
 *   ΔT = -q / (m × Cp)   (temperature change in K)
 */
public class ReactionProcessor {

    /** Default solution mass in grams (approximation for a 250 mL beaker) */
    private static final double DEFAULT_SOLUTION_MASS_G = 250.0;

    /**
     * Process a reaction between given reactants with specified moles.
     *
     * @param reaction     the reaction to process
     * @param inputMoles   map of formula → available moles
     * @param temperature  current temperature in °C
     * @return ReactionResult with products, leftovers, effects, and thermodynamic data
     */
    public static ReactionResult process(Reaction reaction, Map<String, Double> inputMoles, double temperature) {
        return process(reaction, inputMoles, temperature, DEFAULT_SOLUTION_MASS_G);
    }

    /**
     * Process a reaction with specified solution mass.
     *
     * @param reaction       the reaction to process
     * @param inputMoles     map of formula → available moles
     * @param temperature    current temperature in °C
     * @param solutionMassG  mass of solution in grams
     * @return ReactionResult with products, leftovers, effects, and thermodynamic data
     */
    public static ReactionResult process(Reaction reaction, Map<String, Double> inputMoles,
                                          double temperature, double solutionMassG) {
        // Check conditions
        Reaction.ReactionConditions conditions = reaction.getConditions();
        if (conditions.requiresHeat() && temperature < conditions.getMinTemp()) {
            return new ReactionResult(Map.of(), inputMoles,
                    EnumSet.noneOf(ReactionEffect.class), false);
        }
        if (temperature < conditions.getMinTemp() || temperature > conditions.getMaxTemp()) {
            return new ReactionResult(Map.of(), inputMoles,
                    EnumSet.noneOf(ReactionEffect.class), false);
        }

        // Find limiting reagent: min(n_i / coeff_i)
        double limitingRatio = Double.MAX_VALUE;
        for (ReactionComponent reactant : reaction.getReactants()) {
            double available = inputMoles.getOrDefault(reactant.formula(), 0.0);
            double ratio = available / reactant.coefficient();
            limitingRatio = Math.min(limitingRatio, ratio);
        }

        if (limitingRatio <= 0) {
            return new ReactionResult(Map.of(), inputMoles,
                    EnumSet.noneOf(ReactionEffect.class), false);
        }

        // Calculate products
        Map<String, Double> products = new LinkedHashMap<>();
        for (ReactionComponent product : reaction.getProducts()) {
            double productMoles = limitingRatio * product.coefficient();
            products.merge(product.formula(), productMoles, Double::sum);
        }

        // Calculate leftovers
        Map<String, Double> leftovers = new LinkedHashMap<>();
        for (ReactionComponent reactant : reaction.getReactants()) {
            double available = inputMoles.getOrDefault(reactant.formula(), 0.0);
            double consumed = limitingRatio * reactant.coefficient();
            double remaining = available - consumed;
            if (remaining > 0.001) {
                leftovers.put(reactant.formula(), remaining);
            }
        }

        // Calculate reaction enthalpy using Hess's law
        double reactionEnthalpyPerMol = calculateReactionEnthalpy(reaction);
        double totalEnthalpyKJ = limitingRatio * reactionEnthalpyPerMol;
        double temperatureChange = 0.0;

        if (!Double.isNaN(reactionEnthalpyPerMol)) {
            temperatureChange = ThermodynamicData.calculateTemperatureChange(
                    reactionEnthalpyPerMol, limitingRatio, solutionMassG);
        }

        return new ReactionResult(products, leftovers, reaction.getEffects(), true,
                totalEnthalpyKJ, temperatureChange);
    }

    /**
     * Calculate the standard enthalpy of reaction using Hess's law.
     * ΔH°_rxn = Σ(ν_product × ΔHf°_product) - Σ(ν_reactant × ΔHf°_reactant)
     *
     * @param reaction the reaction
     * @return ΔH° in kJ/mol of reaction as written, or NaN if data is missing
     */
    public static double calculateReactionEnthalpy(Reaction reaction) {
        List<Map.Entry<String, Integer>> reactants = new ArrayList<>();
        for (ReactionComponent rc : reaction.getReactants()) {
            reactants.add(Map.entry(rc.formula(), rc.coefficient()));
        }

        List<Map.Entry<String, Integer>> products = new ArrayList<>();
        for (ReactionComponent rc : reaction.getProducts()) {
            products.add(Map.entry(rc.formula(), rc.coefficient()));
        }

        double result = ThermodynamicData.calculateReactionEnthalpy(reactants, products);

        // If exact data missing, try using estimates
        if (Double.isNaN(result)) {
            double productSum = 0.0;
            for (var entry : products) {
                productSum += entry.getValue() * ThermodynamicData.estimateFormationEnthalpy(entry.getKey());
            }
            double reactantSum = 0.0;
            for (var entry : reactants) {
                reactantSum += entry.getValue() * ThermodynamicData.estimateFormationEnthalpy(entry.getKey());
            }
            result = productSum - reactantSum;
        }

        return result;
    }
}
