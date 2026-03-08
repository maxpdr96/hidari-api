package com.hidariapi.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility generators for Brazilian fake data.
 */
public final class BrazilianDataGenerator {

    private BrazilianDataGenerator() {}

    public static String randomCpf() {
        int[] digits = new int[11];
        for (int i = 0; i < 9; i++) {
            digits[i] = ThreadLocalRandom.current().nextInt(10);
        }

        digits[9] = cpfCheckDigit(digits, 9, 10);
        digits[10] = cpfCheckDigit(digits, 10, 11);

        var sb = new StringBuilder(11);
        for (int d : digits) sb.append(d);
        return sb.toString();
    }

    private static int cpfCheckDigit(int[] digits, int length, int weightStart) {
        int sum = 0;
        int weight = weightStart;
        for (int i = 0; i < length; i++) {
            sum += digits[i] * weight--;
        }
        int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }
}
