package org.keke.chemistry.reaction;

import org.keke.chemistry.utils.*;

import java.util.*;

/**
 * Algorithmic prediction of chemical reactions from first principles.
 * This system dynamically predicts reaction products without requiring
 * every reaction to be pre-registered.
 *
 * Supported prediction types:
 * 1. Acid-Base neutralization: acid + base → salt + water
 * 2. Double replacement (metathesis): AB + CD → AD + CB (if precipitate forms)
 * 3. Single replacement: A + BX → AX + B (if A > B in activity series)
 * 4. Acid + metal: metal + acid → salt + H2 (if metal above H in activity series)
 *
 * When a predicted reaction is found, it constructs a full Reaction object
 * with proper stoichiometry and estimated enthalpy from ThermodynamicData.
 */
public class DynamicReactionPredictor {

    /**
     * Try to predict a reaction between two or more chemicals.
     * Returns a Reaction object if a reaction can be predicted, null otherwise.
     *
     * @param formulas the set of reactant formulas
     * @return a predicted Reaction, or null if no reaction can be predicted
     */
    public static Reaction predict(List<String> formulas) {
        if (formulas.size() < 1) return null;

        // Single-reactant decomposition — delegate to DecompositionPredictor
        if (formulas.size() == 1) {
            return DecompositionPredictor.predict(formulas.get(0));
        }

        if (formulas.size() == 2) {
            String f1 = formulas.get(0);
            String f2 = formulas.get(1);

            // Try acid-base neutralization
            Reaction acidBase = tryAcidBase(f1, f2);
            if (acidBase != null) return acidBase;

            // Try metal + acid
            Reaction metalAcid = tryMetalAcid(f1, f2);
            if (metalAcid != null) return metalAcid;

            // Try single replacement (metal + salt)
            Reaction singleReplacement = trySingleReplacement(f1, f2);
            if (singleReplacement != null) return singleReplacement;

            // Try double replacement (metathesis with precipitation)
            Reaction doubleReplacement = tryDoubleReplacement(f1, f2);
            if (doubleReplacement != null) return doubleReplacement;

            // Try metal oxide + water → base (synthesis)
            Reaction oxideWater = tryMetalOxidePlusWater(f1, f2);
            if (oxideWater != null) return oxideWater;

            // Try nonmetal oxide + water → acid (synthesis)
            Reaction nonmetalOxide = tryNonmetalOxidePlusWater(f1, f2);
            if (nonmetalOxide != null) return nonmetalOxide;

            // Try metal + water → base + H2 (reactive metals)
            Reaction metalWater = tryMetalPlusWater(f1, f2);
            if (metalWater != null) return metalWater;

            // Try combustion: organic + O2 → CO2 + H2O
            Reaction combustion = tryCombustion(f1, f2);
            if (combustion != null) return combustion;

            // Try direct synthesis: element + element → compound
            Reaction synthesis = trySynthesis(f1, f2);
            if (synthesis != null) return synthesis;
        }

        return null;
    }

