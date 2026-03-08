package com.hidariapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada do HidariApi — testador de APIs via terminal.
 */
@SpringBootApplication
public class HidariApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(HidariApiApplication.class, args);
    }
}
