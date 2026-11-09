package eu.amadeos.apps.pad.service.format;

import eu.amadeos.apps.pad.domain.Indentation;
import eu.amadeos.apps.pad.domain.SyntaxLanguage;

/**
 * "Edit → Format Document": indents a whole document anew. Only for languages whose structure Pad can
 * read safely without other programs; everything else is left to the user's own indentation.
 */
public final class DocumentFormatter {
    // --8<-- [start:format]
    public boolean supports(SyntaxLanguage language) {
        return language == SyntaxLanguage.JSON || language == SyntaxLanguage.XML;
    }

    /** The formatted text; throws if the text is not valid, so a broken file is never changed. */
    public String format(SyntaxLanguage language, String text, Indentation indentation) throws FormatException {
        return switch (language) {
            case JSON -> JsonFormatter.format(text, indentation.unit());
            case XML -> XmlFormatter.format(text, indentation.unit());
            default -> throw new IllegalArgumentException("Cannot format " + language);
        };
    }
    // --8<-- [end:format]
}
