package org.datazuul.amadeos.pad;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Objects;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;

/**
 * Represents a document tab in amade-pad.
 */
public final class EditorTab extends JPanel {

    private final JTextArea textArea;
    private final JScrollPane scrollPane;
    private final LineNumberMargin lineNumbers;
    private final UndoManager undoManager;
    private File currentFile;
    private boolean modified;
    private Runnable onStateChanged;

    public EditorTab(File file) {
        super(new BorderLayout());

        this.currentFile = file;
        this.undoManager = new UndoManager();

        textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        textArea.setTabSize(4);
        textArea.getDocument().addUndoableEditListener(undoManager);

        lineNumbers = new LineNumberMargin(textArea);
        scrollPane = new JScrollPane(textArea);
        scrollPane.setRowHeaderView(lineNumbers);

        add(scrollPane, BorderLayout.CENTER);

        if (file != null && file.exists()) {
            try {
                String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                textArea.setText(content);
                textArea.setCaretPosition(0);
                undoManager.discardAllEdits();
            } catch (IOException e) {
                textArea.setText("Error reading file: " + e.getMessage());
            }
        }

        this.modified = false;

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setModified(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                setModified(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                setModified(true);
            }
        });
    }

    public JTextArea getTextArea() {
        return textArea;
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    public File getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(File file) {
        this.currentFile = file;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        if (this.modified != modified) {
            this.modified = modified;
            if (onStateChanged != null) {
                onStateChanged.run();
            }
        }
    }

    public void setOnStateChanged(Runnable listener) {
        this.onStateChanged = listener;
    }

    public String getTitle() {
        String base = (currentFile != null) ? currentFile.getName() : "Untitled";
        return modified ? base + " *" : base;
    }

    public void save() throws IOException {
        if (currentFile == null) {
            throw new IllegalStateException("No file associated with document");
        }
        Files.writeString(currentFile.toPath(), textArea.getText(), StandardCharsets.UTF_8);
        setModified(false);
    }

    public void saveAs(File file) throws IOException {
        this.currentFile = Objects.requireNonNull(file);
        save();
    }

    public void applyColors(Color bg, Color fg, Color caretColor) {
        textArea.setBackground(bg);
        textArea.setForeground(fg);
        textArea.setCaretColor(caretColor);
        lineNumbers.setBackground(bg);
        lineNumbers.setForeground(fg);
        scrollPane.getViewport().setBackground(bg);
    }

    public void setLineNumbersVisible(boolean visible) {
        scrollPane.setRowHeaderView(visible ? lineNumbers : null);
    }
}
