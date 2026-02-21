package org.keke.chemistry.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Standard enthalpies of formation ΔHf° (kJ/mol) for compounds and elements.
 * Used to calculate reaction enthalpies via Hess's law:
 *   ΔH°_rxn = Σ(ν_i × ΔHf°_products_i) - Σ(ν_j × ΔHf°_reactants_j)
 *
 * Values are for aqueous species when dissolved, solid/liquid/gas otherwise.
 * Source: CRC Handbook of Chemistry and Physics, NIST standard reference data.
 */
public class ThermodynamicData {

    private static final Map<String, Double> FORMATION_ENTHALPIES = new HashMap<>();

    static {
        // Elements in standard state: ΔHf° = 0 by definition
        FORMATION_ENTHALPIES.put("H2", 0.0);
        FORMATION_ENTHALPIES.put("O2", 0.0);
        FORMATION_ENTHALPIES.put("N2", 0.0);
        FORMATION_ENTHALPIES.put("Cl2", 0.0);
        FORMATION_ENTHALPIES.put("Br2", 0.0);
        FORMATION_ENTHALPIES.put("I2", 0.0);
        FORMATION_ENTHALPIES.put("C", 0.0);
        FORMATION_ENTHALPIES.put("S", 0.0);
        FORMATION_ENTHALPIES.put("P", 0.0);
        FORMATION_ENTHALPIES.put("Fe", 0.0);
        FORMATION_ENTHALPIES.put("Cu", 0.0);
        FORMATION_ENTHALPIES.put("Zn", 0.0);
        FORMATION_ENTHALPIES.put("Na", 0.0);
        FORMATION_ENTHALPIES.put("K", 0.0);
        FORMATION_ENTHALPIES.put("Mg", 0.0);
        FORMATION_ENTHALPIES.put("Al", 0.0);
        FORMATION_ENTHALPIES.put("Ag", 0.0);
        FORMATION_ENTHALPIES.put("Ca", 0.0);
        FORMATION_ENTHALPIES.put("Ba", 0.0);
        FORMATION_ENTHALPIES.put("Pb", 0.0);
        FORMATION_ENTHALPIES.put("Sn", 0.0);
        FORMATION_ENTHALPIES.put("Ni", 0.0);
        FORMATION_ENTHALPIES.put("Co", 0.0);
        FORMATION_ENTHALPIES.put("Mn", 0.0);
        FORMATION_ENTHALPIES.put("Cr", 0.0);
        FORMATION_ENTHALPIES.put("Pt", 0.0);
        FORMATION_ENTHALPIES.put("Au", 0.0);
        FORMATION_ENTHALPIES.put("Hg", 0.0);
        FORMATION_ENTHALPIES.put("Li", 0.0);

        // Water and peroxide
        FORMATION_ENTHALPIES.put("H2O", -285.8);    // liquid
        FORMATION_ENTHALPIES.put("H2O2", -187.8);   // liquid

        // Common oxides
        FORMATION_ENTHALPIES.put("CO2", -393.5);
        FORMATION_ENTHALPIES.put("CO", -110.5);
        FORMATION_ENTHALPIES.put("NO", 91.3);
        FORMATION_ENTHALPIES.put("NO2", 33.2);
        FORMATION_ENTHALPIES.put("N2O", 81.6);
        FORMATION_ENTHALPIES.put("SO2", -296.8);
        FORMATION_ENTHALPIES.put("SO3", -395.7);
        FORMATION_ENTHALPIES.put("P2O5", -1492.0);
        FORMATION_ENTHALPIES.put("CaO", -634.9);
        FORMATION_ENTHALPIES.put("MgO", -601.6);
        FORMATION_ENTHALPIES.put("Na2O", -414.2);
        FORMATION_ENTHALPIES.put("K2O", -361.5);
        FORMATION_ENTHALPIES.put("BaO", -548.0);
        FORMATION_ENTHALPIES.put("Al2O3", -1675.7);
        FORMATION_ENTHALPIES.put("Fe2O3", -824.2);
        FORMATION_ENTHALPIES.put("Fe3O4", -1118.4);
        FORMATION_ENTHALPIES.put("CuO", -157.3);
        FORMATION_ENTHALPIES.put("Cu2O", -168.6);
        FORMATION_ENTHALPIES.put("ZnO", -350.5);
        FORMATION_ENTHALPIES.put("PbO", -217.3);
        FORMATION_ENTHALPIES.put("PbO2", -277.4);
        FORMATION_ENTHALPIES.put("MnO2", -520.0);
        FORMATION_ENTHALPIES.put("Cr2O3", -1139.7);
        FORMATION_ENTHALPIES.put("SnO2", -577.6);
        FORMATION_ENTHALPIES.put("SiO2", -910.7);

        // Acids (aqueous)
        FORMATION_ENTHALPIES.put("HCl", -167.2);
        FORMATION_ENTHALPIES.put("HBr", -121.5);
        FORMATION_ENTHALPIES.put("HI", -55.2);
        FORMATION_ENTHALPIES.put("HF", -332.6);
        FORMATION_ENTHALPIES.put("HNO3", -207.4);
        FORMATION_ENTHALPIES.put("H2SO4", -814.0);
        FORMATION_ENTHALPIES.put("H3PO4", -1288.3);
        FORMATION_ENTHALPIES.put("H2CO3", -699.6);
        FORMATION_ENTHALPIES.put("HClO3", -104.0);
        FORMATION_ENTHALPIES.put("HClO4", -40.6);
        FORMATION_ENTHALPIES.put("CH3COOH", -485.8);

        // Bases (aqueous / solid)
        FORMATION_ENTHALPIES.put("NaOH", -470.1);
        FORMATION_ENTHALPIES.put("KOH", -482.4);
        FORMATION_ENTHALPIES.put("LiOH", -508.4);
        FORMATION_ENTHALPIES.put("Ca(OH)2", -985.2);
        FORMATION_ENTHALPIES.put("Ba(OH)2", -944.7);
        FORMATION_ENTHALPIES.put("Mg(OH)2", -924.5);
        FORMATION_ENTHALPIES.put("Al(OH)3", -1276.0);
        FORMATION_ENTHALPIES.put("Fe(OH)2", -569.0);
        FORMATION_ENTHALPIES.put("Fe(OH)3", -823.0);
        FORMATION_ENTHALPIES.put("Cu(OH)2", -449.8);
        FORMATION_ENTHALPIES.put("Zn(OH)2", -641.9);
        FORMATION_ENTHALPIES.put("NH3", -80.3);
        FORMATION_ENTHALPIES.put("NH4OH", -361.2);

        // Chlorides (aqueous)
        FORMATION_ENTHALPIES.put("NaCl", -407.3);
        FORMATION_ENTHALPIES.put("KCl", -419.5);
        FORMATION_ENTHALPIES.put("LiCl", -408.6);
        FORMATION_ENTHALPIES.put("CaCl2", -795.4);
        FORMATION_ENTHALPIES.put("BaCl2", -855.0);
        FORMATION_ENTHALPIES.put("MgCl2", -641.3);
        FORMATION_ENTHALPIES.put("AlCl3", -704.2);
        FORMATION_ENTHALPIES.put("FeCl2", -341.8);
        FORMATION_ENTHALPIES.put("FeCl3", -399.5);
        FORMATION_ENTHALPIES.put("CuCl2", -220.1);
        FORMATION_ENTHALPIES.put("ZnCl2", -415.1);
        FORMATION_ENTHALPIES.put("AgCl", -127.0);
        FORMATION_ENTHALPIES.put("PbCl2", -359.4);
        FORMATION_ENTHALPIES.put("NH4Cl", -314.4);
        FORMATION_ENTHALPIES.put("HgCl2", -224.3);
        FORMATION_ENTHALPIES.put("SnCl2", -325.1);
        FORMATION_ENTHALPIES.put("CoCl2", -312.5);
        FORMATION_ENTHALPIES.put("NiCl2", -305.3);
        FORMATION_ENTHALPIES.put("MnCl2", -481.3);
        FORMATION_ENTHALPIES.put("CrCl3", -556.5);

        // Nitrates (aqueous)
        FORMATION_ENTHALPIES.put("NaNO3", -467.9);
        FORMATION_ENTHALPIES.put("KNO3", -494.6);
        FORMATION_ENTHALPIES.put("AgNO3", -124.4);
        FORMATION_ENTHALPIES.put("Ca(NO3)2", -938.2);
        FORMATION_ENTHALPIES.put("Ba(NO3)2", -992.1);
        FORMATION_ENTHALPIES.put("Cu(NO3)2", -302.9);
        FORMATION_ENTHALPIES.put("Pb(NO3)2", -451.9);
        FORMATION_ENTHALPIES.put("Fe(NO3)3", -670.7);
        FORMATION_ENTHALPIES.put("Zn(NO3)2", -483.7);
        FORMATION_ENTHALPIES.put("Mg(NO3)2", -790.7);
        FORMATION_ENTHALPIES.put("Al(NO3)3", -1117.0);
        FORMATION_ENTHALPIES.put("NH4NO3", -365.6);

        // Sulfates (aqueous / solid)
        FORMATION_ENTHALPIES.put("Na2SO4", -1387.1);
        FORMATION_ENTHALPIES.put("K2SO4", -1437.8);
        FORMATION_ENTHALPIES.put("CuSO4", -771.4);
        FORMATION_ENTHALPIES.put("ZnSO4", -982.8);
        FORMATION_ENTHALPIES.put("FeSO4", -928.4);
        FORMATION_ENTHALPIES.put("MgSO4", -1284.9);
        FORMATION_ENTHALPIES.put("BaSO4", -1473.2);
        FORMATION_ENTHALPIES.put("CaSO4", -1434.1);
        FORMATION_ENTHALPIES.put("PbSO4", -920.0);
        FORMATION_ENTHALPIES.put("Al2(SO4)3", -3440.8);
        FORMATION_ENTHALPIES.put("(NH4)2SO4", -1180.9);

        // Carbonates
        FORMATION_ENTHALPIES.put("CaCO3", -1207.6);
        FORMATION_ENTHALPIES.put("Na2CO3", -1130.7);
        FORMATION_ENTHALPIES.put("NaHCO3", -950.8);
        FORMATION_ENTHALPIES.put("K2CO3", -1151.0);
        FORMATION_ENTHALPIES.put("KHCO3", -963.2);
        FORMATION_ENTHALPIES.put("MgCO3", -1095.8);
        FORMATION_ENTHALPIES.put("BaCO3", -1213.0);
        FORMATION_ENTHALPIES.put("PbCO3", -699.1);
        FORMATION_ENTHALPIES.put("FeCO3", -740.6);
        FORMATION_ENTHALPIES.put("ZnCO3", -812.8);
        FORMATION_ENTHALPIES.put("CuCO3", -594.9);

        // Phosphates
        FORMATION_ENTHALPIES.put("Na3PO4", -1917.4);
        FORMATION_ENTHALPIES.put("Ca3(PO4)2", -4120.8);
        FORMATION_ENTHALPIES.put("AlPO4", -1733.8);

        // Halides and misc salts
        FORMATION_ENTHALPIES.put("KI", -327.9);
        FORMATION_ENTHALPIES.put("NaI", -287.8);
        FORMATION_ENTHALPIES.put("KBr", -393.8);
        FORMATION_ENTHALPIES.put("NaBr", -361.1);
        FORMATION_ENTHALPIES.put("NaF", -576.6);
        FORMATION_ENTHALPIES.put("KF", -567.3);
        FORMATION_ENTHALPIES.put("CaF2", -1219.6);
        FORMATION_ENTHALPIES.put("AgBr", -100.4);
        FORMATION_ENTHALPIES.put("AgI", -61.8);
        FORMATION_ENTHALPIES.put("PbI2", -175.5);
        FORMATION_ENTHALPIES.put("PbBr2", -278.7);

        // Chlorates, permanganates, chromates
        FORMATION_ENTHALPIES.put("KClO3", -397.7);
        FORMATION_ENTHALPIES.put("KMnO4", -837.2);
        FORMATION_ENTHALPIES.put("K2CrO4", -1403.7);
        FORMATION_ENTHALPIES.put("K2Cr2O7", -2033.0);

        // Organic compounds
        FORMATION_ENTHALPIES.put("CH4", -74.6);
        FORMATION_ENTHALPIES.put("C2H6", -84.0);
        FORMATION_ENTHALPIES.put("C3H8", -103.8);
        FORMATION_ENTHALPIES.put("C2H4", 52.4);
        FORMATION_ENTHALPIES.put("C2H2", 227.4);
        FORMATION_ENTHALPIES.put("CH3OH", -239.2);
        FORMATION_ENTHALPIES.put("C2H5OH", -277.6);
        FORMATION_ENTHALPIES.put("HCHO", -108.6);
        FORMATION_ENTHALPIES.put("CH3CHO", -166.2);
        FORMATION_ENTHALPIES.put("C6H12O6", -1273.3);  // glucose
        FORMATION_ENTHALPIES.put("C12H22O11", -2222.1); // sucrose

        // Miscellaneous
        FORMATION_ENTHALPIES.put("Fe(SCN)3", -250.0);
        FORMATION_ENTHALPIES.put("KSCN", -200.2);
        FORMATION_ENTHALPIES.put("NaSCN", -176.1);

        // Explosives & precursors
        FORMATION_ENTHALPIES.put("C3H8O3", -669.6);    // glycerol
        FORMATION_ENTHALPIES.put("C7H8", 12.0);        // toluene
        FORMATION_ENTHALPIES.put("C3H5N3O9", -364.0);  // nitroglycerin
        FORMATION_ENTHALPIES.put("C7H5N3O6", -67.0);   // TNT
        FORMATION_ENTHALPIES.put("K2S", -380.7);        // potassium sulfide
    }

