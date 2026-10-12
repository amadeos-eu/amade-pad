package org.datazuul.amadeos.pad.ui.swing;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
}
