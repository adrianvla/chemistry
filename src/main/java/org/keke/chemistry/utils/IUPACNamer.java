package org.keke.chemistry.utils;

import java.util.*;

/**
 * Generates IUPAC-style names for inorganic compounds.
 * Covers:
 *   - Elements and diatomic molecules (H2, O2, N2)
 *   - Binary molecular compounds (CO2 → carbon dioxide)
 *   - Binary ionic compounds (NaCl → sodium chloride)
 *   - Ionic with polyatomic ions (CuSO4 → copper(II) sulfate)
 *   - Acids (HCl → hydrochloric acid, H2SO4 → sulfuric acid)
 *   - Hydroxides (NaOH → sodium hydroxide)
 *   - Common names for well-known compounds (H2O → water)
 *   - Hydrates via formula suffix (e.g. CuSO4.5H2O)
 *
 * Falls back to the raw formula when no rule matches.
 */
public class IUPACNamer {

    // ── Common-name overrides ──────────────────────────────────────────

    private static final Map<String, String> COMMON_NAMES = new LinkedHashMap<>();
    static {
        COMMON_NAMES.put("H2O",    "water");
        COMMON_NAMES.put("NH3",    "ammonia");
        COMMON_NAMES.put("CH4",    "methane");
        COMMON_NAMES.put("C2H6",   "ethane");
        COMMON_NAMES.put("C2H4",   "ethylene");
        COMMON_NAMES.put("C2H2",   "acetylene");
        COMMON_NAMES.put("C6H12O6","glucose");
        COMMON_NAMES.put("C12H22O11","sucrose");
        COMMON_NAMES.put("H2O2",   "hydrogen peroxide");
        COMMON_NAMES.put("NO",     "nitric oxide");
        COMMON_NAMES.put("NO2",    "nitrogen dioxide");
        COMMON_NAMES.put("N2O",    "nitrous oxide");
        COMMON_NAMES.put("N2O4",   "dinitrogen tetroxide");
        COMMON_NAMES.put("N2O5",   "dinitrogen pentoxide");
        COMMON_NAMES.put("CO",     "carbon monoxide");
        COMMON_NAMES.put("CO2",    "carbon dioxide");
        COMMON_NAMES.put("SO2",    "sulfur dioxide");
        COMMON_NAMES.put("SO3",    "sulfur trioxide");
        COMMON_NAMES.put("O3",     "ozone");
        COMMON_NAMES.put("C2H5OH", "ethanol");
        COMMON_NAMES.put("CH3OH",  "methanol");
        COMMON_NAMES.put("CH3COOH","acetic acid");
        COMMON_NAMES.put("HCN",    "hydrogen cyanide");
    }

    // ── Greek prefixes for molecular compounds ─────────────────────────

    private static final String[] GREEK = {
            "", "mono", "di", "tri", "tetra", "penta",
            "hexa", "hepta", "octa", "nona", "deca"
    };

    // ── Anion → acid name mappings ─────────────────────────────────────

    /** Polyatomic "-ate" ion → acid name. */
    private static final Map<String, String> OXYACID_NAMES = new LinkedHashMap<>();
    static {
        OXYACID_NAMES.put("SO4",    "sulfuric acid");
        OXYACID_NAMES.put("SO3",    "sulfurous acid");
        OXYACID_NAMES.put("NO3",    "nitric acid");
        OXYACID_NAMES.put("NO2",    "nitrous acid");
        OXYACID_NAMES.put("CO3",    "carbonic acid");
        OXYACID_NAMES.put("PO4",    "phosphoric acid");
        OXYACID_NAMES.put("ClO4",   "perchloric acid");
        OXYACID_NAMES.put("ClO3",   "chloric acid");
        OXYACID_NAMES.put("ClO2",   "chlorous acid");
        OXYACID_NAMES.put("ClO",    "hypochlorous acid");
        OXYACID_NAMES.put("CrO4",   "chromic acid");
        OXYACID_NAMES.put("Cr2O7",  "dichromic acid");
        OXYACID_NAMES.put("MnO4",   "permanganic acid");
        OXYACID_NAMES.put("C2O4",   "oxalic acid");
        OXYACID_NAMES.put("SiO3",   "silicic acid");
        OXYACID_NAMES.put("CH3COO", "acetic acid");
        OXYACID_NAMES.put("HPO4",   "hydrogen phosphoric acid");
        OXYACID_NAMES.put("H2PO4",  "dihydrogen phosphoric acid");
        OXYACID_NAMES.put("HCO3",   "hydrogen carbonic acid");
        OXYACID_NAMES.put("HSO4",   "hydrogen sulfuric acid");
    }

