package eu.amadeos.apps.pad.domain;

import java.util.Optional;

/**
 * How a document is indented: one level is {@code width} spaces, or a tab shown {@code width}
 * columns wide.
 */
public record Indentation(int width, boolean tabs) {
    /** Pad's default: two spaces per level. */
    public static final Indentation DEFAULT = new Indentation(2, false);
    /** The widths the Options menu offers. */
    public static final int[] WIDTHS = {2, 4, 8};

    public Indentation {
        if (width < 1 || width > 16) throw new IllegalArgumentException("Indentation width " + width);
    }

    public Indentation withWidth(int newWidth) {
        return new Indentation(newWidth, tabs);
    }

    public Indentation withTabs(boolean useTabs) {
        return new Indentation(width, useTabs);
    }

    /** The text of one level: a tab or {@code width} spaces. */
    public String unit() {
        return tabs ? "\t" : " ".repeat(width);
    }

    // --8<-- [start:detect]
    /**
     * Guesses the indentation of a text from its first lines: tabs if more lines start with a tab than
     * with spaces, else the step by which the indentation most often grows (2, 3, 4 or 8). Empty if the
     * text has no indented lines, so the language's or the user's default applies.
     */
    public static Optional<Indentation> detect(String text, int tabWidth) {
        int tabLines = 0;
        int spaceLines = 0;
        int[] steps = new int[9];
        int previous = 0;
        int lines = 0;
        for (String line : text.split("\n", 2000)) {
            if (++lines > 1000) break;
            if (line.isBlank()) continue;
            if (line.charAt(0) == '\t') {
                tabLines++;
                continue;
            }
            int spaces = 0;
            while (spaces < line.length() && line.charAt(spaces) == ' ') spaces++;
            if (spaces > 0) spaceLines++;
            // Comment continuation lines (" * ") are one column off; they are not a new level.
            boolean continuation = line.startsWith("*", spaces);
            int step = spaces - previous;
            if (!continuation && (step == 2 || step == 3 || step == 4 || step == 8)) steps[step]++;
            if (!continuation) previous = spaces;
        }
        if (tabLines == 0 && spaceLines == 0) return Optional.empty();
        if (tabLines > spaceLines) return Optional.of(new Indentation(tabWidth, true));
        int best = 0;
        for (int step : new int[]{2, 4, 3, 8}) {
            if (steps[step] > steps[best]) best = step;
        }
        return best == 0 ? Optional.empty() : Optional.of(new Indentation(best, false));
    }
    // --8<-- [end:detect]

    /**
     * The indentation a document opens with: what the text itself uses, else the language's own rule
     * (Makefiles need tabs, YAML forbids them), else the user's default.
     */
    public static Indentation forDocument(String text, SyntaxLanguage language, Indentation defaults) {
        return detect(text, defaults.width()).orElseGet(() -> language.indentation(defaults));
    }
}
