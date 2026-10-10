package org.datazuul.amadeos.pad;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Gutter component rendering line numbers for a JTextArea.
 */
public final class LineNumberMargin extends JComponent implements DocumentListener, CaretListener {

    private final JTextArea textArea;
    private Color gutterBg = new Color(0xDE, 0xD4, 0xCD);
    private Color gutterFg = new Color(0x60, 0x60, 0x60);
    private Color currentLineFg = new Color(0x10, 0x10, 0x10);

    public LineNumberMargin(JTextArea textArea) {
        this.textArea = textArea;
        this.textArea.getDocument().addDocumentListener(this);
        this.textArea.addCaretListener(this);
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        updateWidth();
    }

    private void updateWidth() {
        int lines = textArea.getLineCount();
        int digits = Math.max(3, String.valueOf(lines).length());
        FontMetrics fm = getFontMetrics(getFont());
        int width = fm.charWidth('0') * digits + 14;
        Dimension d = new Dimension(width, textArea.getHeight());
        setPreferredSize(d);
        revalidate();
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
                Rectangle r = textArea.modelToView2D(lineStartOffset).getBounds();

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
        } catch (Exception ignored) {}
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
