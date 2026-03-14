package com.hidariapi.shell;

import org.junit.jupiter.api.Test;
import org.springframework.shell.core.command.Command;
import org.springframework.shell.core.command.CommandOption;
import org.springframework.shell.core.command.CommandRegistry;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LenientCommandParserTest {

    private final LenientCommandParser parser = new LenientCommandParser(commandRegistry());

    @Test
    void mergesMultiWordUrlOptionUntilNextOption() {
        var parsed = parser.parse("get --url http://localhost:8080/chat?msg=Me diga uma curiosidade inutil. --call 2");

        assertEquals("http://localhost:8080/chat?msg=Me diga uma curiosidade inutil.", parsed.options().get(0).value());
        assertEquals("2", parsed.options().get(1).value());
    }

    @Test
    void mergesMultiWordPositionalUrlForHttpCommands() {
        var parsed = parser.parse("get http://localhost:8080/chat?msg=Me diga uma curiosidade inutil. --call 2");

        assertEquals("http://localhost:8080/chat?msg=Me diga uma curiosidade inutil.", parsed.arguments().get(0).value());
        assertEquals("2", parsed.options().get(0).value());
    }

    @Test
    void mergesMultiWordUrlOptionForOtherHttpCommands() {
        var postParsed = parser.parse("post --url http://localhost:8080/chat?msg=Me diga uma curiosidade inutil. --body {}");
        var putParsed = parser.parse("put --url http://localhost:8080/chat?msg=Me diga uma curiosidade inutil. --body {}");
        var deleteParsed = parser.parse("delete --url http://localhost:8080/chat?msg=Me diga uma curiosidade inutil. --call 1");

        assertEquals("http://localhost:8080/chat?msg=Me diga uma curiosidade inutil.", postParsed.options().get(0).value());
        assertEquals("http://localhost:8080/chat?msg=Me diga uma curiosidade inutil.", putParsed.options().get(0).value());
        assertEquals("http://localhost:8080/chat?msg=Me diga uma curiosidade inutil.", deleteParsed.options().get(0).value());
    }

    @Test
    void mergesMultiWordPositionalUrlForOtherHttpCommands() {
        var postParsed = parser.parse("post http://localhost:8080/chat?msg=Me diga uma curiosidade inutil. --body {}");
        var putParsed = parser.parse("put http://localhost:8080/chat?msg=Me diga uma curiosidade inutil. --body {}");
        var deleteParsed = parser.parse("delete http://localhost:8080/chat?msg=Me diga uma curiosidade inutil. --call 1");

        assertEquals("http://localhost:8080/chat?msg=Me diga uma curiosidade inutil.", postParsed.arguments().get(0).value());
        assertEquals("http://localhost:8080/chat?msg=Me diga uma curiosidade inutil.", putParsed.arguments().get(0).value());
        assertEquals("http://localhost:8080/chat?msg=Me diga uma curiosidade inutil.", deleteParsed.arguments().get(0).value());
    }

    private CommandRegistry commandRegistry() {
        var registry = new CommandRegistry();
        registry.registerCommand(Command.builder()
                .name("get")
                .options(
                        CommandOption.with().longName("url").type(String.class).build(),
                        CommandOption.with().longName("call").type(int.class).build())
                .execute(ctx -> ""));
        registry.registerCommand(Command.builder()
                .name("post")
                .options(
                        CommandOption.with().longName("url").type(String.class).build(),
                        CommandOption.with().longName("body").type(String.class).build())
                .execute(ctx -> ""));
        registry.registerCommand(Command.builder()
                .name("put")
                .options(
                        CommandOption.with().longName("url").type(String.class).build(),
                        CommandOption.with().longName("body").type(String.class).build())
                .execute(ctx -> ""));
        registry.registerCommand(Command.builder()
                .name("delete")
                .options(
                        CommandOption.with().longName("url").type(String.class).build(),
                        CommandOption.with().longName("call").type(int.class).build())
                .execute(ctx -> ""));
        return registry;
    }
}
