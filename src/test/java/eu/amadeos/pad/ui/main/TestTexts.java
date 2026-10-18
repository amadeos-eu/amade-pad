package eu.amadeos.pad.ui.main;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import eu.amadeos.commons.i18n.LocaleSettings;
import eu.amadeos.commons.i18n.LocaleStore;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.pad.bootstrap.PadModule;

/** English texts and language settings in a temporary directory, for tests. */
public final class TestTexts {
    private TestTexts() {
    }

    public static Messages english() {
        return Messages.load(PadModule.class, PadModule.MESSAGES, Locale.ENGLISH);
    }

    public static LocaleSettings locales(Path config) {
        return new LocaleSettings(new LocaleStore(config), config.resolve("pad"), Map.of());
    }

    public static Path tempDir() {
        try {
            return Files.createTempDirectory("amade-pad-test");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
