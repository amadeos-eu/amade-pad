package org.datazuul.amadeos.pad.repository;

import org.datazuul.amadeos.pad.domain.TextDocument;
import org.datazuul.amadeos.pad.domain.TextFormat;
import org.datazuul.amadeos.pad.domain.TextFormat.LineEnding;
import org.datazuul.amadeos.pad.domain.UnencodableTextException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Converts between file bytes and {@link TextDocument}s.
 *
 * <p>Decoding detects a byte order mark (UTF-8, UTF-16BE/LE). Without one, the bytes are
 * decoded as UTF-8 if they are valid UTF-8, otherwise as Windows-1252, and as ISO-8859-1 if
 * they contain bytes Windows-1252 leaves undefined; ISO-8859-1 maps every byte, so decoding
 * never fails for legacy 8-bit files. Line separators are normalised to {@code \n} and the most
 * frequent one is remembered. Encoding reverses this and never replaces characters silently.
 */
public final class TextCodec {
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] UTF16BE_BOM = {(byte) 0xFE, (byte) 0xFF};
    private static final byte[] UTF16LE_BOM = {(byte) 0xFF, (byte) 0xFE};
    private static final int BINARY_PROBE_LENGTH = 8192;
    private static final List<Charset> LEGACY_FALLBACKS = List.of(
            Charset.forName("windows-1252"), StandardCharsets.ISO_8859_1);

    public TextDocument decode(byte[] bytes) throws IOException {
        if (startsWith(bytes, UTF8_BOM)) {
            return document(decodeStrict(StandardCharsets.UTF_8, bytes, UTF8_BOM.length), StandardCharsets.UTF_8, true);
        }
        if (startsWith(bytes, UTF16BE_BOM)) {
            return document(decodeStrict(StandardCharsets.UTF_16BE, bytes, 2), StandardCharsets.UTF_16BE, true);
        }
        if (startsWith(bytes, UTF16LE_BOM)) {
            return document(decodeStrict(StandardCharsets.UTF_16LE, bytes, 2), StandardCharsets.UTF_16LE, true);
        }
        if (looksBinary(bytes)) {
            throw new IOException("The file appears to be binary and cannot be edited as text");
        }
        try {
            return document(decodeStrict(StandardCharsets.UTF_8, bytes, 0), StandardCharsets.UTF_8, false);
        } catch (CharacterCodingException notUtf8) {
            for (Charset charset : LEGACY_FALLBACKS) {
                try {
                    return document(decodeStrict(charset, bytes, 0), charset, false);
                } catch (CharacterCodingException tryNext) {
                    // ISO-8859-1 maps every byte, so the loop always returns.
                }
            }
            throw notUtf8;
        }
    }

    public byte[] encode(TextDocument document) throws IOException {
        TextFormat format = document.format();
        String text = document.text().replace("\n", format.lineEnding().separator());
        ByteBuffer encoded;
        try {
            encoded = format.charset().newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(text));
        } catch (CharacterCodingException e) {
            throw new UnencodableTextException(format.charset(), e);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(encoded.remaining() + 3);
        if (format.bom()) out.writeBytes(bom(format.charset()));
        out.write(encoded.array(), encoded.arrayOffset() + encoded.position(), encoded.remaining());
        return out.toByteArray();
    }

    private static TextDocument document(String raw, Charset charset, boolean bom) {
        LineEnding lineEnding = dominantLineEnding(raw);
        String text = raw.replace("\r\n", "\n").replace('\r', '\n');
        return new TextDocument(text, new TextFormat(charset, bom, lineEnding));
    }

    /** Most frequent separator; LF for files without line breaks and for ties with LF. */
    static LineEnding dominantLineEnding(String text) {
        int lf = 0;
        int crlf = 0;
        int cr = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                lf++;
            } else if (c == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    crlf++;
                    i++;
                } else {
                    cr++;
                }
            }
        }
        if (crlf > lf && crlf >= cr) return LineEnding.CRLF;
        if (cr > lf && cr > crlf) return LineEnding.CR;
        return LineEnding.LF;
    }

    private static String decodeStrict(Charset charset, byte[] bytes, int offset) throws CharacterCodingException {
        return charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes, offset, bytes.length - offset))
                .toString();
    }

    /** NUL bytes do not occur in 8-bit or UTF-8 text; UTF-16 files are recognised by their BOM. */
    private static boolean looksBinary(byte[] bytes) {
        int limit = Math.min(bytes.length, BINARY_PROBE_LENGTH);
        for (int i = 0; i < limit; i++) {
            if (bytes[i] == 0) return true;
        }
        return false;
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        return bytes.length >= prefix.length && Arrays.equals(bytes, 0, prefix.length, prefix, 0, prefix.length);
    }

    private static byte[] bom(Charset charset) {
        if (charset.equals(StandardCharsets.UTF_8)) return UTF8_BOM;
        if (charset.equals(StandardCharsets.UTF_16BE)) return UTF16BE_BOM;
        if (charset.equals(StandardCharsets.UTF_16LE)) return UTF16LE_BOM;
        return new byte[0];
    }
}
