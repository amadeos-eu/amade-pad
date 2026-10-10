package org.datazuul.amadeos.pad;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.CaretEvent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

/**
 * Main application frame for amade-pad text editor.
 */
public final class AmadePadApplication extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(AmadePadApplication.class.getName());

    public static final Color MOTIF_BASE = new Color(0xCE, 0xC2, 0xBB);
    public static final Color MOTIF_TEXT = new Color(0x11, 0x11, 0x11);
    public static final Color EDITOR_BG = Color.WHITE;
    public static final Color EDITOR_FG = new Color(0x20, 0x20, 0x20);

    private final JTabbedPane tabbedPane;
    private final JLabel statusLabel;
    private final FindReplaceDialog findDialog;
    private boolean wordWrapEnabled = false;
    private boolean lineNumbersEnabled = true;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AmadeUI.install();

            AmadePadApplication app = new AmadePadApplication();
            if (args != null && args.length > 0) {
                for (String arg : args) {
                    File f = new File(arg);
                    app.openFile(f);
                }
            } else {
                app.newTab(null);
            }
            app.setVisible(true);
        });
    }

    public AmadePadApplication() {
        super("amade-pad — amadeOS Text Editor");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setPreferredSize(new Dimension(800, 600));
        setLayout(new BorderLayout());

        findDialog = new FindReplaceDialog(this);

        // Menu bar
        setJMenuBar(createMenuBar());

        // Tabbed pane for open documents
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(MOTIF_BASE);
        tabbedPane.addChangeListener(e -> updateStatus());
        add(tabbedPane, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 3));
        statusBar.setBackground(MOTIF_BASE);
        statusBar.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        statusLabel = new JLabel("Line: 1, Col: 1 | UTF-8 | 0 chars");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        statusLabel.setForeground(MOTIF_TEXT);
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();
        mb.setBackground(MOTIF_BASE);
        mb.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

        // 1. FILE MENU
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem newItem = new JMenuItem("New");
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newItem.addActionListener(e -> newTab(null));
        fileMenu.add(newItem);

        JMenuItem openItem = new JMenuItem("Open...");
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openItem.addActionListener(e -> chooseAndOpenFile());
        fileMenu.add(openItem);

        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveItem.addActionListener(e -> saveCurrent());
        fileMenu.add(saveItem);

        JMenuItem saveAsItem = new JMenuItem("Save As...");
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        saveAsItem.addActionListener(e -> saveAsCurrent());
        fileMenu.add(saveAsItem);

        fileMenu.addSeparator();

        JMenuItem closeTabItem = new JMenuItem("Close Tab");
        closeTabItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
        closeTabItem.addActionListener(e -> closeActiveTab());
        fileMenu.add(closeTabItem);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener(e -> exitApplication());
        fileMenu.add(exitItem);

        mb.add(fileMenu);

        // 2. EDIT MENU
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);

        JMenuItem undoItem = new JMenuItem("Undo");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        undoItem.addActionListener(e -> {
            EditorTab tab = getActiveTab();
            if (tab != null && tab.getUndoManager().canUndo()) {
                try { tab.getUndoManager().undo(); } catch (CannotUndoException ignored) {}
            }
        });
        editMenu.add(undoItem);

        JMenuItem redoItem = new JMenuItem("Redo");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        redoItem.addActionListener(e -> {
            EditorTab tab = getActiveTab();
            if (tab != null && tab.getUndoManager().canRedo()) {
                try { tab.getUndoManager().redo(); } catch (CannotRedoException ignored) {}
            }
        });
        editMenu.add(redoItem);

        editMenu.addSeparator();

        JMenuItem cutItem = new JMenuItem("Cut");
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        cutItem.addActionListener(e -> {
            EditorTab tab = getActiveTab();
            if (tab != null) tab.getTextArea().cut();
        });
        editMenu.add(cutItem);

        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        copyItem.addActionListener(e -> {
            EditorTab tab = getActiveTab();
            if (tab != null) tab.getTextArea().copy();
        });
        editMenu.add(copyItem);

        JMenuItem pasteItem = new JMenuItem("Paste");
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        pasteItem.addActionListener(e -> {
            EditorTab tab = getActiveTab();
            if (tab != null) tab.getTextArea().paste();
        });
        editMenu.add(pasteItem);

        JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        selectAllItem.addActionListener(e -> {
            EditorTab tab = getActiveTab();
            if (tab != null) tab.getTextArea().selectAll();
        });
        editMenu.add(selectAllItem);

        mb.add(editMenu);

        // 3. SEARCH MENU
        JMenu searchMenu = new JMenu("Search");
        searchMenu.setMnemonic(KeyEvent.VK_S);

        JMenuItem findItem = new JMenuItem("Find...");
        findItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        findItem.addActionListener(e -> {
            EditorTab tab = getActiveTab();
            if (tab != null) findDialog.showFor(tab.getTextArea(), false);
        });
        searchMenu.add(findItem);

        JMenuItem findNextItem = new JMenuItem("Find Next");
        findNextItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        findNextItem.addActionListener(e -> findDialog.findNext());
        searchMenu.add(findNextItem);

        JMenuItem replaceItem = new JMenuItem("Replace...");
        replaceItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK));
        replaceItem.addActionListener(e -> {
            EditorTab tab = getActiveTab();
            if (tab != null) findDialog.showFor(tab.getTextArea(), true);
        });
        searchMenu.add(replaceItem);

        mb.add(searchMenu);

        // 4. VIEW MENU
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);

        JCheckBoxMenuItem lineNumsItem = new JCheckBoxMenuItem("Line Numbers", lineNumbersEnabled);
        lineNumsItem.addActionListener(e -> {
            lineNumbersEnabled = lineNumsItem.isSelected();
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (tabbedPane.getComponentAt(i) instanceof EditorTab tab) {
                    tab.setLineNumbersVisible(lineNumbersEnabled);
                }
            }
        });
        viewMenu.add(lineNumsItem);

        JCheckBoxMenuItem wordWrapItem = new JCheckBoxMenuItem("Word Wrap", wordWrapEnabled);
        wordWrapItem.addActionListener(e -> {
            wordWrapEnabled = wordWrapItem.isSelected();
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (tabbedPane.getComponentAt(i) instanceof EditorTab tab) {
                    tab.getTextArea().setLineWrap(wordWrapEnabled);
                    tab.getTextArea().setWrapStyleWord(wordWrapEnabled);
                }
            }
        });
        viewMenu.add(wordWrapItem);

        mb.add(viewMenu);

        // 5. HELP MENU
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = new JMenuItem("About amade-pad");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        mb.add(helpMenu);

        return mb;
    }

    public EditorTab newTab(File file) {
        EditorTab tab = new EditorTab(file);
        tab.applyColors(EDITOR_BG, EDITOR_FG, Color.BLACK);
        tab.setLineNumbersVisible(lineNumbersEnabled);
        tab.getTextArea().setLineWrap(wordWrapEnabled);
        tab.getTextArea().setWrapStyleWord(wordWrapEnabled);

        tab.getTextArea().addCaretListener((CaretEvent e) -> updateStatus());
        tab.setOnStateChanged(() -> {
            int idx = tabbedPane.indexOfComponent(tab);
            if (idx >= 0) {
                tabbedPane.setTitleAt(idx, tab.getTitle());
            }
        });

        tabbedPane.addTab(tab.getTitle(), tab);
        tabbedPane.setSelectedComponent(tab);
        tab.getTextArea().requestFocusInWindow();
        updateStatus();
        return tab;
    }

    public void openFile(File file) {
        if (file == null || !file.exists()) return;
        // Check if already open
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getComponentAt(i) instanceof EditorTab tab) {
                if (file.equals(tab.getCurrentFile())) {
                    tabbedPane.setSelectedIndex(i);
                    return;
                }
            }
        }
        newTab(file);
    }

    private void chooseAndOpenFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            openFile(chooser.getSelectedFile());
        }
    }

    private void saveCurrent() {
        EditorTab tab = getActiveTab();
        if (tab == null) return;
        if (tab.getCurrentFile() == null) {
            saveAsCurrent();
        } else {
            try {
                tab.save();
                int idx = tabbedPane.getSelectedIndex();
                tabbedPane.setTitleAt(idx, tab.getTitle());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to save: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveAsCurrent() {
        EditorTab tab = getActiveTab();
        if (tab == null) return;
        JFileChooser chooser = new JFileChooser();
        if (tab.getCurrentFile() != null) {
            chooser.setSelectedFile(tab.getCurrentFile());
        }
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                tab.saveAs(chooser.getSelectedFile());
                int idx = tabbedPane.getSelectedIndex();
                tabbedPane.setTitleAt(idx, tab.getTitle());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to save: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean closeActiveTab() {
        int idx = tabbedPane.getSelectedIndex();
        if (idx < 0) return true;
        EditorTab tab = (EditorTab) tabbedPane.getComponentAt(idx);

        if (tab.isModified()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Save changes to " + tab.getTitle() + " before closing?",
                    "Save Changes",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                saveCurrent();
            } else if (choice == JOptionPane.CANCEL_OPTION) {
                return false;
            }
        }

        tabbedPane.removeTabAt(idx);
        if (tabbedPane.getTabCount() == 0) {
            newTab(null);
        }
        return true;
    }

    private void exitApplication() {
        while (tabbedPane.getTabCount() > 0) {
            if (!closeActiveTab()) {
                return; // User cancelled
            }
            if (tabbedPane.getTabCount() == 1 && ((EditorTab) tabbedPane.getComponentAt(0)).getCurrentFile() == null && !((EditorTab) tabbedPane.getComponentAt(0)).isModified()) {
                break;
            }
        }
        dispose();
        System.exit(0);
    }

    private EditorTab getActiveTab() {
        int idx = tabbedPane.getSelectedIndex();
        return (idx >= 0 && tabbedPane.getComponentAt(idx) instanceof EditorTab tab) ? tab : null;
    }

    private void updateStatus() {
        EditorTab tab = getActiveTab();
        if (tab == null) {
            statusLabel.setText("No document open");
            return;
        }

        JTextArea area = tab.getTextArea();
        int caret = area.getCaretPosition();
        int line = 1;
        int col = 1;
        try {
            line = area.getLineOfOffset(caret) + 1;
            int lineStart = area.getLineStartOffset(line - 1);
            col = caret - lineStart + 1;
        } catch (Exception ignored) {}

        int chars = area.getDocument().getLength();
        statusLabel.setText(String.format("Line: %d, Col: %d | UTF-8 | %d chars", line, col, chars));
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(
                this,
                "<html><center><h2>amade-pad 0.1.0</h2>"
                        + "<p>An authentic Motif/CDE-style text editor</p>"
                        + "<p>Designed for the <b>amadeOS</b> desktop environment</p><br>"
                        + "<small>&copy; 2026 datazuul.com &bull; Wolfgang Amadeus Mozart Edition</small></center></html>",
                "About amade-pad",
                JOptionPane.INFORMATION_MESSAGE);
    }
}

