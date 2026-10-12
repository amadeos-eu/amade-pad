package org.datazuul.amadeos.pad.repository;

import java.io.IOException;
import java.nio.file.Path;

public interface DocumentRepository {
    String read(Path path) throws IOException;

    void write(Path path, String content) throws IOException;
}
