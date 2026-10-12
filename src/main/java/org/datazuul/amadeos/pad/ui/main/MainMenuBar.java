package org.datazuul.amadeos.pad.ui.main;

import org.datazuul.amadeos.pad.ui.common.EdtExecutor;
import org.datazuul.amadeos.pad.ui.editor.EditorView.Edit;
import org.datazuul.amadeos.pad.ui.swing.AmadeUI;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public final class MainMenuBar extends JMenuBar {
    private static final int CTRL = InputEvent.CTRL_DOWN_MASK;

    public MainMenuBar(MainPresenter presenter) {
        EdtExecutor.requireEdt();
        setBackground(AmadeUI.BASE);
        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        JMenu file = menu("File", KeyEvent.VK_F);
        item(file, "New", KeyEvent.VK_N, CTRL, presenter::newDocument);
        item(file, "Open...", KeyEvent.VK_O, CTRL, presenter::chooseOpen);
        item(file, "Save", KeyEvent.VK_S, CTRL, () -> presenter.saveCurrent(false));
        item(file, "Save As...", KeyEvent.VK_S, CTRL | InputEvent.SHIFT_DOWN_MASK, () -> presenter.saveCurrent(true));
        file.addSeparator();
        item(file, "Close Tab", KeyEvent.VK_W, CTRL, presenter::closeActiveTab);
        item(file, "Exit", KeyEvent.VK_Q, CTRL, presenter::exit);
        JMenu edit = menu("Edit", KeyEvent.VK_E);
        item(edit, "Undo", KeyEvent.VK_Z, CTRL, () -> presenter.edit(Edit.UNDO));
        item(edit, "Redo", KeyEvent.VK_Y, CTRL, () -> presenter.edit(Edit.REDO));
        edit.addSeparator();
        item(edit, "Cut", KeyEvent.VK_X, CTRL, () -> presenter.edit(Edit.CUT));
        item(edit, "Copy", KeyEvent.VK_C, CTRL, () -> presenter.edit(Edit.COPY));
        item(edit, "Paste", KeyEvent.VK_V, CTRL, () -> presenter.edit(Edit.PASTE));
        item(edit, "Select All", KeyEvent.VK_A, CTRL, () -> presenter.edit(Edit.SELECT_ALL));
        JMenu search = menu("Search", KeyEvent.VK_S);
        item(search, "Find...", KeyEvent.VK_F, CTRL, () -> presenter.search(false));
        item(search, "Find Next", KeyEvent.VK_F3, 0, presenter::findNext);
        item(search, "Replace...", KeyEvent.VK_H, CTRL, () -> presenter.search(true));
        JMenu view = menu("View", KeyEvent.VK_V);
        JCheckBoxMenuItem lines = new JCheckBoxMenuItem("Line Numbers", true);
        lines.addActionListener(e -> presenter.setLineNumbers(lines.isSelected()));
        view.add(lines);
        JCheckBoxMenuItem wrap = new JCheckBoxMenuItem("Word Wrap", false);
        wrap.addActionListener(e -> presenter.setWordWrap(wrap.isSelected()));
        view.add(wrap);
        item(menu("Help", KeyEvent.VK_H), "About amade-pad", 0, 0, presenter::about);
    }

    private JMenu menu(String title, int mnemonic) {
        JMenu menu = new JMenu(title);
        menu.setMnemonic(mnemonic);
        add(menu);
        return menu;
    }

    private void item(JMenu menu, String title, int key, int modifiers, Runnable action) {
        JMenuItem item = new JMenuItem(title);
        if (key != 0) item.setAccelerator(KeyStroke.getKeyStroke(key, modifiers));
        item.addActionListener(e -> action.run());
        menu.add(item);
    }
}