    /**
     * Predict acid-base neutralization: Acid + Base → Salt + Water
     *
     * Mechanism:
     * H_n(A) + M(OH)_m → M_x(A)_y + H2O
     * where the salt formula is balanced by ion charges.
     */
    private static Reaction tryAcidBase(String f1, String f2) {
        String acidFormula, baseFormula;

        if (IonicCompound.isAcid(f1) && IonicCompound.isBase(f2)) {
            acidFormula = f1;
            baseFormula = f2;
        } else if (IonicCompound.isAcid(f2) && IonicCompound.isBase(f1)) {
            acidFormula = f2;
            baseFormula = f1;
        } else {
            return null;
        }

        IonicCompound acid = IonicCompound.decompose(acidFormula);
        IonicCompound base = IonicCompound.decompose(baseFormula);
        if (acid == null || base == null) return null;

        // Acid provides anion, base provides cation
        String cation = base.getCation();
        int cationCharge = base.getCationCharge();
        String anion = acid.getAnion();
        int anionCharge = acid.getAnionCharge();

        // Build salt formula
        String saltFormula = SolubilityTable.buildFormula(cation, cationCharge, anion, anionCharge);

        // Determine stoichiometry:
        // H_a(anion) + cation(OH)_b → salt + H2O
        // Number of H from acid = acid.getCationCount() (since cation is H)
        // Number of OH from base = base.getAnionCount()
        // Each acid molecule provides acid.getCationCount() H+
        // Each base molecule provides base.getAnionCount() OH-
        // To balance: need equal H+ and OH-

        int hPerAcid = acid.getCationCount();   // e.g., H2SO4 → 2
        int ohPerBase = base.getAnionCount();   // e.g., Ca(OH)2 → 2

        // LCM of H and OH counts
        int lcm = lcm(hPerAcid, ohPerBase);
        int acidCoeff = lcm / hPerAcid;
        int baseCoeff = lcm / ohPerBase;
        int waterCoeff = lcm; // each H+ + OH- → one H2O

        // Salt coefficient: based on how many cations we get from bases
        // baseCoeff bases provide baseCoeff × (number of cations per base) cations
        // For simple bases like NaOH: 1 cation per base, so saltCoeff based on charge balance
        // Actually, let's use charge balance for the salt
        int totalCations = baseCoeff; // each base molecule gives 1 cation (for simple hydroxides)
        int numCationsInSalt;
        int numAnionsInSalt;

        // Balance salt: cation^(cationCharge+) and anion^(|anionCharge|-) 
        int absCatCharge = Math.abs(cationCharge);
        int absAnCharge = Math.abs(anionCharge);
        int saltLcm = lcm(absCatCharge, absAnCharge);
        numCationsInSalt = saltLcm / absCatCharge;
        numAnionsInSalt = saltLcm / absAnCharge;

        // How many salt formula units do we produce?
        // From baseCoeff bases, we get baseCoeff cations
        // Each salt unit needs numCationsInSalt cations
        int saltCoeff = baseCoeff / numCationsInSalt;
        if (saltCoeff <= 0) saltCoeff = 1;

        // Verify the acid provides enough anions
        int anionsNeeded = saltCoeff * numAnionsInSalt;
        int anionsFrom = acidCoeff; // each acid gives 1 anion unit
        if (anionsFrom != anionsNeeded) {
            // Recalculate with proper balancing
            // This is a simplified balancer; for complex cases, just construct what we can
            int newLcm = lcm(anionsNeeded, anionsFrom);
            int multiplier = newLcm / anionsFrom;
            acidCoeff *= multiplier;
            baseCoeff *= multiplier;
            waterCoeff *= multiplier;
            saltCoeff *= multiplier;
        }

        // Build reaction components
        List<ReactionComponent> reactants = new ArrayList<>();
        reactants.add(new ReactionComponent(acidFormula, acidCoeff));
        reactants.add(new ReactionComponent(baseFormula, baseCoeff));

        List<ReactionComponent> products = new ArrayList<>();
        products.add(new ReactionComponent(saltFormula, saltCoeff));
        products.add(new ReactionComponent("H2O", waterCoeff));

        // Acid-base neutralizations are exothermic (≈ -57 kJ/mol H2O formed)
        Set<ReactionEffect> effects = EnumSet.of(ReactionEffect.EXOTHERMIC);

        return new Reaction(reactants, products, ReactionType.ACID_BASE,
                Reaction.ReactionConditions.ambient(), effects);
    }

