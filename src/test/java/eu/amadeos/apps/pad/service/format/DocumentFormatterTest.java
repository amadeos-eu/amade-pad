package eu.amadeos.apps.pad.service.format;

import eu.amadeos.apps.pad.domain.Indentation;
import eu.amadeos.apps.pad.domain.SyntaxLanguage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentFormatterTest {
    private final DocumentFormatter formatter = new DocumentFormatter();
    private final Indentation two = Indentation.DEFAULT;

    @Test
    void jsonKeepsTheSpellingOfValues() throws Exception {
        String json = "{\"name\":\"a \\\"b\\\" \\u00e9\",\"n\":1.50e3,\"list\":[true,null,[]],\"empty\":{}}";
        assertEquals("""
                {
                  "name": "a \\"b\\" \\u00e9",
                  "n": 1.50e3,
                  "list": [
                    true,
                    null,
                    []
                  ],
                  "empty": {}
                }
                """, formatter.format(SyntaxLanguage.JSON, json, two));
        assertEquals("[\n\t1\n]\n", formatter.format(SyntaxLanguage.JSON, "[ 1 ]", new Indentation(4, true)));
    }

    @Test
    void brokenJsonNamesTheLine() {
        FormatException e = assertThrows(FormatException.class,
                () -> formatter.format(SyntaxLanguage.JSON, "{\n  \"a\": 1,\n  \"b\" 2\n}", two));
        assertEquals(3, e.line());
        assertEquals("':' expected", e.getMessage());
        assertThrows(FormatException.class, () -> formatter.format(SyntaxLanguage.JSON, "{} x", two));
        assertThrows(FormatException.class, () -> formatter.format(SyntaxLanguage.JSON, "[01]", two));
    }

    @Test
    void xmlIndentsElementsAndKeepsMixedContent() throws Exception {
        String xml = "<?xml version=\"1.0\"?>\n<!-- top -->\n<book id=\"a>b\"><title>Hello <b>you</b></title>"
                + "<empty/><none>  </none>\n\n<code xml:space=\"preserve\">\n  keep\n</code><data><![CDATA[x]]></data></book>";
        assertEquals("""
                <?xml version="1.0"?>
                <!-- top -->
                <book id="a>b">
                  <title>Hello <b>you</b></title>
                  <empty/>
                  <none></none>

                  <code xml:space="preserve">
                  keep
                </code>
                  <data><![CDATA[x]]></data>
                </book>
                """, formatter.format(SyntaxLanguage.XML, xml, two));
    }

    @Test
    void xmlWithDoctypeAndNestedElements() throws Exception {
        String xml = "<!DOCTYPE note [<!ENTITY who \"Ann\">]><note><to>&who;</to><list><i>1</i><i>2</i></list></note>";
        assertEquals("""
                <!DOCTYPE note [<!ENTITY who "Ann">]>
                <note>
                  <to>&who;</to>
                  <list>
                    <i>1</i>
                    <i>2</i>
                  </list>
                </note>
                """, formatter.format(SyntaxLanguage.XML, xml, two));
    }

    @Test
    void brokenXmlIsRefusedAndExternalFilesAreNotRead() throws Exception {
        FormatException e = assertThrows(FormatException.class,
                () -> formatter.format(SyntaxLanguage.XML, "<a>\n<b>\n</a>", two));
        assertEquals(3, e.line());
        String external = "<!DOCTYPE a [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><a>&x;</a>";
        assertEquals("<!DOCTYPE a [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>\n<a>&x;</a>\n",
                formatter.format(SyntaxLanguage.XML, external, two));
    }

    @Test
    void onlyJsonAndXml() {
        assertTrue(formatter.supports(SyntaxLanguage.JSON));
        assertTrue(formatter.supports(SyntaxLanguage.XML));
        assertFalse(formatter.supports(SyntaxLanguage.JAVA));
    }
}
