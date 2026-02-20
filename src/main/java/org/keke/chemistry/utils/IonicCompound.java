package org.keke.chemistry.utils;

import java.util.*;

/**
 * Decomposes ionic compound formulas into their constituent cation and anion.
 * Handles:
 * - Simple binary compounds: NaCl → (Na⁺, Cl⁻)
 * - Compounds with polyatomic ions: CuSO4 → (Cu²⁺, SO4²⁻)
 * - Parenthesized groups: Ca(OH)2 → (Ca²⁺, OH⁻)
 * - Acids: HCl → (H⁺, Cl⁻), H2SO4 → (H⁺, SO4²⁻)
 */
public class IonicCompound {

    private final String cation;
    private final String anion;
    private final int cationCount;
    private final int anionCount;
    private final int cationCharge;
    private final int anionCharge;

    public IonicCompound(String cation, String anion, int cationCount, int anionCount,
                         int cationCharge, int anionCharge) {
        this.cation = cation;
        this.anion = anion;
        this.cationCount = cationCount;
        this.anionCount = anionCount;
        this.cationCharge = cationCharge;
        this.anionCharge = anionCharge;
    }

    public String getCation() { return cation; }
    public String getAnion() { return anion; }
    public int getCationCount() { return cationCount; }
    public int getAnionCount() { return anionCount; }
    public int getCationCharge() { return cationCharge; }
    public int getAnionCharge() { return anionCharge; }

    /**
     * Ordered list of polyatomic anions to check against, longest first
     * to avoid partial matches (e.g., "SO4" before "SO3" before "S").
     */
    private static final List<String> POLYATOMIC_ANIONS = List.of(
            "Cr2O7", "CH3COO", "H2PO4", "HPO4", "HCO3", "HSO4",
            "ClO4", "ClO3", "MnO4", "CrO4",
            "C2O4", "SiO3",
            "SO4", "SO3", "NO3", "CO3", "PO4",
            "SCN", "CN", "OH"
    );

    /**
     * Simple anion elements (halides, chalcogenides).
     */
    private static final Set<String> SIMPLE_ANIONS = Set.of(
            "F", "Cl", "Br", "I", "O", "S"
    );

    /**
     * Try to decompose a chemical formula into its ionic components.
     * Returns null if the formula can't be decomposed (molecular compound, element, etc.).
     */
    public static IonicCompound decompose(String formula) {
        if (formula == null || formula.length() < 2) return null;

        // Special cases: pure elements, diatomic molecules
        if (formula.matches("[A-Z][a-z]?\\d*")) {
            // Single element, not ionic
            return null;
        }

        // Try to identify cation + anion
        String cation = null;
        String anion = null;
        int cationCount = 1;
        int anionCount = 1;

        // Check for acids: H + anion
        if (formula.startsWith("H") && !formula.equals("He") && !formula.equals("Hg")
                && !formula.equals("Hf") && !formula.equals("Ho") && !formula.equals("Hs")) {
            String rest = formula.substring(1);
            // Handle H2SO4, H3PO4, etc.
            int hCount = 1;
            if (!rest.isEmpty() && Character.isDigit(rest.charAt(0))) {
                hCount = rest.charAt(0) - '0';
                rest = rest.substring(1);
            }

            // Check if rest is a known polyatomic anion
            for (String polyAnion : POLYATOMIC_ANIONS) {
                if (rest.equals(polyAnion)) {
                    cation = "H";
                    anion = polyAnion;
                    cationCount = hCount;
                    anionCount = 1;
                    break;
                }
            }

            // Check simple anions
            if (anion == null) {
                for (String simpleAnion : SIMPLE_ANIONS) {
                    if (rest.equals(simpleAnion)) {
                        cation = "H";
                        anion = simpleAnion;
                        cationCount = hCount;
                        anionCount = 1;
                        break;
                    }
                }
            }

            if (anion != null) {
                int cationCharge = SolubilityTable.getCationCharge(cation);
                int anionCharge = SolubilityTable.getAnionCharge(anion);
                if (cationCharge == 0) cationCharge = 1;
                if (anionCharge == 0) anionCharge = -1;
                return new IonicCompound(cation, anion, cationCount, anionCount,
                        cationCharge, anionCharge);
            }
        }

        // Try metal + polyatomic anion
        // Identify the cation (metal at the start)
        String possibleCation = extractLeadingCation(formula);
        if (possibleCation != null && SolubilityTable.getCationCharge(possibleCation) > 0) {
            cation = possibleCation;
            String remainder = formula.substring(cation.length());

            // Parse cation count
            cationCount = 1;
            if (!remainder.isEmpty() && Character.isDigit(remainder.charAt(0))) {
                cationCount = remainder.charAt(0) - '0';
                remainder = remainder.substring(1);
            }

            // Check for parenthesized anion: e.g., Ca(OH)2
            if (remainder.startsWith("(")) {
                int closeIdx = remainder.indexOf(')');
                if (closeIdx > 0) {
                    String innerAnion = remainder.substring(1, closeIdx);
                    String afterClose = remainder.substring(closeIdx + 1);
                    anionCount = 1;
                    if (!afterClose.isEmpty() && Character.isDigit(afterClose.charAt(0))) {
                        anionCount = afterClose.charAt(0) - '0';
                    }

                    // Check if inner is a known polyatomic anion
                    for (String polyAnion : POLYATOMIC_ANIONS) {
                        if (innerAnion.equals(polyAnion)) {
                            anion = polyAnion;
                            break;
                        }
                    }
                }
            }

            // Check remaining for polyatomic anions (no parens)
            if (anion == null) {
                for (String polyAnion : POLYATOMIC_ANIONS) {
                    if (remainder.startsWith(polyAnion)) {
                        anion = polyAnion;
                        String afterAnion = remainder.substring(polyAnion.length());
                        anionCount = 1;
                        if (!afterAnion.isEmpty() && Character.isDigit(afterAnion.charAt(0))) {
                            anionCount = afterAnion.charAt(0) - '0';
                        }
                        break;
                    }
                }
            }

            // Check for simple anion at end
            if (anion == null) {
                for (String simpleAnion : SIMPLE_ANIONS) {
                    if (remainder.startsWith(simpleAnion)) {
                        anion = simpleAnion;
                        String afterAnion = remainder.substring(simpleAnion.length());
                        anionCount = 1;
                        if (!afterAnion.isEmpty() && Character.isDigit(afterAnion.charAt(0))) {
                            anionCount = afterAnion.charAt(0) - '0';
                        }
                        break;
                    }
                }
            }

            if (anion != null) {
                int catCharge = SolubilityTable.getCationCharge(cation);
                int anCharge = SolubilityTable.getAnionCharge(anion);
                if (catCharge == 0) catCharge = inferCationCharge(cationCount, anionCount, anCharge);
                if (anCharge == 0) anCharge = -1;
                return new IonicCompound(cation, anion, cationCount, anionCount, catCharge, anCharge);
            }
        }

        return null;
    }

