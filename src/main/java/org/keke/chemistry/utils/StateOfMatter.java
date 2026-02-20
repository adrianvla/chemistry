package org.keke.chemistry.utils;

/**
 * Represents the physical state of a chemical substance.
 */
public enum StateOfMatter {
    SOLID("Solid"),
    LIQUID("Liquid"),
    GAS("Gas");

    private final String displayName;

    StateOfMatter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
