package com.maez.qubitexplorer.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyntaxHighlighterTest {

    @Test
    fun detect_porExtension() {
        assertEquals(CodeLanguage.KOTLIN, CodeLanguage.detect("Main.kt", ""))
        assertEquals(CodeLanguage.PYTHON, CodeLanguage.detect("script.PY", ""))
        assertEquals(CodeLanguage.JSON, CodeLanguage.detect("package.json", ""))
    }

    @Test
    fun detect_porContenido() {
        assertEquals(CodeLanguage.HTML, CodeLanguage.detect("x", "<!DOCTYPE html><html></html>"))
        assertEquals(CodeLanguage.PYTHON, CodeLanguage.detect("run", "#!/usr/bin/env python3\nprint(1)"))
        assertEquals(CodeLanguage.SHELL, CodeLanguage.detect("run", "#!/bin/bash\necho hi"))
        assertEquals(CodeLanguage.PLAIN, CodeLanguage.detect("notas", "solo texto normal"))
    }

    @Test
    fun highlight_marcaTokensSinSolapes() {
        val code = "val x = \"hola\" // comentario\nfun f() = 42"
        val spans = SyntaxHighlighter.highlight(code, CodeLanguage.KOTLIN)
        fun typeAt(word: String) = spans.firstOrNull { code.substring(it.start, it.end).contains(word) }?.type
        assertEquals(TokenType.STRING, typeAt("hola"))
        assertEquals(TokenType.COMMENT, typeAt("comentario"))
        assertEquals(TokenType.NUMBER, typeAt("42"))
        val sorted = spans.sortedBy { it.start }
        sorted.zipWithNext().forEach { (a, b) -> assertTrue("solape $a / $b", a.end <= b.start) }
    }

    @Test
    fun highlight_textoPlanoOEnormeNoSeColorea() {
        assertTrue(SyntaxHighlighter.highlight("val x = 1", CodeLanguage.PLAIN).isEmpty())
        val huge = "a".repeat(SyntaxHighlighter.MAX_HIGHLIGHT_CHARS + 1)
        assertTrue(SyntaxHighlighter.highlight(huge, CodeLanguage.KOTLIN).isEmpty())
    }

    @Test
    fun outline_encuentraTitulosMarkdownConSuLinea() {
        val md = "# Uno\ntexto\n## Dos\n"
        val items = SyntaxHighlighter.outline(md, CodeLanguage.MARKDOWN)
        assertEquals(listOf(0, 2), items.map { it.line })
        assertEquals("# Uno", items.first().label)
    }
}
