package com.bank.management.util;

/**
 * Masks full PAN for API responses (first 4 + middle mask + last 4).
 */
public final class PanMasking {

    private PanMasking() {
    }

    /**
     * Returns a masked PAN such as {@code 4444 **** **** 1111}.
     */
    public static String mask(String pan) {
        if (pan == null || pan.isBlank()) {
            return "****";
        }
        String digits = pan.replaceAll("\\D", "");
        if (digits.length() < 8) {
            return "****";
        }
        String first = digits.substring(0, 4);
        String last = digits.substring(digits.length() - 4);
        return first + " **** **** " + last;
    }
}