    /**
     * Predict metal + acid → salt + hydrogen gas.
     * Metal must be above hydrogen in the activity series.
     *
     * e.g., Zn + 2HCl → ZnCl2 + H2
     */
    private static Reaction tryMetalAcid(String f1, String f2) {
        String metalFormula, acidFormula;

        // One must be a pure metal, the other an acid
        boolean f1IsMetal = isBareMetal(f1);
        boolean f2IsMetal = isBareMetal(f2);
        boolean f1IsAcid = IonicCompound.isAcid(f1);
        boolean f2IsAcid = IonicCompound.isAcid(f2);

        if (f1IsMetal && f2IsAcid) {
            metalFormula = f1;
            acidFormula = f2;
        } else if (f2IsMetal && f1IsAcid) {
            metalFormula = f2;
            acidFormula = f1;
        } else {
            return null;
        }

        String metal = metalFormula; // e.g., "Zn"
        if (!ActivitySeries.canDisplaceHydrogen(metal)) {
            return null; // Metal is below hydrogen, no reaction
        }

        IonicCompound acid = IonicCompound.decompose(acidFormula);
        if (acid == null) return null;

        String anion = acid.getAnion();
        int anionCharge = acid.getAnionCharge();
        int metalCharge = SolubilityTable.getCationCharge(metal);
        if (metalCharge <= 0) metalCharge = 2; // default to +2

        // Build salt formula
        String saltFormula = SolubilityTable.buildFormula(metal, metalCharge, anion, anionCharge);

        // Stoichiometry:
        // M + n HX → MX_n + n/2 H2
        // where n = metalCharge (number of H+ needed to balance)
        int absAnionCharge = Math.abs(anionCharge);
        int saltLcm = lcm(metalCharge, absAnionCharge);
        int anionsPerSalt = saltLcm / absAnionCharge;
        int acidCoeff = metalCharge; // For +2 metal with -1 anion: 2 acid molecules
        int metalCoeff = 1;
        int saltCoeff = 1;

        // Each acid molecule provides 1 H+
        // metalCoeff metal atoms provide metalCoeff × metalCharge electrons
        // That produces metalCoeff × metalCharge / 2 H2 molecules
        int hPerAcid = acid.getCationCount();
        int totalH = acidCoeff * hPerAcid;
        // H2 produced = totalH / 2
        // Need to handle odd numbers
        if (totalH % 2 != 0) {
            metalCoeff *= 2;
            acidCoeff *= 2;
            saltCoeff *= 2;
            totalH *= 2;
        }
        int h2Coeff = totalH / 2;

        List<ReactionComponent> reactants = new ArrayList<>();
        reactants.add(new ReactionComponent(metalFormula, metalCoeff));
        reactants.add(new ReactionComponent(acidFormula, acidCoeff));

        List<ReactionComponent> products = new ArrayList<>();
        products.add(new ReactionComponent(saltFormula, saltCoeff));
        products.add(new ReactionComponent("H2", h2Coeff));

        Set<ReactionEffect> effects = EnumSet.of(ReactionEffect.GAS_EVOLUTION);
        if (ActivitySeries.getReactivityRank(metal) < 5) {
            // Very reactive metals (Li, K, Ba, Ca, Na) are strongly exothermic
            effects.add(ReactionEffect.EXOTHERMIC);
        }

        return new Reaction(reactants, products, ReactionType.SINGLE_REPLACEMENT,
                Reaction.ReactionConditions.ambient(), effects);
    }

    /**
     * Predict single replacement: Metal A + salt BX → salt AX + Metal B
     * A must be more reactive than B in the activity series.
     *
     * e.g., Zn + CuSO4 → ZnSO4 + Cu
     */
    private static Reaction trySingleReplacement(String f1, String f2) {
        String metalFormula, saltFormula;

        boolean f1IsMetal = isBareMetal(f1);
        boolean f2IsMetal = isBareMetal(f2);
        boolean f1IsSalt = IonicCompound.isSalt(f1);
        boolean f2IsSalt = IonicCompound.isSalt(f2);

        if (f1IsMetal && f2IsSalt) {
            metalFormula = f1;
            saltFormula = f2;
        } else if (f2IsMetal && f1IsSalt) {
            metalFormula = f2;
            saltFormula = f1;
        } else {
            return null;
        }

        String displacingMetal = metalFormula;
        IonicCompound salt = IonicCompound.decompose(saltFormula);
        if (salt == null) return null;

        String displacedMetal = salt.getCation();
        String anion = salt.getAnion();

        // Check activity series
        if (!ActivitySeries.isMoreReactive(displacingMetal, displacedMetal)) {
            return null;
        }

        int newCationCharge = SolubilityTable.getCationCharge(displacingMetal);
        if (newCationCharge <= 0) newCationCharge = 2;
        int anionCharge = salt.getAnionCharge();

        // Build new salt
        String newSaltFormula = SolubilityTable.buildFormula(
                displacingMetal, newCationCharge, anion, anionCharge);

        // Simple 1:1 stoichiometry (may need adjustment for charge differences)
        int metalCoeff = 1;
        int saltCoeff = 1;
        int newSaltCoeff = 1;
        int displacedCoeff = 1;

        // If charges differ, we need to scale
        int oldCationCharge = salt.getCationCharge();
        if (oldCationCharge != newCationCharge && oldCationCharge > 0 && newCationCharge > 0) {
            int lcm = lcm(oldCationCharge, newCationCharge);
            metalCoeff = lcm / newCationCharge;
            displacedCoeff = lcm / oldCationCharge;
            // Recalculate salt coefficients
            saltCoeff = metalCoeff;
            newSaltCoeff = displacedCoeff;
        }

        List<ReactionComponent> reactants = new ArrayList<>();
        reactants.add(new ReactionComponent(metalFormula, metalCoeff));
        reactants.add(new ReactionComponent(saltFormula, saltCoeff));

        List<ReactionComponent> products = new ArrayList<>();
        products.add(new ReactionComponent(newSaltFormula, newSaltCoeff));
        products.add(new ReactionComponent(displacedMetal, displacedCoeff));

        Set<ReactionEffect> effects = EnumSet.of(ReactionEffect.COLOR_CHANGE);

        return new Reaction(reactants, products, ReactionType.SINGLE_REPLACEMENT,
                Reaction.ReactionConditions.ambient(), effects);
    }

