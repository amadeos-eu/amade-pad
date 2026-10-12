package org.datazuul.amadeos.pad.ui.main;

import org.datazuul.amadeos.pad.ui.editor.EditorView;

import java.nio.file.Path;
import java.util.List;

public interface MainView {
    EditorView activeEditor();

    List<EditorView> editors();

    EditorView addEditor();

    void selectEditor(EditorView editor);

    void removeEditor(EditorView editor);

    Path chooseOpenPath();

    Path chooseSavePath(Path currentPath);

    SaveChoice confirmClose(EditorView editor);

    void showError(String message);

    void showSearch(boolean replace);

    void findNext();

    void showAbout();

    void dispose();

    enum SaveChoice {SAVE, DISCARD, CANCEL}
}
