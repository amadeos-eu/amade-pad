package org.datazuul.amadeos.pad.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.datazuul.amadeos.pad.repository.DocumentRepository;
import org.datazuul.amadeos.pad.repository.FileSystemDocumentRepository;
import org.datazuul.amadeos.pad.ui.main.MainView;
import org.datazuul.amadeos.pad.ui.main.MainWindow;

/**
 * Text editor bindings. The background executor, theme and the other shared services come from
 * {@link org.datazuul.amadeos.commons.app.CommonsModule}.
 */
public final class PadModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DocumentRepository.class).to(FileSystemDocumentRepository.class);
        bind(MainWindow.class).in(Scopes.SINGLETON);
        bind(MainView.class).to(MainWindow.class);
    }
}
