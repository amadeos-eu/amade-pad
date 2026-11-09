package eu.amadeos.apps.pad.ui.editor;

import eu.amadeos.commons.theme.DesktopTheme;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.Gutter;

import java.awt.*;

/**
 * The colours of the editor, taken from the desktop theme: text, background and selection as in every
 * other text field, a quiet gutter, and one syntax palette for light and one for dark themes.
 */
final class SyntaxColors {
    // --8<-- [start:palette]
    /** Keyword, type, string, number, comment, annotation, function, tag, attribute, error. */
    private static final Color[] LIGHT = colors(0x0033B3, 0x00797A, 0x067D17, 0x1750EB, 0x7A7A7A, 0x9E7A00,
            0x00627A, 0x8A2BB9, 0xA0522D, 0xC62828);
    private static final Color[] DARK = colors(0xC792EA, 0x4EC9B0, 0x9CCC65, 0x79B8FF, 0x8B949E, 0xE5C07B,
            0x61AFEF, 0xF07178, 0xFFB86C, 0xFF6B6B);
    // --8<-- [end:palette]

    private SyntaxColors() {
    }

    static void apply(DesktopTheme theme, RSyntaxTextArea area, Gutter gutter) {
        Color background = theme.fieldBackground();
        Color text = theme.fieldText();
        area.setBackground(background);
        area.setForeground(text);
        area.setCaretColor(text);
        area.setSelectionColor(theme.selectionBackground());
        area.setSelectedTextColor(theme.selectionText());
        area.setUseSelectedTextColor(true);
        area.setCurrentLineHighlightColor(mix(background, text, 0.07));
        area.setMatchedBracketBGColor(mix(background, text, 0.15));
        area.setMatchedBracketBorderColor(mix(background, text, 0.45));
        area.setSyntaxScheme(scheme(area.getFont(), text, theme.isDark() ? DARK : LIGHT));
        if (gutter != null) {
            gutter.setBackground(theme.softHighlight());
            gutter.setBorderColor(theme.shadow());
            gutter.setLineNumberColor(theme.textInactive());
            gutter.setCurrentLineNumberColor(theme.textActive());
            gutter.setFoldIndicatorForeground(theme.textInactive());
            gutter.setFoldIndicatorArmedForeground(theme.textActive());
            gutter.setFoldBackground(theme.softHighlight());
            gutter.setArmedFoldBackground(theme.softHighlight());
            gutter.setLineNumberFont(area.getFont().deriveFont(12f));
        }
        area.repaint();
    }

    // --8<-- [start:scheme]
    private static SyntaxScheme scheme(Font font, Color text, Color[] p) {
        SyntaxScheme scheme = new SyntaxScheme(font);
        Font bold = font.deriveFont(Font.BOLD);
        Font italic = font.deriveFont(Font.ITALIC);
        for (int type = 0; type < scheme.getStyleCount(); type++) {
            scheme.setStyle(type, new Style(text));
        }
        set(scheme, p[0], bold, TokenTypes.RESERVED_WORD, TokenTypes.RESERVED_WORD_2, TokenTypes.COMMENT_KEYWORD);
        set(scheme, p[1], null, TokenTypes.DATA_TYPE, TokenTypes.VARIABLE);
        set(scheme, p[2], null, TokenTypes.LITERAL_STRING_DOUBLE_QUOTE, TokenTypes.LITERAL_CHAR,
                TokenTypes.LITERAL_BACKQUOTE, TokenTypes.REGEX, TokenTypes.MARKUP_TAG_ATTRIBUTE_VALUE,
                TokenTypes.MARKUP_CDATA);
        set(scheme, p[3], null, TokenTypes.LITERAL_NUMBER_DECIMAL_INT, TokenTypes.LITERAL_NUMBER_FLOAT,
                TokenTypes.LITERAL_NUMBER_HEXADECIMAL, TokenTypes.LITERAL_BOOLEAN, TokenTypes.MARKUP_ENTITY_REFERENCE);
        set(scheme, p[4], italic, TokenTypes.COMMENT_EOL, TokenTypes.COMMENT_MULTILINE,
                TokenTypes.COMMENT_DOCUMENTATION, TokenTypes.COMMENT_MARKUP, TokenTypes.MARKUP_COMMENT);
        set(scheme, p[5], null, TokenTypes.ANNOTATION, TokenTypes.PREPROCESSOR,
                TokenTypes.MARKUP_PROCESSING_INSTRUCTION, TokenTypes.MARKUP_DTD, TokenTypes.MARKUP_CDATA_DELIMITER);
        set(scheme, p[6], null, TokenTypes.FUNCTION);
        set(scheme, p[7], null, TokenTypes.MARKUP_TAG_NAME, TokenTypes.MARKUP_TAG_DELIMITER);
        set(scheme, p[8], null, TokenTypes.MARKUP_TAG_ATTRIBUTE);
        set(scheme, p[9], null, TokenTypes.ERROR_IDENTIFIER, TokenTypes.ERROR_NUMBER_FORMAT,
                TokenTypes.ERROR_STRING_DOUBLE, TokenTypes.ERROR_CHAR);
        return scheme;
    }
    // --8<-- [end:scheme]

    private static void set(SyntaxScheme scheme, Color color, Font font, int... types) {
        for (int type : types) {
            Style style = new Style(color);
            style.font = font;
            scheme.setStyle(type, style);
        }
    }

    private static Color mix(Color a, Color b, double share) {
        return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * share),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * share),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * share));
    }

    private static Color[] colors(int... rgb) {
        Color[] result = new Color[rgb.length];
        for (int i = 0; i < rgb.length; i++) result[i] = new Color(rgb[i]);
        return result;
    }
}
