package org.keke.chemistry.reaction;

import java.util.*;

/**
 * Static registry of chemical reactions, loaded at mod initialization.
 * Provides lookup by reactant formulas (order-independent).
 */
public class ReactionRegistry {
    private static final List<Reaction> REACTIONS = new ArrayList<>();

    static {
        loadReactions();
    }

    private static void loadReactions() {
        // === Acid-Base Neutralization ===
        // HCl + NaOH → NaCl + H2O
        addReaction(
                List.of(new ReactionComponent("HCl", 1), new ReactionComponent("NaOH", 1)),
                List.of(new ReactionComponent("NaCl", 1), new ReactionComponent("H2O", 1)),
                ReactionType.ACID_BASE, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.EXOTHERMIC)
        );
        // H2SO4 + 2NaOH → Na2SO4 + 2H2O
        addReaction(
                List.of(new ReactionComponent("H2SO4", 1), new ReactionComponent("NaOH", 2)),
                List.of(new ReactionComponent("Na2SO4", 1), new ReactionComponent("H2O", 2)),
                ReactionType.ACID_BASE, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.EXOTHERMIC)
        );
        // HNO3 + NaOH → NaNO3 + H2O
        addReaction(
                List.of(new ReactionComponent("HNO3", 1), new ReactionComponent("NaOH", 1)),
                List.of(new ReactionComponent("NaNO3", 1), new ReactionComponent("H2O", 1)),
                ReactionType.ACID_BASE, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.EXOTHERMIC)
        );
        // HCl + KOH → KCl + H2O
        addReaction(
                List.of(new ReactionComponent("HCl", 1), new ReactionComponent("KOH", 1)),
                List.of(new ReactionComponent("KCl", 1), new ReactionComponent("H2O", 1)),
                ReactionType.ACID_BASE, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.EXOTHERMIC)
        );
        // 2HCl + Ca(OH)2 → CaCl2 + 2H2O
        addReaction(
                List.of(new ReactionComponent("HCl", 2), new ReactionComponent("Ca(OH)2", 1)),
                List.of(new ReactionComponent("CaCl2", 1), new ReactionComponent("H2O", 2)),
                ReactionType.ACID_BASE, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.EXOTHERMIC)
        );
        // H2SO4 + 2KOH → K2SO4 + 2H2O
        addReaction(
                List.of(new ReactionComponent("H2SO4", 1), new ReactionComponent("KOH", 2)),
                List.of(new ReactionComponent("K2SO4", 1), new ReactionComponent("H2O", 2)),
                ReactionType.ACID_BASE, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.EXOTHERMIC)
        );

        // === Precipitation ===
        // AgNO3 + NaCl → AgCl + NaNO3
        addReaction(
                List.of(new ReactionComponent("AgNO3", 1), new ReactionComponent("NaCl", 1)),
                List.of(new ReactionComponent("AgCl", 1), new ReactionComponent("NaNO3", 1)),
                ReactionType.PRECIPITATION, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.PRECIPITATE, ReactionEffect.COLOR_CHANGE)
        );
        // BaCl2 + Na2SO4 → BaSO4 + 2NaCl
        addReaction(
                List.of(new ReactionComponent("BaCl2", 1), new ReactionComponent("Na2SO4", 1)),
                List.of(new ReactionComponent("BaSO4", 1), new ReactionComponent("NaCl", 2)),
                ReactionType.PRECIPITATION, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.PRECIPITATE)
        );
        // Pb(NO3)2 + 2KI → PbI2 + 2KNO3
        addReaction(
                List.of(new ReactionComponent("Pb(NO3)2", 1), new ReactionComponent("KI", 2)),
                List.of(new ReactionComponent("PbI2", 1), new ReactionComponent("KNO3", 2)),
                ReactionType.PRECIPITATION, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.PRECIPITATE, ReactionEffect.COLOR_CHANGE)
        );
        // CuSO4 + 2NaOH → Cu(OH)2 + Na2SO4
        addReaction(
                List.of(new ReactionComponent("CuSO4", 1), new ReactionComponent("NaOH", 2)),
                List.of(new ReactionComponent("Cu(OH)2", 1), new ReactionComponent("Na2SO4", 1)),
                ReactionType.PRECIPITATION, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.PRECIPITATE, ReactionEffect.COLOR_CHANGE)
        );
        // FeCl3 + 3NaOH → Fe(OH)3 + 3NaCl
        addReaction(
                List.of(new ReactionComponent("FeCl3", 1), new ReactionComponent("NaOH", 3)),
                List.of(new ReactionComponent("Fe(OH)3", 1), new ReactionComponent("NaCl", 3)),
                ReactionType.PRECIPITATION, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.PRECIPITATE, ReactionEffect.COLOR_CHANGE)
        );

        // === Single Replacement ===
        // Zn + CuSO4 → ZnSO4 + Cu (not solution-based but keeping)
        addReaction(
                List.of(new ReactionComponent("Zn", 1), new ReactionComponent("CuSO4", 1)),
                List.of(new ReactionComponent("ZnSO4", 1), new ReactionComponent("Cu", 1)),
                ReactionType.SINGLE_REPLACEMENT, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.COLOR_CHANGE)
        );
        // Fe + CuSO4 → FeSO4 + Cu
        addReaction(
                List.of(new ReactionComponent("Fe", 1), new ReactionComponent("CuSO4", 1)),
                List.of(new ReactionComponent("FeSO4", 1), new ReactionComponent("Cu", 1)),
                ReactionType.SINGLE_REPLACEMENT, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.COLOR_CHANGE)
        );
        // Zn + 2HCl → ZnCl2 + H2
        addReaction(
                List.of(new ReactionComponent("Zn", 1), new ReactionComponent("HCl", 2)),
                List.of(new ReactionComponent("ZnCl2", 1), new ReactionComponent("H2", 1)),
                ReactionType.SINGLE_REPLACEMENT, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION)
        );
        // Mg + 2HCl → MgCl2 + H2
        addReaction(
                List.of(new ReactionComponent("Mg", 1), new ReactionComponent("HCl", 2)),
                List.of(new ReactionComponent("MgCl2", 1), new ReactionComponent("H2", 1)),
                ReactionType.SINGLE_REPLACEMENT, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.EXOTHERMIC)
        );

        // === Double Replacement ===
        // Na2CO3 + 2HCl → 2NaCl + H2O + CO2
        addReaction(
                List.of(new ReactionComponent("Na2CO3", 1), new ReactionComponent("HCl", 2)),
                List.of(new ReactionComponent("NaCl", 2), new ReactionComponent("H2O", 1), new ReactionComponent("CO2", 1)),
                ReactionType.DOUBLE_REPLACEMENT, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION)
        );
        // NaHCO3 + HCl → NaCl + H2O + CO2
        addReaction(
                List.of(new ReactionComponent("NaHCO3", 1), new ReactionComponent("HCl", 1)),
                List.of(new ReactionComponent("NaCl", 1), new ReactionComponent("H2O", 1), new ReactionComponent("CO2", 1)),
                ReactionType.DOUBLE_REPLACEMENT, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION)
        );
        // CaCO3 + 2HCl → CaCl2 + H2O + CO2
        addReaction(
                List.of(new ReactionComponent("CaCO3", 1), new ReactionComponent("HCl", 2)),
                List.of(new ReactionComponent("CaCl2", 1), new ReactionComponent("H2O", 1), new ReactionComponent("CO2", 1)),
                ReactionType.DOUBLE_REPLACEMENT, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION)
        );

        // === Synthesis ===
        // CaO + H2O → Ca(OH)2
        addReaction(
                List.of(new ReactionComponent("CaO", 1), new ReactionComponent("H2O", 1)),
                List.of(new ReactionComponent("Ca(OH)2", 1)),
                ReactionType.SYNTHESIS, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.EXOTHERMIC)
        );
        // SO3 + H2O → H2SO4
        addReaction(
                List.of(new ReactionComponent("SO3", 1), new ReactionComponent("H2O", 1)),
                List.of(new ReactionComponent("H2SO4", 1)),
                ReactionType.SYNTHESIS, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.EXOTHERMIC)
        );
        // Na2O + H2O → 2NaOH
        addReaction(
                List.of(new ReactionComponent("Na2O", 1), new ReactionComponent("H2O", 1)),
                List.of(new ReactionComponent("NaOH", 2)),
                ReactionType.SYNTHESIS, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.EXOTHERMIC)
        );

        // === Decomposition (requires heat) ===
        // CaCO3 → CaO + CO2
        addReaction(
                List.of(new ReactionComponent("CaCO3", 1)),
                List.of(new ReactionComponent("CaO", 1), new ReactionComponent("CO2", 1)),
                ReactionType.DECOMPOSITION, Reaction.ReactionConditions.heated(),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.ENDOTHERMIC)
        );
        // 2H2O2 → 2H2O + O2
        addReaction(
                List.of(new ReactionComponent("H2O2", 2)),
                List.of(new ReactionComponent("H2O", 2), new ReactionComponent("O2", 1)),
                ReactionType.DECOMPOSITION, new Reaction.ReactionConditions(0, 1000, "MnO2", false),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION)
        );
        // 2KClO3 → 2KCl + 3O2
        addReaction(
                List.of(new ReactionComponent("KClO3", 2)),
                List.of(new ReactionComponent("KCl", 2), new ReactionComponent("O2", 3)),
                ReactionType.DECOMPOSITION, Reaction.ReactionConditions.heated(),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.ENDOTHERMIC)
        );

        // === Redox ===
        // 2Fe + 3Cl2 → 2FeCl3
        addReaction(
                List.of(new ReactionComponent("Fe", 2), new ReactionComponent("Cl2", 3)),
                List.of(new ReactionComponent("FeCl3", 2)),
                ReactionType.REDOX, Reaction.ReactionConditions.heated(),
                EnumSet.of(ReactionEffect.EXOTHERMIC, ReactionEffect.COLOR_CHANGE)
        );
        // Cu + 4HNO3 → Cu(NO3)2 + 2NO2 + 2H2O (concentrated)
        addReaction(
                List.of(new ReactionComponent("Cu", 1), new ReactionComponent("HNO3", 4)),
                List.of(new ReactionComponent("Cu(NO3)2", 1), new ReactionComponent("NO2", 2), new ReactionComponent("H2O", 2)),
                ReactionType.REDOX, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.COLOR_CHANGE)
        );
        // 2KMnO4 + 16HCl → 2KCl + 2MnCl2 + 5Cl2 + 8H2O
        addReaction(
                List.of(new ReactionComponent("KMnO4", 2), new ReactionComponent("HCl", 16)),
                List.of(new ReactionComponent("KCl", 2), new ReactionComponent("MnCl2", 2), new ReactionComponent("Cl2", 5), new ReactionComponent("H2O", 8)),
                ReactionType.REDOX, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.COLOR_CHANGE, ReactionEffect.GAS_EVOLUTION)
        );

        // === More Acid-Base ===
        // NH3 + HCl → NH4Cl
        addReaction(
                List.of(new ReactionComponent("NH3", 1), new ReactionComponent("HCl", 1)),
                List.of(new ReactionComponent("NH4Cl", 1)),
                ReactionType.ACID_BASE, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.EXOTHERMIC)
        );
        // 3NaOH + H3PO4 → Na3PO4 + 3H2O
        addReaction(
                List.of(new ReactionComponent("NaOH", 3), new ReactionComponent("H3PO4", 1)),
                List.of(new ReactionComponent("Na3PO4", 1), new ReactionComponent("H2O", 3)),
                ReactionType.ACID_BASE, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.EXOTHERMIC)
        );

        // === More Precipitation ===
        // AgNO3 + HCl → AgCl + HNO3
        addReaction(
                List.of(new ReactionComponent("AgNO3", 1), new ReactionComponent("HCl", 1)),
                List.of(new ReactionComponent("AgCl", 1), new ReactionComponent("HNO3", 1)),
                ReactionType.PRECIPITATION, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.PRECIPITATE)
        );
        // BaCl2 + H2SO4 → BaSO4 + 2HCl
        addReaction(
                List.of(new ReactionComponent("BaCl2", 1), new ReactionComponent("H2SO4", 1)),
                List.of(new ReactionComponent("BaSO4", 1), new ReactionComponent("HCl", 2)),
                ReactionType.PRECIPITATION, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.PRECIPITATE)
        );
        // CaCl2 + Na2CO3 → CaCO3 + 2NaCl
        addReaction(
                List.of(new ReactionComponent("CaCl2", 1), new ReactionComponent("Na2CO3", 1)),
                List.of(new ReactionComponent("CaCO3", 1), new ReactionComponent("NaCl", 2)),
                ReactionType.PRECIPITATION, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.PRECIPITATE)
        );

        // === Combustion ===
        // CH4 + 2O2 → CO2 + 2H2O
        addReaction(
                List.of(new ReactionComponent("CH4", 1), new ReactionComponent("O2", 2)),
                List.of(new ReactionComponent("CO2", 1), new ReactionComponent("H2O", 2)),
                ReactionType.COMBUSTION, Reaction.ReactionConditions.heated(),
                EnumSet.of(ReactionEffect.EXOTHERMIC)
        );
        // 2H2 + O2 → 2H2O
        addReaction(
                List.of(new ReactionComponent("H2", 2), new ReactionComponent("O2", 1)),
                List.of(new ReactionComponent("H2O", 2)),
                ReactionType.COMBUSTION, Reaction.ReactionConditions.heated(),
                EnumSet.of(ReactionEffect.EXOTHERMIC)
        );
        // C2H5OH + 3O2 → 2CO2 + 3H2O
        addReaction(
                List.of(new ReactionComponent("C2H5OH", 1), new ReactionComponent("O2", 3)),
                List.of(new ReactionComponent("CO2", 2), new ReactionComponent("H2O", 3)),
                ReactionType.COMBUSTION, Reaction.ReactionConditions.heated(),
                EnumSet.of(ReactionEffect.EXOTHERMIC)
        );

        // === Additional reactions ===
        // 2Na + 2H2O → 2NaOH + H2
        addReaction(
                List.of(new ReactionComponent("Na", 2), new ReactionComponent("H2O", 2)),
                List.of(new ReactionComponent("NaOH", 2), new ReactionComponent("H2", 1)),
                ReactionType.SINGLE_REPLACEMENT, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.EXOTHERMIC)
        );
        // K + H2O → KOH + H2 (simplified)
        addReaction(
                List.of(new ReactionComponent("K", 2), new ReactionComponent("H2O", 2)),
                List.of(new ReactionComponent("KOH", 2), new ReactionComponent("H2", 1)),
                ReactionType.SINGLE_REPLACEMENT, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.EXOTHERMIC)
        );
        // Fe + H2SO4 → FeSO4 + H2
        addReaction(
                List.of(new ReactionComponent("Fe", 1), new ReactionComponent("H2SO4", 1)),
                List.of(new ReactionComponent("FeSO4", 1), new ReactionComponent("H2", 1)),
                ReactionType.SINGLE_REPLACEMENT, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION)
        );
        // CuSO4 + Fe → FeSO4 + Cu (duplicate-safe, different entry order)
        // Al + 3HCl → AlCl3 + 3/2 H2 (simplified as 2Al + 6HCl → 2AlCl3 + 3H2)
        addReaction(
                List.of(new ReactionComponent("Al", 2), new ReactionComponent("HCl", 6)),
                List.of(new ReactionComponent("AlCl3", 2), new ReactionComponent("H2", 3)),
                ReactionType.SINGLE_REPLACEMENT, Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.EXOTHERMIC)
        );
    }

    private static void addReaction(List<ReactionComponent> reactants, List<ReactionComponent> products,
                                     ReactionType type, Reaction.ReactionConditions conditions,
                                     Set<ReactionEffect> effects) {
        REACTIONS.add(new Reaction(reactants, products, type, conditions, effects));
    }

    /**
     * Find a reaction matching two input formulas (order-independent).
     * Falls back to dynamic prediction if no registered reaction matches.
     */
    public static Reaction findReaction(String formula1, String formula2) {
        return findReaction(List.of(formula1, formula2));
    }

    /**
     * Find a reaction matching a list of input formulas (order-independent).
     * First checks the static registry, then falls back to the dynamic predictor.
     */
    public static Reaction findReaction(List<String> formulas) {
        // Check static registry first
        for (Reaction reaction : REACTIONS) {
            if (matchesReactants(reaction, formulas)) {
                return reaction;
            }
        }
        // Fall back to dynamic prediction from chemistry rules
        return DynamicReactionPredictor.predict(formulas);
    }

    /**
     * Find all possible reactions given a set of formulas (checks all pairs).
     * Used by the multi-reactant beaker to find reactions among its contents.
     *
     * @param formulas all formulas present in the beaker
     * @return the first reaction found, or null
     */
    public static Reaction findReactionAmong(List<String> formulas) {
        // Try all pairs
        for (int i = 0; i < formulas.size(); i++) {
            for (int j = i + 1; j < formulas.size(); j++) {
                Reaction reaction = findReaction(formulas.get(i), formulas.get(j));
                if (reaction != null) {
                    return reaction;
                }
            }
        }
        // Try single-component reactions (decomposition)
        for (String formula : formulas) {
            Reaction reaction = findReaction(List.of(formula));
            if (reaction != null) {
                return reaction;
            }
        }
        return null;
    }

    private static boolean matchesReactants(Reaction reaction, List<String> inputFormulas) {
        List<ReactionComponent> reactants = reaction.getReactants();

        // Build a set of formula strings from reactants
        Set<String> reactantFormulas = new HashSet<>();
        for (ReactionComponent rc : reactants) {
            reactantFormulas.add(rc.formula());
        }

        Set<String> inputSet = new HashSet<>(inputFormulas);

        return reactantFormulas.equals(inputSet);
    }

    public static List<Reaction> getAllReactions() {
        return Collections.unmodifiableList(REACTIONS);
    }
}
