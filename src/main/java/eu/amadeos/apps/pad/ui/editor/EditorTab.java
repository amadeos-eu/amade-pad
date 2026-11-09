package eu.amadeos.apps.pad.ui.editor;

import eu.amadeos.apps.pad.domain.DocumentState;
import eu.amadeos.apps.pad.domain.Indentation;
import eu.amadeos.apps.pad.domain.SyntaxLanguage;
import eu.amadeos.apps.pad.domain.TextDocument;
import eu.amadeos.apps.pad.domain.TextFormat;
import eu.amadeos.commons.swing.AmadeUI;
import eu.amadeos.commons.swing.EdtExecutor;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.ExpandedFoldRenderStrategy;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;

/**
 * One document: a syntax-colouring text area (RSyntaxTextArea) in a scroll pane whose gutter shows the
 * line numbers and the fold markers.
 */
public final class EditorTab extends JPanel implements EditorView {
    private final RSyntaxTextArea textArea = new RSyntaxTextArea();
    private final DocumentState state = new DocumentState();
    private final RTextScrollPane scrollPane;
    private SyntaxLanguage language = SyntaxLanguage.PLAIN;
    private boolean languageChosen;
    private Indentation indentation = Indentation.DEFAULT;
    private Runnable onStateChanged = () -> {
    };

    public EditorTab() {
        super(new BorderLayout());
        EdtExecutor.requireEdt();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        textArea.setAntiAliasingEnabled(true);
        textArea.setCodeFoldingEnabled(true);
        textArea.setAutoIndentEnabled(true);
        textArea.setPaintMatchedBracketPair(true);
        textArea.setAnimateBracketMatching(false);
        textArea.setFadeCurrentLineHighlight(false);
        scrollPane = new RTextScrollPane(textArea, true);
        scrollPane.setFoldIndicatorEnabled(true);
        // Show the fold markers all the time, not only under the mouse, so blocks can be found.
        scrollPane.getGutter().setExpandedFoldRenderStrategy(ExpandedFoldRenderStrategy.ALWAYS);
        // Colours of the text, the syntax and the gutter come from the desktop theme, also after a change.
        AmadeUI.style(this, (tab, theme) -> SyntaxColors.apply(theme, textArea, scrollPane.getGutter()));
        installSmartBackspace();
        setIndentation(indentation);
        add(scrollPane, BorderLayout.CENTER);
        // --8<-- [start:changes]
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                changed();
            }

            public void removeUpdate(DocumentEvent e) {
                changed();
            }

            public void changedUpdate(DocumentEvent e) {
                // Attribute changes (the colouring) do not change the text.
            }

            private void changed() {
                state.changed();
                onStateChanged.run();
            }
        });
        // --8<-- [end:changes]
    }

    public RSyntaxTextArea getTextArea() {
        EdtExecutor.requireEdt();
        return textArea;
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
        textArea.discardAllEdits();
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
                if (textArea.canUndo()) textArea.undoLastAction();
            }
            case REDO -> {
                if (textArea.canRedo()) textArea.redoLastAction();
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
        scrollPane.setLineNumbersEnabled(visible);
    }

    @Override
    public SyntaxLanguage language() {
        EdtExecutor.requireEdt();
        return language;
    }

    // --8<-- [start:language]
    @Override
    public void setLanguage(SyntaxLanguage newLanguage, boolean chosen) {
        EdtExecutor.requireEdt();
        language = newLanguage;
        languageChosen = chosen;
        textArea.setSyntaxEditingStyle(newLanguage.mediaType());
        onStateChanged.run();
    }
    // --8<-- [end:language]

    @Override
    public boolean languageChosen() {
        EdtExecutor.requireEdt();
        return languageChosen;
    }

    @Override
    public Indentation indentation() {
        EdtExecutor.requireEdt();
        return indentation;
    }

    // --8<-- [start:indentation]
    @Override
    public void setIndentation(Indentation newIndentation) {
        EdtExecutor.requireEdt();
        indentation = newIndentation;
        textArea.setTabSize(newIndentation.width());
        // With spaces, the Tab key and the automatic indentation after Enter insert spaces.
        textArea.setTabsEmulated(!newIndentation.tabs());
        onStateChanged.run();
    }
    // --8<-- [end:indentation]

    @Override
    public void replaceText(String text) {
        EdtExecutor.requireEdt();
        if (text.equals(textArea.getText())) return;
        int line = textArea.getCaretLineNumber();
        textArea.beginAtomicEdit();
        try {
            textArea.replaceRange(text, 0, textArea.getDocument().getLength());
        } finally {
            textArea.endAtomicEdit();
        }
        try {
            textArea.setCaretPosition(textArea.getLineStartOffset(Math.min(line, textArea.getLineCount() - 1)));
        } catch (BadLocationException e) {
            textArea.setCaretPosition(0);
        }
    }

    @Override
    public void setCodeFolding(boolean enabled) {
        EdtExecutor.requireEdt();
        textArea.setCodeFoldingEnabled(enabled);
    }

    @Override
    public void setBracketMatching(boolean enabled) {
        EdtExecutor.requireEdt();
        textArea.setBracketMatchingEnabled(enabled);
    }

    // --8<-- [start:backspace]
    /**
     * With spaces for indentation, Backspace in the leading spaces of a line removes a whole level, as a
     * tab would be removed, instead of a single space.
     */
    private void installSmartBackspace() {
        Object key = textArea.getInputMap().get(KeyStroke.getKeyStroke("BACK_SPACE"));
        Action plain = key == null ? null : textArea.getActionMap().get(key);
        if (plain == null) return;
        textArea.getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!removeIndentLevel()) plain.actionPerformed(e);
            }
        });
    }

    boolean removeIndentLevel() {
        if (indentation.tabs() || !textArea.isEditable() || textArea.getSelectionStart() != textArea.getSelectionEnd()) {
            return false;
        }
        try {
            int caret = textArea.getCaretPosition();
            int lineStart = textArea.getLineStartOffset(textArea.getCaretLineNumber());
            int column = caret - lineStart;
            if (column == 0) return false;
            String before = textArea.getText(lineStart, column);
            if (!before.isBlank() || before.indexOf('\t') >= 0) return false;
            int remove = column % indentation.width() == 0 ? indentation.width() : column % indentation.width();
            textArea.replaceRange("", caret - remove, caret);
            return true;
        } catch (BadLocationException e) {
            return false;
        }
    }
    // --8<-- [end:backspace]
}
