package org.keke.chemistry.utils;

/**
 * Available beaker sizes. Each variant has a volume capacity (mL),
 * a display name, and a recipe tier hint for crafting.
 */
public enum BeakerSize {
    TINY    (  50, "50 mL Beaker"),
    SMALL   ( 100, "100 mL Beaker"),
    MEDIUM  ( 250, "250 mL Beaker"),
    LARGE   ( 500, "500 mL Beaker"),
    XLARGE  (1000, "1000 mL Beaker"),
    XXLARGE (2000, "2000 mL Beaker");

    private final double capacityMl;
    private final String displayName;

    BeakerSize(double capacityMl, String displayName) {
        this.capacityMl = capacityMl;
        this.displayName = displayName;
    }

    public double getCapacityMl() { return capacityMl; }
    public String getDisplayName() { return displayName; }

    /** Find the BeakerSize closest to a given capacity. */
    public static BeakerSize fromCapacity(double ml) {
        BeakerSize best = MEDIUM;
        double bestDist = Double.MAX_VALUE;
        for (BeakerSize size : values()) {
            double dist = Math.abs(size.capacityMl - ml);
            if (dist < bestDist) {
                bestDist = dist;
                best = size;
            }
        }
        return best;
    }

    /** Get the BeakerSize by ordinal name (for NBT storage). */
    public static BeakerSize fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
