package eu.amadeos.pad.domain;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * The document contains characters that the target charset cannot store.
 */
public final class UnencodableTextException extends IOException {
    private final transient Charset charset;

    public UnencodableTextException(Charset charset, Throwable cause) {
        super("The document contains characters that cannot be stored as " + charset.name(), cause);
        this.charset = charset;
    }

    public Charset charset() {
        return charset;
    }
}
