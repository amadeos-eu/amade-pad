package eu.amadeos.apps.pad.repository;

import java.io.IOException;

/** The file holds binary data, not text. */
public final class BinaryFileException extends IOException {
    public BinaryFileException() {
        super("The file appears to be binary and cannot be edited as text");
    }
}
