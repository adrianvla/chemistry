package org.keke.chemistry.utils;

import java.util.*;

/**
 * Solubility rules for ionic compounds in aqueous solution.
 * Used for predicting precipitation reactions: when two soluble ionic compounds
 * are mixed, check if swapping ions produces an insoluble product.
 *
 * Rules follow the standard general chemistry solubility guidelines:
 * 1. All nitrates (NO3⁻) are soluble.
 * 2. All alkali metal (Li, Na, K, Rb, Cs) and ammonium (NH4) salts are soluble.
 * 3. All chlorides, bromides, iodides are soluble EXCEPT Ag⁺, Pb²⁺, Hg₂²⁺.
 * 4. All sulfates are soluble EXCEPT Ba²⁺, Pb²⁺, Ca²⁺(slightly), Sr²⁺.
 * 5. Most hydroxides are insoluble EXCEPT alkali metals and Ba²⁺, Ca²⁺(slightly).
 * 6. Most carbonates, phosphates, sulfides, and chromates are insoluble
 *    EXCEPT alkali metals and ammonium salts.
 */
public class SolubilityTable {

    /**
     * Common polyatomic anions with their formulas and charges.
     */
    public static final Map<String, Integer> ANION_CHARGES = new LinkedHashMap<>();

    /**
     * Common cation charges (most common oxidation state).
     */
    public static final Map<String, Integer> CATION_CHARGES = new LinkedHashMap<>();

    private static final Set<String> ALWAYS_SOLUBLE_CATIONS = Set.of(
            "Na", "K", "Li", "Rb", "Cs", "NH4"
    );

    private static final Set<String> INSOLUBLE_HALIDE_CATIONS = Set.of(
            "Ag", "Pb", "Hg"
    );

    private static final Set<String> INSOLUBLE_SULFATE_CATIONS = Set.of(
            "Ba", "Pb", "Ca", "Sr"
    );

    private static final Set<String> SOLUBLE_HYDROXIDE_CATIONS = Set.of(
            "Na", "K", "Li", "Ba", "Ca", "Sr", "Rb", "Cs", "NH4"
    );

    static {
        // Polyatomic anion charges
        ANION_CHARGES.put("SO4", -2);
        ANION_CHARGES.put("NO3", -1);
        ANION_CHARGES.put("OH", -1);
        ANION_CHARGES.put("CO3", -2);
        ANION_CHARGES.put("PO4", -3);
        ANION_CHARGES.put("SO3", -2);
        ANION_CHARGES.put("ClO3", -1);
        ANION_CHARGES.put("ClO4", -1);
        ANION_CHARGES.put("CrO4", -2);
        ANION_CHARGES.put("Cr2O7", -2);
        ANION_CHARGES.put("MnO4", -1);
        ANION_CHARGES.put("C2O4", -2);  // oxalate
        ANION_CHARGES.put("SCN", -1);
        ANION_CHARGES.put("CN", -1);
        ANION_CHARGES.put("S", -2);
        ANION_CHARGES.put("Cl", -1);
        ANION_CHARGES.put("Br", -1);
        ANION_CHARGES.put("I", -1);
        ANION_CHARGES.put("F", -1);
        ANION_CHARGES.put("O", -2);
        ANION_CHARGES.put("HCO3", -1);
        ANION_CHARGES.put("HSO4", -1);
        ANION_CHARGES.put("H2PO4", -1);
        ANION_CHARGES.put("HPO4", -2);
        ANION_CHARGES.put("CH3COO", -1); // acetate
        ANION_CHARGES.put("SiO3", -2);
        ANION_CHARGES.put("N3", -1);     // azide

        // Common cation charges
        CATION_CHARGES.put("H", 1);
        CATION_CHARGES.put("Li", 1);
        CATION_CHARGES.put("Na", 1);
        CATION_CHARGES.put("K", 1);
        CATION_CHARGES.put("Rb", 1);
        CATION_CHARGES.put("Cs", 1);
        CATION_CHARGES.put("NH4", 1);
        CATION_CHARGES.put("Ag", 1);
        CATION_CHARGES.put("Cu", 2);   // Cu²⁺ most common in solution
        CATION_CHARGES.put("Mg", 2);
        CATION_CHARGES.put("Ca", 2);
        CATION_CHARGES.put("Sr", 2);
        CATION_CHARGES.put("Ba", 2);
        CATION_CHARGES.put("Zn", 2);
        CATION_CHARGES.put("Fe", 3);   // Fe³⁺ in FeCl3, but Fe²⁺ also common
        CATION_CHARGES.put("Pb", 2);
        CATION_CHARGES.put("Sn", 2);
        CATION_CHARGES.put("Ni", 2);
        CATION_CHARGES.put("Co", 2);
        CATION_CHARGES.put("Mn", 2);
        CATION_CHARGES.put("Cr", 3);
        CATION_CHARGES.put("Al", 3);
        CATION_CHARGES.put("Hg", 2);
        CATION_CHARGES.put("Bi", 3);
    }

