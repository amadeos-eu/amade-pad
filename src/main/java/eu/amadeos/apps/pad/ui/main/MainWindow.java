package eu.amadeos.apps.pad.ui.main;

import com.google.inject.Inject;
import eu.amadeos.commons.i18n.LocaleSettings;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.apps.pad.service.SearchService;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.apps.pad.ui.editor.EditorTab;
import eu.amadeos.apps.pad.ui.editor.EditorView;
import eu.amadeos.apps.pad.ui.search.FindReplaceDialog;
import eu.amadeos.commons.swing.AmadeUI;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MainWindow extends JFrame implements MainView {
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel status = new JLabel();
    private final FindReplaceDialog search;
    private final Messages msg;
    private final LocaleSettings locales;

    @Inject
    public MainWindow(SearchService searchService, Messages msg, LocaleSettings locales) {
        super(msg.get("window.title"));
        EdtExecutor.requireEdt();
        this.msg = msg;
        this.locales = locales;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setPreferredSize(new Dimension(800, 600));
        setLayout(new BorderLayout());
        search = new FindReplaceDialog(this, searchService, msg, () -> {
            EditorTab tab = (EditorTab) activeEditor();
            return tab == null ? null : tab.getTextArea();
        });
        tabs.addChangeListener(e -> updateStatus());
        add(tabs, BorderLayout.CENTER);
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 3));
        statusBar.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        statusBar.add(status);
        add(statusBar, BorderLayout.SOUTH);
        updateStatus();
        pack();
        setLocationRelativeTo(null);
    }

    public void bind(MainPresenter presenter) {
        EdtExecutor.requireEdt();
        setJMenuBar(new MainMenuBar(presenter, msg, locales));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                presenter.exit();
            }
        });
    }

    @Override
    public EditorView activeEditor() {
        EdtExecutor.requireEdt();
        return (EditorTab) tabs.getSelectedComponent();
    }

    @Override
    public List<EditorView> editors() {
        EdtExecutor.requireEdt();
        List<EditorView> result = new ArrayList<>();
        for (int i = 0; i < tabs.getTabCount(); i++) result.add((EditorTab) tabs.getComponentAt(i));
        return result;
    }

    @Override
    public EditorView addEditor() {
        EdtExecutor.requireEdt();
        EditorTab editor = new EditorTab();
        editor.setOnStateChanged(() -> {
            int index = tabs.indexOfComponent(editor);
            if (index >= 0) tabs.setTitleAt(index, title(editor));
            updateStatus();
        });
        editor.getTextArea().addCaretListener(e -> updateStatus());
        tabs.addTab(title(editor), editor);
        tabs.setSelectedComponent(editor);
        editor.getTextArea().requestFocusInWindow();
        updateStatus();
        return editor;
    }

    @Override
    public void selectEditor(EditorView editor) {
        EdtExecutor.requireEdt();
        tabs.setSelectedComponent((EditorTab) editor);
    }

    @Override
    public void removeEditor(EditorView editor) {
        EdtExecutor.requireEdt();
        tabs.remove((EditorTab) editor);
    }

    @Override
    public Path chooseOpenPath() {
        EdtExecutor.requireEdt();
        JFileChooser chooser = new JFileChooser();
        return chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().toPath() : null;
    }

    @Override
    public Path chooseSavePath(Path currentPath) {
        EdtExecutor.requireEdt();
        JFileChooser chooser = new JFileChooser();
        if (currentPath != null) chooser.setSelectedFile(currentPath.toFile());
        return chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile().toPath() : null;
    }

    @Override
    public SaveChoice confirmClose(EditorView editor) {
        EdtExecutor.requireEdt();
        int choice = JOptionPane.showConfirmDialog(this, msg.format("dialog.save-changes.message", title(editor)),
                msg.get("dialog.save-changes.title"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        return switch (choice) {
            case JOptionPane.YES_OPTION -> SaveChoice.SAVE;
            case JOptionPane.NO_OPTION -> SaveChoice.DISCARD;
            default -> SaveChoice.CANCEL;
        };
    }

    // --8<-- [start:show-error]
    @Override
    public void showError(String message) {
        EdtExecutor.requireEdt();
        // Passing a text component avoids interpreting file names/messages as Swing HTML.
        javax.swing.JTextArea text = new javax.swing.JTextArea(message);
        text.setEditable(false);
        text.setOpaque(false); // shows the dialog colour of the current theme
        JOptionPane.showMessageDialog(this, text, msg.get("dialog.error.title"), JOptionPane.ERROR_MESSAGE);
    }
    // --8<-- [end:show-error]

    @Override
    public void showInfo(String message) {
        EdtExecutor.requireEdt();
        JTextArea text = new JTextArea(message);
        text.setEditable(false);
        text.setOpaque(false);
        JOptionPane.showMessageDialog(this, text, getTitle(), JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public boolean confirmSaveAsUtf8(String charsetName) {
        EdtExecutor.requireEdt();
        JTextArea text = new JTextArea(msg.format("dialog.save-utf8.message", charsetName));
        text.setEditable(false);
        text.setOpaque(false); // shows the dialog colour of the current theme
        return JOptionPane.showConfirmDialog(this, text, msg.get("dialog.save-utf8.title"), JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    @Override
    public void showSearch(boolean replace) {
        EdtExecutor.requireEdt();
        if (activeEditor() != null) search.showFor(replace);
    }

    @Override
    public void findNext() {
        EdtExecutor.requireEdt();
        search.findNext();
    }

    @Override
    public void showAbout() {
        EdtExecutor.requireEdt();
        JOptionPane.showMessageDialog(this,
                "<html><center><h2>amade-pad 0.1.0</h2>"
                        + "<p>" + msg.get("dialog.about.text") + "</p>"
                        + "<p>" + msg.get("dialog.about.desktop") + "</p><br>"
                        + "<small>&copy; 2026 datazuul.com &bull; Wolfgang Amadeus Mozart Edition</small></center></html>",
                msg.get("dialog.about.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void dispose() {
        EdtExecutor.requireEdt();
        search.dispose();
        super.dispose();
    }

    private String title(EditorView editor) {
        return editor.state().title(msg.get("document.untitled"));
    }

    private void updateStatus() {
        EditorTab editor = (EditorTab) activeEditor();
        if (editor == null) {
            status.setText(msg.get("status.no-document"));
            return;
        }
        JTextArea area = editor.getTextArea();
        try {
            // Document listeners can run before the caret is adjusted after a removal.
            int caret = Math.min(area.getCaretPosition(), area.getDocument().getLength());
            int line = area.getLineOfOffset(caret);
            int column = caret - area.getLineStartOffset(line) + 1;
            status.setText(msg.format("status.position", line + 1, column, editor.state().format().label(),
                    area.getDocument().getLength()));
        } catch (BadLocationException e) {
            throw new IllegalStateException("Invalid caret position", e);
        }
    }
}
