package org.keke.chemistry.reaction;

import org.keke.chemistry.utils.IonicCompound;

import java.util.*;

/**
 * Predicts single-reactant decomposition reactions based on anion templates.
 * Templates cover the most common inorganic thermal decompositions.
 *
 * Supported decomposition patterns:
 *   - Carbonates  → Metal oxide + CO2
 *   - Bicarbonates → Metal oxide + CO2 + H2O
 *   - Hydroxides  → Metal oxide + H2O   (transition/heavy metals)
 *   - Nitrates    → Metal oxide + NO2 + O2  (heavy metals)
 *                   or nitrite + O2   (alkali/alkaline earth)
 *   - Chlorates   → Metal chloride + O2
 *   - Hydrogen peroxide → H2O + O2
 *   - Metal oxides with unstable higher states (e.g. HgO → Hg + O2)
 *
 * All decompositions require heat ({@link Reaction.ReactionConditions#heated()}).
 */
public class DecompositionPredictor {

    /** Alkali metals: decompose nitrate to nitrite + O2 instead of oxide. */
    private static final Set<String> ALKALI_METALS = Set.of(
            "Li", "Na", "K", "Rb", "Cs", "Fr"
    );

    /** Alkaline earth metals: also decompose nitrate to nitrite pathway. */
    private static final Set<String> ALKALINE_EARTH_METALS = Set.of(
            "Be", "Mg", "Ca", "Sr", "Ba", "Ra"
    );

    /** Metals whose oxides decompose upon heating. */
    private static final Set<String> UNSTABLE_OXIDE_METALS = Set.of(
            "Hg", "Au", "Pt", "Ag"
    );

    /** Metals whose hydroxides decompose upon heating (transition/heavy). */
    private static final Set<String> HYDROXIDE_DECOMPOSE_METALS = Set.of(
            "Cu", "Fe", "Al", "Zn", "Mn", "Co", "Ni", "Cr",
            "Pb", "Sn", "Cd", "Hg", "Bi", "Ti", "Mg"
    );

    /**
     * Try to predict a decomposition reaction for a single formula.
     * Returns null if no decomposition pattern matches.
     */
    public static Reaction predict(String formula) {
        if (formula == null || formula.isEmpty()) return null;

        // Special case: hydrogen peroxide
        if ("H2O2".equals(formula)) {
            return buildReaction(
                    List.of(new ReactionComponent("H2O2", 2)),
                    List.of(new ReactionComponent("H2O", 2), new ReactionComponent("O2", 1)),
                    EnumSet.of(ReactionEffect.GAS_EVOLUTION)
            );
        }

        // Decompose the formula into ionic components
        IonicCompound ionic = IonicCompound.decompose(formula);
        if (ionic == null) return null;

        String cation = ionic.getCation();
        String anion = ionic.getAnion();
        int cationCount = ionic.getCationCount();
        int anionCount = ionic.getAnionCount();
        int cationCharge = ionic.getCationCharge();

        // Route to the appropriate template
        return switch (anion) {
            case "CO3"   -> decomposeCarbonate(cation, cationCount, anionCount, cationCharge);
            case "HCO3"  -> decomposeBicarbonate(cation, cationCount, anionCount, cationCharge);
            case "OH"    -> decomposeHydroxide(cation, cationCount, anionCount, cationCharge);
            case "NO3"   -> decomposeNitrate(cation, cationCount, anionCount, cationCharge);
            case "ClO3"  -> decomposeChlorate(cation, cationCount, anionCount, cationCharge);
            case "ClO4"  -> decomposPerchlorate(cation, cationCount, anionCount, cationCharge);
            case "SO3"   -> decomposeSulfite(cation, cationCount, anionCount, cationCharge);
            case "N3"    -> decomposeAzide(cation, cationCount, anionCount, cationCharge);
            default -> {
                // Check for metal oxide decomposition (e.g. HgO)
                if ("O".equals(anion) && UNSTABLE_OXIDE_METALS.contains(cation)) {
                    yield decomposeMetalOxide(cation, cationCount, anionCount);
                }
                yield null;
            }
        };
    }

    // ── Decomposition templates ────────────────────────────────────────

