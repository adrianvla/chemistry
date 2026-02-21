package org.keke.chemistry.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Central database of physical properties per compound: density, melting/boiling points.
 * Data sourced from NIST/CRC Handbook reference values.
 * For unknown compounds, estimation methods provide reasonable fallback values.
 */
public class CompoundPhysicalData {

    /**
     * Stores physical properties for a single compound.
     */
    public static class PhysicalEntry {
        public final double densityGPerMl;    // g/mL for solids/liquids, g/L for gases at STP (stored as g/mL internally)
        public final double meltingPointK;     // K (Double.NaN if decomposes or sublimes)
        public final double boilingPointK;     // K (Double.NaN if decomposes before boiling)
        public final double heatOfFusionKJ;    // kJ/mol (latent heat of melting)
        public final double heatOfVapKJ;       // kJ/mol (latent heat of vaporization)

        public PhysicalEntry(double densityGPerMl, double meltingPointK, double boilingPointK,
                             double heatOfFusionKJ, double heatOfVapKJ) {
            this.densityGPerMl = densityGPerMl;
            this.meltingPointK = meltingPointK;
            this.boilingPointK = boilingPointK;
            this.heatOfFusionKJ = heatOfFusionKJ;
            this.heatOfVapKJ = heatOfVapKJ;
        }

        public PhysicalEntry(double densityGPerMl, double meltingPointK, double boilingPointK) {
            this(densityGPerMl, meltingPointK, boilingPointK, Double.NaN, Double.NaN);
        }
    }

    private static final Map<String, PhysicalEntry> DATA = new HashMap<>();

