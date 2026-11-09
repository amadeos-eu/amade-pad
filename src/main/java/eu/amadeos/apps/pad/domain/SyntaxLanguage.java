package eu.amadeos.apps.pad.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The languages Pad colours. Each has the media type the highlighter knows it by, the file name
 * extensions and names it is recognised by, and the interpreters of a {@code #!} first line.
 */
public enum SyntaxLanguage {
    // --8<-- [start:languages]
    PLAIN("", "text/plain", List.of("txt", "text", "log")),
    ASSEMBLER("Assembler (x86)", "text/asm", List.of("asm", "s")),
    BATCH("Batch", "text/bat", List.of("bat", "cmd")),
    C("C", "text/c", List.of("c", "h")),
    CPP("C++", "text/cpp", List.of("cpp", "cc", "cxx", "hpp", "hh", "hxx")),
    CSHARP("C#", "text/cs", List.of("cs")),
    CSS("CSS", "text/css", List.of("css")),
    CSV("CSV", "text/csv", List.of("csv")),
    DART("Dart", "text/dart", List.of("dart")),
    DOCKERFILE("Dockerfile", "text/dockerfile", List.of("dockerfile"), Set.of("Dockerfile", "Containerfile")),
    GO("Go", "text/golang", List.of("go")),
    GROOVY("Groovy / Gradle", "text/groovy", List.of("groovy", "gradle", "gvy")),
    HTML("HTML", "text/html", List.of("html", "htm", "xhtml")),
    INI("INI", "text/ini", List.of("ini", "conf", "cfg", "desktop", "service", "toml")),
    JAVA("Java", "text/java", List.of("java")),
    JAVASCRIPT("JavaScript", "text/javascript", List.of("js", "mjs", "cjs")),
    JSON("JSON", "text/json", List.of("json", "geojson", "webmanifest")),
    KOTLIN("Kotlin", "text/kotlin", List.of("kt", "kts")),
    LATEX("LaTeX", "text/latex", List.of("tex", "sty", "cls")),
    LUA("Lua", "text/lua", List.of("lua")),
    MAKEFILE("Makefile", "text/makefile", List.of("mk", "mak"), Set.of("Makefile", "makefile", "GNUmakefile")),
    MARKDOWN("Markdown", "text/markdown", List.of("md", "markdown")),
    PERL("Perl", "text/perl", List.of("pl", "pm")),
    PHP("PHP", "text/php", List.of("php")),
    PROPERTIES("Properties", "text/properties", List.of("properties")),
    PYTHON("Python", "text/python", List.of("py", "pyw")),
    RUBY("Ruby", "text/ruby", List.of("rb"), Set.of("Rakefile", "Gemfile")),
    RUST("Rust", "text/rust", List.of("rs")),
    SCALA("Scala", "text/scala", List.of("scala", "sc")),
    SHELL("Shell", "text/unix", List.of("sh", "bash", "zsh", "ksh"),
            Set.of(".bashrc", ".profile", ".bash_profile", ".bash_aliases", ".zshrc")),
    SQL("SQL", "text/sql", List.of("sql")),
    TCL("Tcl", "text/tcl", List.of("tcl")),
    TYPESCRIPT("TypeScript", "text/typescript", List.of("ts", "tsx", "mts")),
    XML("XML", "text/xml", List.of("xml", "xsd", "xsl", "xslt", "svg", "pom", "fxml", "metainfo", "plist")),
    YAML("YAML", "text/yaml", List.of("yaml", "yml"));
    // --8<-- [end:languages]

    private final String label;
    private final String mediaType;
    private final List<String> extensions;
    private final Set<String> fileNames;

    SyntaxLanguage(String label, String mediaType, List<String> extensions) {
        this(label, mediaType, extensions, Set.of());
    }

    SyntaxLanguage(String label, String mediaType, List<String> extensions, Set<String> fileNames) {
        this.label = label;
        this.mediaType = mediaType;
        this.extensions = extensions;
        this.fileNames = fileNames;
    }

    /** The name in menus and the status bar; empty for plain text, whose name is translated. */
    public String label() {
        return label;
    }

    /** The highlighter's name for the language, e.g. {@code text/java}. */
    public String mediaType() {
        return mediaType;
    }

    /**
     * The indentation a new or unindented document of this language starts with: Makefiles and Go
     * use tabs, YAML must not.
     */
    public Indentation indentation(Indentation defaults) {
        return switch (this) {
            case MAKEFILE, GO -> defaults.withTabs(true);
            case YAML -> defaults.withTabs(false);
            default -> defaults;
        };
    }

    // --8<-- [start:detect]
    /**
     * Recognises the language of a file: by its exact name (Makefile, Dockerfile), its extension, a
     * {@code #!} first line or an XML declaration. Plain text if nothing fits.
     */
    public static SyntaxLanguage detect(Path path, String text) {
        if (path != null && path.getFileName() != null) {
            String name = path.getFileName().toString();
            for (SyntaxLanguage language : values()) {
                if (language.fileNames.contains(name)) return language;
            }
            String lower = name.toLowerCase(Locale.ROOT);
            if (lower.startsWith("dockerfile")) return DOCKERFILE;
            int dot = lower.lastIndexOf('.');
            if (dot >= 0) {
                String extension = lower.substring(dot + 1);
                for (SyntaxLanguage language : values()) {
                    if (language.extensions.contains(extension)) return language;
                }
            }
        }
        return fromContent(text);
    }
    // --8<-- [end:detect]

    private static SyntaxLanguage fromContent(String text) {
        if (text == null) return PLAIN;
        int end = text.indexOf('\n');
        String first = (end < 0 ? text : text.substring(0, end)).strip();
        if (first.startsWith("<?xml")) return XML;
        if (first.regionMatches(true, 0, "<!doctype html", 0, 14)) return HTML;
        if (!first.startsWith("#!")) return PLAIN;
        String command = first.substring(2).strip();
        String[] words = command.split("\\s+");
        String program = words[0].substring(words[0].lastIndexOf('/') + 1);
        if (program.equals("env") && words.length > 1) {
            program = words[words.length > 2 && words[1].equals("-S") ? 2 : 1];
        }
        if (program.matches("(ba|z|k|da)?sh")) return SHELL;
        if (program.startsWith("python")) return PYTHON;
        if (program.startsWith("perl")) return PERL;
        if (program.startsWith("ruby")) return RUBY;
        if (program.equals("node")) return JAVASCRIPT;
        if (program.startsWith("php")) return PHP;
        if (program.startsWith("lua")) return LUA;
        if (program.equals("groovy")) return GROOVY;
        if (program.equals("tclsh") || program.equals("wish")) return TCL;
        if (program.equals("make")) return MAKEFILE;
        return PLAIN;
    }
}
