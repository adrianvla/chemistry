package org.keke.chemistry.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps specific ions to their known solution colors.
 * Cu²⁺ → blue, Fe³⁺ → yellow-brown, Fe²⁺ → pale green,
 * CrO₄²⁻ → yellow, MnO₄⁻ → purple, Co²⁺ → pink,
 * Ni²⁺ → green, Cr³⁺ → violet, etc.
 */
public class IonColor {
    private static final Map<String, Integer> ION_COLORS = new HashMap<>();

    static {
        // Cation colors (symbol → color in solution)
        ION_COLORS.put("Cu", 0x1E90FF);   // Cu²⁺ blue
        ION_COLORS.put("Fe", 0xB87333);   // Fe³⁺ yellow-brown
        ION_COLORS.put("Co", 0xFF69B4);   // Co²⁺ pink
        ION_COLORS.put("Ni", 0x00CC00);   // Ni²⁺ green
        ION_COLORS.put("Cr", 0x8A2BE2);   // Cr³⁺ violet
        ION_COLORS.put("Mn", 0x800080);   // MnO₄⁻ purple
        ION_COLORS.put("Au", 0xFFD700);   // Au³⁺ gold
        ION_COLORS.put("Br", 0xCC6600);   // Br₂ brown-orange
        ION_COLORS.put("I", 0x8B4513);    // I₂ brown
    }

    /**
     * Returns the ion color for a given element symbol, or -1 if the element is colorless.
     */
    public static int getIonColor(String symbol) {
        return ION_COLORS.getOrDefault(symbol, -1);
    }

    /**
     * Returns true if the element produces a colored ion in solution.
     */
    public static boolean hasColor(String symbol) {
        return ION_COLORS.containsKey(symbol);
    }
}
