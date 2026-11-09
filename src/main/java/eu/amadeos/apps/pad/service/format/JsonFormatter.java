package eu.amadeos.apps.pad.service.format;

/**
 * Re-indents JSON. It works on the tokens of the text, so strings and numbers keep their exact
 * spelling (no {@code 1.0} for {@code 1}, no re-escaped characters), and checks the grammar on the way.
 * Empty objects and arrays stay {@code {}} and {@code []}.
 */
final class JsonFormatter {
    private final String text;
    private final String unit;
    private final StringBuilder out = new StringBuilder();
    private int pos;

    private JsonFormatter(String text, String unit) {
        this.text = text;
        this.unit = unit;
    }

    static String format(String text, String unit) throws FormatException {
        JsonFormatter formatter = new JsonFormatter(text, unit);
        formatter.skipSpace();
        formatter.value(0);
        formatter.skipSpace();
        if (formatter.pos < text.length()) throw formatter.error("unexpected text after the end");
        return formatter.out.append('\n').toString();
    }

    private void value(int depth) throws FormatException {
        if (pos >= text.length()) throw error("value expected");
        char c = text.charAt(pos);
        switch (c) {
            case '{' -> container(depth, '}', true);
            case '[' -> container(depth, ']', false);
            case '"' -> out.append(string());
            default -> out.append(literal());
        }
    }

    private void container(int depth, char close, boolean object) throws FormatException {
        out.append(text.charAt(pos++));
        skipSpace();
        if (peek() == close) {
            out.append(close);
            pos++;
            return;
        }
        while (true) {
            out.append('\n').append(unit.repeat(depth + 1));
            if (object) {
                if (peek() != '"') throw error("name in quotes expected");
                out.append(string());
                skipSpace();
                if (peek() != ':') throw error("':' expected");
                pos++;
                out.append(": ");
                skipSpace();
            }
            value(depth + 1);
            skipSpace();
            char next = peek();
            if (next == ',') {
                pos++;
                out.append(',');
                skipSpace();
            } else if (next == close) {
                pos++;
                out.append('\n').append(unit.repeat(depth)).append(close);
                return;
            } else {
                throw error("',' or '" + close + "' expected");
            }
        }
    }

    private String string() throws FormatException {
        int start = pos++;
        while (pos < text.length()) {
            char c = text.charAt(pos++);
            if (c == '\\') pos++;
            else if (c == '"') return text.substring(start, pos);
            else if (c == '\n') break;
        }
        pos = start;
        throw error("unterminated string");
    }

    private String literal() throws FormatException {
        int start = pos;
        while (pos < text.length() && "{}[],:\" \t\r\n".indexOf(text.charAt(pos)) < 0) pos++;
        String word = text.substring(start, pos);
        if (word.equals("true") || word.equals("false") || word.equals("null")
                || word.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")) {
            return word;
        }
        pos = start;
        throw error(word.isEmpty() ? "value expected" : "not a JSON value: " + word);
    }

    private void skipSpace() {
        while (pos < text.length() && " \t\r\n".indexOf(text.charAt(pos)) >= 0) pos++;
    }

    private char peek() {
        return pos < text.length() ? text.charAt(pos) : '\0';
    }

    private FormatException error(String message) {
        int line = 1;
        for (int i = 0; i < Math.min(pos, text.length()); i++) {
            if (text.charAt(i) == '\n') line++;
        }
        return new FormatException(line, message);
    }
}
