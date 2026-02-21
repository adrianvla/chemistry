package org.keke.chemistry.utils;

/**
 * Material type for beakers. Glass beakers crack under thermal stress,
 * while metal beakers are heat-resistant but opaque.
 */
public enum BeakerMaterial {
    GLASS("Glass", false, 573.15),     // cracks above 300°C
    METAL("Metal", true, 1773.15);     // withstands up to ~1500°C

    private final String displayName;
    private final boolean heatResistant;
    private final double maxSafeTempK;

    BeakerMaterial(String displayName, boolean heatResistant, double maxSafeTempK) {
        this.displayName = displayName;
        this.heatResistant = heatResistant;
        this.maxSafeTempK = maxSafeTempK;
    }

    public String getDisplayName() { return displayName; }
    public boolean isHeatResistant() { return heatResistant; }
    public double getMaxSafeTempK() { return maxSafeTempK; }

    public static BeakerMaterial fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return GLASS;
        }
    }
}
