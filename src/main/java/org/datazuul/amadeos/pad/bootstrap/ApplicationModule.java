package org.datazuul.amadeos.pad.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.datazuul.amadeos.pad.repository.FileSystemDocumentRepository;
import org.datazuul.amadeos.pad.repository.DocumentRepository;
import org.datazuul.amadeos.pad.ui.main.MainView;
import org.datazuul.amadeos.pad.ui.main.MainWindow;

import java.util.concurrent.ExecutorService;

public final class ApplicationModule extends AbstractModule {
    private final ExecutorService executor;

    public ApplicationModule(ExecutorService executor) {
        this.executor = executor;
    }

    @Override
    protected void configure() {
        bind(ExecutorService.class).toInstance(executor);
        bind(DocumentRepository.class).to(FileSystemDocumentRepository.class);
        bind(MainWindow.class).in(Scopes.SINGLETON);
        bind(MainView.class).to(MainWindow.class);
    }
}
