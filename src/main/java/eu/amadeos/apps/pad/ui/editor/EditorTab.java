package eu.amadeos.apps.pad.ui.editor;

import eu.amadeos.apps.pad.domain.DocumentState;
import eu.amadeos.apps.pad.domain.TextDocument;
import eu.amadeos.apps.pad.domain.TextFormat;
import eu.amadeos.commons.swing.EdtExecutor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.nio.file.Path;

public final class EditorTab extends JPanel implements EditorView {
    private final JTextArea textArea = new JTextArea();
    private final UndoManager undoManager = new UndoManager();
    private final DocumentState state = new DocumentState();
    private final LineNumberMargin lineNumbers;
    private final JScrollPane scrollPane;
    private Runnable onStateChanged = () -> {
    };

    public EditorTab() {
        super(new BorderLayout());
        EdtExecutor.requireEdt();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        textArea.setTabSize(4);
        // Background, text and caret colours come from the desktop theme (TextArea.* in AmadeUI).
        textArea.getDocument().addUndoableEditListener(undoManager);
        lineNumbers = new LineNumberMargin(textArea);
        scrollPane = new JScrollPane(textArea);
        scrollPane.setRowHeaderView(lineNumbers);
        add(scrollPane, BorderLayout.CENTER);
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            public void changedUpdate(DocumentEvent e) {
                changed();
            }

            private void changed() {
                state.changed();
                onStateChanged.run();
            }
        });
    }

    public JTextArea getTextArea() {
        EdtExecutor.requireEdt();
        return textArea;
    }

    public UndoManager getUndoManager() {
        EdtExecutor.requireEdt();
        return undoManager;
    }

    public void setOnStateChanged(Runnable listener) {
        EdtExecutor.requireEdt();
        onStateChanged = listener;
    }

    @Override
    public DocumentState state() {
        EdtExecutor.requireEdt();
        return state;
    }

    @Override
    public String text() {
        EdtExecutor.requireEdt();
        return textArea.getText();
    }

    @Override
    public void load(Path path, TextDocument document) {
        EdtExecutor.requireEdt();
        textArea.setText(document.text());
        textArea.setCaretPosition(0);
        undoManager.discardAllEdits();
        state.loaded(path, document.format());
        onStateChanged.run();
    }

    @Override
    public void saved(Path path, long revision, TextFormat format) {
        EdtExecutor.requireEdt();
        state.saved(path, revision, format);
        onStateChanged.run();
    }

    @Override
    public void edit(Edit action) {
        EdtExecutor.requireEdt();
        switch (action) {
            case UNDO -> {
                if (undoManager.canUndo()) undoManager.undo();
            }
            case REDO -> {
                if (undoManager.canRedo()) undoManager.redo();
            }
            case CUT -> textArea.cut();
            case COPY -> textArea.copy();
            case PASTE -> textArea.paste();
            case SELECT_ALL -> textArea.selectAll();
        }
    }

    @Override
    public void setWordWrap(boolean enabled) {
        EdtExecutor.requireEdt();
        textArea.setLineWrap(enabled);
        textArea.setWrapStyleWord(enabled);
    }

    @Override
    public void setLineNumbersVisible(boolean visible) {
        EdtExecutor.requireEdt();
        scrollPane.setRowHeaderView(visible ? lineNumbers : null);
    }
}
