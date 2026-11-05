package eu.amadeos.apps.pad.ui.main;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class WindowIconTest {
    @Test
    void theWindowIconIsPackaged() {
        // MainWindow sets it as the frame's icon; without it the window shows the Java logo
        assertNotNull(MainWindow.class.getResource("/icons/amade-pad.png"));
    }
}