    /**
     * Extract the leading cation symbol (one or two characters) from a formula.
     */
    private static String extractLeadingCation(String formula) {
        if (formula.isEmpty()) return null;

        char first = formula.charAt(0);
        if (!Character.isUpperCase(first)) return null;

        // Try two-character symbol first (e.g., "Na", "Ca")
        if (formula.length() >= 2 && Character.isLowerCase(formula.charAt(1))) {
            String twoChar = formula.substring(0, 2);
            // Make sure it's not the start of a polyatomic group
            if (SolubilityTable.getCationCharge(twoChar) > 0 || Element.fromSymbol(twoChar) != null) {
                return twoChar;
            }
        }

        // Single character
        String oneChar = formula.substring(0, 1);
        if (SolubilityTable.getCationCharge(oneChar) > 0 || Element.fromSymbol(oneChar) != null) {
            return oneChar;
        }

        return null;
    }

    /**
     * Infer cation charge from stoichiometry and known anion charge.
     * Total positive charge = total negative charge.
     */
    private static int inferCationCharge(int cationCount, int anionCount, int anionCharge) {
        int totalNegative = anionCount * Math.abs(anionCharge);
        if (cationCount <= 0) return 1;
        return totalNegative / cationCount;
    }

    /**
     * Check if a formula represents an acid (starts with H, donates protons).
     */
    public static boolean isAcid(String formula) {
        if (formula == null || formula.isEmpty()) return false;
        if (formula.equals("H2O") || formula.equals("H2O2") || formula.equals("H2")) return false;
        IonicCompound ic = decompose(formula);
        return ic != null && ic.cation.equals("H");
    }

    /**
     * Check if a formula represents a metal hydroxide base.
     */
    public static boolean isBase(String formula) {
        if (formula == null || formula.isEmpty()) return false;
        IonicCompound ic = decompose(formula);
        return ic != null && ic.anion.equals("OH") && !ic.cation.equals("H");
    }

    /**
     * Check if a formula represents an ionic salt (not acid, not base).
     */
    public static boolean isSalt(String formula) {
        IonicCompound ic = decompose(formula);
        if (ic == null) return false;
        return !ic.cation.equals("H") && !ic.anion.equals("OH");
    }

    @Override
    public String toString() {
        return cation + (cationCount > 1 ? cationCount : "") + " + " +
                anion + (anionCount > 1 ? anionCount : "");
    }
}