    static {
        // ── Elements (standard state forms) ────────────────────────────
        //                         density  mp(K)   bp(K)   ΔHfus   ΔHvap
        put("H2",    0.00009, 14.01,  20.28,  0.117, 0.904);
        put("He",    0.000164, 0.95,   4.22,  0.021, 0.083);
        put("Li",    0.534,  453.65, 1603.0, 3.00,  136.0);
        put("Be",    1.85,   1560.0, 2742.0, 7.90,  297.0);
        put("B",     2.34,   2349.0, 4200.0, 50.2,  508.0);
        put("C",     2.27,   3823.0, 4098.0, 117.0,  715.0);  // graphite; sublimes
        put("N2",    0.00125, 63.15,  77.36,  0.720,  5.57);
        put("O2",    0.00143, 54.36,  90.20,  0.444,  6.82);
        put("F2",    0.0017, 53.48,  85.03,  0.510,  6.62);
        put("Ne",    0.0009, 24.56,  27.07,  0.335,  1.71);
        put("Na",    0.968,  370.95, 1156.0, 2.60,  97.4);
        put("Mg",    1.738,  923.0,  1363.0, 8.48,  128.0);
        put("Al",    2.70,   933.47, 2792.0, 10.7,  284.0);
        put("Si",    2.33,   1687.0, 3538.0, 50.2,  383.0);
        put("P",     1.82,   317.3,  553.7,  0.66,  12.4);   // white phosphorus
        put("S",     2.07,   388.36, 717.8,  1.73,  45.0);
        put("Cl2",   0.00321, 171.6,  239.11, 6.41,  20.4);
        put("Ar",    0.00178, 83.81, 87.30,  1.18,  6.43);
        put("K",     0.862,  336.7,  1032.0, 2.33,  76.9);
        put("Ca",    1.55,   1115.0, 1757.0, 8.54,  155.0);
        put("Fe",    7.874,  1811.0, 3134.0, 13.8,  340.0);
        put("Co",    8.90,   1768.0, 3200.0, 16.2,  377.0);
        put("Ni",    8.908,  1728.0, 3186.0, 17.5,  370.0);
        put("Cu",    8.96,   1357.8, 2835.0, 13.3,  300.0);
        put("Zn",    7.14,   692.7,  1180.0, 7.07,  115.0);
        put("Br2",   3.12,   265.8,  332.0,  10.6,  29.6);
        put("Kr",    0.00375, 115.8,  119.9,  1.64,  9.08);
        put("Rb",    1.532,  312.5,  961.0,  2.19,  69.0);
        put("Sr",    2.64,   1050.0, 1655.0, 7.43,  137.0);
        put("Ag",    10.49,  1234.9, 2435.0, 11.3,  254.0);
        put("Cd",    8.65,   594.2,  1040.0, 6.21,  99.6);
        put("Sn",    7.265,  505.1,  2875.0, 7.03,  296.0);
        put("Sb",    6.697,  903.8,  1860.0, 19.9,  193.0);
        put("I2",    4.933,  386.9,  457.6,  15.5,  41.6);
        put("Xe",    0.00589, 161.4,  165.1,  2.27,  12.6);
        put("Cs",    1.93,   301.6,  944.0,  2.09,  63.9);
        put("Ba",    3.51,   1000.0, 2118.0, 7.12,  142.0);
        put("W",     19.25,  3695.0, 5828.0, 52.3,  807.0);
        put("Pt",    21.45,  2041.4, 4098.0, 22.2,  510.0);
        put("Au",    19.30,  1337.3, 3129.0, 12.6,  334.0);
        put("Hg",    13.534, 234.3,  629.9,  2.29,  59.1);
        put("Pb",    11.34,  600.6,  2022.0, 4.77,  178.0);
        put("Bi",    9.78,   544.6,  1837.0, 11.3,  151.0);
        put("Mn",    7.21,   1519.0, 2334.0, 12.9,  221.0);
        put("Cr",    7.19,   2180.0, 2944.0, 21.0,  347.0);
        put("Ti",    4.507,  1941.0, 3560.0, 14.2,  425.0);
        put("V",     6.11,   2183.0, 3680.0, 21.5,  459.0);
        put("Ga",    5.91,   302.9,  2477.0, 5.59,  254.0);
        put("Ge",    5.323,  1211.4, 3106.0, 36.9,  334.0);
        put("As",    5.727,  1090.0, 887.0,  24.4,  34.8);  // sublimes
        put("Se",    4.81,   494.0,  958.0,  6.69,  95.5);

        // ── Water and peroxides ────────────────────────────────────────
        put("H2O",   1.00,   273.15, 373.15, 6.01,  40.7);
        put("H2O2",  1.45,   272.7,  423.4,  12.5,  51.6);

        // ── Common oxides ──────────────────────────────────────────────
        put("CO2",   0.00198, 216.6,  194.7);  // sublimes at 194.7K (1 atm)
        put("CO",    0.00125, 68.1,   81.6);
        put("NO",    0.00134, 109.5,  121.4);
        put("NO2",   0.00190, 261.9,  294.3);
        put("N2O",   0.00198, 182.3,  184.7);
        put("SO2",   0.00263, 201.0,  263.1);
        put("SO3",   1.92,   289.9,  318.0);
        put("P2O5",  2.39,   613.0,  633.0);
        put("CaO",   3.34,   2886.0, 3123.0);
        put("MgO",   3.58,   3125.0, 3873.0);
        put("Na2O",  2.27,   1405.0, 2223.0);
        put("K2O",   2.32,   1013.0, Double.NaN);  // decomposes
        put("BaO",   5.72,   2196.0, 2273.0);
        put("Al2O3", 3.95,   2345.0, 3250.0);
        put("Fe2O3", 5.24,   1838.0, Double.NaN);  // decomposes at ~1839K
        put("Fe3O4", 5.17,   1870.0, Double.NaN);
        put("CuO",   6.31,   1599.0, Double.NaN);
        put("Cu2O",  6.0,    1505.0, 2073.0);
        put("ZnO",   5.61,   2248.0, Double.NaN);
        put("PbO",   9.53,   1161.0, 1750.0);
        put("PbO2",  9.38,   563.0,  Double.NaN);
        put("MnO2",  5.03,   808.0,  Double.NaN);  // decomposes
        put("Cr2O3", 5.22,   2708.0, 4273.0);
        put("SnO2",  6.95,   1903.0, 2173.0);
        put("SiO2",  2.65,   1986.0, 2503.0);

        // ── Acids ──────────────────────────────────────────────────────
        put("HCl",   1.19,   247.0,  383.0);   // hydrochloric acid (aqueous, ~37%)
        put("HBr",   0.00363, 186.3,  206.8);
        put("HI",    0.00566, 222.4,  237.8);
        put("HF",    0.00082, 189.6,  292.7);
        put("HNO3",  1.51,   231.0,  356.0);
        put("H2SO4", 1.84,   283.5,  610.0);
        put("H3PO4", 1.88,   315.5,  431.0);
        put("H2CO3", 1.0,    273.0,  Double.NaN);  // unstable, decomposes
        put("HClO3", 1.0,    273.0,  Double.NaN);
        put("HClO4", 1.76,   171.0,  376.0);
        put("CH3COOH", 1.049, 289.8, 391.1, 11.7, 23.7);

        // ── Bases ──────────────────────────────────────────────────────
        put("NaOH",  2.13,   596.0,  1661.0);
        put("KOH",   2.12,   679.0,  1600.0);
        put("LiOH",  1.46,   744.0,  1197.0);
        put("Ca(OH)2", 2.21, 853.0,  Double.NaN);  // decomposes
        put("Ba(OH)2", 3.74, 681.0,  Double.NaN);
        put("Mg(OH)2", 2.34, 623.0,  Double.NaN);
        put("Al(OH)3", 2.42, 573.0,  Double.NaN);
        put("Fe(OH)2", 3.4,  423.0,  Double.NaN);
        put("Fe(OH)3", 3.12, 408.0,  Double.NaN);
        put("Cu(OH)2", 3.37, 353.0,  Double.NaN);
        put("Zn(OH)2", 3.05, 398.0,  Double.NaN);
        put("NH3",   0.00073, 195.4, 239.8, 5.66, 23.3);
        put("NH4OH", 0.90,   237.0,  311.0);

        // ── Chlorides ──────────────────────────────────────────────────
        put("NaCl",  2.17,   1074.0, 1686.0, 28.2, 170.0);
        put("KCl",   1.98,   1043.0, 1693.0, 26.3, 167.0);
        put("LiCl",  2.07,   878.0,  1655.0);
        put("CaCl2", 2.15,   1048.0, 2208.0);
        put("BaCl2", 3.86,   1234.0, 2100.0);
        put("MgCl2", 2.32,   987.0,  1685.0);
        put("AlCl3", 2.48,   465.7,  453.0);  // sublimes
        put("FeCl2", 3.16,   950.0,  1296.0);
        put("FeCl3", 2.90,   580.0,  588.0);  // sublimes
        put("CuCl2", 3.39,   871.0,  1266.0);
        put("ZnCl2", 2.91,   566.0,  1005.0);
        put("AgCl",  5.56,   728.0,  1820.0);
        put("PbCl2", 5.85,   774.0,  1223.0);
        put("NH4Cl", 1.53,   611.0,  520.0);  // sublimes at ~520K
        put("HgCl2", 5.43,   549.0,  577.0);
        put("SnCl2", 3.95,   520.0,  896.0);
        put("CoCl2", 3.36,   1008.0, 1322.0);
        put("NiCl2", 3.55,   1274.0, 1260.0);
        put("MnCl2", 2.98,   923.0,  1498.0);
        put("CrCl3", 2.87,   1425.0, 1573.0);

        // ── Nitrates ───────────────────────────────────────────────────
        put("NaNO3",  2.26,  581.0,  653.0);
        put("KNO3",   2.11,  607.0,  673.0);
        put("AgNO3",  4.35,  485.0,  713.0);
        put("Ca(NO3)2", 2.50, 834.0, Double.NaN);  // decomposes
        put("Ba(NO3)2", 3.24, 863.0, Double.NaN);
        put("Cu(NO3)2", 3.05, 529.0, 443.0);
        put("Pb(NO3)2", 4.53, 543.0, Double.NaN);
        put("Fe(NO3)3", 1.68, 320.0, Double.NaN);
        put("Zn(NO3)2", 2.07, 383.0, Double.NaN);
        put("Mg(NO3)2", 2.30, 362.0, Double.NaN);
        put("Al(NO3)3", 1.72, 346.0, Double.NaN);
        put("NH4NO3",  1.73,  442.8,  483.0);

        // ── Sulfates ───────────────────────────────────────────────────
        put("Na2SO4", 2.66,  1157.0, Double.NaN);
        put("K2SO4",  2.66,  1342.0, 1962.0);
        put("CuSO4",  3.60,  833.0,  Double.NaN);
        put("ZnSO4",  3.54,  953.0,  Double.NaN);
        put("FeSO4",  2.84,  953.0,  Double.NaN);
        put("MgSO4",  2.66,  1397.0, Double.NaN);
        put("BaSO4",  4.50,  1853.0, Double.NaN);
        put("CaSO4",  2.96,  1733.0, Double.NaN);
        put("PbSO4",  6.29,  1360.0, Double.NaN);
        put("Al2(SO4)3", 2.71, 1043.0, Double.NaN);
        put("(NH4)2SO4", 1.77, 508.0, Double.NaN);

        // ── Carbonates ─────────────────────────────────────────────────
        put("CaCO3",  2.71,  1612.0, Double.NaN);  // decomposes ~1098K
        put("Na2CO3", 2.54,  1124.0, Double.NaN);
        put("NaHCO3", 2.20,  323.0,  Double.NaN);  // decomposes ~323K
        put("K2CO3",  2.43,  1164.0, Double.NaN);
        put("KHCO3",  2.17,  373.0,  Double.NaN);
        put("MgCO3",  3.05,  623.0,  Double.NaN);  // decomposes
        put("BaCO3",  4.29,  1084.0, Double.NaN);
        put("PbCO3",  6.58,  588.0,  Double.NaN);
        put("FeCO3",  3.96,  588.0,  Double.NaN);
        put("ZnCO3",  4.40,  573.0,  Double.NaN);
        put("CuCO3",  4.0,   473.0,  Double.NaN);

        // ── Phosphates ─────────────────────────────────────────────────
        put("Na3PO4",    2.54, 1856.0, Double.NaN);
        put("Ca3(PO4)2", 3.14, 1943.0, Double.NaN);
        put("AlPO4",     2.57, 1873.0, Double.NaN);

        // ── Halides (non-chloride) ─────────────────────────────────────
        put("KI",    3.12,  954.0,  1603.0);
        put("NaI",   3.67,  934.0,  1577.0);
        put("KBr",   2.74,  1007.0, 1708.0);
        put("NaBr",  3.21,  1028.0, 1663.0);
        put("NaF",   2.56,  1269.0, 1977.0);
        put("KF",    2.48,  1131.0, 1778.0);
        put("CaF2",  3.18,  1691.0, 2806.0);
        put("AgBr",  6.47,  705.0,  1775.0);
        put("AgI",   5.68,  831.0,  1779.0);
        put("PbI2",  6.16,  683.0,  1145.0);
        put("PbBr2", 6.66,  644.0,  1165.0);

        // ── Chlorates, permanganates, chromates ────────────────────────
        put("KClO3", 2.32,  629.0,  673.0);
        put("KMnO4", 2.70,  513.0,  Double.NaN);
        put("K2CrO4", 2.73, 1241.0, Double.NaN);
        put("K2Cr2O7", 2.68, 671.0, 773.0);

        // ── Organic compounds ──────────────────────────────────────────
        put("CH4",    0.000656, 90.7,  111.7, 0.94, 8.18);
        put("C2H6",   0.00131,  90.4,  184.6, 2.86, 14.7);
        put("C3H8",   0.00202,  85.5,  231.1, 3.52, 19.0);
        put("C2H4",   0.00117,  104.0, 169.4, 3.35, 13.5);
        put("C2H2",   0.00107,  192.3, 189.3, 3.77, 16.0);  // sublimes
        put("CH3OH",  0.792,    175.5, 337.7, 3.16, 35.2);
        put("C2H5OH", 0.789,    159.1, 351.4, 4.93, 38.6);
        put("HCHO",   0.000815, 181.0, 254.0);  // formaldehyde, gas at STP
        put("CH3CHO", 0.788,    150.2, 293.3);
        put("C6H12O6", 1.54,   419.0, Double.NaN);   // glucose decomposes
        put("C12H22O11", 1.55, 459.0, Double.NaN);   // sucrose decomposes

        // ── Misc ───────────────────────────────────────────────────────
        put("Fe(SCN)3", 1.5,  473.0,  Double.NaN);  // estimate
        put("KSCN",    1.886, 446.0,  773.0);
        put("NaSCN",   1.735, 596.0,  Double.NaN);

        // ── Explosives & precursors ────────────────────────────────────
        put("C3H8O3",   1.261, 291.0, 563.0,  18.3, 91.7);  // glycerol
        put("C7H8",     0.867, 178.2, 383.8,  6.64, 33.2);  // toluene
        put("C3H5N3O9", 1.599, 286.2, Double.NaN);           // nitroglycerin (decomposes)
        put("C7H5N3O6", 1.654, 354.0, 513.0);               // TNT
        put("K2S",      1.80,  1113.0, Double.NaN);          // potassium sulfide

        // ── Azides ─────────────────────────────────────────────────────
        put("Ba(N3)2", 2.94,  593.0,  Double.NaN);  // decomposes ~320°C
        put("NaN3",    1.846, 548.0,  Double.NaN);   // decomposes
        put("Pb(N3)2", 4.71,  623.0,  Double.NaN);   // decomposes (explosive)

        // ── Silane & Mg2Si ─────────────────────────────────────────────
        put("SiH4",    0.00068, 88.5, 161.0);  // gas at STP
        put("Mg2Si",   1.94,   1375.0, Double.NaN);
    }

