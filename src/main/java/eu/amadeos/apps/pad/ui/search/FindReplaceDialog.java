package eu.amadeos.apps.pad.ui.search;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.apps.pad.service.SearchService;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.commons.swing.AmadeUI;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

/**
 * Find and Replace dialog for amade-pad.
 */
public final class FindReplaceDialog extends JDialog {

    private final JTextField findField = new JTextField(20);
    private final JTextField replaceField = new JTextField(20);
    private final JCheckBox matchCaseCheck;
    private final JCheckBox wrapAroundCheck;
    private final JButton replaceBtn;
    private final JButton replaceAllBtn;
    private final Messages msg;
    private boolean replaceMode = true;

    private final SearchService search;
    private final Supplier<JTextArea> activeEditor;

    public FindReplaceDialog(Frame owner, SearchService search, Messages msg, Supplier<JTextArea> activeEditor) {
        super(owner, msg.get("search.title"), false);
        EdtExecutor.requireEdt();
        this.msg = msg;
        matchCaseCheck = new JCheckBox(msg.get("search.match-case"));
        wrapAroundCheck = new JCheckBox(msg.get("search.wrap-around"), true);
        replaceBtn = new JButton(msg.get("search.replace"));
        replaceAllBtn = new JButton(msg.get("search.replace-all"));
        this.search = search;
        this.activeEditor = activeEditor;
        setLayout(new BorderLayout(8, 8));

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 4, 12));
        formPanel.add(new JLabel(msg.get("search.find")));
        formPanel.add(findField);
        formPanel.add(new JLabel(msg.get("search.replace-with")));
        formPanel.add(replaceField);

        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        optionsPanel.add(matchCaseCheck);
        optionsPanel.add(wrapAroundCheck);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(optionsPanel, BorderLayout.SOUTH);
        add(centerPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton findBtn = new JButton(msg.get("search.find-next"));
        findBtn.addActionListener(e -> findNext());

        replaceBtn.addActionListener(e -> replaceCurrent());

        replaceAllBtn.addActionListener(e -> replaceAll());

        JButton closeBtn = new JButton(msg.get("search.close"));
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
        setTitle(msg.get(isReplaceMode ? "search.title.replace" : "search.title.find"));
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
            JOptionPane.showMessageDialog(this, msg.format("search.not-found", query), msg.get("search.title.find"),
                    JOptionPane.INFORMATION_MESSAGE);
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
            JOptionPane.showMessageDialog(this, msg.format("search.replaced", count), msg.get("search.replace-all"),
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, msg.format("search.not-found", query), msg.get("search.replace-all"),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

}
