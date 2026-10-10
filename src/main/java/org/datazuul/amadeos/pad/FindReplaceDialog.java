package org.datazuul.amadeos.pad;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Find and Replace dialog for amade-pad.
 */
public final class FindReplaceDialog extends JDialog {

    private final JTextField findField = new JTextField(20);
    private final JTextField replaceField = new JTextField(20);
    private final JCheckBox matchCaseCheck = new JCheckBox("Match case");
    private final JCheckBox wrapAroundCheck = new JCheckBox("Wrap around", true);

    private JTextArea activeTextArea;

    public FindReplaceDialog(Frame owner) {
        super(owner, "Find and Replace", false);
        setLayout(new BorderLayout(8, 8));

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        formPanel.add(new JLabel("Find:"));
        formPanel.add(findField);
        formPanel.add(new JLabel("Replace with:"));
        formPanel.add(replaceField);

        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(matchCaseCheck);
        optionsPanel.add(wrapAroundCheck);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(optionsPanel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton findBtn = new JButton("Find Next");
        findBtn.addActionListener(e -> findNext());

        JButton replaceBtn = new JButton("Replace");
        replaceBtn.addActionListener(e -> replaceCurrent());

        JButton replaceAllBtn = new JButton("Replace All");
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

    public void showFor(JTextArea textArea, boolean isReplaceMode) {
        this.activeTextArea = textArea;
        setTitle(isReplaceMode ? "Replace" : "Find");
        replaceField.setEnabled(isReplaceMode);
        setVisible(true);
        findField.requestFocusInWindow();
        findField.selectAll();
    }

    public boolean findNext() {
        if (activeTextArea == null) return false;
        String query = findField.getText();
        if (query.isEmpty()) return false;

        String text = activeTextArea.getText();
        boolean matchCase = matchCaseCheck.isSelected();
        int caret = activeTextArea.getCaretPosition();

        int index = search(text, query, caret, matchCase);
        if (index < 0 && wrapAroundCheck.isSelected()) {
            index = search(text, query, 0, matchCase);
        }

        if (index >= 0) {
            activeTextArea.setSelectionStart(index);
            activeTextArea.setSelectionEnd(index + query.length());
            return true;
        } else {
            JOptionPane.showMessageDialog(this, "Text not found: \"" + query + "\"", "Find", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
    }

    private void replaceCurrent() {
        if (activeTextArea == null) return;
        String selected = activeTextArea.getSelectedText();
        String query = findField.getText();
        String replacement = replaceField.getText();

        boolean match = (selected != null) && (matchCaseCheck.isSelected() ? selected.equals(query) : selected.equalsIgnoreCase(query));
        if (match) {
            activeTextArea.replaceSelection(replacement);
        }
        findNext();
    }

    private void replaceAll() {
        if (activeTextArea == null) return;
        String query = findField.getText();
        if (query.isEmpty()) return;
        String replacement = replaceField.getText();

        String text = activeTextArea.getText();
        boolean matchCase = matchCaseCheck.isSelected();

        int count = 0;
        int pos = 0;
        StringBuilder sb = new StringBuilder();

        while (true) {
            int idx = search(text, query, pos, matchCase);
            if (idx < 0) {
                sb.append(text.substring(pos));
                break;
            }
            sb.append(text, pos, idx);
            sb.append(replacement);
            pos = idx + query.length();
            count++;
        }

        if (count > 0) {
            activeTextArea.setText(sb.toString());
            JOptionPane.showMessageDialog(this, "Replaced " + count + " occurrences.", "Replace All", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Text not found: \"" + query + "\"", "Replace All", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private int search(String text, String query, int fromIndex, boolean matchCase) {
        if (matchCase) {
            return text.indexOf(query, fromIndex);
        } else {
            return text.toLowerCase().indexOf(query.toLowerCase(), fromIndex);
        }
    }
}
