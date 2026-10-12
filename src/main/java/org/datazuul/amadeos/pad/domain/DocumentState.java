package org.datazuul.amadeos.pad.domain;

import java.nio.file.Path;

/**
 * Document metadata confined to the UI thread; persistence receives immutable snapshots.
 */
public final class DocumentState {
    private Path path;
    private long revision;
    private long savedRevision;

    public Path path() {
        return path;
    }

    public long revision() {
        return revision;
    }

    public boolean isModified() {
        return revision != savedRevision;
    }

    public String title() {
        return (path == null ? "Untitled" : path.getFileName().toString()) + (isModified() ? " *" : "");
    }

    public void changed() {
        revision++;
    }

    public void loaded(Path path) {
        this.path = path;
        revision = 0;
        savedRevision = 0;
    }

    public void saved(Path path, long snapshotRevision) {
        this.path = path;
        savedRevision = snapshotRevision;
    }
}
