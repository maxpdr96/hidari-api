package com.hidariapi.shell;

import com.hidariapi.service.LanguageService;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Shared shell helpers for translated text and ANSI styling.
 */
public abstract class LocalizedSupport {

    protected static final AttributedStyle BOLD = AttributedStyle.DEFAULT.bold();
    protected static final AttributedStyle DIM = AttributedStyle.DEFAULT.faint();
    protected static final AttributedStyle CYAN = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
    protected static final AttributedStyle GREEN = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
    protected static final AttributedStyle YELLOW = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
    protected static final AttributedStyle RED = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
    protected static final AttributedStyle MAGENTA = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
    protected static final AttributedStyle WHITE = AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE);
    protected static final AttributedStyle BOLD_CYAN = BOLD.foreground(AttributedStyle.CYAN);
    protected static final AttributedStyle BOLD_GREEN = BOLD.foreground(AttributedStyle.GREEN);
    protected static final AttributedStyle BOLD_RED = BOLD.foreground(AttributedStyle.RED);
    protected static final AttributedStyle BOLD_YELLOW = BOLD.foreground(AttributedStyle.YELLOW);

    protected final LanguageService lang;

    protected LocalizedSupport(LanguageService lang) {
        this.lang = lang;
    }

    protected String t(String pt, String en) {
        return lang.t(pt, en);
    }

    protected String styled(AttributedStyle style, String text) {
        return new AttributedStringBuilder()
                .style(style)
                .append(text)
                .toAttributedString()
                .toAnsi();
    }

    protected String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max - 1) + "~" : text;
    }
}
