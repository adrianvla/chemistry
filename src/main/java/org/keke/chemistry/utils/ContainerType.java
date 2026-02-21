package org.keke.chemistry.utils;

/**
 * All container types in the chemistry mod. Each has physical properties
 * that determine capacity, throwability, sealability, and heat resistance.
 */
public enum ContainerType {
    BEAKER      (250.0, "Beaker",      true,  false, false, 573.15),
    PETRI_DISH  ( 50.0, "Petri Dish",  false, false, false, 773.15),
    TEST_TUBE   ( 25.0, "Test Tube",   false, false, false, 573.15),
    AMPULE      ( 10.0, "Ampule",      true,  true,  true,  573.15),
    BOTTLE      (100.0, "Bottle",      true,  true,  false, 573.15),
    CRUCIBLE    (200.0, "Crucible",    false, false, false, 2273.15),
    GAS_COLLECTOR(500.0,"Gas Collector",false, true,  false, 573.15);

    private final double defaultCapacityMl;
    private final String displayName;
    private final boolean throwable;
    private final boolean sealable;
    private final boolean requiresToolToOpen;
    private final double maxSafeTempK;

    ContainerType(double defaultCapacityMl, String displayName, boolean throwable,
                  boolean sealable, boolean requiresToolToOpen, double maxSafeTempK) {
        this.defaultCapacityMl = defaultCapacityMl;
        this.displayName = displayName;
        this.throwable = throwable;
        this.sealable = sealable;
        this.requiresToolToOpen = requiresToolToOpen;
        this.maxSafeTempK = maxSafeTempK;
    }

    public double getDefaultCapacityMl() { return defaultCapacityMl; }
    public String getDisplayName()       { return displayName; }
    public boolean isThrowable()         { return throwable; }
    public boolean isSealable()          { return sealable; }
    public boolean requiresToolToOpen()  { return requiresToolToOpen; }
    public double getMaxSafeTempK()      { return maxSafeTempK; }

    public static ContainerType fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return BEAKER;
        }
    }
}
