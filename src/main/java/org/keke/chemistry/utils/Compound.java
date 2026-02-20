package org.keke.chemistry.utils;

import java.util.Map;

/**
 * Represents a chemical compound with its formula, composition, molar mass, and solution color.
 */
public class Compound {
    private final String formula;
    private final Map<Element, Integer> composition;
    private final double molarMass;
    private final int solutionColor;

    public Compound(String formula) {
        this.formula = formula;
        this.composition = FormulaParser.parse(formula);
        this.molarMass = computeMolarMass();
        this.solutionColor = CalculateColor.calculateColor(formula);
    }

    private double computeMolarMass() {
        double mass = 0.0;
        for (Map.Entry<Element, Integer> entry : composition.entrySet()) {
            mass += entry.getKey().getMolarMass() * entry.getValue();
        }
        return mass;
    }

    public String getFormula() { return formula; }
    public Map<Element, Integer> getComposition() { return composition; }
    public double getMolarMass() { return molarMass; }
    public int getSolutionColor() { return solutionColor; }

    public static double computeMolarMass(String formula) {
        Map<Element, Integer> comp = FormulaParser.parse(formula);
        double mass = 0.0;
        for (Map.Entry<Element, Integer> entry : comp.entrySet()) {
            mass += entry.getKey().getMolarMass() * entry.getValue();
        }
        return mass;
    }
}
