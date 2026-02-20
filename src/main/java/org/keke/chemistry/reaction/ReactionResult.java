package org.keke.chemistry.reaction;

import java.util.*;

/**
 * The result of processing a chemical reaction, including stoichiometric products,
 * unreacted leftovers, observable effects, and thermodynamic data.
 */
public class ReactionResult {
    private final Map<String, Double> products;       // formula → moles
    private final Map<String, Double> leftovers;      // formula → moles (unreacted)
    private final Set<ReactionEffect> effects;
    private final boolean success;
    private final double enthalpyKJ;                  // total heat released/absorbed (kJ)
    private final double temperatureChangeK;          // temperature change in solution (K)

    public ReactionResult(Map<String, Double> products, Map<String, Double> leftovers,
                          Set<ReactionEffect> effects, boolean success) {
        this(products, leftovers, effects, success, 0.0, 0.0);
    }

    public ReactionResult(Map<String, Double> products, Map<String, Double> leftovers,
                          Set<ReactionEffect> effects, boolean success,
                          double enthalpyKJ, double temperatureChangeK) {
        this.products = products;
        this.leftovers = leftovers;
        this.effects = effects;
        this.success = success;
        this.enthalpyKJ = enthalpyKJ;
        this.temperatureChangeK = temperatureChangeK;
    }

    public Map<String, Double> getProducts() { return products; }
    public Map<String, Double> getLeftovers() { return leftovers; }
    public Set<ReactionEffect> getEffects() { return effects; }
    public double getEnthalpyKJ() { return enthalpyKJ; }
    public double getTemperatureChangeK() { return temperatureChangeK; }
    public boolean isSuccess() { return success; }
}