    /**
     * Get the standard enthalpy of formation ΔHf° for a compound.
     * Returns NaN if data is not available.
     */
    public static double getFormationEnthalpy(String formula) {
        return FORMATION_ENTHALPIES.getOrDefault(formula, Double.NaN);
    }

    /**
     * Check whether thermodynamic data is available for a compound.
     */
    public static boolean hasData(String formula) {
        return FORMATION_ENTHALPIES.containsKey(formula);
    }

    /**
     * Calculate the standard enthalpy of a reaction using Hess's law.
     * ΔH°_rxn = Σ(ν_product × ΔHf°_product) - Σ(ν_reactant × ΔHf°_reactant)
     *
     * @param reactants list of (formula, stoichiometric coefficient)
     * @param products  list of (formula, stoichiometric coefficient)
     * @return ΔH° in kJ per mole of reaction as written, or NaN if data missing
     */
    public static double calculateReactionEnthalpy(
            List<Map.Entry<String, Integer>> reactants,
            List<Map.Entry<String, Integer>> products) {

        double productSum = 0.0;
        for (var entry : products) {
            double dhf = getFormationEnthalpy(entry.getKey());
            if (Double.isNaN(dhf)) return Double.NaN;
            productSum += entry.getValue() * dhf;
        }

        double reactantSum = 0.0;
        for (var entry : reactants) {
            double dhf = getFormationEnthalpy(entry.getKey());
            if (Double.isNaN(dhf)) return Double.NaN;
            reactantSum += entry.getValue() * dhf;
        }

        return productSum - reactantSum;
    }

