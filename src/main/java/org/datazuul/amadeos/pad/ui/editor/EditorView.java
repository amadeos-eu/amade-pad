package org.datazuul.amadeos.pad.ui.editor;

import org.datazuul.amadeos.pad.domain.DocumentState;
import org.datazuul.amadeos.pad.domain.TextDocument;
import org.datazuul.amadeos.pad.domain.TextFormat;

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
