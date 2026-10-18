package eu.amadeos.pad.ui.search;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import eu.amadeos.pad.service.SearchService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class FindReplaceDialogTest {
    @Test
    void replaceActionsAreInactiveInFindMode() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            JFrame owner = new JFrame();
            JTextArea area = new JTextArea("one one");
            FindReplaceDialog dialog = new FindReplaceDialog(owner, new SearchService(), eu.amadeos.pad.ui.main.TestTexts.english(),
                    () -> area);
            try {
                field(dialog, "findField").setText("one");
                field(dialog, "replaceField").setText("two");

                dialog.showFor(false);
                dialog.replaceAll();
                dialog.replaceCurrent();
                assertEquals("one one", area.getText());

                dialog.showFor(true);
                area.setCaretPosition(0);
                dialog.replaceCurrent(); // selects the first match
                dialog.replaceCurrent(); // replaces it and selects the next one
                assertEquals("two one", area.getText());
            } finally {
                dialog.dispose();
                owner.dispose();
            }
        });
    }

    private static JTextField field(FindReplaceDialog dialog, String name) {
        try {
            Field field = FindReplaceDialog.class.getDeclaredField(name);
            field.setAccessible(true);
            return (JTextField) field.get(dialog);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