    private static void put(String formula, double density, double mpK, double bpK) {
        DATA.put(formula, new PhysicalEntry(density, mpK, bpK));
    }

    private static void put(String formula, double density, double mpK, double bpK,
                             double hFus, double hVap) {
        DATA.put(formula, new PhysicalEntry(density, mpK, bpK, hFus, hVap));
    }

    // ── Public API ─────────────────────────────────────────────────────

    /**
     * Get the state of matter for a compound at a given temperature.
     * Uses melting and boiling points from the database.
     * Falls back to estimation if the compound is unknown.
     */
    public static StateOfMatter getStateAtTemperature(String formula, double tempK) {
        PhysicalEntry entry = DATA.get(formula);
        if (entry != null) {
            return stateFromEntry(entry, tempK);
        }
        // Fallback: estimate from composition
        PhysicalEntry estimated = estimateProperties(formula);
        return stateFromEntry(estimated, tempK);
    }

    private static StateOfMatter stateFromEntry(PhysicalEntry entry, double tempK) {
        double mp = entry.meltingPointK;
        double bp = entry.boilingPointK;

        // If boiling point is NaN (decomposes), treat as solid/liquid only
        if (Double.isNaN(bp)) {
            if (Double.isNaN(mp)) return StateOfMatter.SOLID;
            return tempK < mp ? StateOfMatter.SOLID : StateOfMatter.LIQUID;
        }
        if (Double.isNaN(mp)) {
            // Sublimation case: goes directly solid→gas
            return tempK < bp ? StateOfMatter.SOLID : StateOfMatter.GAS;
        }

        // Handle compounds where bp < mp (sublimation, e.g. CO2, I2 at 1 atm)
        if (bp < mp) {
            return tempK < bp ? StateOfMatter.SOLID : StateOfMatter.GAS;
        }

        if (tempK < mp) return StateOfMatter.SOLID;
        if (tempK < bp) return StateOfMatter.LIQUID;
        return StateOfMatter.GAS;
    }

