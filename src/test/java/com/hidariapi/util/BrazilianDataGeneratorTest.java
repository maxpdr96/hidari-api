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

    @Test
    void randomCnpjGeneratesValidCnpjDigits() {
        for (int i = 0; i < 50; i++) {
            var cnpj = BrazilianDataGenerator.randomCnpj();
            assertTrue(cnpj.matches("\\d{14}"));
            assertTrue(isValidCnpj(cnpj));
        }
    }

    @Test
    void randomCepGeneratesEightDigits() {
        for (int i = 0; i < 20; i++) {
            assertTrue(BrazilianDataGenerator.randomCep().matches("\\d{8}"));
        }
    }

    @Test
    void randomPhoneBrGeneratesElevenDigitsStartingWithNineAfterDdd() {
        for (int i = 0; i < 20; i++) {
            assertTrue(BrazilianDataGenerator.randomPhoneBr().matches("\\d{2}9\\d{8}"));
        }
    }

    @Test
    void randomFullNameBrHasAtLeastThreeParts() {
        for (int i = 0; i < 20; i++) {
            var name = BrazilianDataGenerator.randomFullNameBr();
            assertTrue(name.split(" ").length >= 3);
        }
    }

    @Test
    void randomAddressBrHasExpectedSections() {
        for (int i = 0; i < 20; i++) {
            var address = BrazilianDataGenerator.randomAddressBr();
            assertTrue(address.contains(" - "));
            assertTrue(address.contains(", "));
            assertTrue(address.matches(".*\\d{8}$"));
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

    private boolean isValidCnpj(String cnpj) {
        int[] d = cnpj.chars().map(c -> c - '0').toArray();
        int d13 = cnpjCheckDigit(d, 12);
        int d14 = cnpjCheckDigit(d, 13);
        return d[12] == d13 && d[13] == d14;
    }

    private int cnpjCheckDigit(int[] digits, int length) {
        int[] weights = length == 12
                ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += digits[i] * weights[i];
        }
        int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }
}
