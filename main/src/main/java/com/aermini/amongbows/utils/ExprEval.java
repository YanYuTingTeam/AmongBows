package com.aermini.amongbows.utils;

import java.util.ArrayList;
import java.util.List;

public class ExprEval {

    public static boolean eval(String expr) {
        if (expr == null || expr.trim().isEmpty()) return true;
        try {
            List<String> tokens = tokenize(expr);
            return parseOr(tokens, new int[]{0});
        } catch (Exception e) {
            return false;
        }
    }

    private static List<String> tokenize(String s) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '&' && i + 1 < s.length() && s.charAt(i + 1) == '&') {
                tokens.add("&&");
                i += 2;
                continue;
            }
            if (c == '|' && i + 1 < s.length() && s.charAt(i + 1) == '|') {
                tokens.add("||");
                i += 2;
                continue;
            }
            if (c == '=') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '=') {
                    tokens.add("==");
                    i += 2;
                } else {
                    tokens.add("==");
                    i++;
                }
                continue;
            }
            if (c == '!' && i + 1 < s.length() && s.charAt(i + 1) == '=') {
                tokens.add("!=");
                i += 2;
                continue;
            }
            if ((c == '<' || c == '>')) {
                if (i + 1 < s.length() && s.charAt(i + 1) == '=') {
                    tokens.add(c + "=");
                    i += 2;
                } else {
                    tokens.add(String.valueOf(c));
                    i++;
                }
                continue;
            }
            int j = i;
            while (j < s.length() && "&|=!<>".indexOf(s.charAt(j)) < 0 && !Character.isWhitespace(s.charAt(j))) j++;
            if (j == i) j++;
            tokens.add(s.substring(i, j));
            i = j;
        }
        return tokens;
    }

    private static boolean parseOr(List<String> t, int[] idx) {
        boolean v = parseAnd(t, idx);
        while (idx[0] < t.size() && "||".equals(t.get(idx[0]))) {
            idx[0]++;
            v = parseAnd(t, idx) || v;
        }
        return v;
    }

    private static boolean parseAnd(List<String> t, int[] idx) {
        boolean v = parseCompare(t, idx);
        while (idx[0] < t.size() && "&&".equals(t.get(idx[0]))) {
            idx[0]++;
            v = parseCompare(t, idx) && v;
        }
        return v;
    }

    private static boolean parseCompare(List<String> t, int[] idx) {
        String left = t.get(idx[0]++);
        if (idx[0] >= t.size()) return truthy(left);
        String op = t.get(idx[0]);
        if (!isOp(op)) return truthy(left);
        idx[0]++;
        String right = t.get(idx[0]++);
        return compare(left, op, right);
    }

    private static boolean isOp(String s) {
        return s.equals("==") || s.equals("!=") || s.equals("<") || s.equals(">")
            || s.equals("<=") || s.equals(">=");
    }

    private static boolean truthy(String s) {
        return s.equalsIgnoreCase("true") || (isNumber(s) && Double.parseDouble(s) != 0);
    }

    private static boolean compare(String a, String op, String b) {
        Double na = isNumber(a) ? Double.parseDouble(a) : null;
        Double nb = isNumber(b) ? Double.parseDouble(b) : null;
        if (na != null && nb != null) {
            int c = Double.compare(na, nb);
            switch (op) {
                case "==": return c == 0;
                case "!=": return c != 0;
                case "<": return c < 0;
                case ">": return c > 0;
                case "<=": return c <= 0;
                default: return c >= 0;
            }
        }
        if (op.equals("==")) return a.equals(b);
        if (op.equals("!=")) return !a.equals(b);
        return false;
    }

    private static boolean isNumber(String s) {
        if (s == null || s.isEmpty()) return false;
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
