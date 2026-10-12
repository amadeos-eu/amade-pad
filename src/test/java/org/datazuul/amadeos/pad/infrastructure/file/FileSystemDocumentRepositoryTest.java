package org.datazuul.amadeos.pad.infrastructure.file;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.datazuul.amadeos.pad.domain.TextDocument;
import org.datazuul.amadeos.pad.domain.TextFormat;
import org.datazuul.amadeos.pad.domain.UnencodableTextException;
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
        write(path, content);
        assertEquals(content, read(path));
        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(path));
        write(path, "short");
        assertEquals("short", read(path));
    }

    @Test
    void replacesFileWithoutLeavingTempFiles() throws IOException {
        Path path = directory.resolve("sample.txt");
        write(path, "first");
        write(path, "second");
        assertEquals("second", read(path));
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
        write(path, "#!/bin/sh\necho hi\n");
        assertEquals(permissions, Files.getPosixFilePermissions(path));
    }

    @Test
    void newFilesAreNotOwnerOnly() throws IOException {
        assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        Path path = directory.resolve("new.txt");
        write(path, "content");
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
        write(link, "new");
        assertTrue(Files.isSymbolicLink(link));
        assertEquals("new", Files.readString(real));
    }

    @Test
    void propagatesReadAndWriteFailures() {
        assertThrows(IOException.class, () -> read(directory.resolve("missing.txt")));
        assertThrows(IOException.class, () -> write(directory, "cannot overwrite a directory"));
    }

    @Test
    void preservesCharsetBomAndLineEndingsOfExistingFile() throws IOException {
        Path path = directory.resolve("legacy.txt");
        byte[] original = "Gr\u00fc\u00dfe\r\nzweite Zeile\r\n".getBytes(Charset.forName("windows-1252"));
        Files.write(path, original);
        TextDocument document = repository.read(path);
        assertEquals("Gr\u00fc\u00dfe\nzweite Zeile\n", document.text());
        assertEquals(new TextFormat(Charset.forName("windows-1252"), false, TextFormat.LineEnding.CRLF), document.format());
        repository.write(path, document);
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    @Test
    void unencodableTextLeavesExistingFileUntouched() throws IOException {
        Path path = directory.resolve("legacy.txt");
        // The umlaut is invalid UTF-8 here, so the file is detected as Windows-1252.
        byte[] original = "alt \u00e4\n".getBytes(Charset.forName("windows-1252"));
        Files.write(path, original);
        TextFormat format = repository.read(path).format();
        assertEquals(Charset.forName("windows-1252"), format.charset());
        TextDocument edited = new TextDocument("neu \u2603", format);
        assertThrows(UnencodableTextException.class, () -> repository.write(path, edited));
        assertArrayEquals(original, Files.readAllBytes(path));
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(List.of(path), files.toList());
        }
    }

    private void write(Path path, String text) throws IOException {
        repository.write(path, new TextDocument(text, TextFormat.DEFAULT));
    }

    private String read(Path path) throws IOException {
        return repository.read(path).text();
    }
}
