package eu.amadeos.apps.pad.service.format;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Re-indents XML. The text is first checked with the JDK's parser (no external files or entities are
 * loaded), then indented piece by piece, so tags, attributes, comments and entities keep their
 * spelling. An element that holds text next to other elements, such as {@code <p>Hello <b>you</b></p>},
 * or that asks for {@code xml:space="preserve"}, stays exactly as it is, because there every space counts.
 * One empty line between elements is kept.
 */
final class XmlFormatter {
    private enum Kind {TEXT, CDATA, COMMENT, INSTRUCTION, DOCTYPE, START, EMPTY, END}

    private record Token(Kind kind, int start, int end) {
    }

    private static final class Node {
        final Token token;
        final List<Node> children = new ArrayList<>();
        Token close;

        Node(Token token) {
            this.token = token;
        }
    }

    private final String text;
    private final String unit;
    private final StringBuilder out = new StringBuilder();

    private XmlFormatter(String text, String unit) {
        this.text = text;
        this.unit = unit;
    }

    static String format(String text, String unit) throws FormatException {
        check(text);
        XmlFormatter formatter = new XmlFormatter(text, unit);
        formatter.children(formatter.tree(), 0);
        return formatter.out.toString();
    }

    private static void check(String text) throws FormatException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.newSAXParser().parse(new InputSource(new StringReader(text)), new DefaultHandler());
        } catch (SAXParseException e) {
            throw new FormatException(Math.max(1, e.getLineNumber()), e.getMessage());
        } catch (SAXException | ParserConfigurationException | IOException e) {
            throw new FormatException(1, e.getMessage());
        }
    }

    private List<Node> tree() {
        Node root = new Node(null);
        List<Node> open = new ArrayList<>(List.of(root));
        int pos = 0;
        while (pos < text.length()) {
            Token token = token(pos);
            pos = token.end;
            Node parent = open.getLast();
            switch (token.kind) {
                case END -> {
                    parent.close = token;
                    open.removeLast();
                }
                case START -> {
                    Node node = new Node(token);
                    parent.children.add(node);
                    open.add(node);
                }
                default -> parent.children.add(new Node(token));
            }
        }
        return root.children;
    }

    private Token token(int pos) {
        if (text.charAt(pos) != '<') {
            int end = text.indexOf('<', pos);
            return new Token(Kind.TEXT, pos, end < 0 ? text.length() : end);
        }
        if (text.startsWith("<!--", pos)) return until(Kind.COMMENT, pos, "-->");
        if (text.startsWith("<![CDATA[", pos)) return until(Kind.CDATA, pos, "]]>");
        if (text.startsWith("<?", pos)) return until(Kind.INSTRUCTION, pos, "?>");
        if (text.startsWith("<!", pos)) return new Token(Kind.DOCTYPE, pos, tagEnd(pos, true));
        if (text.startsWith("</", pos)) return new Token(Kind.END, pos, tagEnd(pos, false));
        int end = tagEnd(pos, false);
        return new Token(text.charAt(end - 2) == '/' ? Kind.EMPTY : Kind.START, pos, end);
    }

    private Token until(Kind kind, int pos, String terminator) {
        int end = text.indexOf(terminator, pos);
        return new Token(kind, pos, end < 0 ? text.length() : end + terminator.length());
    }

    /** The end of a tag; quoted attribute values and a DOCTYPE's [internal subset] may hold a '>'. */
    private int tagEnd(int pos, boolean brackets) {
        char quote = 0;
        int depth = 0;
        for (int i = pos + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (brackets && c == '[') {
                depth++;
            } else if (brackets && c == ']') {
                depth--;
            } else if (c == '>' && depth <= 0) {
                return i + 1;
            }
        }
        return text.length();
    }

    private void children(List<Node> nodes, int depth) {
        boolean wrote = false;
        for (Node node : nodes) {
            Token token = node.token;
            if (token.kind == Kind.TEXT) {
                // Whitespace between elements; an empty line in it is kept as one empty line.
                if (wrote && raw(token).chars().filter(c -> c == '\n').count() > 1) out.append('\n');
                continue;
            }
            wrote = true;
            if (token.kind == Kind.START) element(node, depth);
            else line(depth, raw(token));
        }
    }

    private void element(Node node, int depth) {
        boolean onlyWhitespace = node.children.stream()
                .allMatch(child -> child.token.kind == Kind.TEXT && raw(child.token).isBlank());
        if (onlyWhitespace) {
            line(depth, raw(node.token) + raw(node.close));
        } else if (keepsSpace(node)) {
            line(depth, text.substring(node.token.start, node.close.end));
        } else {
            line(depth, raw(node.token));
            children(node.children, depth + 1);
            line(depth, raw(node.close));
        }
    }

    /** Text next to elements, CDATA or xml:space="preserve": every character inside counts. */
    private boolean keepsSpace(Node node) {
        if (raw(node.token).matches("(?s).*\\sxml:space\\s*=\\s*[\"']preserve[\"'].*")) return true;
        for (Node child : node.children) {
            Kind kind = child.token.kind;
            if (kind == Kind.CDATA || (kind == Kind.TEXT && !raw(child.token).isBlank())) return true;
        }
        return false;
    }

    private String raw(Token token) {
        return text.substring(token.start, token.end);
    }

    private void line(int depth, String content) {
        out.append(unit.repeat(depth)).append(content).append('\n');
    }
}
