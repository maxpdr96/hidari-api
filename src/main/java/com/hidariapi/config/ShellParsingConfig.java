package com.hidariapi.config;

import com.hidariapi.shell.LenientCommandParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;

@Configuration
public class ShellParsingConfig {

    @Bean
    @Primary
    public CommandParser commandParser(CommandRegistry commandRegistry) {
        return new LenientCommandParser(commandRegistry);
    }
}
