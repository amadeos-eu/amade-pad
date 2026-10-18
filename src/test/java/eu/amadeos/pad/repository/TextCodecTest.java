package eu.amadeos.pad.repository;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import eu.amadeos.pad.domain.TextDocument;
import eu.amadeos.pad.domain.TextFormat;
import eu.amadeos.pad.domain.TextFormat.LineEnding;
import eu.amadeos.pad.domain.UnencodableTextException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextCodecTest {
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");
    private final TextCodec codec = new TextCodec();

    @Test
    void plainAsciiAndEmptyFilesAreUtf8WithLf() throws IOException {
        assertEquals(new TextDocument("a\nb\n", TextFormat.DEFAULT), codec.decode("a\nb\n".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(new TextDocument("", TextFormat.DEFAULT), codec.decode(new byte[0]));
    }

    @Test
    void validUtf8IsPreferredOverLegacyCharsets() throws IOException {
        TextDocument document = codec.decode("Grüße ♫".getBytes(StandardCharsets.UTF_8));
        assertEquals("Grüße ♫", document.text());
        assertEquals(StandardCharsets.UTF_8, document.format().charset());
    }

    @Test
    void invalidUtf8FallsBackToWindows1252() throws IOException {
        TextDocument document = codec.decode("Grüße €".getBytes(WINDOWS_1252));
        assertEquals("Grüße €", document.text());
        assertEquals(WINDOWS_1252, document.format().charset());
    }

    @Test
    void bytesUndefinedInWindows1252FallBackToIso88591() throws IOException {
        TextDocument document = codec.decode(new byte[]{'a', (byte) 0x81, (byte) 0xE4});
        assertEquals("a\u0081ä", document.text());
        assertEquals(StandardCharsets.ISO_8859_1, document.format().charset());
    }

    @Test
    void byteOrderMarksAreDetectedAndNotPartOfTheText() throws IOException {
        TextDocument utf8 = codec.decode(bytes(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, "hi".getBytes(StandardCharsets.UTF_8)));
        assertEquals(new TextDocument("hi", new TextFormat(StandardCharsets.UTF_8, true, LineEnding.LF)), utf8);
        TextDocument utf16le = codec.decode(bytes(new byte[]{(byte) 0xFF, (byte) 0xFE}, "x\r\ny".getBytes(StandardCharsets.UTF_16LE)));
        assertEquals(new TextDocument("x\ny", new TextFormat(StandardCharsets.UTF_16LE, true, LineEnding.CRLF)), utf16le);
        TextDocument utf16be = codec.decode(bytes(new byte[]{(byte) 0xFE, (byte) 0xFF}, "z".getBytes(StandardCharsets.UTF_16BE)));
        assertEquals(StandardCharsets.UTF_16BE, utf16be.format().charset());
        assertEquals("z", utf16be.text());
    }

    @Test
    void lineEndingsAreNormalisedAndTheDominantOneIsRemembered() throws IOException {
        assertEquals(new TextDocument("a\nb\n", new TextFormat(StandardCharsets.UTF_8, false, LineEnding.CRLF)),
                codec.decode("a\r\nb\r\n".getBytes(StandardCharsets.UTF_8)));
        assertEquals(LineEnding.CR, codec.decode("a\rb\rc".getBytes(StandardCharsets.UTF_8)).format().lineEnding());
        TextDocument mixed = codec.decode("a\r\nb\r\nc\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("a\nb\nc\n", mixed.text());
        assertEquals(LineEnding.CRLF, mixed.format().lineEnding());
        assertEquals(LineEnding.LF, TextCodec.dominantLineEnding("a\r\nb\n"));
    }

    @Test
    void roundTripsByteForByte() throws IOException {
        byte[][] files = {
                "a\nb\n".getBytes(StandardCharsets.UTF_8),
                "Grüße\r\nx\r\n".getBytes(StandardCharsets.UTF_8),
                bytes(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, "bom\n".getBytes(StandardCharsets.UTF_8)),
                "äöü €\r\n".getBytes(WINDOWS_1252),
                bytes(new byte[]{(byte) 0xFF, (byte) 0xFE}, "x\r\ny".getBytes(StandardCharsets.UTF_16LE)),
                "mac\rclassic\r".getBytes(StandardCharsets.UTF_8),
        };
        for (byte[] file : files) {
            assertArrayEquals(file, codec.encode(codec.decode(file)));
        }
    }

    @Test
    void binaryFilesAreRejected() {
        IOException error = assertThrows(IOException.class, () -> codec.decode(new byte[]{'P', 'K', 3, 4, 0, 0}));
        assertTrue(error.getMessage().contains("binary"));
    }

    @Test
    void unencodableCharactersFailInsteadOfBeingReplaced() {
        TextDocument document = new TextDocument("snowman ☃", new TextFormat(WINDOWS_1252, false, LineEnding.LF));
        UnencodableTextException error = assertThrows(UnencodableTextException.class, () -> codec.encode(document));
        assertEquals(WINDOWS_1252, error.charset());
    }

    private static byte[] bytes(byte[] prefix, byte[] rest) {
        byte[] result = new byte[prefix.length + rest.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(rest, 0, result, prefix.length, rest.length);
        return result;
    }
}