    /** Simple anion element → binary acid name. */
    private static final Map<String, String> BINARY_ACID_NAMES = new LinkedHashMap<>();
    static {
        BINARY_ACID_NAMES.put("F",  "hydrofluoric acid");
        BINARY_ACID_NAMES.put("Cl", "hydrochloric acid");
        BINARY_ACID_NAMES.put("Br", "hydrobromic acid");
        BINARY_ACID_NAMES.put("I",  "hydroiodic acid");
        BINARY_ACID_NAMES.put("S",  "hydrosulfuric acid");
        BINARY_ACID_NAMES.put("CN", "hydrocyanic acid");
        BINARY_ACID_NAMES.put("SCN","thiocyanic acid");
    }

    // ── Polyatomic ion → IUPAC name ────────────────────────────────────

    private static final Map<String, String> POLYATOMIC_ION_NAMES = new LinkedHashMap<>();
    static {
        POLYATOMIC_ION_NAMES.put("SO4",    "sulfate");
        POLYATOMIC_ION_NAMES.put("SO3",    "sulfite");
        POLYATOMIC_ION_NAMES.put("NO3",    "nitrate");
        POLYATOMIC_ION_NAMES.put("NO2",    "nitrite");
        POLYATOMIC_ION_NAMES.put("CO3",    "carbonate");
        POLYATOMIC_ION_NAMES.put("PO4",    "phosphate");
        POLYATOMIC_ION_NAMES.put("ClO4",   "perchlorate");
        POLYATOMIC_ION_NAMES.put("ClO3",   "chlorate");
        POLYATOMIC_ION_NAMES.put("ClO2",   "chlorite");
        POLYATOMIC_ION_NAMES.put("ClO",    "hypochlorite");
        POLYATOMIC_ION_NAMES.put("CrO4",   "chromate");
        POLYATOMIC_ION_NAMES.put("Cr2O7",  "dichromate");
        POLYATOMIC_ION_NAMES.put("MnO4",   "permanganate");
        POLYATOMIC_ION_NAMES.put("C2O4",   "oxalate");
        POLYATOMIC_ION_NAMES.put("SiO3",   "silicate");
        POLYATOMIC_ION_NAMES.put("OH",     "hydroxide");
        POLYATOMIC_ION_NAMES.put("CN",     "cyanide");
        POLYATOMIC_ION_NAMES.put("SCN",    "thiocyanate");
        POLYATOMIC_ION_NAMES.put("CH3COO", "acetate");
        POLYATOMIC_ION_NAMES.put("HCO3",   "hydrogen carbonate");
        POLYATOMIC_ION_NAMES.put("HSO4",   "hydrogen sulfate");
        POLYATOMIC_ION_NAMES.put("HPO4",   "hydrogen phosphate");
        POLYATOMIC_ION_NAMES.put("H2PO4",  "dihydrogen phosphate");
        POLYATOMIC_ION_NAMES.put("O2",     "peroxide");
        POLYATOMIC_ION_NAMES.put("NH4",    "ammonium");
    }

    // ── Element names and anion stems ──────────────────────────────────

