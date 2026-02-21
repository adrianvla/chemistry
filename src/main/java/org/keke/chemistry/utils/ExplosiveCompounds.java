package org.keke.chemistry.utils;

import java.util.Map;
import java.util.Set;

/**
 * Algorithmic detection of explosive compounds based on chemical properties.
 * Uses oxygen balance, nitrogen content, and structural analysis rather than
 * hardcoded compound lists.
 *
 * Oxygen balance (OB%):  OB = -1600 × (2×C + H/2 + M − O) / MW
 * where C = carbon atoms, H = hydrogen atoms, M = metal atoms needing oxygen,
 * O = oxygen atoms, MW = molecular weight.
 *
 * Compounds are considered explosive if they have:
 *   1. High nitrogen and oxygen content relative to carbon/hydrogen
 *   2. Oxygen balance in the range −100% to +40%
 *   3. Contain N–O or N–N bonds (nitro, nitrate, azide groups)
 *
 * Known explosive families:
 *   - Nitrate esters: R-ONO2 (nitroglycerin, PETN, nitrocellulose)
 *   - Nitroaromatics: Ar-NO2 (TNT, picric acid, tetryl)
 *   - Nitramines: R2N-NO2 (RDX, HMX)
 *   - Azides: R-N3 (lead azide, sodium azide)
 *   - Perchlorates with fuel
 */
public class ExplosiveCompounds {

    /**
     * Well-known explosive formulas with their detonation power relative to TNT.
     * Power is in kJ per mole of the compound upon detonation.
     * TNT: 4184 kJ/kg  → ~950 kJ/mol (MW=227.13)
     */
    private static final Map<String, Double> KNOWN_EXPLOSIVES = Map.ofEntries(
            // Nitroglycerin: C3H5N3O9, MW=227.09, detonation energy ~6300 kJ/kg → ~1431 kJ/mol
            Map.entry("C3H5N3O9", 1431.0),
            // TNT: C7H5N3O6, MW=227.13, detonation energy ~4184 kJ/kg → ~950 kJ/mol
            Map.entry("C7H5N3O6", 950.0),
            // RDX: C3H6N6O6, MW=222.12, ~5360 kJ/kg → ~1190 kJ/mol
            Map.entry("C3H6N6O6", 1190.0),
            // PETN: C5H8N4O12, MW=316.14, ~5810 kJ/kg → ~1836 kJ/mol
            Map.entry("C5H8N4O12", 1836.0),
            // Picric acid: C6H3N3O7, MW=229.10, ~4184 kJ/kg → ~959 kJ/mol
            Map.entry("C6H3N3O7", 959.0),
            // Lead azide: Pb(N3)2 → PbN6, MW=291.24, ~1630 kJ/kg → ~475 kJ/mol (primary)
            Map.entry("PbN6", 475.0),
            // Sodium azide: NaN3, MW=65.01, ~decomposes releasing N2 + Na
            Map.entry("NaN3", 200.0),
            // Ammonium nitrate: NH4NO3, MW=80.04, ~1585 kJ/kg → ~127 kJ/mol (ANFO)
            Map.entry("NH4NO3", 127.0),
            // Mercury fulminate: Hg(CNO)2 → HgC2N2O2, MW=284.62, primary explosive
            Map.entry("HgC2N2O2", 500.0),
            // Potassium chlorate (can detonate with organics): KClO3
            Map.entry("KClO3", 80.0)
    );

    /** Elements considered metallic for oxygen balance calculation. */
    private static final Set<String> METAL_SYMBOLS = Set.of(
            "Li", "Na", "K", "Rb", "Cs", "Be", "Mg", "Ca", "Sr", "Ba",
            "Al", "Fe", "Cu", "Zn", "Pb", "Ag", "Hg", "Mn", "Cr", "Ti"
    );

    /**
     * Check if a compound is explosive.
     * @param formula the chemical formula
     * @return true if the compound can detonate
     */
    public static boolean isExplosive(String formula) {
        if (formula == null || formula.isEmpty()) return false;
        if (KNOWN_EXPLOSIVES.containsKey(formula)) return true;
        return computeExplosivePowerKJPerMol(formula) > 50.0;
    }

    /**
     * Get the explosive power in kJ per mole. Returns 0 for non-explosives.
     * For known explosives, uses tabulated values.
     * For unknown compounds, estimates from oxygen balance and composition.
     */
    public static double getExplosivePowerKJPerMol(String formula) {
        if (formula == null || formula.isEmpty()) return 0.0;
        Double known = KNOWN_EXPLOSIVES.get(formula);
        if (known != null) return known;
        return computeExplosivePowerKJPerMol(formula);
    }

