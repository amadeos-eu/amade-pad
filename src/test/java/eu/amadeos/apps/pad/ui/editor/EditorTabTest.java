package eu.amadeos.apps.pad.ui.editor;

import java.nio.file.Path;
import javax.swing.SwingUtilities;
import eu.amadeos.apps.pad.domain.TextDocument;
import eu.amadeos.apps.pad.domain.TextFormat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EditorTabTest {
    @Test
    void newDocumentAndDirtyTitle() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EditorTab tab = new EditorTab();
            assertNull(tab.state().path());
            assertFalse(tab.state().isModified());
            assertEquals("Untitled", tab.state().title());
            tab.getTextArea().setText("Hello amadeOS!");
            assertTrue(tab.state().isModified());
            assertEquals("Untitled *", tab.state().title());
        });
    }

    @Test
    void loadAndSaveSnapshot() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EditorTab tab = new EditorTab();
            Path path = Path.of("sample.txt");
            tab.load(path, new TextDocument("Initial content", TextFormat.DEFAULT));
            assertEquals("sample.txt", tab.state().title());
            assertEquals("Initial content", tab.text());
            assertFalse(tab.state().isModified());
            assertFalse(tab.getUndoManager().canUndo());
            tab.getTextArea().setText("Updated content");
            long revision = tab.state().revision();
            tab.saved(path, revision, TextFormat.DEFAULT);
            assertFalse(tab.state().isModified());
            tab.getTextArea().append(" changed while saving");
            tab.saved(path, revision, TextFormat.DEFAULT);
            assertTrue(tab.state().isModified());
        });
    }

    @Test
    void undoRedo() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EditorTab tab = new EditorTab();
            tab.getTextArea().setText("Step 1");
            assertTrue(tab.getUndoManager().canUndo());
            tab.edit(EditorView.Edit.UNDO);
            assertEquals("", tab.text());
            assertTrue(tab.getUndoManager().canRedo());
            tab.edit(EditorView.Edit.REDO);
            assertEquals("Step 1", tab.text());
        });
    }

    @Test
    void rejectsOffEdtConstruction() {
        assertThrows(IllegalStateException.class, EditorTab::new);
    }
}
