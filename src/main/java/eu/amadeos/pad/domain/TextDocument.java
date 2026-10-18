package eu.amadeos.pad.domain;

import java.util.Objects;

/**
 * Immutable document contents with {@code \n} line separators, plus the on-disk format.
 */
public record TextDocument(String text, TextFormat format) {
    public TextDocument {
        Objects.requireNonNull(text);
        Objects.requireNonNull(format);
    }

    public TextDocument withFormat(TextFormat newFormat) {
        return new TextDocument(text, newFormat);
    }
}