    /**
     * Convert explosive power to Minecraft explosion radius.
     * TNT block = power 4.0 in Minecraft.
     * 1 mol TNT = 950 kJ → power 4.0
     * So 1 kJ ≈ 4.0/950 ≈ 0.00421 power units
     * Scale factor accounts for container effects (confinement amplifies).
     *
     * @param formula the explosive formula
     * @param moles   amount in moles
     * @return Minecraft explosion power (4.0 = vanilla TNT block)
     */
    public static float calculateExplosionPower(String formula, double moles) {
        double powerPerMol = getExplosivePowerKJPerMol(formula);
        if (powerPerMol <= 0) return 0.0f;
        // Scale: 1 mol TNT (950 kJ/mol) → power 4.0
        // power = moles × (powerPerMol / 950) × 4.0
        float power = (float) (moles * (powerPerMol / 950.0) * 4.0);
        // Cap at 40.0 to prevent world destruction (charged creeper is 6.0)
        return Math.min(40.0f, power);
    }

    /**
     * Calculate total explosion power for a mixture of compounds.
     */
    public static float calculateTotalExplosionPower(Map<String, Double> contents) {
        float totalPower = 0.0f;
        for (var entry : contents.entrySet()) {
            totalPower += calculateExplosionPower(entry.getKey(), entry.getValue());
        }
        return Math.min(40.0f, totalPower);
    }

    /**
     * Check if a mixture has any explosive component.
     */
    public static boolean hasExplosiveComponent(Map<String, Double> contents) {
        for (String formula : contents.keySet()) {
            if (isExplosive(formula)) return true;
        }
        return false;
    }

    /**
     * Algorithmically estimate explosive power from elemental composition.
     * Uses oxygen balance and nitrogen content as primary indicators.
     */
    private static double computeExplosivePowerKJPerMol(String formula) {
        Map<Element, Integer> composition = FormulaParser.parse(formula);
        if (composition.isEmpty()) return 0.0;

        int nC = getCount(composition, "C");
        int nH = getCount(composition, "H");
        int nN = getCount(composition, "N");
        int nO = getCount(composition, "O");
        int nCl = getCount(composition, "Cl");
        int nMetal = 0;
        for (var entry : composition.entrySet()) {
            if (METAL_SYMBOLS.contains(entry.getKey().getSymbol())) {
                nMetal += entry.getValue();
            }
        }

        // Must contain nitrogen AND oxygen (or be an azide with N≥3)
        if (nN == 0) return 0.0;
        if (nO == 0 && nN < 3) return 0.0;

        // Calculate molecular weight
        double mw = Compound.computeMolarMass(formula);
        if (mw <= 0) return 0.0;

        // Oxygen balance: OB% = -1600 × (2C + H/2 + M - O) / MW
        double ob = -1600.0 * (2.0 * nC + nH / 2.0 + nMetal - nO) / mw;

        // Compounds with OB in range [-120, +40] may be explosive
        if (ob < -120.0 || ob > 40.0) return 0.0;

        // Nitrogen-to-weight ratio (higher = more energetic)
        double nRatio = (nN * 14.007) / mw;

        // Oxygen-to-weight ratio
        double oRatio = (nO * 15.999) / mw;

        // A rough heuristic: compounds with high N ratio and moderate OB are explosive
        // Minimum thresholds for explosive character
        if (nRatio < 0.10 && Math.abs(ob) > 80) return 0.0;

        // Estimate energy: empirically, explosive power correlates with
        // (N content × O availability × confinement)
        // Rough: kJ/mol ≈ 400 × nN × (1 + nO/(2C+H+1)) for organic explosives
        double energy = 400.0 * nN * (1.0 + (double) nO / (2.0 * nC + nH + 1.0));

        // Azide bonus (very energetic N-N bonds)
        if (nN >= 3 && nC == 0) {
            energy = Math.max(energy, 200.0 * nN);
        }

        // If energy is too low, not really explosive
        if (energy < 50.0) return 0.0;

        // Cap at reasonable maximum
        return Math.min(energy, 3000.0);
    }

    private static int getCount(Map<Element, Integer> composition, String symbol) {
        Element e = Element.fromSymbol(symbol);
        if (e == null) return 0;
        return composition.getOrDefault(e, 0);
    }
}
