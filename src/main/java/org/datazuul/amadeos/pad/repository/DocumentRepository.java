package org.datazuul.amadeos.pad.repository;

import org.datazuul.amadeos.pad.domain.TextDocument;

import java.io.IOException;
import java.nio.file.Path;

public interface DocumentRepository {
    TextDocument read(Path path) throws IOException;

    void write(Path path, TextDocument document) throws IOException;
}
