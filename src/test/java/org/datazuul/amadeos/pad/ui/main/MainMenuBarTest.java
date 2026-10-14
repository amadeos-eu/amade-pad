package org.datazuul.amadeos.pad.ui.main;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import org.datazuul.amadeos.pad.ui.editor.EditorView;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MainMenuBarTest {
    @Test
    void retainsMenusAcceleratorsAndPresenterWiring() throws Exception {
        MainPresenter presenter = mock(MainPresenter.class);
        SwingUtilities.invokeAndWait(() -> {
            MainMenuBar bar = new MainMenuBar(presenter, TestTexts.english(), TestTexts.locales(TestTexts.tempDir()));
            assertEquals(6, bar.getMenuCount());
            assertEquals("File", bar.getMenu(0).getText());
            assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
                    bar.getMenu(0).getItem(2).getAccelerator());
            assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                    bar.getMenu(0).getItem(3).getAccelerator());
            bar.getMenu(0).getItem(0).doClick();
            bar.getMenu(0).getItem(1).doClick();
            bar.getMenu(0).getItem(2).doClick();
            bar.getMenu(0).getItem(3).doClick();
            bar.getMenu(0).getItem(5).doClick();
            bar.getMenu(0).getItem(6).doClick();
            bar.getMenu(1).getItem(0).doClick();
            bar.getMenu(2).getItem(0).doClick();
            bar.getMenu(2).getItem(1).doClick();
            bar.getMenu(2).getItem(2).doClick();
            JCheckBoxMenuItem lines = (JCheckBoxMenuItem) bar.getMenu(3).getItem(0);
            assertTrue(lines.isSelected());
            lines.doClick();
            bar.getMenu(3).getItem(1).doClick();
            JMenu language = (JMenu) bar.getMenu(4).getItem(0);
            assertEquals("Language", language.getText());
            assertEquals(7, language.getItemCount(), "desktop setting, separator, five languages");
            language.getItem(3).doClick(); // Deutsch
            bar.getMenu(5).getItem(0).doClick();
        });
        verify(presenter).newDocument();
        verify(presenter).chooseOpen();
        verify(presenter).saveCurrent(false);
        verify(presenter).saveCurrent(true);
        verify(presenter).closeActiveTab();
        verify(presenter).exit();
        verify(presenter).edit(EditorView.Edit.UNDO);
        verify(presenter).search(false);
        verify(presenter).findNext();
        verify(presenter).search(true);
        verify(presenter).setLineNumbers(false);
        verify(presenter).setWordWrap(true);
        verify(presenter).setLanguage(java.util.Optional.of(org.datazuul.amadeos.commons.i18n.Language.GERMAN));
        verify(presenter).about();
    }
}
