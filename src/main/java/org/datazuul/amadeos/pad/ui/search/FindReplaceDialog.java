package org.datazuul.amadeos.pad.ui.search;

import org.datazuul.amadeos.pad.service.SearchService;
import org.datazuul.amadeos.pad.ui.common.EdtExecutor;
import org.datazuul.amadeos.pad.ui.swing.AmadeUI;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 * Find and Replace dialog for amade-pad.
 */
public final class FindReplaceDialog extends JDialog {

    private final JTextField findField = new JTextField(20);
    private final JTextField replaceField = new JTextField(20);
    private final JCheckBox matchCaseCheck = new JCheckBox("Match case");
    private final JCheckBox wrapAroundCheck = new JCheckBox("Wrap around", true);
    private final JButton replaceBtn = new JButton("Replace");
    private final JButton replaceAllBtn = new JButton("Replace All");
    private boolean replaceMode = true;

    private final SearchService search;
    private final Supplier<JTextArea> activeEditor;

    public FindReplaceDialog(Frame owner, SearchService search, Supplier<JTextArea> activeEditor) {
        super(owner, "Find and Replace", false);
        EdtExecutor.requireEdt();
        this.search = search;
        this.activeEditor = activeEditor;
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(AmadeUI.BASE);

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        formPanel.setBackground(AmadeUI.BASE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        formPanel.add(new JLabel("Find:"));
        formPanel.add(findField);
        formPanel.add(new JLabel("Replace with:"));
        formPanel.add(replaceField);

        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.setBackground(AmadeUI.BASE);
        matchCaseCheck.setBackground(AmadeUI.BASE);
        wrapAroundCheck.setBackground(AmadeUI.BASE);
        optionsPanel.add(matchCaseCheck);
        optionsPanel.add(wrapAroundCheck);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(AmadeUI.BASE);
        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(optionsPanel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(AmadeUI.BASE);
        JButton findBtn = new JButton("Find Next");
        findBtn.addActionListener(e -> findNext());

        replaceBtn.addActionListener(e -> replaceCurrent());

        replaceAllBtn.addActionListener(e -> replaceAll());

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> setVisible(false));

        btnPanel.add(findBtn);
        btnPanel.add(replaceBtn);
        btnPanel.add(replaceAllBtn);
        btnPanel.add(closeBtn);
        add(btnPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    public void showFor(boolean isReplaceMode) {
        EdtExecutor.requireEdt();
        setTitle(isReplaceMode ? "Replace" : "Find");
        // In find mode the replace actions must not run with a stale "Replace with" value.
        replaceMode = isReplaceMode;
        replaceField.setEnabled(isReplaceMode);
        replaceBtn.setEnabled(isReplaceMode);
        replaceAllBtn.setEnabled(isReplaceMode);
        setVisible(true);
        findField.requestFocusInWindow();
        findField.selectAll();
    }

    public boolean findNext() {
        EdtExecutor.requireEdt();
        JTextArea activeTextArea = activeEditor.get();
        if (activeTextArea == null) return false;
        String query = findField.getText();
        if (query.isEmpty()) return false;

        String text = activeTextArea.getText();
        boolean matchCase = matchCaseCheck.isSelected();
        int caret = activeTextArea.getCaretPosition();

        int index = search.find(text, query, caret, matchCase, wrapAroundCheck.isSelected());

        if (index >= 0) {
            activeTextArea.setSelectionStart(index);
            activeTextArea.setSelectionEnd(index + query.length());
            return true;
        } else {
            JOptionPane.showMessageDialog(this, "Text not found: \"" + query + "\"", "Find", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
    }

    void replaceCurrent() {
        EdtExecutor.requireEdt();
        if (!replaceMode) return;
        JTextArea activeTextArea = activeEditor.get();
        if (activeTextArea == null) return;
        String selected = activeTextArea.getSelectedText();
        String query = findField.getText();
        String replacement = replaceField.getText();

        boolean match = search.matches(selected, query, matchCaseCheck.isSelected());
        if (match) {
            activeTextArea.replaceSelection(replacement);
        }
        findNext();
    }

    void replaceAll() {
        EdtExecutor.requireEdt();
        if (!replaceMode) return;
        JTextArea activeTextArea = activeEditor.get();
        if (activeTextArea == null) return;
        String query = findField.getText();
        if (query.isEmpty()) return;
        String replacement = replaceField.getText();

        String text = activeTextArea.getText();
        boolean matchCase = matchCaseCheck.isSelected();

        SearchService.Replacement result = search.replaceAll(text, query, replacement, matchCase);
        int count = result.count();

        if (count > 0) {
            activeTextArea.setText(result.text());
            JOptionPane.showMessageDialog(this, "Replaced " + count + " occurrences.", "Replace All", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Text not found: \"" + query + "\"", "Replace All", JOptionPane.INFORMATION_MESSAGE);
        }
    }

}