    /**
     * Predict double replacement (metathesis):
     * AB + CD → AD + CB
     * Reaction proceeds if at least one product is insoluble (precipitate),
     * a gas, or water.
     */
    private static Reaction tryDoubleReplacement(String f1, String f2) {
        IonicCompound ic1 = IonicCompound.decompose(f1);
        IonicCompound ic2 = IonicCompound.decompose(f2);
        if (ic1 == null || ic2 == null) return null;

        // Both must be ionic compounds (not acid + base, handled above)
        // Skip if either is an acid or base (handled by tryAcidBase)
        if (ic1.getCation().equals("H") || ic2.getCation().equals("H")) return null;
        if (ic1.getAnion().equals("OH") || ic2.getAnion().equals("OH")) return null;

        // Swap ions: A-B + C-D → A-D + C-B
        String cation1 = ic1.getCation();
        int charge1 = ic1.getCationCharge();
        String anion1 = ic1.getAnion();
        int anionCharge1 = ic1.getAnionCharge();

        String cation2 = ic2.getCation();
        int charge2 = ic2.getCationCharge();
        String anion2 = ic2.getAnion();
        int anionCharge2 = ic2.getAnionCharge();

        // Product 1: cation1 + anion2
        String product1 = SolubilityTable.buildFormula(cation1, charge1, anion2, anionCharge2);
        boolean p1Soluble = SolubilityTable.isSoluble(cation1, anion2);

        // Product 2: cation2 + anion1
        String product2 = SolubilityTable.buildFormula(cation2, charge2, anion1, anionCharge1);
        boolean p2Soluble = SolubilityTable.isSoluble(cation2, anion1);

        // Reaction only proceeds if at least one product is insoluble (precipitate)
        if (p1Soluble && p2Soluble) {
            return null; // No driving force
        }

        // Simple 1:1 stoichiometry
        List<ReactionComponent> reactants = new ArrayList<>();
        reactants.add(new ReactionComponent(f1, 1));
        reactants.add(new ReactionComponent(f2, 1));

        List<ReactionComponent> products = new ArrayList<>();
        products.add(new ReactionComponent(product1, 1));
        products.add(new ReactionComponent(product2, 1));

        Set<ReactionEffect> effects = EnumSet.noneOf(ReactionEffect.class);
        if (!p1Soluble || !p2Soluble) {
            effects.add(ReactionEffect.PRECIPITATE);
        }
        effects.add(ReactionEffect.COLOR_CHANGE);

        return new Reaction(reactants, products, ReactionType.DOUBLE_REPLACEMENT,
                Reaction.ReactionConditions.ambient(), effects);
    }

    /**
     * Check if a formula represents a bare metal element (single element, not in compound).
     */
    private static boolean isBareMetal(String formula) {
        if (formula == null || formula.isEmpty()) return false;
        // Check if it's a single element symbol
        Element elem = Element.fromSymbol(formula);
        if (elem == null) return false;
        // Check if it's actually a metal (not a nonmetal)
        return ActivitySeries.isInSeries(formula);
    }

    // ── Metal oxide + Water → Metal hydroxide ─────────────────────────

    /** Known metal oxides: formula → (metal symbol, metal charge) */
    private static final Map<String, int[]> METAL_OXIDES = Map.ofEntries(
            Map.entry("Na2O", new int[]{1}),   // Na+
            Map.entry("K2O", new int[]{1}),
            Map.entry("Li2O", new int[]{1}),
            Map.entry("CaO", new int[]{2}),
            Map.entry("BaO", new int[]{2}),
            Map.entry("SrO", new int[]{2}),
            Map.entry("MgO", new int[]{2})
    );

