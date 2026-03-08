package com.hidariapi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BrazilianDataGeneratorTest {

    @Test
    void randomCpfGeneratesValidCpfDigits() {
        for (int i = 0; i < 50; i++) {
            var cpf = BrazilianDataGenerator.randomCpf();
            assertTrue(cpf.matches("\\d{11}"));
            assertTrue(isValidCpf(cpf));
        }
    }

    private boolean isValidCpf(String cpf) {
        int[] d = cpf.chars().map(c -> c - '0').toArray();
        int d10 = checkDigit(d, 9, 10);
        int d11 = checkDigit(d, 10, 11);
        return d[9] == d10 && d[10] == d11;
    }

    private int checkDigit(int[] d, int length, int weightStart) {
        int sum = 0;
        int weight = weightStart;
        for (int i = 0; i < length; i++) {
            sum += d[i] * weight--;
        }
        int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }
}
