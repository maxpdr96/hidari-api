package com.hidariapi.shell;

import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandArgument;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.CommandParser;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.ParsedInput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Parser that preserves multi-word URL values for HTTP commands without requiring quotes.
 */
public class LenientCommandParser implements CommandParser {

    private static final Set<String> SINGLE_URL_ARGUMENT_COMMANDS = Set.of(
            "get", "post", "put", "patch", "delete", "head", "options", "bench");

    private static final String SEND_COMMAND = "send";

    private final CommandRegistry commandRegistry;

    public LenientCommandParser(CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    @Override
    public ParsedInput parse(String input) {
        List<String> words = List.of(input.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)"));

        String commandName = words.get(0);
        ParsedInput.Builder parsedInputBuilder = ParsedInput.builder().commandName(commandName);
        if (words.size() == 1) {
            return parsedInputBuilder.build();
        }

        if (words.size() == 2 && (words.get(1).equals("--help") || words.get(1).equals("-h"))) {
            parsedInputBuilder.addOption(CommandOption.with().shortName('h').longName("help").value("true").build());
            return parsedInputBuilder.build();
        }

        List<String> remainingWords = words.subList(1, words.size());

        int firstNonCommandWordIndex = 1;
        String prefix = commandName;
        for (String remainingWord : remainingWords) {
            String potentialCommandName = prefix + " " + remainingWord;
            List<Command> commands = commandRegistry.getCommandsByPrefix(potentialCommandName);
            if (commands.isEmpty()) {
                break;
            }
            prefix = potentialCommandName;
            firstNonCommandWordIndex++;
        }
        List<String> subCommands = firstNonCommandWordIndex == 1 ? Collections.emptyList()
                : words.subList(1, firstNonCommandWordIndex);

        for (String subCommand : subCommands) {
            parsedInputBuilder.addSubCommand(subCommand);
        }

        List<String> optionsAndArguments = words.subList(firstNonCommandWordIndex, words.size());
        optionsAndArguments = mergeMultiWordUrlValues(commandName, optionsAndArguments);
        if (!optionsAndArguments.isEmpty() && isArgumentSeparator(optionsAndArguments.get(0))) {
            optionsAndArguments = optionsAndArguments.subList(1, optionsAndArguments.size());
            int argumentIndex = 0;
            for (String remainingWord : optionsAndArguments) {
                parsedInputBuilder.addArgument(parseArgument(argumentIndex++, remainingWord));
            }
        } else {
            int argumentIndex = 0;
            for (int i = 0; i < optionsAndArguments.size(); i++) {
                String currentWord = optionsAndArguments.get(i);
                String nextWord = i + 1 < optionsAndArguments.size() ? optionsAndArguments.get(i + 1) : null;
                if (isOption(currentWord)) {
                    if (currentWord.contains("=")) {
                        parsedInputBuilder.addOption(parseOption(currentWord));
                    } else {
                        if (nextWord == null || isOption(nextWord) || isArgumentSeparator(nextWord)) {
                            if (!isBooleanOption(commandName, currentWord)) {
                                throw new IllegalArgumentException("Option '" + currentWord + "' requires a value");
                            }
                            nextWord = "true";
                        } else {
                            i++;
                        }
                        parsedInputBuilder.addOption(parseOption(currentWord + "=" + nextWord));
                    }
                } else {
                    parsedInputBuilder.addArgument(parseArgument(argumentIndex++, currentWord));
                }
            }
        }
        return parsedInputBuilder.build();
    }

    private List<String> mergeMultiWordUrlValues(String commandName, List<String> words) {
        if (words.isEmpty()) return words;

        var normalized = new ArrayList<String>();
        int argumentIndex = 0;

        for (int i = 0; i < words.size(); i++) {
            String currentWord = words.get(i);
            if (isOption(currentWord)) {
                if (isUrlOption(currentWord)) {
                    var merged = mergeOptionValue(words, i);
                    normalized.add(merged.token());
                    i = merged.lastIndex();
                } else {
                    normalized.add(currentWord);
                }
                continue;
            }

            if (shouldMergeUrlArgument(commandName, argumentIndex)) {
                var merged = mergeArgumentValue(words, i);
                normalized.add(merged.token());
                i = merged.lastIndex();
            } else {
                normalized.add(currentWord);
            }
            argumentIndex++;
        }

        return normalized;
    }

    private boolean shouldMergeUrlArgument(String commandName, int argumentIndex) {
        if (SINGLE_URL_ARGUMENT_COMMANDS.contains(commandName)) {
            return argumentIndex == 0;
        }
        return SEND_COMMAND.equals(commandName) && argumentIndex == 1;
    }

    private boolean isUrlOption(String word) {
        return word.equals("--url") || word.startsWith("--url=");
    }

    private MergeResult mergeOptionValue(List<String> words, int optionIndex) {
        String optionToken = words.get(optionIndex);
        int startIndex = optionIndex + 1;
        String optionPrefix = optionToken;
        StringBuilder value = new StringBuilder();

        if (optionToken.contains("=")) {
            String[] parts = optionToken.split("=", 2);
            optionPrefix = parts[0];
            value.append(parts[1]);
        }

        int lastIndex = optionIndex;
        for (int i = startIndex; i < words.size(); i++) {
            String candidate = words.get(i);
            if (isOption(candidate) || isArgumentSeparator(candidate)) {
                break;
            }
            if (!value.isEmpty()) {
                value.append(' ');
            }
            value.append(candidate);
            lastIndex = i;
        }

        if (value.isEmpty()) {
            return new MergeResult(optionToken, optionIndex);
        }
        return new MergeResult(optionPrefix + "=" + value, lastIndex);
    }

    private MergeResult mergeArgumentValue(List<String> words, int argumentIndex) {
        StringBuilder value = new StringBuilder(words.get(argumentIndex));
        int lastIndex = argumentIndex;
        for (int i = argumentIndex + 1; i < words.size(); i++) {
            String candidate = words.get(i);
            if (isOption(candidate) || isArgumentSeparator(candidate)) {
                break;
            }
            value.append(' ').append(candidate);
            lastIndex = i;
        }
        return new MergeResult(value.toString(), lastIndex);
    }

    private boolean isArgumentSeparator(String word) {
        return word.equals("--");
    }

    private boolean isOption(String word) {
        return (word.startsWith("-") || word.startsWith("--")) && !isArgumentSeparator(word);
    }

    private CommandOption parseOption(String word) {
        char shortName = ' ';
        String longName = "";
        String value = "";
        if (word.startsWith("--")) {
            word = word.substring(2);
            String[] tokens = word.split("=", 2);
            longName = tokens[0];
            if (tokens.length > 1) {
                value = tokens[1];
            }
        } else if (word.startsWith("-")) {
            word = word.substring(1);
            String[] tokens = word.split("=", 2);
            shortName = tokens[0].charAt(0);
            if (tokens.length > 1) {
                value = tokens[1];
            }
        }
        return CommandOption.with()
                .shortName(shortName)
                .longName(longName)
                .value(unquoteAndUnescapeQuoted(value))
                .build();
    }

    private CommandArgument parseArgument(int index, String word) {
        return new CommandArgument(index, unquoteAndUnescapeQuoted(word));
    }

    private String unquoteAndUnescapeQuoted(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
            s = s.replace("\\\"", "\"").replace("\\\\", "\\");
        }
        return s;
    }

    private boolean isBooleanOption(String commandName, String currentWord) {
        return Optional.ofNullable(commandRegistry.getCommandByName(commandName))
                .map(Command::getOptions)
                .orElse(List.of())
                .stream()
                .filter(o -> o.isOptionEqual(currentWord))
                .anyMatch(o -> o.type() == boolean.class || o.type() == Boolean.class);
    }

    private record MergeResult(String token, int lastIndex) {}
}
