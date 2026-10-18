package eu.amadeos.pad.ui.main;

import java.util.Locale;
import java.util.Map;
import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.TranslationCheck;
import eu.amadeos.pad.bootstrap.PadModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslationTest {
    @Test
    void everyLanguageHasEveryText() {
        assertEquals(Map.of(), TranslationCheck.problems(PadModule.class, PadModule.MESSAGES));
    }

    @Test
    void textsAreFormattedInTheLanguage() {
        Messages german = Messages.load(PadModule.class, PadModule.MESSAGES, Locale.GERMANY);
        assertEquals("Zeile: 12, Spalte: 3 | UTF-8 | LF | 1.234 Zeichen",
                german.format("status.position", 12, 3, "UTF-8 | LF", 1234));
        assertEquals("1 Vorkommen ersetzt.", german.format("search.replaced", 1));
        Messages french = Messages.load(PadModule.class, PadModule.MESSAGES, Locale.FRANCE);
        assertEquals("Fermer l'onglet", french.get("menu.file.close-tab"));
        assertEquals("Texte introuvable : « l'eau »", french.format("search.not-found", "l'eau"));
        Messages english = Messages.load(PadModule.class, PadModule.MESSAGES, Locale.ENGLISH);
        assertEquals("Replaced 3 occurrences.", english.format("search.replaced", 3));
    }
}