    /**
     * Get the density of a compound in g/mL.
     * For gases, returns a small number (~0.001 g/mL range).
     */
    public static double getDensity(String formula, StateOfMatter state) {
        PhysicalEntry entry = DATA.get(formula);
        if (entry != null) {
            return adjustDensityForState(entry, state);
        }
        PhysicalEntry estimated = estimateProperties(formula);
        return adjustDensityForState(estimated, state);
    }

    /**
     * Get the density of a compound at a given temperature (determines state automatically).
     */
    public static double getDensity(String formula, double tempK) {
        StateOfMatter state = getStateAtTemperature(formula, tempK);
        return getDensity(formula, state);
    }

    /**
     * Get the melting point in Kelvin, or NaN if unknown/decomposes.
     */
    public static double getMeltingPointK(String formula) {
        PhysicalEntry entry = DATA.get(formula);
        if (entry != null) return entry.meltingPointK;
        return estimateProperties(formula).meltingPointK;
    }

    /**
     * Get the boiling point in Kelvin, or NaN if unknown/decomposes.
     */
    public static double getBoilingPointK(String formula) {
        PhysicalEntry entry = DATA.get(formula);
        if (entry != null) return entry.boilingPointK;
        return estimateProperties(formula).boilingPointK;
    }

    /**
     * Get heat of fusion in kJ/mol, or NaN if unknown.
     */
    public static double getHeatOfFusionKJ(String formula) {
        PhysicalEntry entry = DATA.get(formula);
        if (entry != null) return entry.heatOfFusionKJ;
        return Double.NaN;
    }