    private static final Map<String, String> ELEMENT_NAMES = new LinkedHashMap<>();
    private static final Map<String, String> ANION_STEMS = new LinkedHashMap<>();
    static {
        ELEMENT_NAMES.put("H",  "hydrogen");   ANION_STEMS.put("H",  "hydride");
        ELEMENT_NAMES.put("He", "helium");
        ELEMENT_NAMES.put("Li", "lithium");
        ELEMENT_NAMES.put("Be", "beryllium");
        ELEMENT_NAMES.put("B",  "boron");       ANION_STEMS.put("B",  "boride");
        ELEMENT_NAMES.put("C",  "carbon");      ANION_STEMS.put("C",  "carbide");
        ELEMENT_NAMES.put("N",  "nitrogen");    ANION_STEMS.put("N",  "nitride");
        ELEMENT_NAMES.put("O",  "oxygen");      ANION_STEMS.put("O",  "oxide");
        ELEMENT_NAMES.put("F",  "fluorine");    ANION_STEMS.put("F",  "fluoride");
        ELEMENT_NAMES.put("Ne", "neon");
        ELEMENT_NAMES.put("Na", "sodium");
        ELEMENT_NAMES.put("Mg", "magnesium");
        ELEMENT_NAMES.put("Al", "aluminum");
        ELEMENT_NAMES.put("Si", "silicon");     ANION_STEMS.put("Si", "silicide");
        ELEMENT_NAMES.put("P",  "phosphorus");  ANION_STEMS.put("P",  "phosphide");
        ELEMENT_NAMES.put("S",  "sulfur");      ANION_STEMS.put("S",  "sulfide");
        ELEMENT_NAMES.put("Cl", "chlorine");    ANION_STEMS.put("Cl", "chloride");
        ELEMENT_NAMES.put("Ar", "argon");
        ELEMENT_NAMES.put("K",  "potassium");
        ELEMENT_NAMES.put("Ca", "calcium");
        ELEMENT_NAMES.put("Sc", "scandium");
        ELEMENT_NAMES.put("Ti", "titanium");
        ELEMENT_NAMES.put("V",  "vanadium");
        ELEMENT_NAMES.put("Cr", "chromium");
        ELEMENT_NAMES.put("Mn", "manganese");
        ELEMENT_NAMES.put("Fe", "iron");
        ELEMENT_NAMES.put("Co", "cobalt");
        ELEMENT_NAMES.put("Ni", "nickel");
        ELEMENT_NAMES.put("Cu", "copper");
        ELEMENT_NAMES.put("Zn", "zinc");
        ELEMENT_NAMES.put("Ga", "gallium");
        ELEMENT_NAMES.put("Ge", "germanium");   ANION_STEMS.put("Ge", "germanide");
        ELEMENT_NAMES.put("As", "arsenic");      ANION_STEMS.put("As", "arsenide");
        ELEMENT_NAMES.put("Se", "selenium");     ANION_STEMS.put("Se", "selenide");
        ELEMENT_NAMES.put("Br", "bromine");      ANION_STEMS.put("Br", "bromide");
        ELEMENT_NAMES.put("Kr", "krypton");
        ELEMENT_NAMES.put("Rb", "rubidium");
        ELEMENT_NAMES.put("Sr", "strontium");
        ELEMENT_NAMES.put("Y",  "yttrium");
        ELEMENT_NAMES.put("Zr", "zirconium");
        ELEMENT_NAMES.put("Nb", "niobium");
        ELEMENT_NAMES.put("Mo", "molybdenum");
        ELEMENT_NAMES.put("Tc", "technetium");
        ELEMENT_NAMES.put("Ru", "ruthenium");
        ELEMENT_NAMES.put("Rh", "rhodium");
        ELEMENT_NAMES.put("Pd", "palladium");
        ELEMENT_NAMES.put("Ag", "silver");
        ELEMENT_NAMES.put("Cd", "cadmium");
        ELEMENT_NAMES.put("In", "indium");
        ELEMENT_NAMES.put("Sn", "tin");          ANION_STEMS.put("Sn", "stannide");
        ELEMENT_NAMES.put("Sb", "antimony");     ANION_STEMS.put("Sb", "antimonide");
        ELEMENT_NAMES.put("Te", "tellurium");    ANION_STEMS.put("Te", "telluride");
        ELEMENT_NAMES.put("I",  "iodine");       ANION_STEMS.put("I",  "iodide");
        ELEMENT_NAMES.put("Xe", "xenon");
        ELEMENT_NAMES.put("Cs", "cesium");
        ELEMENT_NAMES.put("Ba", "barium");
        ELEMENT_NAMES.put("La", "lanthanum");
        ELEMENT_NAMES.put("Ce", "cerium");
        ELEMENT_NAMES.put("Pr", "praseodymium");
        ELEMENT_NAMES.put("Nd", "neodymium");
        ELEMENT_NAMES.put("Pm", "promethium");
        ELEMENT_NAMES.put("Sm", "samarium");
        ELEMENT_NAMES.put("Eu", "europium");
        ELEMENT_NAMES.put("Gd", "gadolinium");
        ELEMENT_NAMES.put("Tb", "terbium");
        ELEMENT_NAMES.put("Dy", "dysprosium");
        ELEMENT_NAMES.put("Ho", "holmium");
        ELEMENT_NAMES.put("Er", "erbium");
        ELEMENT_NAMES.put("Tm", "thulium");
        ELEMENT_NAMES.put("Yb", "ytterbium");
        ELEMENT_NAMES.put("Lu", "lutetium");
        ELEMENT_NAMES.put("Hf", "hafnium");
        ELEMENT_NAMES.put("Ta", "tantalum");
        ELEMENT_NAMES.put("W",  "tungsten");
        ELEMENT_NAMES.put("Re", "rhenium");
        ELEMENT_NAMES.put("Os", "osmium");
        ELEMENT_NAMES.put("Ir", "iridium");
        ELEMENT_NAMES.put("Pt", "platinum");
        ELEMENT_NAMES.put("Au", "gold");
        ELEMENT_NAMES.put("Hg", "mercury");
        ELEMENT_NAMES.put("Tl", "thallium");
        ELEMENT_NAMES.put("Pb", "lead");         ANION_STEMS.put("Pb", "plumbide");
        ELEMENT_NAMES.put("Bi", "bismuth");      ANION_STEMS.put("Bi", "bismuthide");
        ELEMENT_NAMES.put("Po", "polonium");
        ELEMENT_NAMES.put("At", "astatine");     ANION_STEMS.put("At", "astatide");
        ELEMENT_NAMES.put("Rn", "radon");
        ELEMENT_NAMES.put("Fr", "francium");
        ELEMENT_NAMES.put("Ra", "radium");
        ELEMENT_NAMES.put("Ac", "actinium");
        ELEMENT_NAMES.put("Th", "thorium");
        ELEMENT_NAMES.put("Pa", "protactinium");
        ELEMENT_NAMES.put("U",  "uranium");
        ELEMENT_NAMES.put("Np", "neptunium");
        ELEMENT_NAMES.put("Pu", "plutonium");
        ELEMENT_NAMES.put("Am", "americium");
        ELEMENT_NAMES.put("Cm", "curium");
    }