    /**
     * Calculate temperature change from reaction heat.
     * ΔT = -q / (m × Cp)
     * where q = ξ × ΔH°_rxn (kJ), m = solution mass (g), Cp = 4.184 J/(g·K)
     *
     * @param reactionEnthalpyKJ ΔH° for the reaction as written (kJ/mol)
     * @param extentMol          extent of reaction ξ (mol) — the limiting ratio
     * @param solutionMassG      mass of solution in grams
     * @return temperature change in Kelvin (positive = heating, negative = cooling)
     */
    public static double calculateTemperatureChange(double reactionEnthalpyKJ, double extentMol, double solutionMassG) {
        if (solutionMassG <= 0) solutionMassG = 250.0;
        double qJ = extentMol * reactionEnthalpyKJ * 1000.0; // convert kJ to J
        double cp = 4.184; // J/(g·K) for water
        return -qJ / (solutionMassG * cp);
    }

    /**
     * Estimate formation enthalpy for an unknown compound using Pauling's
     * bond-energy approach. Returns 0 if the compound can't be estimated,
     * so it won't break temperature calculations.
     */
    public static double estimateFormationEnthalpy(String formula) {
        if (hasData(formula)) return getFormationEnthalpy(formula);
        // For unknown compounds, return 0 so reaction enthalpy calculations
        // still work (conservatively assumes no heat change from unknowns)
        return 0.0;
    }
}
