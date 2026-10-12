package org.datazuul.amadeos.pad.ui.editor;

import org.datazuul.amadeos.pad.domain.DocumentState;

import java.nio.file.Path;

public interface EditorView {
    DocumentState state();

    String text();

    void load(Path path, String content);

    void saved(Path path, long revision);

    void edit(Edit action);

    void setWordWrap(boolean enabled);

    void setLineNumbersVisible(boolean visible);

    enum Edit {UNDO, REDO, CUT, COPY, PASTE, SELECT_ALL}
}