    /** Metals that can have multiple oxidation states → need Roman numeral. */
    private static final Set<String> VARIABLE_CHARGE_METALS = Set.of(
            "Fe", "Cu", "Co", "Ni", "Mn", "Cr", "V", "Ti",
            "Sn", "Pb", "Hg", "Au", "Pt", "Pd", "W", "Mo"
    );

    /** Non-metals and metalloids that form molecular (covalent) compounds. */
    private static final Set<String> NONMETALS = Set.of(
            "H", "He", "C", "N", "O", "F", "Ne", "P", "S", "Cl",
            "Ar", "Se", "Br", "Kr", "I", "Xe", "At", "Rn",
            "B", "Si", "Ge", "As", "Sb", "Te"
    );

    // ── Public API ─────────────────────────────────────────────────────

    /**
     * Return the IUPAC name for a chemical formula.
     * Falls back to the raw formula if no naming rule matches.
     */
    public static String getName(String formula) {
        if (formula == null || formula.isEmpty()) return "";

        // Check common-name overrides first
        if (COMMON_NAMES.containsKey(formula)) {
            return COMMON_NAMES.get(formula);
        }

        // Single element or diatomic element
        String elemName = tryElementName(formula);
        if (elemName != null) return elemName;

        // Use IonicCompound decomposition for structured naming
        IonicCompound ionic = IonicCompound.decompose(formula);
        if (ionic != null) {
            return nameFromIonic(ionic);
        }

        // Try binary molecular naming (two non-metal elements)
        String molName = tryBinaryMolecular(formula);
        if (molName != null) return molName;

        // Fallback
        return formula;
    }

    // ── Internal naming methods ────────────────────────────────────────

    /**
     * Try to name as a single element or diatomic molecule.
     * "Fe" → "iron", "O2" → "oxygen", "S8" → "sulfur"
     */
    private static String tryElementName(String formula) {
        // Match patterns like "Fe", "O2", "S8", "N2"
        if (!formula.matches("[A-Z][a-z]?\\d*")) return null;

        String sym = formula.replaceAll("\\d+", "");
        return ELEMENT_NAMES.get(sym);
    }

