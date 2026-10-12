package org.datazuul.amadeos.pad.bootstrap;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.datazuul.amadeos.pad.ui.main.MainPresenter;
import org.datazuul.amadeos.pad.ui.main.MainWindow;
import org.datazuul.amadeos.pad.ui.swing.AmadeUI;

import javax.swing.*;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AmadePadApplication {
    private AmadePadApplication() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AmadeUI.install();
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            Injector injector = Guice.createInjector(new ApplicationModule(executor));
            MainWindow window = injector.getInstance(MainWindow.class);
            MainPresenter presenter = injector.getInstance(MainPresenter.class);
            window.bind(presenter);
            if (args.length == 0) presenter.newDocument();
            else for (String arg : args) presenter.open(Path.of(arg));
            window.setVisible(true);
        });
    }
}
