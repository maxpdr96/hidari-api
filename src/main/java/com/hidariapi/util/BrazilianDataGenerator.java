package com.hidariapi.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility generators for Brazilian fake data.
 */
public final class BrazilianDataGenerator {

    private static final String[] FIRST_NAMES = {
            "Ana", "Bruno", "Carlos", "Daniela", "Eduardo", "Fernanda", "Gabriel", "Helena", "Igor", "Juliana",
            "Lucas", "Mariana", "Nicolas", "Patricia", "Rafael", "Sabrina", "Tiago", "Vanessa", "Yuri", "Camila"
    };
    private static final String[] LAST_NAMES = {
            "Silva", "Santos", "Oliveira", "Souza", "Lima", "Pereira", "Costa", "Rodrigues", "Almeida", "Nascimento",
            "Araujo", "Carvalho", "Gomes", "Martins", "Rocha", "Dias", "Barbosa", "Freitas", "Mendes", "Moreira"
    };
    private static final String[] STREETS = {
            "Rua das Flores", "Avenida Brasil", "Rua XV de Novembro", "Rua Sao Jose", "Rua Principal",
            "Avenida Paulista", "Rua do Comercio", "Rua da Matriz", "Rua das Acacias", "Avenida Central"
    };
    private static final String[] NEIGHBORHOODS = {
            "Centro", "Jardim America", "Vila Nova", "Boa Vista", "Santo Antonio",
            "Santa Cecilia", "Bela Vista", "Liberdade", "Jardim Europa", "Cidade Nova"
    };
    private static final String[] CITIES = {
            "Sao Paulo", "Rio de Janeiro", "Belo Horizonte", "Curitiba", "Porto Alegre",
            "Salvador", "Recife", "Fortaleza", "Goiania", "Brasilia"
    };
    private static final String[] STATES = {"SP", "RJ", "MG", "PR", "RS", "BA", "PE", "CE", "GO", "DF"};

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

    public static String randomCnpj() {
        int[] digits = new int[14];
        for (int i = 0; i < 12; i++) {
            digits[i] = ThreadLocalRandom.current().nextInt(10);
        }

        digits[12] = cnpjCheckDigit(digits, 12);
        digits[13] = cnpjCheckDigit(digits, 13);

        var sb = new StringBuilder(14);
        for (int d : digits) sb.append(d);
        return sb.toString();
    }

    public static String randomCep() {
        int value = ThreadLocalRandom.current().nextInt(1_0000_0000);
        return String.format("%08d", value);
    }

    public static String randomPhoneBr() {
        int ddd = ThreadLocalRandom.current().nextInt(11, 100);
        int suffix = ThreadLocalRandom.current().nextInt(1_0000_0000);
        return String.format("%02d9%08d", ddd, suffix);
    }

    public static String randomFullNameBr() {
        var first = FIRST_NAMES[ThreadLocalRandom.current().nextInt(FIRST_NAMES.length)];
        var last1 = LAST_NAMES[ThreadLocalRandom.current().nextInt(LAST_NAMES.length)];
        var last2 = LAST_NAMES[ThreadLocalRandom.current().nextInt(LAST_NAMES.length)];
        return first + " " + last1 + " " + last2;
    }

    public static String randomAddressBr() {
        var street = STREETS[ThreadLocalRandom.current().nextInt(STREETS.length)];
        int number = ThreadLocalRandom.current().nextInt(1, 9999);
        var neighborhood = NEIGHBORHOODS[ThreadLocalRandom.current().nextInt(NEIGHBORHOODS.length)];
        int idx = ThreadLocalRandom.current().nextInt(CITIES.length);
        var city = CITIES[idx];
        var state = STATES[idx];
        var cep = randomCep();
        return street + ", " + number + " - " + neighborhood + ", " + city + " - " + state + ", " + cep;
    }

    private static int cnpjCheckDigit(int[] digits, int length) {
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