    /**
     * MCO3 → MO + CO2
     * Balanced: M_a(CO3)_b → M_a(O)_b + b·CO2
     */
    private static Reaction decomposeCarbonate(String cation, int cationCount,
                                                int anionCount, int cationCharge) {
        String oxide = buildOxide(cation, cationCount, anionCount, cationCharge);
        return buildReaction(
                List.of(new ReactionComponent(buildFormula(cation, cationCount, "CO3", anionCount), 1)),
                List.of(new ReactionComponent(oxide, 1),
                        new ReactionComponent("CO2", anionCount)),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.ENDOTHERMIC)
        );
    }

    /**
     * M(HCO3)_n → M_a(CO3)_b + CO2 + H2O (simplified to oxide route for most)
     * Simplified: 2MHCO3 → MCO3 + CO2 + H2O  (when cation is monovalent)
     */
    private static Reaction decomposeBicarbonate(String cation, int cationCount,
                                                  int anionCount, int cationCharge) {
        // Simplified: produces the oxide + CO2 + H2O
        String oxide = buildOxide(cation, cationCount, anionCount, cationCharge);
        return buildReaction(
                List.of(new ReactionComponent(buildFormula(cation, cationCount, "HCO3", anionCount), 1)),
                List.of(new ReactionComponent(oxide, 1),
                        new ReactionComponent("CO2", anionCount),
                        new ReactionComponent("H2O", anionCount)),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.ENDOTHERMIC)
        );
    }

    /**
     * M(OH)_n → MO_(n/2) + (n/2)H2O
     * Only for metals whose hydroxides are thermally unstable.
     */
    private static Reaction decomposeHydroxide(String cation, int cationCount,
                                                int anionCount, int cationCharge) {
        if (!HYDROXIDE_DECOMPOSE_METALS.contains(cation)) return null;

        String oxide = buildOxide(cation, cationCount, anionCount, cationCharge);

        // Need to produce integer stoichiometry
        // M_a(OH)_b → M_{2a}O_b + b·H2O  (scaled by 2 if b is odd)
        int reactCoeff = 1;
        int waterCoeff = anionCount;

        return buildReaction(
                List.of(new ReactionComponent(buildFormula(cation, cationCount, "OH", anionCount), reactCoeff)),
                List.of(new ReactionComponent(oxide, reactCoeff),
                        new ReactionComponent("H2O", waterCoeff)),
                EnumSet.of(ReactionEffect.ENDOTHERMIC)
        );
    }

    /**
     * Nitrate decomposition depends on metal activity:
     *   Alkali/alkaline earth: 2M(NO3)_n → 2M(NO2)_n + n·O2
     *   Heavy metals: 2M(NO3)_n → M_2O_n + 2n·NO2 + (n/2)·O2
     */
    private static Reaction decomposeNitrate(String cation, int cationCount,
                                              int anionCount, int cationCharge) {
        String formula = buildFormula(cation, cationCount, "NO3", anionCount);

        if (ALKALI_METALS.contains(cation) || ALKALINE_EARTH_METALS.contains(cation)) {
            // → nitrite + O2
            String nitrite = buildFormula(cation, cationCount, "NO2", anionCount);
            return buildReaction(
                    List.of(new ReactionComponent(formula, 2)),
                    List.of(new ReactionComponent(nitrite, 2),
                            new ReactionComponent("O2", anionCount)),
                    EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.ENDOTHERMIC)
            );
        } else {
            // Heavy metal → oxide + NO2 + O2
            // 2M(NO3)_n → M_2O_n + 2n·NO2 + (n/2)·O2
            // If n is odd, scale by 2 for integer stoichiometry
            String oxide = buildOxide(cation, cationCount, anionCount, cationCharge);

            int formulaCoeff;
            int oxideCoeff;
            int no2Coeff;
            int o2Coeff;

            if (anionCount % 2 == 0) {
                formulaCoeff = 2;
                oxideCoeff = 2;
                no2Coeff = 2 * anionCount;
                o2Coeff = anionCount / 2;
            } else {
                // Scale ×2: 4M(NO3)_n → 2 M_2O_n + 4n·NO2 + n·O2
                formulaCoeff = 4;
                oxideCoeff = 4;
                no2Coeff = 4 * anionCount;
                o2Coeff = anionCount;
            }

            return buildReaction(
                    List.of(new ReactionComponent(formula, formulaCoeff)),
                    List.of(new ReactionComponent(oxide, oxideCoeff),
                            new ReactionComponent("NO2", no2Coeff),
                            new ReactionComponent("O2", o2Coeff)),
                    EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.COLOR_CHANGE,
                            ReactionEffect.ENDOTHERMIC)
            );
        }
    }

    /**
     * 2M(ClO3)_n → 2MCl_n + 3n·O2
     */
    private static Reaction decomposeChlorate(String cation, int cationCount,
                                               int anionCount, int cationCharge) {
        String formula = buildFormula(cation, cationCount, "ClO3", anionCount);
        String chloride = buildFormula(cation, cationCount, "Cl", anionCount);
        return buildReaction(
                List.of(new ReactionComponent(formula, 2)),
                List.of(new ReactionComponent(chloride, 2),
                        new ReactionComponent("O2", 3 * anionCount)),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.EXOTHERMIC)
        );
    }

    /**
     * M(ClO4)_n → MCl_n + 2n·O2
     */
    private static Reaction decomposPerchlorate(String cation, int cationCount,
                                                 int anionCount, int cationCharge) {
        String formula = buildFormula(cation, cationCount, "ClO4", anionCount);
        String chloride = buildFormula(cation, cationCount, "Cl", anionCount);
        return buildReaction(
                List.of(new ReactionComponent(formula, 1)),
                List.of(new ReactionComponent(chloride, 1),
                        new ReactionComponent("O2", 2 * anionCount)),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.EXOTHERMIC)
        );
    }

    /**
     * M_a(SO3)_b → M_aO_b + b·SO2
     */
    private static Reaction decomposeSulfite(String cation, int cationCount,
                                              int anionCount, int cationCharge) {
        String formula = buildFormula(cation, cationCount, "SO3", anionCount);
        String oxide = buildOxide(cation, cationCount, anionCount, cationCharge);
        return buildReaction(
                List.of(new ReactionComponent(formula, 1)),
                List.of(new ReactionComponent(oxide, 1),
                        new ReactionComponent("SO2", anionCount)),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.ENDOTHERMIC)
        );
    }

    /**
     * 2MO → 2M + O2  (unstable oxide metals: Hg, Au, etc.)
     */
    private static Reaction decomposeMetalOxide(String cation, int cationCount, int anionCount) {
        String formula = buildFormula(cation, cationCount, "O", anionCount);
        return buildReaction(
                List.of(new ReactionComponent(formula, 2)),
                List.of(new ReactionComponent(cation, 2 * cationCount),
                        new ReactionComponent("O2", anionCount)),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.ENDOTHERMIC)
        );
    }

    /**
     * M_a(N3)_b → a·M + (3b/2)·N2
     * Azide decomposition: releases the metal and nitrogen gas.
     * e.g. Ba(N3)2 → Ba + 3N2
     *      NaN3 → Na + 3/2 N2 → scaled: 2NaN3 → 2Na + 3N2
     */
    private static Reaction decomposeAzide(String cation, int cationCount,
                                            int anionCount, int cationCharge) {
        String formula = buildFormula(cation, cationCount, "N3", anionCount);

        // Each N3 yields 3/2 N2.  b azide groups → 3b/2 N2 per formula unit.
        // To get integer coefficients: multiply formula by 2 if 3*anionCount is odd.
        int n2Numerator = 3 * anionCount;  // numerator when denominator is 2
        int formulaCoeff;
        int n2Coeff;
        int metalCoeff;

        if (n2Numerator % 2 == 0) {
            formulaCoeff = 1;
            n2Coeff = n2Numerator / 2;
            metalCoeff = cationCount;
        } else {
            formulaCoeff = 2;
            n2Coeff = n2Numerator;  // 2 × (3b/2) = 3b
            metalCoeff = 2 * cationCount;
        }

        return buildReaction(
                List.of(new ReactionComponent(formula, formulaCoeff)),
                List.of(new ReactionComponent(cation, metalCoeff),
                        new ReactionComponent("N2", n2Coeff)),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.EXOTHERMIC)
        );
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /**
     * Build a formula string from cation + anion with counts.
     * buildFormula("Ca", 1, "OH", 2) → "Ca(OH)2"
     * buildFormula("Na", 1, "Cl", 1) → "NaCl"
     * buildFormula("Na", 2, "SO4", 1) → "Na2SO4"
     */
    private static String buildFormula(String cation, int cationCount, String anion, int anionCount) {
        StringBuilder sb = new StringBuilder();
        sb.append(cation);
        if (cationCount > 1) sb.append(cationCount);

        boolean polyatomic = anion.length() > 2 || (anion.length() == 2 && Character.isUpperCase(anion.charAt(1)));
        if (anionCount > 1 && polyatomic) {
            sb.append("(").append(anion).append(")").append(anionCount);
        } else {
            sb.append(anion);
            if (anionCount > 1) sb.append(anionCount);
        }
        return sb.toString();
    }

    /**
     * Build the metal oxide formula from a cation and its charge.
     * Uses the charge to determine the oxide formula:
     *   charge 1 → M2O, charge 2 → MO, charge 3 → M2O3
     */
    private static String buildOxide(String cation, int cationCount, int anionCount, int cationCharge) {
        // Oxide has O²⁻, so for cation with charge +n:
        // balance: n = 2(oxygens per cation), so cation:oxygen = 2:n
        // simplify by GCD
        int metalCount = 2;
        int oxygenCount = cationCharge;
        int gcd = gcd(metalCount, oxygenCount);
        metalCount /= gcd;
        oxygenCount /= gcd;

        StringBuilder sb = new StringBuilder();
        sb.append(cation);
        if (metalCount > 1) sb.append(metalCount);
        sb.append("O");
        if (oxygenCount > 1) sb.append(oxygenCount);
        return sb.toString();
    }

    private static int gcd(int a, int b) {
        while (b != 0) { int t = b; b = a % b; a = t; }
        return a;
    }

    private static Reaction buildReaction(List<ReactionComponent> reactants,
                                           List<ReactionComponent> products,
                                           Set<ReactionEffect> effects) {
        return new Reaction(reactants, products, ReactionType.DECOMPOSITION,
                Reaction.ReactionConditions.heated(), effects);
    }
}
