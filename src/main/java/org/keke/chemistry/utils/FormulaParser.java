package org.keke.chemistry.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses chemical formula strings like "H2SO4", "Ca(OH)2", "Cu(NO3)2", "KMnO4",
 * "(NH4)2[Ce(NO3)6]" into a Map of Element to stoichiometric count.
 * Supports both () and [] bracket notation.
 */
public class FormulaParser {

    public static Map<Element, Integer> parse(String formula) {
        if (formula == null || formula.isEmpty()) {
            return new HashMap<>();
        }
        int[] pos = {0};
        Map<Element, Integer> result = parseGroup(formula, pos, '\0');
        return result;
    }

    private static Map<Element, Integer> parseGroup(String formula, int[] pos, char closingBracket) {
        Map<Element, Integer> result = new HashMap<>();

        while (pos[0] < formula.length()) {
            char c = formula.charAt(pos[0]);

            if (c == '(' || c == '[') {
                char expectedClose = (c == '(') ? ')' : ']';
                pos[0]++; // skip opening bracket
                Map<Element, Integer> inner = parseGroup(formula, pos, expectedClose);
                // expect closing bracket
                if (pos[0] < formula.length() && formula.charAt(pos[0]) == expectedClose) {
                    pos[0]++; // skip closing bracket
                }
                int multiplier = parseNumber(formula, pos);
                for (Map.Entry<Element, Integer> entry : inner.entrySet()) {
                    result.merge(entry.getKey(), entry.getValue() * multiplier, Integer::sum);
                }
            } else if (c == ')' || c == ']') {
                break;
            } else if (Character.isUpperCase(c)) {
                String symbol = parseSymbol(formula, pos);
                int count = parseNumber(formula, pos);
                Element element = Element.fromSymbol(symbol);
                if (element != null) {
                    result.merge(element, count, Integer::sum);
                }
            } else {
                pos[0]++;
            }
        }
        return result;
    }

    private static String parseSymbol(String formula, int[] pos) {
        StringBuilder sb = new StringBuilder();
        sb.append(formula.charAt(pos[0]));
        pos[0]++;
        while (pos[0] < formula.length() && Character.isLowerCase(formula.charAt(pos[0]))) {
            sb.append(formula.charAt(pos[0]));
            pos[0]++;
        }
        return sb.toString();
    }

    private static int parseNumber(String formula, int[] pos) {
        StringBuilder sb = new StringBuilder();
        while (pos[0] < formula.length() && Character.isDigit(formula.charAt(pos[0]))) {
            sb.append(formula.charAt(pos[0]));
            pos[0]++;
        }
        if (sb.length() == 0) return 1;
        return Integer.parseInt(sb.toString());
    }
}
