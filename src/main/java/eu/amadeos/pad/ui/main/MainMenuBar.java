package eu.amadeos.pad.ui.main;

import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.LocaleSettings;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.pad.ui.editor.EditorView.Edit;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MainMenuBar extends JMenuBar {
    private static final int CTRL = InputEvent.CTRL_DOWN_MASK;
    private final Messages msg;

    public MainMenuBar(MainPresenter presenter, Messages msg, LocaleSettings locales) {
        EdtExecutor.requireEdt();
        this.msg = msg;
        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        JMenu file = menu("menu.file");
        item(file, "menu.file.new", KeyEvent.VK_N, CTRL, presenter::newDocument);
        item(file, "menu.file.open", KeyEvent.VK_O, CTRL, presenter::chooseOpen);
        item(file, "menu.file.save", KeyEvent.VK_S, CTRL, () -> presenter.saveCurrent(false));
        item(file, "menu.file.save-as", KeyEvent.VK_S, CTRL | InputEvent.SHIFT_DOWN_MASK, () -> presenter.saveCurrent(true));
        file.addSeparator();
        item(file, "menu.file.close-tab", KeyEvent.VK_W, CTRL, presenter::closeActiveTab);
        item(file, "menu.file.exit", KeyEvent.VK_Q, CTRL, presenter::exit);
        JMenu edit = menu("menu.edit");
        item(edit, "menu.edit.undo", KeyEvent.VK_Z, CTRL, () -> presenter.edit(Edit.UNDO));
        item(edit, "menu.edit.redo", KeyEvent.VK_Y, CTRL, () -> presenter.edit(Edit.REDO));
        edit.addSeparator();
        item(edit, "menu.edit.cut", KeyEvent.VK_X, CTRL, () -> presenter.edit(Edit.CUT));
        item(edit, "menu.edit.copy", KeyEvent.VK_C, CTRL, () -> presenter.edit(Edit.COPY));
        item(edit, "menu.edit.paste", KeyEvent.VK_V, CTRL, () -> presenter.edit(Edit.PASTE));
        item(edit, "menu.edit.select-all", KeyEvent.VK_A, CTRL, () -> presenter.edit(Edit.SELECT_ALL));
        JMenu search = menu("menu.search");
        item(search, "menu.search.find", KeyEvent.VK_F, CTRL, () -> presenter.search(false));
        item(search, "menu.search.find-next", KeyEvent.VK_F3, 0, presenter::findNext);
        item(search, "menu.search.replace", KeyEvent.VK_H, CTRL, () -> presenter.search(true));
        JMenu view = menu("menu.view");
        JCheckBoxMenuItem lines = new JCheckBoxMenuItem(msg.get("menu.view.line-numbers"), true);
        lines.addActionListener(e -> presenter.setLineNumbers(lines.isSelected()));
        view.add(lines);
        JCheckBoxMenuItem wrap = new JCheckBoxMenuItem(msg.get("menu.view.word-wrap"), false);
        wrap.addActionListener(e -> presenter.setWordWrap(wrap.isSelected()));
        view.add(wrap);
        JMenu options = menu("menu.options");
        options.add(languageMenu(presenter, locales));
        item(menu("menu.help"), "menu.help.about", 0, 0, presenter::about);
    }

    /** "Desktop setting (Deutsch)" and the five languages; the choice applies at the next start. */
    private JMenu languageMenu(MainPresenter presenter, LocaleSettings locales) {
        JMenu menu = new JMenu(msg.get("menu.options.language"));
        Messages commons = Messages.commons(msg.locale());
        List<Optional<Language>> choices = new ArrayList<>();
        choices.add(Optional.empty());
        for (Language language : Language.values()) choices.add(Optional.of(language));
        ButtonGroup group = new ButtonGroup();
        Optional<Language> current = locales.applicationLanguage();
        for (Optional<Language> choice : choices) {
            String label = choice.map(Language::nativeName).orElseGet(() -> commons.format("language.follow-desktop",
                    locales.desktopResolution().language().nativeName()));
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, choice.equals(current));
            item.setName(choice.map(Language::code).orElse(LocaleSettings.FOLLOW_DESKTOP));
            item.addActionListener(e -> presenter.setLanguage(choice));
            group.add(item);
            menu.add(item);
            if (choice.isEmpty()) menu.addSeparator();
        }
        return menu;
    }

    private JMenu menu(String key) {
        JMenu menu = Mnemonics.apply(new JMenu(), msg.get(key));
        add(menu);
        return menu;
    }

    private void item(JMenu menu, String key, int keyCode, int modifiers, Runnable action) {
        JMenuItem item = new JMenuItem(msg.get(key));
        if (keyCode != 0) item.setAccelerator(KeyStroke.getKeyStroke(keyCode, modifiers));
        item.addActionListener(e -> action.run());
        menu.add(item);
    }
}
