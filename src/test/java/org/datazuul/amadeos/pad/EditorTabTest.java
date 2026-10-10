package org.datazuul.amadeos.pad;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class EditorTabTest {

    @Test
    void testNewUntitledDocument() {
        EditorTab tab = new EditorTab(null);
        assertNull(tab.getCurrentFile());
        assertFalse(tab.isModified());
        assertEquals("Untitled", tab.getTitle());

        tab.getTextArea().setText("Hello amadeOS!");
        assertTrue(tab.isModified());
        assertEquals("Untitled *", tab.getTitle());
    }

    @Test
    void testLoadAndSaveDocument(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("sample.txt");
        Files.writeString(testFile, "Initial content");

        EditorTab tab = new EditorTab(testFile.toFile());
        assertEquals("sample.txt", tab.getTitle());
        assertEquals("Initial content", tab.getTextArea().getText());
        assertFalse(tab.isModified());

        tab.getTextArea().setText("Updated content");
        assertTrue(tab.isModified());
        assertEquals("sample.txt *", tab.getTitle());

        tab.save();
        assertFalse(tab.isModified());
        assertEquals("sample.txt", tab.getTitle());
        assertEquals("Updated content", Files.readString(testFile));
    }

    @Test
    void testUndoRedo() {
        EditorTab tab = new EditorTab(null);
        tab.getTextArea().setText("Step 1");
        assertTrue(tab.getUndoManager().canUndo());

        tab.getUndoManager().undo();
        assertEquals("", tab.getTextArea().getText());

        assertTrue(tab.getUndoManager().canRedo());
        tab.getUndoManager().redo();
        assertEquals("Step 1", tab.getTextArea().getText());
    }
}
