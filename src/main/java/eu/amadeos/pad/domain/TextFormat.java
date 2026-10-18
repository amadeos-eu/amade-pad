package eu.amadeos.pad.domain;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * How a document is stored on disk. The editor always works with {@code \n}; the format
 * remembers the charset, byte order mark and line separator so saving restores them.
 */
public record TextFormat(Charset charset, boolean bom, LineEnding lineEnding) {
    public static final TextFormat DEFAULT = new TextFormat(StandardCharsets.UTF_8, false, LineEnding.LF);

    public TextFormat {
        Objects.requireNonNull(charset);
        Objects.requireNonNull(lineEnding);
    }

    /** Same line ending, stored as UTF-8 without a byte order mark. */
    public TextFormat asUtf8() {
        return new TextFormat(StandardCharsets.UTF_8, false, lineEnding);
    }

    /** True for charsets that can store every character the editor can hold. */
    public boolean isUnicode() {
        return charset.equals(StandardCharsets.UTF_8)
                || charset.equals(StandardCharsets.UTF_16BE)
                || charset.equals(StandardCharsets.UTF_16LE);
    }

    /** Short label for the status bar, e.g. "UTF-8 BOM | CRLF". */
    public String label() {
        return charset.name() + (bom ? " BOM" : "") + " | " + lineEnding.name();
    }

    public enum LineEnding {
        LF("\n"), CRLF("\r\n"), CR("\r");

        private final String separator;

        LineEnding(String separator) {
            this.separator = separator;
        }

        public String separator() {
            return separator;
        }
    }
}
