package org.datazuul.amadeos.pad.ui.editor;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Rectangle2D;

/**
 * Gutter component rendering line numbers for a JTextArea.
 */
public final class LineNumberMargin extends JComponent implements DocumentListener, CaretListener {

    private final JTextArea textArea;
    private final Color gutterBg = new Color(0xDE, 0xD4, 0xCD);
    private final Color gutterFg = new Color(0x60, 0x60, 0x60);
    private final Color currentLineFg = new Color(0x10, 0x10, 0x10);
    private int digits;

    public LineNumberMargin(JTextArea textArea) {
        this.textArea = textArea;
        this.textArea.getDocument().addDocumentListener(this);
        this.textArea.addCaretListener(this);
        // The text area's height changes without document events (word wrap, window resize),
        // so the margin follows its actual size instead of a height captured on the last edit.
        this.textArea.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                revalidate();
                repaint();
            }
        });
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        digits = digits();
    }

    private int digits() {
        return Math.max(3, String.valueOf(textArea.getLineCount()).length());
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) return super.getPreferredSize();
        FontMetrics fm = getFontMetrics(getFont());
        return new Dimension(fm.charWidth('0') * digits + 14, textArea.getHeight());
    }

    private void updateWidth() {
        int current = digits();
        if (current != digits) {
            digits = current;
            revalidate();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(gutterBg);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Right border line
        g2.setColor(new Color(0xBB, 0xB0, 0xA8));
        g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());

        FontMetrics fm = getFontMetrics(getFont());
        int fontAscent = fm.getAscent();

        Rectangle clip = g2.getClipBounds();
        Point startPoint = new Point(0, clip.y);
        Point endPoint = new Point(0, clip.y + clip.height);

        int startPos = textArea.viewToModel2D(startPoint);
        int endPos = textArea.viewToModel2D(endPoint);

        try {
            int startLine = textArea.getLineOfOffset(startPos);
            int endLine = textArea.getLineOfOffset(endPos);
            int currentLine = textArea.getLineOfOffset(textArea.getCaretPosition());

            for (int i = startLine; i <= endLine; i++) {
                int lineStartOffset = textArea.getLineStartOffset(i);
                Rectangle2D bounds = textArea.modelToView2D(lineStartOffset);
                if (bounds == null) continue;
                Rectangle r = bounds.getBounds();

                String lineStr = String.valueOf(i + 1);
                int stringWidth = fm.stringWidth(lineStr);
                int x = getWidth() - stringWidth - 6;
                int y = r.y + fontAscent;

                if (i == currentLine) {
                    g2.setColor(currentLineFg);
                    g2.drawString(lineStr, x, y);
                } else {
                    g2.setColor(gutterFg);
                    g2.drawString(lineStr, x, y);
                }
            }
        } catch (javax.swing.text.BadLocationException e) {
            org.slf4j.LoggerFactory.getLogger(LineNumberMargin.class).warn("Could not render line numbers", e);
        }
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        updateWidth();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        updateWidth();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        updateWidth();
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        repaint();
    }
}
