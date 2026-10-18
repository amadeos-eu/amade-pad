package eu.amadeos.pad.ui.editor;

import java.awt.Font;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LineNumberMarginTest {
    private JTextArea textArea;
    private LineNumberMargin margin;
    private JScrollPane scrollPane;

    @Test
    void followsTextAreaHeightWhenWordWrapChangesIt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            textArea = new JTextArea();
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            margin = new LineNumberMargin(textArea);
            scrollPane = new JScrollPane(textArea);
            scrollPane.setRowHeaderView(margin);
            scrollPane.setSize(600, 400);
            textArea.setText("x ".repeat(4000));
        });
        layout();
        SwingUtilities.invokeAndWait(() -> textArea.setLineWrap(true));
        layout();
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(textArea.getHeight() > scrollPane.getViewport().getHeight(), "wrapped text must scroll");
            assertEquals(textArea.getHeight(), margin.getHeight());
        });
    }

    @Test
    void widensForMoreDigits() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            textArea = new JTextArea();
            margin = new LineNumberMargin(textArea);
            int threeDigits = margin.getPreferredSize().width;
            textArea.setText("\n".repeat(1000));
            assertTrue(margin.getPreferredSize().width > threeDigits);
        });
    }

    /** Lays out the scroll pane; resize events are delivered between the passes. */
    private void layout() throws Exception {
        for (int i = 0; i < 3; i++) {
            SwingUtilities.invokeAndWait(() -> {
                scrollPane.doLayout();
                scrollPane.getViewport().doLayout();
                scrollPane.getRowHeader().doLayout();
            });
        }
    }
}