    /**
     * Get heat of vaporization in kJ/mol, or NaN if unknown.
     */
    public static double getHeatOfVaporizationKJ(String formula) {
        PhysicalEntry entry = DATA.get(formula);
        if (entry != null) return entry.heatOfVapKJ;
        return Double.NaN;
    }

    /**
     * Check if physical data is available for this compound.
     */
    public static boolean hasData(String formula) {
        return DATA.containsKey(formula);
    }

    /**
     * Get the raw PhysicalEntry, or null.
     */
    public static PhysicalEntry getEntry(String formula) {
        return DATA.get(formula);
    }

    // ── Estimation for unknown compounds ───────────────────────────────

    /**
     * Adjusts the stored density based on state of matter.
     * Solid/liquid use stored density; gas uses ideal-gas-like approximation.
     */
    private static double adjustDensityForState(PhysicalEntry entry, StateOfMatter state) {
        if (state == StateOfMatter.GAS) {
            // If stored density is already gas-like (< 0.01), use it
            if (entry.densityGPerMl < 0.01) {
                return entry.densityGPerMl;
            }
            // Otherwise estimate gas density from molar mass (~M/24400 at STP)
            // Very rough but prevents liquid density from being used for gas phase
            return entry.densityGPerMl * 0.001;
        }
        return entry.densityGPerMl;
    }

