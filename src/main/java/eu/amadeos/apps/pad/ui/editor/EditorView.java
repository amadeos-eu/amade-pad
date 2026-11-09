package eu.amadeos.apps.pad.ui.editor;

import eu.amadeos.apps.pad.domain.DocumentState;
import eu.amadeos.apps.pad.domain.Indentation;
import eu.amadeos.apps.pad.domain.SyntaxLanguage;
import eu.amadeos.apps.pad.domain.TextDocument;
import eu.amadeos.apps.pad.domain.TextFormat;

import java.nio.file.Path;

public interface EditorView {
    DocumentState state();

    String text();

    void load(Path path, TextDocument document);

    void saved(Path path, long revision, TextFormat format);

    void edit(Edit action);

    void setWordWrap(boolean enabled);

    void setLineNumbersVisible(boolean visible);

    // --8<-- [start:syntax]
    SyntaxLanguage language();

    /** Colours the text as the language; {@code chosen} if the user picked it in View → Syntax. */
    void setLanguage(SyntaxLanguage language, boolean chosen);

    /** True once the user picked the language; it is then no longer guessed from the file name. */
    boolean languageChosen();

    Indentation indentation();

    /** What Tab, Enter and Backspace insert and remove at the start of a line. */
    void setIndentation(Indentation indentation);

    /** Replaces the whole text as one step that Undo takes back, keeping the cursor on its line. */
    void replaceText(String text);

    void setCodeFolding(boolean enabled);

    void setBracketMatching(boolean enabled);
    // --8<-- [end:syntax]

    enum Edit {UNDO, REDO, CUT, COPY, PASTE, SELECT_ALL}
}
