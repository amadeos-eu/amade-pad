package org.datazuul.amadeos.pad.infrastructure.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.datazuul.amadeos.pad.repository.FileSystemDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class FileSystemDocumentRepositoryTest {
    @TempDir Path directory;
    private final FileSystemDocumentRepository repository = new FileSystemDocumentRepository();

    @Test
    void roundTripsUtf8AndOverwritesExistingFile() throws IOException {
        Path path = directory.resolve("sample.txt");
        String content = "Gr\u00fc\u00dfe \u266b\nsecond line";
        repository.write(path, content);
        assertEquals(content, repository.read(path));
        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(path));
        repository.write(path, "short");
        assertEquals("short", repository.read(path));
    }

    @Test
    void propagatesReadAndWriteFailures() {
        assertThrows(IOException.class, () -> repository.read(directory.resolve("missing.txt")));
        assertThrows(IOException.class, () -> repository.write(directory, "cannot overwrite a directory"));
    }
}
