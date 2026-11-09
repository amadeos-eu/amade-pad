package eu.amadeos.apps.pad.ui.editor;

import java.nio.file.Path;
import javax.swing.SwingUtilities;
import eu.amadeos.apps.pad.domain.Indentation;
import eu.amadeos.apps.pad.domain.SyntaxLanguage;
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
            assertFalse(tab.getTextArea().canUndo());
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
            assertTrue(tab.getTextArea().canUndo());
            tab.edit(EditorView.Edit.UNDO);
            assertEquals("", tab.text());
            assertTrue(tab.getTextArea().canRedo());
            tab.edit(EditorView.Edit.REDO);
            assertEquals("Step 1", tab.text());
        });
    }

    @Test
    void backspaceInLeadingSpacesRemovesAWholeLevel() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EditorTab tab = new EditorTab();
            tab.setIndentation(new Indentation(4, false));
            tab.getTextArea().setText("        x");
            tab.getTextArea().setCaretPosition(8);
            assertTrue(tab.removeIndentLevel());
            assertEquals("    x", tab.text());
            tab.getTextArea().setText("      x"); // six spaces: back to the level at four
            tab.getTextArea().setCaretPosition(6);
            assertTrue(tab.removeIndentLevel());
            assertEquals("    x", tab.text());
            tab.getTextArea().setCaretPosition(5);
            assertFalse(tab.removeIndentLevel(), "after text Backspace removes one character");
            tab.setIndentation(new Indentation(4, true));
            tab.getTextArea().setCaretPosition(4);
            assertFalse(tab.removeIndentLevel(), "with tabs Backspace stays as it is");
        });
    }

    @Test
    void indentationAndLanguageReachTheTextArea() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EditorTab tab = new EditorTab();
            assertEquals(Indentation.DEFAULT, tab.indentation());
            assertTrue(tab.getTextArea().getTabsEmulated());
            assertEquals(2, tab.getTextArea().getTabSize());
            tab.setIndentation(new Indentation(8, true));
            assertFalse(tab.getTextArea().getTabsEmulated());
            assertEquals(8, tab.getTextArea().getTabSize());
            tab.setLanguage(SyntaxLanguage.PYTHON, true);
            assertEquals("text/python", tab.getTextArea().getSyntaxEditingStyle());
            assertTrue(tab.languageChosen());
            assertFalse(tab.state().isModified(), "choosing a language is no change of the text");
        });
    }

    @Test
    void replacedTextIsOneUndoStep() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EditorTab tab = new EditorTab();
            tab.load(Path.of("a.json"), new TextDocument("{\"a\":1}", TextFormat.DEFAULT));
            tab.replaceText("{\n  \"a\": 1\n}\n");
            assertEquals("{\n  \"a\": 1\n}\n", tab.text());
            assertTrue(tab.state().isModified());
            tab.edit(EditorView.Edit.UNDO);
            assertEquals("{\"a\":1}", tab.text());
        });
    }

    @Test
    void rejectsOffEdtConstruction() {
        assertThrows(IllegalStateException.class, EditorTab::new);
    }
}
