package eu.amadeos.apps.pad.domain;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyntaxLanguageTest {
    @Test
    void recognisesExtensionsAndFileNames() {
        assertEquals(SyntaxLanguage.JAVA, SyntaxLanguage.detect(Path.of("/src/Main.java"), ""));
        assertEquals(SyntaxLanguage.YAML, SyntaxLanguage.detect(Path.of("ci.YML"), ""));
        assertEquals(SyntaxLanguage.MAKEFILE, SyntaxLanguage.detect(Path.of("Makefile"), ""));
        assertEquals(SyntaxLanguage.DOCKERFILE, SyntaxLanguage.detect(Path.of("Dockerfile.dev"), ""));
        assertEquals(SyntaxLanguage.SHELL, SyntaxLanguage.detect(Path.of("/home/alex/.bashrc"), ""));
        assertEquals(SyntaxLanguage.GROOVY, SyntaxLanguage.detect(Path.of("build.gradle"), ""));
        assertEquals(SyntaxLanguage.PLAIN, SyntaxLanguage.detect(Path.of("notes.txt"), "#!/bin/sh"));
    }

    @Test
    void recognisesShebangAndXmlWithoutExtension() {
        assertEquals(SyntaxLanguage.PYTHON, SyntaxLanguage.detect(Path.of("tool"), "#!/usr/bin/env python3\nprint()"));
        assertEquals(SyntaxLanguage.SHELL, SyntaxLanguage.detect(Path.of("run"), "#!/bin/bash -e\n"));
        assertEquals(SyntaxLanguage.PERL, SyntaxLanguage.detect(null, "#!/usr/bin/env -S perl -w\n"));
        assertEquals(SyntaxLanguage.XML, SyntaxLanguage.detect(Path.of("data"), "<?xml version=\"1.0\"?><a/>"));
        assertEquals(SyntaxLanguage.PLAIN, SyntaxLanguage.detect(Path.of("README"), "Hello"));
        assertEquals(SyntaxLanguage.PLAIN, SyntaxLanguage.detect(null, null));
    }

    @Test
    void languagesHaveTheirOwnIndentationRules() {
        Indentation defaults = new Indentation(2, false);
        assertEquals(new Indentation(2, true), SyntaxLanguage.MAKEFILE.indentation(defaults));
        assertEquals(new Indentation(4, false), SyntaxLanguage.YAML.indentation(new Indentation(4, true)));
        assertEquals(defaults, SyntaxLanguage.JAVA.indentation(defaults));
    }
}
