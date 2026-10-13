package org.datazuul.amadeos.pad.ui.main;

import com.google.inject.Inject;
import org.datazuul.amadeos.pad.service.SearchService;
import org.datazuul.amadeos.commons.swing.EdtExecutor;
import org.datazuul.amadeos.pad.ui.editor.EditorTab;
import org.datazuul.amadeos.pad.ui.editor.EditorView;
import org.datazuul.amadeos.pad.ui.search.FindReplaceDialog;
import org.datazuul.amadeos.commons.swing.AmadeUI;

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
    private final JLabel status = new JLabel("Line: 1, Col: 1 | UTF-8 | LF | 0 chars");
    private final FindReplaceDialog search;

    @Inject
    public MainWindow(SearchService searchService) {
        super("amade-pad — amadeOS Text Editor");
        EdtExecutor.requireEdt();
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setPreferredSize(new Dimension(800, 600));
        setLayout(new BorderLayout());
        search = new FindReplaceDialog(this, searchService, () -> {
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
        pack();
        setLocationRelativeTo(null);
    }

    public void bind(MainPresenter presenter) {
        EdtExecutor.requireEdt();
        setJMenuBar(new MainMenuBar(presenter));
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
            if (index >= 0) tabs.setTitleAt(index, editor.state().title());
            updateStatus();
        });
        editor.getTextArea().addCaretListener(e -> updateStatus());
        tabs.addTab(editor.state().title(), editor);
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
        int choice = JOptionPane.showConfirmDialog(this, "Save changes to " + editor.state().title() + " before closing?",
                "Save Changes", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        return switch (choice) {
            case JOptionPane.YES_OPTION -> SaveChoice.SAVE;
            case JOptionPane.NO_OPTION -> SaveChoice.DISCARD;
            default -> SaveChoice.CANCEL;
        };
    }

    @Override
    public void showError(String message) {
        EdtExecutor.requireEdt();
        // Passing a text component avoids interpreting file names/messages as Swing HTML.
        javax.swing.JTextArea text = new javax.swing.JTextArea(message);
        text.setEditable(false);
        text.setOpaque(false); // shows the dialog colour of the current theme
        JOptionPane.showMessageDialog(this, text, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public boolean confirmSaveAsUtf8(String charsetName) {
        EdtExecutor.requireEdt();
        JTextArea text = new JTextArea("The document contains characters that cannot be stored as " + charsetName
                + ".\nSave it as UTF-8 instead?");
        text.setEditable(false);
        text.setOpaque(false); // shows the dialog colour of the current theme
        return JOptionPane.showConfirmDialog(this, text, "Save as UTF-8", JOptionPane.YES_NO_OPTION,
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
                        + "<p>An authentic Motif/CDE-style text editor</p>"
                        + "<p>Designed for the <b>amadeOS</b> desktop environment</p><br>"
                        + "<small>&copy; 2026 datazuul.com &bull; Wolfgang Amadeus Mozart Edition</small></center></html>",
                "About amade-pad", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void dispose() {
        EdtExecutor.requireEdt();
        search.dispose();
        super.dispose();
    }

    private void updateStatus() {
        EditorTab editor = (EditorTab) activeEditor();
        if (editor == null) {
            status.setText("No document open");
            return;
        }
        JTextArea area = editor.getTextArea();
        try {
            // Document listeners can run before the caret is adjusted after a removal.
            int caret = Math.min(area.getCaretPosition(), area.getDocument().getLength());
            int line = area.getLineOfOffset(caret);
            int column = caret - area.getLineStartOffset(line) + 1;
            status.setText("Line: " + (line + 1) + ", Col: " + column + " | " + editor.state().format().label()
                    + " | " + area.getDocument().getLength() + " chars");
        } catch (BadLocationException e) {
            throw new IllegalStateException("Invalid caret position", e);
        }
    }
}
