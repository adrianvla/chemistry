package org.keke.chemistry.utils;

/**
 * Utility methods for formatting chemical formulas with Unicode subscripts.
 */
public class FormulaUtils {

    private static final char[] SUBSCRIPT_DIGITS = {
            '\u2080', '\u2081', '\u2082', '\u2083', '\u2084',
            '\u2085', '\u2086', '\u2087', '\u2088', '\u2089'
    };

    /**
     * Formats a chemical formula string with Unicode subscript digits.
     * E.g., "H2SO4" → "H₂SO₄", "Ca(OH)2" → "Ca(OH)₂"
     */
    public static String formatFormula(String formula) {
        if (formula == null || formula.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < formula.length(); i++) {
            char c = formula.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(SUBSCRIPT_DIGITS[c - '0']);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
