package org.keke.chemistry.utils;

import java.util.*;

/**
 * Metal activity (reactivity) series for predicting single-replacement reactions.
 * A more reactive metal will displace a less reactive metal from its compound.
 *
 * Li > K > Ba > Ca > Na > Mg > Al > Zn > Fe > Ni > Sn > Pb > H > Cu > Hg > Ag > Pt > Au
 *
 * For a reaction M + XY → MY + X:
 * M must be higher (more reactive) than X in the series.
 */
public class ActivitySeries {

    // Ordered from most reactive (index 0) to least reactive
    private static final List<String> SERIES = List.of(
            "Li", "K", "Ba", "Ca", "Na",
            "Mg", "Al", "Zn", "Cr", "Fe",
            "Co", "Ni", "Sn", "Pb", "H",
            "Cu", "Hg", "Ag", "Pt", "Au"
    );

    private static final Map<String, Integer> REACTIVITY_INDEX = new HashMap<>();

    static {
        for (int i = 0; i < SERIES.size(); i++) {
            REACTIVITY_INDEX.put(SERIES.get(i), i);
        }
    }

    /**
     * Check if metal A is more reactive than metal B.
     * A more reactive metal has a lower index in the activity series.
     *
     * @param metalA the displacing metal
     * @param metalB the metal being displaced
     * @return true if metalA can displace metalB
     */
    public static boolean isMoreReactive(String metalA, String metalB) {
        Integer indexA = REACTIVITY_INDEX.get(metalA);
        Integer indexB = REACTIVITY_INDEX.get(metalB);
        if (indexA == null || indexB == null) return false;
        return indexA < indexB;
    }

    /**
     * Check if a metal can displace hydrogen from an acid.
     * Metals above H in the activity series can react with acids to produce H2.
     */
    public static boolean canDisplaceHydrogen(String metal) {
        return isMoreReactive(metal, "H");
    }

    /**
     * Check if a metal is in the activity series.
     */
    public static boolean isInSeries(String symbol) {
        return REACTIVITY_INDEX.containsKey(symbol);
    }

    /**
     * Get the reactivity rank of a metal (lower = more reactive).
     * Returns -1 if the metal is not in the series.
     */
    public static int getReactivityRank(String symbol) {
        return REACTIVITY_INDEX.getOrDefault(symbol, -1);
    }
}
