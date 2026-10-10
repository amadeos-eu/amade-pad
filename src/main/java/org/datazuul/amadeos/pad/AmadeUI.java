package org.datazuul.amadeos.pad;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicToggleButtonUI;

/**
 * Provides authentic Motif 3D styling and theme colors for amadeOS Swing applications,
 * eliminating default Metal/Ocean blueish controls and replacing them with workstation 3D bevels.
 */
public final class AmadeUI {

    // Warm Motif Palette matching amadeOS DesktopTheme
    public static final Color BASE = new Color(0xCE, 0xC2, 0xBB);
    public static final Color HIGHLIGHT = Color.WHITE;
    public static final Color SOFT_HIGHLIGHT = new Color(0xEA, 0xE5, 0xE2);
    public static final Color SHADOW = new Color(0x8C, 0x82, 0x7A);
    public static final Color DARK_SHADOW = new Color(0x42, 0x3B, 0x35);
    public static final Color SUNKEN_BG = new Color(0xBA, 0xAF, 0xA8);
    public static final Color TEXT_ACTIVE = new Color(0x11, 0x11, 0x11);
    public static final Color TEXT_INACTIVE = new Color(0x75, 0x6E, 0x68);

    private AmadeUI() {}

    /**
     * Installs the amadeOS Motif 3D look-and-feel centrally into UIManager.
     */
    public static void install() {
        try {
            // Start with base CrossPlatform / Basic LAF
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Global Palette
        UIManager.put("control", new ColorUIResource(BASE));
        UIManager.put("controlHighlight", new ColorUIResource(HIGHLIGHT));
        UIManager.put("controlLtHighlight", new ColorUIResource(HIGHLIGHT));
        UIManager.put("controlShadow", new ColorUIResource(SHADOW));
        UIManager.put("controlDkShadow", new ColorUIResource(DARK_SHADOW));
        UIManager.put("controlText", new ColorUIResource(TEXT_ACTIVE));

        // Panels & Dialogs
        UIManager.put("Panel.background", new ColorUIResource(BASE));
        UIManager.put("OptionPane.background", new ColorUIResource(BASE));
        UIManager.put("OptionPane.messageForeground", new ColorUIResource(TEXT_ACTIVE));

        // Buttons
        UIManager.put("ButtonUI", AmadeButtonUI.class.getName());
        UIManager.put("Button.background", new ColorUIResource(BASE));
        UIManager.put("Button.foreground", new ColorUIResource(TEXT_ACTIVE));
        UIManager.put("Button.select", new ColorUIResource(SUNKEN_BG));
        UIManager.put("Button.focus", new ColorUIResource(SHADOW));
        UIManager.put("Button.border", new MotifBevelBorder(true, 2));

        // Toggle Buttons
        UIManager.put("ToggleButtonUI", AmadeToggleButtonUI.class.getName());
        UIManager.put("ToggleButton.background", new ColorUIResource(BASE));
        UIManager.put("ToggleButton.foreground", new ColorUIResource(TEXT_ACTIVE));
        UIManager.put("ToggleButton.select", new ColorUIResource(SUNKEN_BG));
        UIManager.put("ToggleButton.border", new MotifBevelBorder(true, 2));

        // Menus
        UIManager.put("MenuBar.background", new ColorUIResource(BASE));
        UIManager.put("MenuBar.border", new MotifBevelBorder(true, 2));
        UIManager.put("Menu.background", new ColorUIResource(BASE));
        UIManager.put("Menu.foreground", new ColorUIResource(TEXT_ACTIVE));
        UIManager.put("Menu.selectionBackground", new ColorUIResource(SOFT_HIGHLIGHT));
        UIManager.put("Menu.selectionForeground", new ColorUIResource(TEXT_ACTIVE));
        UIManager.put("MenuItem.background", new ColorUIResource(BASE));
        UIManager.put("MenuItem.foreground", new ColorUIResource(TEXT_ACTIVE));
        UIManager.put("MenuItem.selectionBackground", new ColorUIResource(SOFT_HIGHLIGHT));
        UIManager.put("MenuItem.selectionForeground", new ColorUIResource(TEXT_ACTIVE));
        UIManager.put("PopupMenu.background", new ColorUIResource(BASE));
        UIManager.put("PopupMenu.border", new MotifBevelBorder(true, 2));

        // Text Fields & Areas
        UIManager.put("TextField.background", new ColorUIResource(Color.WHITE));
        UIManager.put("TextField.foreground", new ColorUIResource(TEXT_ACTIVE));
        UIManager.put("TextField.selectionBackground", new ColorUIResource(new Color(0x4A, 0x6B, 0x82)));
        UIManager.put("TextField.selectionForeground", new ColorUIResource(Color.WHITE));
        UIManager.put("TextField.border", new MotifBevelBorder(false, 2));

        // ScrollPanes & ScrollBars
        UIManager.put("ScrollPane.background", new ColorUIResource(BASE));
        UIManager.put("ScrollPane.border", new MotifBevelBorder(false, 2));
        UIManager.put("ScrollBar.background", new ColorUIResource(SUNKEN_BG));
        UIManager.put("ScrollBar.thumb", new ColorUIResource(BASE));
        UIManager.put("ScrollBar.track", new ColorUIResource(SUNKEN_BG));

        // TabbedPane
        UIManager.put("TabbedPane.background", new ColorUIResource(BASE));
        UIManager.put("TabbedPane.foreground", new ColorUIResource(TEXT_ACTIVE));
        UIManager.put("TabbedPane.selected", new ColorUIResource(SOFT_HIGHLIGHT));
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(2, 2, 2, 2));
    }