    /**
     * Metal oxide + water → metal hydroxide (synthesis).
     * e.g. CaO + H2O → Ca(OH)2
     *      Na2O + H2O → 2NaOH
     */
    private static Reaction tryMetalOxidePlusWater(String f1, String f2) {
        String oxide, water;
        if (f1.equals("H2O") && isMetalOxide(f2)) { oxide = f2; water = f1; }
        else if (f2.equals("H2O") && isMetalOxide(f1)) { oxide = f1; water = f2; }
        else return null;

        // Parse the oxide to get the metal
        Map<Element, Integer> comp = FormulaParser.parse(oxide);
        String metal = null;
        int metalCount = 0;
        int oxygenCount = 0;
        for (var entry : comp.entrySet()) {
            if (entry.getKey().getSymbol().equals("O")) {
                oxygenCount = entry.getValue();
            } else {
                metal = entry.getKey().getSymbol();
                metalCount = entry.getValue();
            }
        }
        if (metal == null) return null;

        int metalCharge = SolubilityTable.getCationCharge(metal);
        if (metalCharge <= 0) metalCharge = (oxygenCount * 2) / metalCount;

        // Product: metal hydroxide M(OH)_n where n = metalCharge
        String hydroxide = SolubilityTable.buildFormula(metal, metalCharge, "OH", -1);

        // Stoichiometry: M_a O_b + b H2O → a M(OH)_(2b/a)
        // Simplified: 1 oxide + oxygenCount H2O → metalCount hydroxide
        List<ReactionComponent> reactants = new ArrayList<>();
        reactants.add(new ReactionComponent(oxide, 1));
        reactants.add(new ReactionComponent("H2O", oxygenCount));

        List<ReactionComponent> products = new ArrayList<>();
        products.add(new ReactionComponent(hydroxide, metalCount));

        return new Reaction(reactants, products, ReactionType.SYNTHESIS,
                Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.EXOTHERMIC));
    }

    private static boolean isMetalOxide(String formula) {
        Map<Element, Integer> comp = FormulaParser.parse(formula);
        if (comp.size() != 2) return false;
        boolean hasOxygen = false;
        boolean hasMetal = false;
        for (Element e : comp.keySet()) {
            if (e.getSymbol().equals("O")) hasOxygen = true;
            else if (SolubilityTable.getCationCharge(e.getSymbol()) > 0) hasMetal = true;
        }
        return hasOxygen && hasMetal;
    }

    // ── Nonmetal oxide + Water → Acid ─────────────────────────────────

    /** Nonmetal oxide → acid product mapping */
    private static final Map<String, String> NONMETAL_OXIDE_TO_ACID = Map.of(
            "SO3", "H2SO4",
            "SO2", "H2SO3",
            "CO2", "H2CO3",
            "N2O5", "HNO3",
            "N2O3", "HNO2",
            "P2O5", "H3PO4",
            "P4O10", "H3PO4",
            "Cl2O7", "HClO4",
            "Cl2O5", "HClO3"
    );

    /**
     * Nonmetal oxide + water → acid (synthesis).
     * e.g. SO3 + H2O → H2SO4
     *      CO2 + H2O → H2CO3
     */
    private static Reaction tryNonmetalOxidePlusWater(String f1, String f2) {
        String oxide, water;
        if (f1.equals("H2O") && NONMETAL_OXIDE_TO_ACID.containsKey(f2)) { oxide = f2; water = f1; }
        else if (f2.equals("H2O") && NONMETAL_OXIDE_TO_ACID.containsKey(f1)) { oxide = f1; water = f2; }
        else return null;

        String acid = NONMETAL_OXIDE_TO_ACID.get(oxide);

        // For most: oxide + H2O → acid (1:1:1 stoichiometry)
        int oxideCoeff = 1;
        int waterCoeff = 1;
        int acidCoeff = 1;

        // Special cases for P2O5 and N2O5 etc.
        if (oxide.equals("P2O5")) {
            // P2O5 + 3H2O → 2H3PO4
            waterCoeff = 3;
            acidCoeff = 2;
        } else if (oxide.equals("P4O10")) {
            // P4O10 + 6H2O → 4H3PO4
            waterCoeff = 6;
            acidCoeff = 4;
        } else if (oxide.equals("N2O5")) {
            // N2O5 + H2O → 2HNO3
            acidCoeff = 2;
        } else if (oxide.equals("N2O3")) {
            // N2O3 + H2O → 2HNO2
            acidCoeff = 2;
        } else if (oxide.equals("Cl2O7")) {
            // Cl2O7 + H2O → 2HClO4
            acidCoeff = 2;
        } else if (oxide.equals("Cl2O5")) {
            // Cl2O5 + H2O → 2HClO3
            acidCoeff = 2;
        }

        List<ReactionComponent> reactants = new ArrayList<>();
        reactants.add(new ReactionComponent(oxide, oxideCoeff));
        reactants.add(new ReactionComponent("H2O", waterCoeff));

        List<ReactionComponent> products = new ArrayList<>();
        products.add(new ReactionComponent(acid, acidCoeff));

        return new Reaction(reactants, products, ReactionType.SYNTHESIS,
                Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.EXOTHERMIC));
    }

    // ── Metal + Water → Base + H2 ─────────────────────────────────────

    /** Metals reactive enough to react with water (above Mg roughly). */
    private static final Set<String> WATER_REACTIVE_METALS = Set.of(
            "Li", "Na", "K", "Rb", "Cs", "Ca", "Sr", "Ba"
    );

    /**
     * Reactive metal + water → metal hydroxide + hydrogen gas.
     * e.g. 2Na + 2H2O → 2NaOH + H2
     *      Ca + 2H2O → Ca(OH)2 + H2
     */
    private static Reaction tryMetalPlusWater(String f1, String f2) {
        String metal, water;
        if (f1.equals("H2O") && WATER_REACTIVE_METALS.contains(f2)) { metal = f2; water = f1; }
        else if (f2.equals("H2O") && WATER_REACTIVE_METALS.contains(f1)) { metal = f1; water = f2; }
        else return null;

        int metalCharge = SolubilityTable.getCationCharge(metal);
        if (metalCharge <= 0) metalCharge = 1;

        String hydroxide = SolubilityTable.buildFormula(metal, metalCharge, "OH", -1);

        // Stoichiometry: 2M + 2H2O → 2MOH + H2 (for charge +1)
        //                M + 2H2O → M(OH)2 + H2 (for charge +2)
        int metalCoeff = 2;
        int waterCoeff = 2;
        int hydroxideCoeff = 2;
        int h2Coeff = 1;

        if (metalCharge == 2) {
            metalCoeff = 1;
            waterCoeff = 2;
            hydroxideCoeff = 1;
            h2Coeff = 1;
        } else if (metalCharge == 1) {
            metalCoeff = 2;
            waterCoeff = 2;
            hydroxideCoeff = 2;
            h2Coeff = 1;
        }

        List<ReactionComponent> reactants = new ArrayList<>();
        reactants.add(new ReactionComponent(metal, metalCoeff));
        reactants.add(new ReactionComponent("H2O", waterCoeff));

        List<ReactionComponent> products = new ArrayList<>();
        products.add(new ReactionComponent(hydroxide, hydroxideCoeff));
        products.add(new ReactionComponent("H2", h2Coeff));

        Set<ReactionEffect> effects = EnumSet.of(ReactionEffect.GAS_EVOLUTION, ReactionEffect.EXOTHERMIC);

        return new Reaction(reactants, products, ReactionType.SINGLE_REPLACEMENT,
                Reaction.ReactionConditions.ambient(), effects);
    }

    // ── Combustion: organic + O2 → CO2 + H2O ─────────────────────────

    /**
     * Combustion of organic compounds containing C, H, and optionally O.
     * CxHyOz + (x + y/4 - z/2) O2 → x CO2 + y/2 H2O
     *
     * Uses integer coefficients by multiplying through.
     */
    private static Reaction tryCombustion(String f1, String f2) {
        String organic, o2;
        if (f1.equals("O2")) { o2 = f1; organic = f2; }
        else if (f2.equals("O2")) { o2 = f2; organic = f1; }
        else return null;

        Map<Element, Integer> comp = FormulaParser.parse(organic);
        if (comp.isEmpty()) return null;

        int carbons = 0, hydrogens = 0, oxygens = 0;
        boolean hasOtherElements = false;

        for (var entry : comp.entrySet()) {
            String sym = entry.getKey().getSymbol();
            switch (sym) {
                case "C" -> carbons = entry.getValue();
                case "H" -> hydrogens = entry.getValue();
                case "O" -> oxygens = entry.getValue();
                default -> hasOtherElements = true;
            }
        }

        // Must have carbon to be an organic combustion
        if (carbons == 0 || hasOtherElements) return null;

        // CxHyOz + a O2 → x CO2 + (y/2) H2O
        // Oxygen balance: 2x + y/2 = 2a + z  →  a = x + y/4 - z/2
        // Multiply everything by 4 to clear fractions:
        // 4 CxHyOz + (4x + y - 2z) O2 → 4x CO2 + 2y H2O
        int organicCoeff = 4;
        int o2Coeff = 4 * carbons + hydrogens - 2 * oxygens;
        int co2Coeff = 4 * carbons;
        int h2oCoeff = 2 * hydrogens;

        if (o2Coeff <= 0) return null;

        // Simplify by GCD
        int g = gcd(gcd(organicCoeff, o2Coeff), gcd(co2Coeff, h2oCoeff));
        organicCoeff /= g;
        o2Coeff /= g;
        co2Coeff /= g;
        h2oCoeff /= g;

        List<ReactionComponent> reactants = new ArrayList<>();
        reactants.add(new ReactionComponent(organic, organicCoeff));
        reactants.add(new ReactionComponent("O2", o2Coeff));

        List<ReactionComponent> products = new ArrayList<>();
        products.add(new ReactionComponent("CO2", co2Coeff));
        products.add(new ReactionComponent("H2O", h2oCoeff));

        return new Reaction(reactants, products, ReactionType.COMBUSTION,
                Reaction.ReactionConditions.heated(),
                EnumSet.of(ReactionEffect.EXOTHERMIC));
    }

    // ── Direct Synthesis: element + element → compound ───────────────

    /** Diatomic non-metal elements: formula → (symbol, atoms per molecule). */
    private static final Map<String, String[]> DIATOMIC_ELEMENTS = Map.of(
            "H2",  new String[]{"H", "2"},
            "O2",  new String[]{"O", "2"},
            "N2",  new String[]{"N", "2"},
            "F2",  new String[]{"F", "2"},
            "Cl2", new String[]{"Cl", "2"},
            "Br2", new String[]{"Br", "2"},
            "I2",  new String[]{"I", "2"}
    );

    /** Halogens in order of reactivity. */
    private static final Set<String> HALOGENS = Set.of("F", "Cl", "Br", "I");

    /** Chalcogens that form simple binary compounds with metals. */
    private static final Set<String> CHALCOGENS = Set.of("O", "S", "Se", "Te");

    /**
     * Direct synthesis: two elements combine to form a compound.
     * Covers:
     *   - Metal + halogen → metal halide (e.g. 2Na + Cl2 → 2NaCl)
     *   - Metal + O2 → metal oxide (e.g. 4Na + O2 → 2Na2O, 2Mg + O2 → 2MgO)
     *   - Metal + S → metal sulfide (e.g. Fe + S → FeS)
     *   - H2 + halogen → hydrogen halide (e.g. H2 + Cl2 → 2HCl)
     */
    private static Reaction trySynthesis(String f1, String f2) {
        // Resolve each formula to its elemental symbol and atoms-per-molecule
        String sym1 = resolveElementSymbol(f1);
        String sym2 = resolveElementSymbol(f2);
        if (sym1 == null || sym2 == null) return null;

        int atomsPerMol1 = resolveAtomsPerMolecule(f1);
        int atomsPerMol2 = resolveAtomsPerMolecule(f2);

        // Determine which is the electropositive partner (cation) and electronegative (anion)
        String metalSym, nonmetalSym;
        String metalFormula, nonmetalFormula;
        int metalAtoms, nonmetalAtoms;

        boolean sym1IsMetal = !HALOGENS.contains(sym1) && !CHALCOGENS.contains(sym1)
                && !"N".equals(sym1) && !"P".equals(sym1) && !"H".equals(sym1);
        boolean sym2IsMetal = !HALOGENS.contains(sym2) && !CHALCOGENS.contains(sym2)
                && !"N".equals(sym2) && !"P".equals(sym2) && !"H".equals(sym2);

        // H2 + halogen → hydrogen halide
        if ("H".equals(sym1) && HALOGENS.contains(sym2)) {
            return buildHydrogenHalide(f1, sym1, atomsPerMol1, f2, sym2, atomsPerMol2);
        }
        if ("H".equals(sym2) && HALOGENS.contains(sym1)) {
            return buildHydrogenHalide(f2, sym2, atomsPerMol2, f1, sym1, atomsPerMol1);
        }

        // Need one metal and one non-metal
        if (sym1IsMetal && !sym2IsMetal) {
            metalSym = sym1; metalFormula = f1; metalAtoms = atomsPerMol1;
            nonmetalSym = sym2; nonmetalFormula = f2; nonmetalAtoms = atomsPerMol2;
        } else if (sym2IsMetal && !sym1IsMetal) {
            metalSym = sym2; metalFormula = f2; metalAtoms = atomsPerMol2;
            nonmetalSym = sym1; nonmetalFormula = f1; nonmetalAtoms = atomsPerMol1;
        } else {
            return null; // Two metals or two non-metals (except H+halogen handled above)
        }

        int metalCharge = SolubilityTable.getCationCharge(metalSym);
        if (metalCharge <= 0) metalCharge = 2;

        int nonmetalCharge;
        if (HALOGENS.contains(nonmetalSym)) nonmetalCharge = -1;
        else if (CHALCOGENS.contains(nonmetalSym)) nonmetalCharge = -2;
        else if ("N".equals(nonmetalSym)) nonmetalCharge = -3;
        else if ("P".equals(nonmetalSym)) nonmetalCharge = -3;
        else return null;

        String product = SolubilityTable.buildFormula(metalSym, metalCharge, nonmetalSym, nonmetalCharge);

        // Stoichiometry: balance metal atoms and nonmetal atoms
        // In the product formula, determine atoms of each per formula unit
        Map<Element, Integer> prodComp = FormulaParser.parse(product);
        int metalPerProd = 0, nonmetalPerProd = 0;
        for (var entry : prodComp.entrySet()) {
            if (entry.getKey().getSymbol().equals(metalSym)) metalPerProd = entry.getValue();
            if (entry.getKey().getSymbol().equals(nonmetalSym)) nonmetalPerProd = entry.getValue();
        }
        if (metalPerProd == 0 || nonmetalPerProd == 0) return null;

        // Balance: a×metalAtoms metal atoms = prodCoeff×metalPerProd
        //          b×nonmetalAtoms nonmetal atoms = prodCoeff×nonmetalPerProd
        // Find smallest integer solution
        int lcmMetal = lcm(metalAtoms, metalPerProd);
        int metalMolCoeff = lcmMetal / metalAtoms;
        int prodFromMetal = lcmMetal / metalPerProd;

        // Check nonmetal balance
        int nonmetalNeeded = prodFromMetal * nonmetalPerProd;
        int nonmetalMolCoeff;
        if (nonmetalNeeded % nonmetalAtoms == 0) {
            nonmetalMolCoeff = nonmetalNeeded / nonmetalAtoms;
        } else {
            // Scale everything up
            int factor = nonmetalAtoms / gcd(nonmetalNeeded, nonmetalAtoms);
            metalMolCoeff *= factor;
            prodFromMetal *= factor;
            nonmetalNeeded = prodFromMetal * nonmetalPerProd;
            nonmetalMolCoeff = nonmetalNeeded / nonmetalAtoms;
        }

        List<ReactionComponent> reactants = new ArrayList<>();
        reactants.add(new ReactionComponent(metalFormula, metalMolCoeff));
        reactants.add(new ReactionComponent(nonmetalFormula, nonmetalMolCoeff));

        List<ReactionComponent> products = new ArrayList<>();
        products.add(new ReactionComponent(product, prodFromMetal));

        return new Reaction(reactants, products, ReactionType.SYNTHESIS,
                Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.EXOTHERMIC));
    }

    /**
     * H2 + X2 → 2HX
     */
    private static Reaction buildHydrogenHalide(String hFormula, String hSym, int hAtoms,
                                                  String xFormula, String xSym, int xAtoms) {
        String product = "H" + xSym;
        // H2 + Cl2 → 2HCl ; balance: 1:1 → 2 product
        int hCoeff = 1;
        int xCoeff = 1;
        int prodCoeff = 2;

        // For I2/Br2 which are diatomic, this already works
        // But if someone adds "Cl" as monatomic, handle gracefully
        if (hAtoms == 1) { hCoeff = 2; }
        if (xAtoms == 1) { xCoeff = 2; }

        List<ReactionComponent> reactants = new ArrayList<>();
        reactants.add(new ReactionComponent(hFormula, hCoeff));
        reactants.add(new ReactionComponent(xFormula, xCoeff));

        List<ReactionComponent> products = new ArrayList<>();
        products.add(new ReactionComponent(product, prodCoeff));

        return new Reaction(reactants, products, ReactionType.SYNTHESIS,
                Reaction.ReactionConditions.ambient(),
                EnumSet.of(ReactionEffect.EXOTHERMIC));
    }

    /**
     * Resolve a formula to its element symbol. Returns null if not a pure element.
     */
    private static String resolveElementSymbol(String formula) {
        if (DIATOMIC_ELEMENTS.containsKey(formula)) {
            return DIATOMIC_ELEMENTS.get(formula)[0];
        }
        // Single element symbols: "Fe", "Na", "S", etc.
        Element elem = Element.fromSymbol(formula);
        return elem != null ? elem.getSymbol() : null;
    }

    /**
     * How many atoms of the element per molecule of this formula.
     */
    private static int resolveAtomsPerMolecule(String formula) {
        if (DIATOMIC_ELEMENTS.containsKey(formula)) {
            return Integer.parseInt(DIATOMIC_ELEMENTS.get(formula)[1]);
        }
        return 1;
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int t = b;
            b = a % b;
            a = t;
        }
        return a;
    }

    private static int lcm(int a, int b) {
        if (a == 0 || b == 0) return 1;
        return Math.abs(a * b) / gcd(a, b);
    }
}