    /**
     * Estimate physical properties for compounds not in the database.
     * Uses additive molar volume method: V_m ≈ Σ(atom_contributions).
     * Provides rough but reasonable values for any dynamically-created compound.
     */
    public static PhysicalEntry estimateProperties(String formula) {
        // Check single-element case
        Element singleElement = Element.fromSymbol(formula);
        if (singleElement != null) {
            // Pure element not in database — estimate from atomic number
            double mm = singleElement.getMolarMass();
            int z = singleElement.getAtomicNumber();
            double estDensity = estimateElementDensity(z, mm);
            double estMp = estimateElementMeltingPoint(z);
            double estBp = estMp * 1.5;  // rough ratio
            return new PhysicalEntry(estDensity, estMp, estBp);
        }

        // Parse formula and estimate from composition
        Map<Element, Integer> composition = FormulaParser.parse(formula);
        if (composition.isEmpty()) {
            // Unparseable — return water-like defaults
            return new PhysicalEntry(1.0, 273.15, 373.15);
        }

        double totalMolarMass = Compound.computeMolarMass(formula);
        if (totalMolarMass <= 0) totalMolarMass = 100.0;

        // Additive molar volume method
        double estimatedMolarVolume = 0;
        int totalAtoms = 0;
        boolean hasCarbon = false;
        boolean hasHydrogen = false;
        boolean hasMetal = false;

        for (var entry : composition.entrySet()) {
            Element el = entry.getKey();
            int count = entry.getValue();
            totalAtoms += count;
            if (el == Element.C) hasCarbon = true;
            if (el == Element.H) hasHydrogen = true;
            if (isMetalElement(el)) hasMetal = true;

            // Approximate atomic volume contributions (mL/mol)
            estimatedMolarVolume += count * getAtomicVolumeContribution(el);
        }

        double estDensity;
        if (estimatedMolarVolume > 0) {
            estDensity = totalMolarMass / estimatedMolarVolume;
        } else {
            estDensity = 1.0;
        }

        // Estimate melting/boiling from molecular character
        double estMp, estBp;
        if (hasMetal && !hasCarbon) {
            // Ionic compound: high melting point
            estMp = 500 + totalMolarMass * 2;
            estBp = estMp + 500;
        } else if (hasCarbon && hasHydrogen) {
            // Organic: lower melting point, scales with molar mass
            estMp = 150 + totalMolarMass * 0.5;
            estBp = estMp + 80 + totalMolarMass * 0.3;
        } else {
            // Covalent inorganic
            estMp = 200 + totalMolarMass;
            estBp = estMp + 100 + totalMolarMass * 0.5;
        }

        return new PhysicalEntry(estDensity, estMp, estBp);
    }

