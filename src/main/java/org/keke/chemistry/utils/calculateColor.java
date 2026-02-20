package org.keke.chemistry.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Computes solution colors from chemical formulas using ion-color-weighted blending
 * with known-compound overrides.
 *
 * Color calculation uses molar-fraction-weighted average in linear RGB space:
 * color_channel = sum(n_i * c_i_channel) / sum(n_i)
 *
 * Colorless compounds (all colorless ions) return a slight blue tint (like water).
 */
public class CalculateColor {

    // Override map for well-known compound colors
    private static final Map<String, Integer> COMPOUND_OVERRIDES = new HashMap<>();

    static {
        COMPOUND_OVERRIDES.put("KMnO4", 0x7B006B);    // deep purple
        COMPOUND_OVERRIDES.put("CuSO4", 0x0066CC);    // vivid blue
        COMPOUND_OVERRIDES.put("FeCl3", 0xCC7700);    // amber
        COMPOUND_OVERRIDES.put("FeCl2", 0x90EE90);    // pale green
        COMPOUND_OVERRIDES.put("CuCl2", 0x00BFFF);    // sky blue
        COMPOUND_OVERRIDES.put("Cu(NO3)2", 0x0088CC); // blue
        COMPOUND_OVERRIDES.put("CoCl2", 0xFF69B4);    // pink
        COMPOUND_OVERRIDES.put("NiCl2", 0x00CC00);    // green
        COMPOUND_OVERRIDES.put("K2CrO4", 0xFFFF00);   // yellow
        COMPOUND_OVERRIDES.put("K2Cr2O7", 0xFF8C00);  // orange
        COMPOUND_OVERRIDES.put("CuO", 0x000000);      // black
        COMPOUND_OVERRIDES.put("Fe2O3", 0x8B0000);    // dark red/rust
        COMPOUND_OVERRIDES.put("AgNO3", 0xFFFFFF);    // colorless
        COMPOUND_OVERRIDES.put("H2O", 0xD0E8FF);      // slight blue tint
        COMPOUND_OVERRIDES.put("NaCl", 0xD0E8FF);     // colorless in solution
        COMPOUND_OVERRIDES.put("HCl", 0xD0E8FF);      // colorless
        COMPOUND_OVERRIDES.put("NaOH", 0xD0E8FF);     // colorless
        COMPOUND_OVERRIDES.put("H2SO4", 0xD0E8FF);    // colorless
        COMPOUND_OVERRIDES.put("HNO3", 0xD0E8FF);     // colorless
        COMPOUND_OVERRIDES.put("KOH", 0xD0E8FF);      // colorless
        COMPOUND_OVERRIDES.put("Ca(OH)2", 0xD0E8FF);  // colorless
        COMPOUND_OVERRIDES.put("NH3", 0xD0E8FF);      // colorless
        COMPOUND_OVERRIDES.put("BaCl2", 0xD0E8FF);    // colorless
    }

    /**
     * Calculate the solution color for a chemical formula.
     */
    public static int calculateColor(String formula) {
        if (formula == null || formula.isEmpty()) {
            return 0x00000000; // transparent/empty
        }

        // Check override map first
        if (COMPOUND_OVERRIDES.containsKey(formula)) {
            return COMPOUND_OVERRIDES.get(formula);
        }

        // Parse formula and compute weighted average
        Map<Element, Integer> composition = FormulaParser.parse(formula);
        if (composition.isEmpty()) {
            return 0xD0E8FF;
        }

        // Check if compound contains any colored ion
        boolean hasColoredIon = false;
        for (Element e : composition.keySet()) {
            if (IonColor.hasColor(e.getSymbol())) {
                hasColoredIon = true;
                break;
            }
        }

        if (!hasColoredIon) {
            return 0xD0E8FF; // colorless → slight blue tint like water
        }

        // Weighted average of ion colors in linear RGB space
        double totalR = 0, totalG = 0, totalB = 0;
        int totalCount = 0;

        for (Map.Entry<Element, Integer> entry : composition.entrySet()) {
            Element elem = entry.getKey();
            int count = entry.getValue();
            int color = elem.getIonColorRgb();

            // Only include colored ions in the weighted average
            if (IonColor.hasColor(elem.getSymbol())) {
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;

                totalR += count * r;
                totalG += count * g;
                totalB += count * b;
                totalCount += count;
            }
        }

        if (totalCount == 0) {
            return 0xD0E8FF;
        }

        int r = Math.min(255, (int) (totalR / totalCount));
        int g = Math.min(255, (int) (totalG / totalCount));
        int b = Math.min(255, (int) (totalB / totalCount));

        return (r << 16) | (g << 8) | b;
    }

    /**
     * Calculate color with concentration-dependent intensity.
     * Dilute solutions have lighter/more transparent colors.
     *
     * @param formula  the chemical formula
     * @param moles    amount of substance in moles
     * @param volumeMl volume in milliliters
     * @return color int with concentration-dependent intensity
     */
    public static int calculateColorWithConcentration(String formula, double moles, double volumeMl) {
        int baseColor = calculateColor(formula);
        if (baseColor == 0x00000000 || baseColor == 0xD0E8FF) {
            return baseColor;
        }

        double volumeL = volumeMl / 1000.0;
        if (volumeL <= 0) volumeL = 0.05; // default 50mL
        double concentration = moles / volumeL;
        double cMax = 1.0; // reference concentration (1 mol/L)
        double intensity = Math.min(1.0, concentration / cMax);

        // Blend towards white (diluted) based on intensity
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;

        r = (int) (255 - intensity * (255 - r));
        g = (int) (255 - intensity * (255 - g));
        b = (int) (255 - intensity * (255 - b));

        return (r << 16) | (g << 8) | b;
    }

    /**
     * Calculate blended color for a multi-reactant beaker.
     * Each formula's color is weighted by its mole fraction.
     *
     * @param contents map of formula → moles
     * @return blended color int
     */
    public static int calculateBlendedColor(Map<String, Double> contents) {
        if (contents == null || contents.isEmpty()) {
            return 0x00000000;
        }
        if (contents.size() == 1) {
            var entry = contents.entrySet().iterator().next();
            return calculateColorWithConcentration(entry.getKey(), entry.getValue(), 50.0);
        }

        double totalMoles = 0;
        for (double v : contents.values()) totalMoles += v;
        if (totalMoles <= 0) return 0x00000000;

        double totalR = 0, totalG = 0, totalB = 0;

        for (var entry : contents.entrySet()) {
            int color = calculateColor(entry.getKey());
            double weight = entry.getValue() / totalMoles;

            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            totalR += weight * r;
            totalG += weight * g;
            totalB += weight * b;
        }

        int r = Math.min(255, (int) totalR);
        int g = Math.min(255, (int) totalG);
        int b = Math.min(255, (int) totalB);

        return (r << 16) | (g << 8) | b;
    }
}
