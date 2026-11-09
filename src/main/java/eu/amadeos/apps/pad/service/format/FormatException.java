package eu.amadeos.apps.pad.service.format;

/** The document is not valid in its language, so it is left as it is; {@link #line()} counts from 1. */
public final class FormatException extends Exception {
    private final int line;

    public FormatException(int line, String message) {
        super(message);
        this.line = line;
    }

    public int line() {
        return line;
    }
}