    private static double getAtomicVolumeContribution(Element el) {
        // Approximate molar volume contributions in mL/mol per atom
        // Based on typical atomic radii and packing
        return switch (el.getSymbol()) {
            case "H" -> 3.7;
            case "C" -> 8.0;
            case "N" -> 6.5;
            case "O" -> 5.5;
            case "F" -> 5.0;
            case "Cl" -> 12.0;
            case "Br" -> 15.5;
            case "I" -> 20.0;
            case "S" -> 12.0;
            case "P" -> 11.0;
            case "Si" -> 12.0;
            case "Na" -> 10.0;
            case "K" -> 18.0;
            case "Ca" -> 13.0;
            case "Mg" -> 9.5;
            case "Al" -> 10.0;
            case "Fe" -> 7.1;
            case "Cu" -> 7.1;
            case "Zn" -> 9.2;
            case "Ag" -> 10.3;
            case "Ba" -> 20.0;
            case "Pb" -> 18.3;
            default -> 10.0;  // generic fallback
        };
    }

    private static double estimateElementDensity(int z, double molarMass) {
        // Rough estimation based on periodic trends
        if (z <= 2) return 0.001;  // gases
        if (z <= 10) return molarMass / 20.0;
        if (z <= 18) return molarMass / 15.0;
        return molarMass / 10.0;
    }

    private static double estimateElementMeltingPoint(int z) {
        if (z <= 2) return 20.0;
        if (z <= 10) return 200.0 + z * 50;
        if (z <= 18) return 300.0 + z * 30;
        if (z <= 36) return 500.0 + z * 20;
        return 600.0 + z * 15;
    }

    private static boolean isMetalElement(Element el) {
        String sym = el.getSymbol();
        // Non-metals and metalloids
        return switch (sym) {
            case "H", "He", "C", "N", "O", "F", "Ne", "P", "S", "Cl", "Ar",
                 "Se", "Br", "Kr", "I", "Xe", "At", "Rn", "Og",
                 "B", "Si", "Ge", "As", "Sb", "Te" -> false;
            default -> true;
        };
    }
}