    /**
     * Name an ionic compound using the IonicCompound decomposition.
     */
    private static String nameFromIonic(IonicCompound ic) {
        String cation = ic.getCation();
        String anion = ic.getAnion();

        // Acid naming (cation is H)
        if ("H".equals(cation)) {
            return nameAcid(anion);
        }

        // Build cation name
        String cationName = nameCation(cation, ic.getCationCharge());

        // Build anion name
        String anionName = nameAnion(anion);

        return cationName + " " + anionName;
    }

    /**
     * Name an acid from its anion.
     */
    private static String nameAcid(String anion) {
        // Check oxyacid map
        if (OXYACID_NAMES.containsKey(anion)) {
            return OXYACID_NAMES.get(anion);
        }
        // Check binary acid map
        if (BINARY_ACID_NAMES.containsKey(anion)) {
            return BINARY_ACID_NAMES.get(anion);
        }
        // Fallback
        return "hydrogen " + nameAnion(anion);
    }

    /**
     * Name a cation, adding Roman numeral for variable-charge metals.
     */
    private static String nameCation(String cation, int charge) {
        // Polyatomic cation (NH4)
        if (POLYATOMIC_ION_NAMES.containsKey(cation)) {
            return POLYATOMIC_ION_NAMES.get(cation);
        }

        String eName = ELEMENT_NAMES.getOrDefault(cation, cation.toLowerCase());

        if (VARIABLE_CHARGE_METALS.contains(cation) && charge > 0) {
            return eName + "(" + toRoman(charge) + ")";
        }
        return eName;
    }

    /**
     * Name an anion (polyatomic or simple element).
     */
    private static String nameAnion(String anion) {
        if (POLYATOMIC_ION_NAMES.containsKey(anion)) {
            return POLYATOMIC_ION_NAMES.get(anion);
        }
        if (ANION_STEMS.containsKey(anion)) {
            return ANION_STEMS.get(anion);
        }
        // Unknown anion — return lowercase
        return anion.toLowerCase();
    }

    /**
     * Try binary molecular naming for two-non-metal compounds.
     * Only matches formulas with exactly two element symbols.
     * E.g., "PCl5" → "phosphorus pentachloride"
     */
    private static String tryBinaryMolecular(String formula) {
        // Parse the formula
        Map<Element, Integer> comp = FormulaParser.parse(formula);
        if (comp.size() != 2) return null;

        // Both must be non-metals
        List<Map.Entry<Element, Integer>> entries = new ArrayList<>(comp.entrySet());
        Element first = entries.get(0).getKey();
        Element second = entries.get(1).getKey();

        if (!NONMETALS.contains(first.getSymbol()) || !NONMETALS.contains(second.getSymbol())) {
            return null;
        }

        int count1 = entries.get(0).getValue();
        int count2 = entries.get(1).getValue();

        // Electronegativity ordering: first element is less electronegative
        String name1 = ELEMENT_NAMES.getOrDefault(first.getSymbol(), first.getSymbol().toLowerCase());
        String name2 = nameAnion(second.getSymbol());

        // Apply Greek prefixes
        String prefix1 = (count1 > 1 && count1 < GREEK.length) ? GREEK[count1] : "";
        String prefix2 = (count2 > 0 && count2 < GREEK.length) ? GREEK[count2] : "";

        // Drop trailing 'a'/'o' from prefix if name starts with vowel
        if (!prefix1.isEmpty() && startsWithVowel(name1)) {
            prefix1 = dropTrailingVowel(prefix1);
        }
        if (!prefix2.isEmpty() && startsWithVowel(name2)) {
            prefix2 = dropTrailingVowel(prefix2);
        }

        return prefix1 + name1 + " " + prefix2 + name2;
    }

    // ── Utility ────────────────────────────────────────────────────────

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            default -> String.valueOf(n);
        };
    }

    private static boolean startsWithVowel(String s) {
        if (s.isEmpty()) return false;
        char c = Character.toLowerCase(s.charAt(0));
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    private static String dropTrailingVowel(String prefix) {
        if (prefix.isEmpty()) return prefix;
        char last = prefix.charAt(prefix.length() - 1);
        if (last == 'a' || last == 'o') {
            return prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }
}
