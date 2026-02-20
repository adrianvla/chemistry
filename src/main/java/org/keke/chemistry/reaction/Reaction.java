package org.keke.chemistry.reaction;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a chemical reaction with reactants, products, type, conditions, and observable effects.
 */
public class Reaction {
    private final List<ReactionComponent> reactants;
    private final List<ReactionComponent> products;
    private final ReactionType type;
    private final ReactionConditions conditions;
    private final Set<ReactionEffect> effects;

    public Reaction(List<ReactionComponent> reactants, List<ReactionComponent> products,
                    ReactionType type, ReactionConditions conditions, Set<ReactionEffect> effects) {
        this.reactants = reactants;
        this.products = products;
        this.type = type;
        this.conditions = conditions;
        this.effects = effects;
    }

    public List<ReactionComponent> getReactants() { return reactants; }
    public List<ReactionComponent> getProducts() { return products; }
    public ReactionType getType() { return type; }
    public ReactionConditions getConditions() { return conditions; }
    public Set<ReactionEffect> getEffects() { return effects; }

    public static class ReactionConditions {
        private final int minTemp;
        private final int maxTemp;
        private final String catalyst;
        private final boolean requiresHeat;

        public ReactionConditions(int minTemp, int maxTemp, String catalyst, boolean requiresHeat) {
            this.minTemp = minTemp;
            this.maxTemp = maxTemp;
            this.catalyst = catalyst;
            this.requiresHeat = requiresHeat;
        }

        public static ReactionConditions ambient() {
            return new ReactionConditions(0, 1000, "", false);
        }

        public static ReactionConditions heated() {
            return new ReactionConditions(50, 1000, "", true);
        }

        public int getMinTemp() { return minTemp; }
        public int getMaxTemp() { return maxTemp; }
        public String getCatalyst() { return catalyst; }
        public boolean requiresHeat() { return requiresHeat; }
    }
}
