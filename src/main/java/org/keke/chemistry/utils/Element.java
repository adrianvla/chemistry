package org.keke.chemistry.utils;

import java.util.HashMap;
import java.util.Map;

public enum Element {
    // Period 1
    H("H", 1, 1.008, 0xFFFFFF),
    He("He", 2, 4.003, 0xFFFFFF),
    // Period 2
    Li("Li", 3, 6.941, 0xFFFFFF),
    Be("Be", 4, 9.012, 0xFFFFFF),
    B("B", 5, 10.81, 0xFFFFFF),
    C("C", 6, 12.011, 0xFFFFFF),
    N("N", 7, 14.007, 0xFFFFFF),
    O("O", 8, 15.999, 0xFFFFFF),
    F("F", 9, 18.998, 0xFFFFFF),
    Ne("Ne", 10, 20.180, 0xFFFFFF),
    // Period 3
    Na("Na", 11, 22.990, 0xFFFFFF),
    Mg("Mg", 12, 24.305, 0xFFFFFF),
    Al("Al", 13, 26.982, 0xFFFFFF),
    Si("Si", 14, 28.086, 0xFFFFFF),
    P("P", 15, 30.974, 0xFFFFFF),
    S("S", 16, 32.065, 0xFFFFFF),
    Cl("Cl", 17, 35.453, 0xFFFFFF),
    Ar("Ar", 18, 39.948, 0xFFFFFF),
    // Period 4
    K("K", 19, 39.098, 0xFFFFFF),
    Ca("Ca", 20, 40.078, 0xFFFFFF),
    Sc("Sc", 21, 44.956, 0xCCCCCC),
    Ti("Ti", 22, 47.867, 0xFFFFFF),
    V("V", 23, 50.942, 0xFFFFFF),
    Cr("Cr", 24, 51.996, 0x8A2BE2),  // Cr³⁺ violet
    Mn("Mn", 25, 54.938, 0x800080),  // MnO₄⁻ purple
    Fe("Fe", 26, 55.845, 0xB87333),  // Fe³⁺ yellow-brown
    Co("Co", 27, 58.933, 0xFF69B4),  // Co²⁺ pink
    Ni("Ni", 28, 58.693, 0x00CC00),  // Ni²⁺ green
    Cu("Cu", 29, 63.546, 0x1E90FF),  // Cu²⁺ blue
    Zn("Zn", 30, 65.380, 0xFFFFFF),
    Ga("Ga", 31, 69.723, 0xFFFFFF),
    Ge("Ge", 32, 72.630, 0xCCCCCC),
    As("As", 33, 74.922, 0xFFFFFF),
    Se("Se", 34, 78.960, 0xFFFFFF),
    Br("Br", 35, 79.904, 0xCC6600),  // Br₂ brown-orange
    Kr("Kr", 36, 83.798, 0xFFFFFF),
    // Period 5
    Rb("Rb", 37, 85.468, 0xFFFFFF),
    Sr("Sr", 38, 87.620, 0xFFFFFF),
    Y("Y", 39, 88.906, 0xCCCCCC),
    Zr("Zr", 40, 91.224, 0xCCCCCC),
    Nb("Nb", 41, 92.906, 0xCCCCCC),
    Mo("Mo", 42, 95.950, 0xCCCCCC),
    Tc("Tc", 43, 98.000, 0xCCCCCC),
    Ru("Ru", 44, 101.070, 0xCCCCCC),
    Rh("Rh", 45, 102.906, 0xCCCCCC),
    Pd("Pd", 46, 106.420, 0xCCCCCC),
    Ag("Ag", 47, 107.868, 0xFFFFFF),
    Cd("Cd", 48, 112.411, 0xFFFFFF),
    In("In", 49, 114.818, 0xCCCCCC),
    Sn("Sn", 50, 118.710, 0xFFFFFF),
    Sb("Sb", 51, 121.760, 0xFFFFFF),
    Te("Te", 52, 127.600, 0xCCCCCC),
    I("I", 53, 126.904, 0x8B4513),   // I₂ brown
    Xe("Xe", 54, 131.293, 0xCCCCCC),
    // Period 6
    Cs("Cs", 55, 132.905, 0xFFFFFF),
    Ba("Ba", 56, 137.327, 0xFFFFFF),
    La("La", 57, 138.905, 0xCCCCCC),
    Ce("Ce", 58, 140.116, 0xCCCCCC),
    Pr("Pr", 59, 140.908, 0x90EE90),  // Pr³⁺ green
    Nd("Nd", 60, 144.242, 0xCC99FF),  // Nd³⁺ lilac
    Pm("Pm", 61, 145.000, 0xCCCCCC),
    Sm("Sm", 62, 150.360, 0xCCCCCC),
    Eu("Eu", 63, 151.964, 0xCCCCCC),
    Gd("Gd", 64, 157.250, 0xCCCCCC),
    Tb("Tb", 65, 158.925, 0xCCCCCC),
    Dy("Dy", 66, 162.500, 0xCCCCCC),
    Ho("Ho", 67, 164.930, 0xFFCC00),  // Ho³⁺ yellow
    Er("Er", 68, 167.259, 0xFF99CC),  // Er³⁺ pink
    Tm("Tm", 69, 168.934, 0xCCCCCC),
    Yb("Yb", 70, 173.045, 0xCCCCCC),
    Lu("Lu", 71, 174.967, 0xCCCCCC),
    Hf("Hf", 72, 178.490, 0xCCCCCC),
    Ta("Ta", 73, 180.948, 0xCCCCCC),
    W("W", 74, 183.840, 0xFFFFFF),
    Re("Re", 75, 186.207, 0xCCCCCC),
    Os("Os", 76, 190.230, 0xCCCCCC),
    Ir("Ir", 77, 192.217, 0xCCCCCC),
    Pt("Pt", 78, 195.084, 0xFFFFFF),
    Au("Au", 79, 196.967, 0xFFD700),  // Au³⁺ gold
    Hg("Hg", 80, 200.590, 0xFFFFFF),
    Tl("Tl", 81, 204.383, 0xCCCCCC),
    Pb("Pb", 82, 207.200, 0xFFFFFF),
    Bi("Bi", 83, 208.980, 0xFFFFFF),
    Po("Po", 84, 209.000, 0xCCCCCC),
    At("At", 85, 210.000, 0xCCCCCC),
    Rn("Rn", 86, 222.000, 0xCCCCCC),
    // Period 7
    Fr("Fr", 87, 223.000, 0xCCCCCC),
    Ra("Ra", 88, 226.000, 0xCCCCCC),
    Ac("Ac", 89, 227.000, 0xCCCCCC),
    Th("Th", 90, 232.038, 0xCCCCCC),
    Pa("Pa", 91, 231.036, 0xCCCCCC),
    U("U", 92, 238.029, 0x00FF00),   // UO₂²⁺ fluorescent green
    Np("Np", 93, 237.000, 0xCCCCCC),
    Pu("Pu", 94, 244.000, 0xCCCCCC),
    Am("Am", 95, 243.000, 0xCCCCCC),
    Cm("Cm", 96, 247.000, 0xCCCCCC),
    Bk("Bk", 97, 247.000, 0xCCCCCC),
    Cf("Cf", 98, 251.000, 0xCCCCCC),
    Es("Es", 99, 252.000, 0xCCCCCC),
    Fm("Fm", 100, 257.000, 0xCCCCCC),
    Md("Md", 101, 258.000, 0xCCCCCC),
    No("No", 102, 259.000, 0xCCCCCC),
    Lr("Lr", 103, 266.000, 0xCCCCCC),
    Rf("Rf", 104, 267.000, 0xCCCCCC),
    Db("Db", 105, 268.000, 0xCCCCCC),
    Sg("Sg", 106, 269.000, 0xCCCCCC),
    Bh("Bh", 107, 270.000, 0xCCCCCC),
    Hs("Hs", 108, 277.000, 0xCCCCCC),
    Mt("Mt", 109, 278.000, 0xCCCCCC),
    Ds("Ds", 110, 281.000, 0xCCCCCC),
    Rg("Rg", 111, 282.000, 0xCCCCCC),
    Cn("Cn", 112, 285.000, 0xCCCCCC),
    Nh("Nh", 113, 286.000, 0xCCCCCC),
    Fl("Fl", 114, 289.000, 0xCCCCCC),
    Mc("Mc", 115, 290.000, 0xCCCCCC),
    Lv("Lv", 116, 293.000, 0xCCCCCC),
    Ts("Ts", 117, 294.000, 0xCCCCCC),
    Og("Og", 118, 294.000, 0xCCCCCC);

    private final String symbol;
    private final int atomicNumber;
    private final double molarMass;
    private final int ionColorRgb;

    private static final Map<String, Element> BY_SYMBOL = new HashMap<>();

    static {
        for (Element e : values()) {
            BY_SYMBOL.put(e.symbol, e);
        }
    }

    Element(String symbol, int atomicNumber, double molarMass, int ionColorRgb) {
        this.symbol = symbol;
        this.atomicNumber = atomicNumber;
        this.molarMass = molarMass;
        this.ionColorRgb = ionColorRgb;
    }

    public String getSymbol() { return symbol; }
    public int getAtomicNumber() { return atomicNumber; }
    public double getMolarMass() { return molarMass; }
    public int getIonColorRgb() { return ionColorRgb; }

    public static Element fromSymbol(String symbol) {
        return BY_SYMBOL.get(symbol);
    }

    public static boolean isElement(String symbol) {
        return BY_SYMBOL.containsKey(symbol);
    }
}
