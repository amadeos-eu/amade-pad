package eu.amadeos.pad.repository;

import eu.amadeos.pad.domain.TextDocument;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * Reads and writes text documents in their detected format (see {@link TextCodec}).
 * Writes go to a temporary file in the target directory,
 * which is flushed to disk and then atomically renamed over the target, so a crash or a full
 * disk never leaves a truncated document behind.
 */
public final class FileSystemDocumentRepository implements DocumentRepository {
    private final TextCodec codec = new TextCodec();

    @Override
    public TextDocument read(Path path) throws IOException {
        return codec.decode(Files.readAllBytes(path));
    }

    @Override
    public void write(Path path, TextDocument document) throws IOException {
        // Encode first: an unencodable character must fail before any file is touched.
        byte[] bytes = codec.encode(document);
        // Write through symlinks to the real file instead of replacing the link itself.
        Path target = Files.exists(path) ? path.toRealPath() : path.toAbsolutePath();
        if (Files.isDirectory(target)) {
            throw new IOException("Cannot overwrite a directory: " + target);
        }
        Path directory = target.getParent();
        Path temp = Files.createTempFile(directory, "." + target.getFileName() + ".", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            copyPermissions(target, temp);
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /**
     * Keeps the permissions of an existing file. Temp files are created owner-only, so a new
     * document gets the conventional rw-r--r-- (umask 022) instead.
     */
    private static void copyPermissions(Path original, Path replacement) throws IOException {
        if (!Files.exists(original)) {
            if (Files.getFileAttributeView(replacement, PosixFileAttributeView.class) != null) {
                Files.setPosixFilePermissions(replacement, PosixFilePermissions.fromString("rw-r--r--"));
            }
            return;
        }
        PosixFileAttributeView view = Files.getFileAttributeView(original, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (view == null) {
            return;
        }
        PosixFileAttributes attributes = view.readAttributes();
        Files.setPosixFilePermissions(replacement, attributes.permissions());
        try {
            Files.getFileAttributeView(replacement, PosixFileAttributeView.class).setGroup(attributes.group());
        } catch (IOException | SecurityException ignored) {
            // Changing the group requires membership; keeping the default group is acceptable.
        }
    }
}
