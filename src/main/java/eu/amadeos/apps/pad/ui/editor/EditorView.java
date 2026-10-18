package eu.amadeos.apps.pad.ui.editor;

import eu.amadeos.apps.pad.domain.DocumentState;
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

    enum Edit {UNDO, REDO, CUT, COPY, PASTE, SELECT_ALL}
}
