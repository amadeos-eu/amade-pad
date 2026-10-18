package eu.amadeos.apps.pad.bootstrap;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.Scopes;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.apps.pad.repository.DocumentRepository;
import eu.amadeos.apps.pad.repository.FileSystemDocumentRepository;
import eu.amadeos.apps.pad.ui.main.MainView;
import eu.amadeos.apps.pad.ui.main.MainWindow;

/**
 * Text editor bindings. The background executor, theme and the other shared services come from
 * {@link eu.amadeos.commons.app.CommonsModule}.
 */
public final class PadModule extends AbstractModule {
    /** The texts, {@code messages.properties} and its translations. */
    public static final String MESSAGES = "eu.amadeos.apps.pad.messages";

    @Override
    protected void configure() {
        bind(DocumentRepository.class).to(FileSystemDocumentRepository.class);
        bind(MainWindow.class).in(Scopes.SINGLETON);
        bind(MainView.class).to(MainWindow.class);
    }

    /** The texts in the language {@code AmadeApplication} chose. */
    @Provides
    @Singleton
    Messages messages(java.util.Locale locale) {
        return Messages.load(PadModule.class, MESSAGES, locale);
    }
}
