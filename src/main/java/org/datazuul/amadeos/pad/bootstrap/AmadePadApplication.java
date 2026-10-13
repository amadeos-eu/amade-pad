package org.datazuul.amadeos.pad.bootstrap;

import org.datazuul.amadeos.commons.app.AmadeApplication;
import org.datazuul.amadeos.commons.app.AppInfo;
import org.datazuul.amadeos.pad.ui.main.MainPresenter;
import org.datazuul.amadeos.pad.ui.main.MainWindow;

import java.nio.file.Path;

public final class AmadePadApplication {
    public static final AppInfo APP = new AppInfo("amade-pad", "Text Editor", "pad");

    private AmadePadApplication() {
    }

    public static void main(String[] args) {
        AmadeApplication.launch(APP, injector -> {
            MainWindow window = injector.getInstance(MainWindow.class);
            MainPresenter presenter = injector.getInstance(MainPresenter.class);
            window.bind(presenter);
            if (args.length == 0) presenter.newDocument();
            else for (String arg : args) presenter.open(Path.of(arg));
            window.setVisible(true);
        }, new PadModule());
    }
}