    /**
     * Motif 3D Bevel Border drawing authentic 3D chiseled workstation borders.
     */
    public static class MotifBevelBorder extends AbstractBorder {
        private final boolean raised;
        private final int thickness;

        public MotifBevelBorder(boolean raised, int thickness) {
            this.raised = raised;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            Color tl1 = raised ? HIGHLIGHT : DARK_SHADOW;
            Color tl2 = raised ? SOFT_HIGHLIGHT : SHADOW;
            Color br1 = raised ? DARK_SHADOW : HIGHLIGHT;
            Color br2 = raised ? SHADOW : SOFT_HIGHLIGHT;

            for (int i = 0; i < thickness; i++) {
                Color tl = (i == 0) ? tl1 : tl2;
                Color br = (i == 0) ? br1 : br2;

                g2.setColor(tl);
                g2.drawLine(x + i, y + i, x + width - 1 - i, y + i);
                g2.drawLine(x + i, y + i, x + i, y + height - 1 - i);

                g2.setColor(br);
                g2.drawLine(x + i, y + height - 1 - i, x + width - 1 - i, y + height - 1 - i);
                g2.drawLine(x + width - 1 - i, y + i, x + width - 1 - i, y + height - 1 - i);
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness + 3, thickness + 8, thickness + 3, thickness + 8);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = thickness + 3;
            insets.left = thickness + 8;
            insets.bottom = thickness + 3;
            insets.right = thickness + 8;
            return insets;
        }
    }

    /**
     * Authentic 3D Motif ButtonUI matching workstation window buttons.
     */
    public static class AmadeButtonUI extends BasicButtonUI {
        private static final AmadeButtonUI INSTANCE = new AmadeButtonUI();

        public static javax.swing.plaf.ComponentUI createUI(JComponent c) {
            return INSTANCE;
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            AbstractButton b = (AbstractButton) c;
            b.setOpaque(true);
            b.setFocusPainted(false);
            b.setRolloverEnabled(true);
            b.setBackground(BASE);
            b.setForeground(TEXT_ACTIVE);
            b.setBorder(new MotifBevelBorder(true, 2));
            b.setFont(b.getFont().deriveFont(Font.BOLD, 12.0f));
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            boolean isPressed = model.isArmed() && model.isPressed();
            boolean isHovered = model.isRollover();

            Color bg;
            if (isPressed) {
                bg = SUNKEN_BG;
                b.setBorder(new MotifBevelBorder(false, 2));
            } else if (isHovered) {
                bg = SOFT_HIGHLIGHT;
                b.setBorder(new MotifBevelBorder(true, 2));
            } else {
                bg = BASE;
                b.setBorder(new MotifBevelBorder(true, 2));
            }

            g2.setColor(bg);
            g2.fillRect(0, 0, c.getWidth(), c.getHeight());

            // If pressed, offset text & icon by 1px (classic Motif button click feedback)
            if (isPressed) {
                g2.translate(1, 1);
            }

            super.paint(g2, c);

            if (isPressed) {
                g2.translate(-1, -1);
            }
        }
    }

    /**
     * Authentic 3D Motif ToggleButtonUI (sunken when selected).
     */
    public static class AmadeToggleButtonUI extends BasicToggleButtonUI {
        private static final AmadeToggleButtonUI INSTANCE = new AmadeToggleButtonUI();

        public static javax.swing.plaf.ComponentUI createUI(JComponent c) {
            return INSTANCE;
        }

        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            AbstractButton b = (AbstractButton) c;
            b.setOpaque(true);
            b.setFocusPainted(false);
            b.setRolloverEnabled(true);
            b.setBackground(BASE);
            b.setForeground(TEXT_ACTIVE);
            b.setBorder(new MotifBevelBorder(true, 2));
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            ButtonModel model = b.getModel();
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            boolean isSelected = model.isSelected();
            boolean isPressed = model.isArmed() && model.isPressed();
            boolean isHovered = model.isRollover();

            Color bg;
            boolean raised;
            if (isPressed || isSelected) {
                bg = SUNKEN_BG;
                raised = false;
            } else if (isHovered) {
                bg = SOFT_HIGHLIGHT;
                raised = true;
            } else {
                bg = BASE;
                raised = true;
            }

            b.setBorder(new MotifBevelBorder(raised, 2));
            g2.setColor(bg);
            g2.fillRect(0, 0, c.getWidth(), c.getHeight());

            if (!raised) {
                g2.translate(1, 1);
            }

            super.paint(g2, c);

            if (!raised) {
                g2.translate(-1, -1);
            }
        }
    }
}