    /**
     * Determine whether a salt made of the given cation and anion is soluble in water.
     *
     * @param cation the cation symbol (e.g., "Na", "Cu", "NH4")
     * @param anion  the anion formula (e.g., "Cl", "SO4", "OH")
     * @return true if the compound is expected to be soluble
     */
    public static boolean isSoluble(String cation, String anion) {
        // Rule 1: All alkali metal and ammonium salts are soluble
        if (ALWAYS_SOLUBLE_CATIONS.contains(cation)) {
            return true;
        }

        // Rule 2: All nitrates are soluble
        if (anion.equals("NO3")) {
            return true;
        }

        // Rule 3: All acetates are soluble (except silver acetate is slightly soluble)
        if (anion.equals("CH3COO")) {
            return true;
        }

        // Rule 4: Halides (Cl, Br, I)
        if (anion.equals("Cl") || anion.equals("Br") || anion.equals("I")) {
            return !INSOLUBLE_HALIDE_CATIONS.contains(cation);
        }

        // Rule 5: Fluorides — most are insoluble except alkali metals
        if (anion.equals("F")) {
            return false; // already handled alkali metals above
        }

        // Rule 6: Sulfates
        if (anion.equals("SO4")) {
            return !INSOLUBLE_SULFATE_CATIONS.contains(cation);
        }

        // Rule 7: Hydroxides
        if (anion.equals("OH")) {
            return SOLUBLE_HYDROXIDE_CATIONS.contains(cation);
        }

        // Rule 8: Carbonates, phosphates, sulfides, chromates — mostly insoluble
        if (Set.of("CO3", "PO4", "S", "CrO4", "C2O4", "SiO3", "SO3").contains(anion)) {
            return false; // alkali metals already handled
        }

        // Default: assume soluble
        return true;
    }

    /**
     * Build the formula of an ionic compound from a cation and anion,
     * using proper subscripts to balance charges.
     *
     * @param cation       cation symbol
     * @param cationCharge positive charge of cation
     * @param anion        anion formula
     * @param anionCharge  negative charge of anion (as negative int)
     * @return the formula string, e.g. "CaCl2", "Al2(SO4)3"
     */
    public static String buildFormula(String cation, int cationCharge, String anion, int anionCharge) {
        int absAnionCharge = Math.abs(anionCharge);

        // Find lowest common multiple to balance
        int lcm = lcm(cationCharge, absAnionCharge);
        int numCations = lcm / cationCharge;
        int numAnions = lcm / absAnionCharge;

        StringBuilder sb = new StringBuilder();

        // Cation
        if (cation.equals("NH4") && numCations > 1) {
            sb.append("(").append(cation).append(")").append(numCations);
        } else if (numCations > 1) {
            sb.append(cation).append(numCations);
        } else {
            sb.append(cation);
        }

        // Anion
        if (anion.length() > 2 && numAnions > 1) {
            // Polyatomic anion with subscript > 1: use parentheses
            sb.append("(").append(anion).append(")").append(numAnions);
        } else if (numAnions > 1) {
            sb.append(anion).append(numAnions);
        } else {
            sb.append(anion);
        }

        return sb.toString();
    }

    /**
     * Get the charge of a cation. Returns 0 if unknown.
     */
    public static int getCationCharge(String cation) {
        return CATION_CHARGES.getOrDefault(cation, 0);
    }

    /**
     * Get the charge of an anion. Returns 0 if unknown.
     */
    public static int getAnionCharge(String anion) {
        return ANION_CHARGES.getOrDefault(anion, 0);
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int t = b;
            b = a % b;
            a = t;
        }
        return a;
    }

    private static int lcm(int a, int b) {
        return Math.abs(a * b) / gcd(a, b);
    }
}
