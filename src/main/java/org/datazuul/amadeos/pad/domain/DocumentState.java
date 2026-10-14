package org.datazuul.amadeos.pad.domain;

import java.nio.file.Path;

/**
 * Document metadata confined to the UI thread; persistence receives immutable snapshots.
 */
public final class DocumentState {
    private Path path;
    private long revision;
    private long savedRevision;
    private TextFormat format = TextFormat.DEFAULT;

    public Path path() {
        return path;
    }

    public TextFormat format() {
        return format;
    }

    public long revision() {
        return revision;
    }

    public boolean isModified() {
        return revision != savedRevision;
    }

    public String title() {
        return title("Untitled");
    }

    /** File name, or {@code untitled} for a new document, with {@code *} when modified. */
    public String title(String untitled) {
        return (path == null ? untitled : path.getFileName().toString()) + (isModified() ? " *" : "");
    }

    public void changed() {
        revision++;
    }

    public void loaded(Path path, TextFormat format) {
        this.path = path;
        this.format = format;
        revision = 0;
        savedRevision = 0;
    }

    public void saved(Path path, long snapshotRevision, TextFormat format) {
        this.path = path;
        this.format = format;
        savedRevision = snapshotRevision;
    }
}
