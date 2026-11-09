package eu.amadeos.apps.pad.ui.main;

import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.LocaleSettings;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.apps.pad.domain.Indentation;
import eu.amadeos.apps.pad.domain.SyntaxLanguage;
import eu.amadeos.apps.pad.ui.editor.EditorView.Edit;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MainMenuBar extends JMenuBar {
    private static final int CTRL = InputEvent.CTRL_DOWN_MASK;
    private final Messages msg;
    private final JMenu edit;

    public MainMenuBar(MainPresenter presenter, Messages msg, LocaleSettings locales) {
        EdtExecutor.requireEdt();
        this.msg = msg;
        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        // --8<-- [start:menus]
        JMenu file = menu("menu.file");
        item(file, "menu.file.new", KeyEvent.VK_N, CTRL, presenter::newDocument);
        item(file, "menu.file.open", KeyEvent.VK_O, CTRL, presenter::chooseOpen);
        item(file, "menu.file.save", KeyEvent.VK_S, CTRL, () -> presenter.saveCurrent(false));
        item(file, "menu.file.save-as", KeyEvent.VK_S, CTRL | InputEvent.SHIFT_DOWN_MASK, () -> presenter.saveCurrent(true));
        file.addSeparator();
        item(file, "menu.file.close-tab", KeyEvent.VK_W, CTRL, presenter::closeActiveTab);
        item(file, "menu.file.exit", KeyEvent.VK_Q, CTRL, presenter::exit);
        // --8<-- [end:menus]
        edit = menu("menu.edit");
        item(edit, "menu.edit.undo", KeyEvent.VK_Z, CTRL, () -> presenter.edit(Edit.UNDO));
        item(edit, "menu.edit.redo", KeyEvent.VK_Y, CTRL, () -> presenter.edit(Edit.REDO));
        edit.addSeparator();
        item(edit, "menu.edit.cut", KeyEvent.VK_X, CTRL, () -> presenter.edit(Edit.CUT));
        item(edit, "menu.edit.copy", KeyEvent.VK_C, CTRL, () -> presenter.edit(Edit.COPY));
        item(edit, "menu.edit.paste", KeyEvent.VK_V, CTRL, () -> presenter.edit(Edit.PASTE));
        item(edit, "menu.edit.select-all", KeyEvent.VK_A, CTRL, () -> presenter.edit(Edit.SELECT_ALL));
        edit.addSeparator();
        item(edit, "menu.edit.format", KeyEvent.VK_F, CTRL | InputEvent.SHIFT_DOWN_MASK, presenter::formatDocument);
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
        view.addSeparator();
        JCheckBoxMenuItem folding = new JCheckBoxMenuItem(msg.get("menu.view.code-folding"), true);
        folding.addActionListener(e -> presenter.setCodeFolding(folding.isSelected()));
        view.add(folding);
        JCheckBoxMenuItem brackets = new JCheckBoxMenuItem(msg.get("menu.view.bracket-matching"), true);
        brackets.addActionListener(e -> presenter.setBracketMatching(brackets.isSelected()));
        view.add(brackets);
        view.addSeparator();
        view.add(syntaxMenu(presenter));
        JMenu options = menu("menu.options");
        options.add(languageMenu(presenter, locales));
        options.add(indentationMenu(presenter));
        item(menu("menu.help"), "menu.help.about", 0, 0, presenter::about);
    }

    // --8<-- [start:syntax-menu]
    /** View → Syntax: every language Pad colours; the check mark follows the current document. */
    private JMenu syntaxMenu(MainPresenter presenter) {
        JMenu menu = new JMenu(msg.get("menu.view.syntax"));
        ButtonGroup group = new ButtonGroup();
        for (SyntaxLanguage language : SyntaxLanguage.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(presenter.languageName(language));
            item.setName(language.name());
            item.addActionListener(e -> presenter.setSyntax(language));
            group.add(item);
            menu.add(item);
            if (language == SyntaxLanguage.PLAIN) menu.addSeparator();
        }
        onOpen(menu, () -> presenter.activeLanguage().ifPresentOrElse(
                current -> select(menu, current.name()), group::clearSelection));
        return menu;
    }
    // --8<-- [end:syntax-menu]

    // --8<-- [start:indentation-menu]
    /** Options → Indentation: 2, 4 or 8 columns, spaces or tabs; shows the current document's. */
    private JMenu indentationMenu(MainPresenter presenter) {
        JMenu menu = new JMenu(msg.get("menu.options.indentation"));
        ButtonGroup group = new ButtonGroup();
        for (int width : Indentation.WIDTHS) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(msg.format("menu.options.indentation.width", width));
            item.setName("width-" + width);
            item.addActionListener(e -> presenter.setIndentation(presenter.activeIndentation().withWidth(width)));
            group.add(item);
            menu.add(item);
        }
        menu.addSeparator();
        JCheckBoxMenuItem tabs = new JCheckBoxMenuItem(msg.get("menu.options.indentation.tabs"));
        tabs.setName("tabs");
        tabs.addActionListener(e -> presenter.setIndentation(presenter.activeIndentation().withTabs(tabs.isSelected())));
        menu.add(tabs);
        onOpen(menu, () -> {
            Indentation current = presenter.activeIndentation();
            group.clearSelection();
            select(menu, "width-" + current.width());
            tabs.setSelected(current.tabs());
        });
        return menu;
    }
    // --8<-- [end:indentation-menu]

    private static void select(JMenu menu, String name) {
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item != null && name.equals(item.getName())) item.setSelected(true);
        }
    }

    private static void onOpen(JMenu menu, Runnable refresh) {
        menu.addMenuListener(new MenuListener() {
            @Override
            public void menuSelected(MenuEvent e) {
                refresh.run();
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuCanceled(MenuEvent e) {
            }
        });
    }

    // --8<-- [start:language-menu]
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
    // --8<-- [end:language-menu]

    // --8<-- [start:helpers]
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
    // --8<-- [end:helpers]

    /** The Edit menu; the window adds Special Characters to it (amade-commons). */
    JMenu editMenu() {
        return edit;
    }
}
