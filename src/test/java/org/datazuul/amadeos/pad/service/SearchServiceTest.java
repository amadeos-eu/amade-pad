package org.datazuul.amadeos.pad.service;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SearchServiceTest {
    private final SearchService service = new SearchService();

    @Test
    void respectsCaseCaretAndWrap() {
        assertEquals(4, service.find("one One one", "one", 3, false, false));
        assertEquals(8, service.find("one One one", "one", 3, true, false));
        assertEquals(0, service.find("one One one", "one", 11, true, true));
        assertEquals(-1, service.find("one", "one", 3, true, false));
        assertEquals(-1, service.find("one", "", 0, false, true));
    }

    @Test
    void caseInsensitiveSearchKeepsUnicodeOffsetsAndIgnoresDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertEquals(2, service.find("\u0130 INDEX", "index", 0, false, false));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void replacesLiteralNonOverlappingMatches() {
        assertEquals(new SearchService.Replacement("$1 $1", 2), service.replaceAll("one ONE", "one", "$1", false));
        assertEquals(new SearchService.Replacement("bb", 2), service.replaceAll("aaaa", "aa", "b", true));
        assertEquals(new SearchService.Replacement("text", 0), service.replaceAll("text", "", "x", false));
        assertTrue(service.matches("ONE", "one", false));
        assertFalse(service.matches("ONE", "one", true));
    }
}
