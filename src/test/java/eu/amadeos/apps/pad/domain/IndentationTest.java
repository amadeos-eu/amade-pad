package eu.amadeos.apps.pad.domain;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IndentationTest {
    @Test
    void detectsSpacesTabsAndNothing() {
        String twoSpaces = "a:\n  b:\n    c: 1\n  d: 2\n";
        assertEquals(Optional.of(new Indentation(2, false)), Indentation.detect(twoSpaces, 8));
        String fourSpacesWithJavadoc = "class A {\n    /**\n     * Doc.\n     */\n    void m() {\n        x();\n    }\n}\n";
        assertEquals(Optional.of(new Indentation(4, false)), Indentation.detect(fourSpacesWithJavadoc, 2));
        assertEquals(Optional.of(new Indentation(8, true)), Indentation.detect("all:\n\tcc main.c\n\tstrip a.out\n", 8));
        assertEquals(Optional.empty(), Indentation.detect("one\ntwo\n", 2));
    }

    @Test
    void documentUsesItsOwnThenTheLanguageThenTheDefault() {
        Indentation defaults = Indentation.DEFAULT;
        assertEquals(new Indentation(4, false),
                Indentation.forDocument("a {\n    b\n}", SyntaxLanguage.JAVA, defaults));
        assertEquals(new Indentation(2, true), Indentation.forDocument("all: x\n", SyntaxLanguage.MAKEFILE, defaults));
        assertEquals(defaults, Indentation.forDocument("", SyntaxLanguage.JAVA, defaults));
    }

    @Test
    void unitAndLimits() {
        assertEquals("  ", Indentation.DEFAULT.unit());
        assertEquals("\t", new Indentation(4, true).unit());
        assertThrows(IllegalArgumentException.class, () -> new Indentation(0, false));
    }
}
