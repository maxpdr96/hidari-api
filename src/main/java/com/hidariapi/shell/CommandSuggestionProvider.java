package com.hidariapi.shell;

import com.hidariapi.service.LanguageService;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class CommandSuggestionProvider extends LocalizedSupport {

    public CommandSuggestionProvider(LanguageService lang) {
        super(lang);
    }

    public String suggest(String typed, List<String> commands) {
        String unknown = firstToken(typed == null ? "" : typed.trim());
        String base = t("Comando nao encontrado: ", "Command not found: ") + typed;
        if (unknown == null || unknown.isBlank() || commands == null || commands.isEmpty()) return base;

        String suggestion = closestCommand(unknown, commands);
        if (suggestion == null) return base;
        return base + "\n" + t("Voce quis dizer: ", "Did you mean: ") + suggestion;
    }

    private String firstToken(String text) {
        int idx = text.indexOf(' ');
        return idx < 0 ? text : text.substring(0, idx);
    }

    private String closestCommand(String unknown, List<String> commands) {
        return commands.stream()
                .distinct()
                .sorted(Comparator.comparingInt(c -> score(unknown, c)))
                .filter(c -> score(unknown, c) <= 3 || c.startsWith(unknown) || unknown.startsWith(c))
                .findFirst()
                .orElse(null);
    }

    private int score(String a, String b) {
        return levenshtein(a.toLowerCase(), b.toLowerCase());
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[a.length()][b.length()];
    }
}
