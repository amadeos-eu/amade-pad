package org.datazuul.amadeos.pad.infrastructure.file;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.datazuul.amadeos.pad.repository.FileSystemDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FileSystemDocumentRepositoryTest {
    @TempDir Path directory;
    private final FileSystemDocumentRepository repository = new FileSystemDocumentRepository();

    @Test
    void roundTripsUtf8AndOverwritesExistingFile() throws IOException {
        Path path = directory.resolve("sample.txt");
        String content = "Grüße ♫\nsecond line";
        repository.write(path, content);
        assertEquals(content, repository.read(path));
        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(path));
        repository.write(path, "short");
        assertEquals("short", repository.read(path));
    }

    @Test
    void replacesFileWithoutLeavingTempFiles() throws IOException {
        Path path = directory.resolve("sample.txt");
        repository.write(path, "first");
        repository.write(path, "second");
        assertEquals("second", repository.read(path));
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(List.of(path), files.toList());
        }
    }

    @Test
    void keepsPermissionsOfExistingFile() throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        Path path = directory.resolve("script.sh");
        Files.writeString(path, "#!/bin/sh\n");
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxr-x---");
        Files.setPosixFilePermissions(path, permissions);
        repository.write(path, "#!/bin/sh\necho hi\n");
        assertEquals(permissions, Files.getPosixFilePermissions(path));
    }

    @Test
    void newFilesAreNotOwnerOnly() throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        Path path = directory.resolve("new.txt");
        repository.write(path, "content");
        assertEquals(PosixFilePermissions.fromString("rw-r--r--"), Files.getPosixFilePermissions(path));
    }

    @Test
    void writesThroughSymlinkInsteadOfReplacingIt() throws IOException {
        Path real = directory.resolve("real.txt");
        Path link = directory.resolve("link.txt");
        Files.writeString(real, "old");
        try {
            Files.createSymbolicLink(link, real);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "symlinks not supported");
        }
        repository.write(link, "new");
        assertTrue(Files.isSymbolicLink(link));
        assertEquals("new", Files.readString(real));
    }

    @Test
    void propagatesReadAndWriteFailures() {
        assertThrows(IOException.class, () -> repository.read(directory.resolve("missing.txt")));
        assertThrows(IOException.class, () -> repository.write(directory, "cannot overwrite a directory"));
    }
}
