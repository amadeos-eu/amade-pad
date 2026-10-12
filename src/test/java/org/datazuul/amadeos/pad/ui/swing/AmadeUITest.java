package org.datazuul.amadeos.pad.ui.swing;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmadeUITest {
    @Test
    void installsUsableMetalScrollBarGradient() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AmadeUI.install();
            List<?> gradient = assertInstanceOf(List.class, UIManager.get("ScrollBar.gradient"));
            assertEquals(5, gradient.size());

            JScrollBar scrollBar = new JScrollBar(JScrollBar.VERTICAL);
            scrollBar.setSize(20, 120);
            scrollBar.doLayout();
            BufferedImage image = new BufferedImage(20, 120, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                scrollBar.paint(graphics);
            } finally {
                graphics.dispose();
            }
        });
    }

    @Test
    void menuCheckIconShowsCheckmarkOnlyWhenSelected() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Icon icon = new AmadeUI.MotifMenuCheckIcon();
            assertEquals(0, interiorMarkPixels(icon, new JCheckBoxMenuItem("Word Wrap", false)));
            assertTrue(interiorMarkPixels(icon, new JCheckBoxMenuItem("Line Numbers", true)) > 0);
        });
    }

    /** Counts pixels inside the bevel that differ from the sunken background, i.e. the check mark. */
    private static int interiorMarkPixels(Icon icon, JCheckBoxMenuItem item) {
        BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            icon.paintIcon(item, graphics, 0, 0);
        } finally {
            graphics.dispose();
        }
        int background = AmadeUI.SUNKEN_BG.getRGB();
        int count = 0;
        for (int y = 1; y < icon.getIconHeight() - 1; y++) {
            for (int x = 1; x < icon.getIconWidth() - 1; x++) {
                if (image.getRGB(x, y) != background) count++;
            }
        }
        return count;
    }
}
