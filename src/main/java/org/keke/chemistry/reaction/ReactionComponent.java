package org.keke.chemistry.reaction;

/**
 * A component of a chemical reaction (reactant or product) with formula and stoichiometric coefficient.
 */
public record ReactionComponent(String formula, int coefficient) {
}
