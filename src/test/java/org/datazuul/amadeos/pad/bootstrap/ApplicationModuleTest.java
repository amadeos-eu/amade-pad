package org.datazuul.amadeos.pad.bootstrap;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import javax.swing.SwingUtilities;
import org.datazuul.amadeos.pad.ui.editor.EditorTab;
import org.datazuul.amadeos.pad.ui.main.MainMenuBar;
import org.datazuul.amadeos.pad.ui.main.MainPresenter;
import org.datazuul.amadeos.pad.ui.main.MainView;
import org.datazuul.amadeos.pad.ui.main.MainWindow;
import org.datazuul.amadeos.pad.ui.swing.AmadeUI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class ApplicationModuleTest {
    @TempDir Path directory;

    @Test
    void wiresSingleWindowAndClosesExecutor() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            SwingUtilities.invokeAndWait(() -> {
                AmadeUI.install();
                Injector injector = Guice.createInjector(new ApplicationModule(executor));
                MainWindow window = injector.getInstance(MainWindow.class);
                try {
                    assertSame(window, injector.getInstance(MainView.class));
                    MainPresenter presenter = injector.getInstance(MainPresenter.class);
                    window.bind(presenter);
                    presenter.newDocument();
                    assertEquals(1, window.editors().size());
                    assertInstanceOf(MainMenuBar.class, window.getJMenuBar());
                    assertTrue(presenter.exit().join());
                    assertTrue(executor.isShutdown());
                } finally {
                    window.dispose();
                }
            });
        }

    }

    @Test
    void opensEditsAndSavesThroughRealSwingAndVirtualThreadIo() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        Path path = directory.resolve("sample.txt");
        Files.writeString(path, "initial text");
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Injector injector = onEdt(() -> Guice.createInjector(new ApplicationModule(executor)));
            MainWindow window = onEdt(() -> injector.getInstance(MainWindow.class));
            MainPresenter presenter = onEdt(() -> injector.getInstance(MainPresenter.class));
            try {
                onEdt(() -> {
                    window.bind(presenter);
                    return null;
                });
                onEdt(() -> presenter.open(path)).join();
                onEdt(() -> {
                    EditorTab editor = (EditorTab) window.activeEditor();
                    assertEquals("initial text", editor.text());
                    editor.getTextArea().setCaretPosition(editor.text().length());
                    editor.getTextArea().setText("short");
                    assertEquals("sample.txt *", editor.state().title());
                    return null;
                });
                assertTrue(onEdt(() -> presenter.saveCurrent(false)).join());
                assertEquals("short", Files.readString(path));
                assertTrue(onEdt(presenter::exit).join());
                assertTrue(executor.isShutdown());
            } finally {
                onEdt(() -> {
                    window.dispose();
                    return null;
                });
            }
        }
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }
}
