package eu.amadeos.apps.pad.repository;

import com.google.inject.Inject;
import eu.amadeos.apps.pad.domain.Indentation;
import eu.amadeos.commons.config.AppDirectories;
import eu.amadeos.commons.config.PropertiesFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Properties;

/**
 * The editor choices that outlive the program, in {@code ~/.config/amadeos/pad/editor.properties}:
 * the indentation of new documents.
 */
public class EditorSettings {
    private static final Logger LOG = LoggerFactory.getLogger(EditorSettings.class);
    private final Path file;

    @Inject
    public EditorSettings(AppDirectories directories) {
        this.file = directories.configFile("editor.properties");
    }

    public Indentation indentation() {
        try {
            Properties properties = PropertiesFile.load(file);
            int width = Integer.parseInt(properties.getProperty("indent.width", "2").strip());
            boolean tabs = Boolean.parseBoolean(properties.getProperty("indent.tabs", "false").strip());
            return new Indentation(width, tabs);
        } catch (UncheckedIOException | IllegalArgumentException e) {
            LOG.warn("Ignoring unreadable {}", file, e);
            return Indentation.DEFAULT;
        }
    }

    /** Stores the indentation; throws {@link UncheckedIOException} if the file cannot be written. */
    public void setIndentation(Indentation indentation) {
        Properties properties = PropertiesFile.load(file);
        properties.setProperty("indent.width", Integer.toString(indentation.width()));
        properties.setProperty("indent.tabs", Boolean.toString(indentation.tabs()));
        PropertiesFile.store(file, properties, "amade-pad editor settings");
    }
}
