package com.maez.qubitexplorer.services

import java.util.Locale

/** Tipos de token que el editor colorea. */
enum class TokenType { KEYWORD, TYPE, STRING, NUMBER, COMMENT, FUNCTION, TAG, ATTRIBUTE, PROPERTY, PUNCTUATION, META, VARIABLE }

data class TokenSpan(val start: Int, val end: Int, val type: TokenType)

/** Elemento del esquema del archivo (función, clase, selector, etiqueta...). */
data class OutlineItem(val label: String, val kind: String, val line: Int)

enum class CodeLanguage(val label: String) {
    PLAIN("Texto"),
    KOTLIN("Kotlin"), JAVA("Java"), JAVASCRIPT("JavaScript"), TYPESCRIPT("TypeScript"),
    C_CPP("C/C++"), CSHARP("C#"), GO("Go"), RUST("Rust"), SWIFT("Swift"), PHP("PHP"),
    PYTHON("Python"), RUBY("Ruby"),
    HTML("HTML"), XML("XML"), CSS("CSS"), JSON("JSON"), YAML("YAML"),
    SQL("SQL"), SHELL("Shell"), BATCH("Batch"), POWERSHELL("PowerShell"),
    MARKDOWN("Markdown"), INI("INI / Config");

    companion object {
        fun detect(fileName: String, content: String): CodeLanguage {
            val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            byExtension(ext)?.let { return it }
            // Detección por contenido cuando la extensión no alcanza
            val head = content.take(400).trimStart()
            val lowerHead = head.lowercase(Locale.ROOT)
            return when {
                lowerHead.startsWith("<!doctype html") || lowerHead.startsWith("<html") -> HTML
                lowerHead.startsWith("<?xml") || (head.startsWith("<") && head.contains("</")) -> XML
                head.startsWith("#!") && head.lineSequence().first().contains("python") -> PYTHON
                head.startsWith("#!") -> SHELL
                (head.startsWith("{") || head.startsWith("[")) && runCatching { looksLikeJson(content) }.getOrDefault(false) -> JSON
                Regex("""^\s*(def |class |import |from \S+ import )""", RegexOption.MULTILINE).containsMatchIn(head) -> PYTHON
                Regex("""^[.#]?[\w-]+\s*\{[^}]*:[^}]*;""").containsMatchIn(head) -> CSS
                Regex("""^\[[^\]]+\]\s*$""", RegexOption.MULTILINE).containsMatchIn(head) -> INI
                else -> PLAIN
            }
        }

        fun byExtension(ext: String): CodeLanguage? = when (ext) {
            "kt", "kts", "gradle" -> KOTLIN
            "java" -> JAVA
            "js", "mjs", "cjs", "jsx" -> JAVASCRIPT
            "ts", "tsx" -> TYPESCRIPT
            "c", "h", "cpp", "cc", "cxx", "hpp", "hh", "ino" -> C_CPP
            "cs" -> CSHARP
            "go" -> GO
            "rs" -> RUST
            "swift" -> SWIFT
            "php" -> PHP
            "py", "pyw", "pyi" -> PYTHON
            "rb" -> RUBY
            "html", "htm", "xhtml", "vue", "svelte" -> HTML
            "xml", "svg", "xsd", "xsl", "xslt", "plist", "csproj", "fxml", "rss", "kml", "gpx" -> XML
            "css", "scss", "sass", "less" -> CSS
            "json", "jsonc", "geojson", "webmanifest" -> JSON
            "yaml", "yml" -> YAML
            "sql" -> SQL
            "sh", "bash", "zsh", "fish", "profile", "bashrc" -> SHELL
            "bat", "cmd" -> BATCH
            "ps1", "psm1" -> POWERSHELL
            "md", "markdown" -> MARKDOWN
            "ini", "cfg", "conf", "properties", "toml", "env", "editorconfig", "gitconfig" -> INI
            "txt", "log", "csv", "tsv" -> PLAIN
            else -> null
        }

        private fun looksLikeJson(s: String): Boolean {
            val t = s.trim()
            return (t.startsWith("{") && t.endsWith("}")) || (t.startsWith("[") && t.endsWith("]"))
        }

        /** Extensiones que se abren en el editor integrado por defecto. */
        val EDITABLE_EXTENSIONS = setOf(
            "txt", "log", "csv", "tsv", "md", "markdown", "ini", "cfg", "conf", "properties", "toml", "env",
            "kt", "kts", "gradle", "java", "js", "mjs", "cjs", "jsx", "ts", "tsx", "c", "h", "cpp", "cc", "hpp",
            "cs", "go", "rs", "swift", "php", "py", "pyw", "rb", "html", "htm", "xhtml", "vue", "xml", "svg",
            "xsd", "xsl", "plist", "css", "scss", "sass", "less", "json", "jsonc", "yaml", "yml", "sql",
            "sh", "bash", "zsh", "bat", "cmd", "ps1", "srt", "vtt", "nfo", "gitignore", "editorconfig"
        )
    }

    val isMarkup: Boolean get() = this == HTML || this == XML

    /** Prefijo de comentario de línea, para "comentar/descomentar línea". */
    val lineComment: String?
        get() = when (this) {
            PYTHON, RUBY, YAML, SHELL, POWERSHELL, INI -> "#"
            SQL -> "--"
            BATCH -> "REM "
            HTML, XML, CSS, JSON, MARKDOWN, PLAIN -> null
            else -> "//"
        }
}

object SyntaxHighlighter {

    private data class Rule(val regex: Regex, val type: TokenType, val group: Int = 0)

    private fun kw(vararg words: String) = Regex("""\b(?:${words.joinToString("|")})\b""")

    private val ML = setOf(RegexOption.MULTILINE)
    private val DQ = Rule(Regex(""""(?:\\.|[^"\\\n])*""""), TokenType.STRING)
    private val SQ = Rule(Regex("""'(?:\\.|[^'\\\n])*'"""), TokenType.STRING)
    private val BT = Rule(Regex("""`(?:\\.|[^`\\])*`"""), TokenType.STRING)
    private val NUM = Rule(Regex("""\b(?:0[xX][0-9a-fA-F_]+|\d[\d_]*(?:\.\d+)?(?:[eE][+-]?\d+)?[fFdDlLuU]*)\b"""), TokenType.NUMBER)
    private val SLASH_COMMENT = Rule(Regex("""//[^\n]*"""), TokenType.COMMENT)
    private val BLOCK_COMMENT = Rule(Regex("""/\*[\s\S]*?(?:\*/|$)"""), TokenType.COMMENT)
    private val HASH_COMMENT = Rule(Regex("""#[^\n]*"""), TokenType.COMMENT)
    private val FUNC_CALL = Rule(Regex("""\b([A-Za-z_]\w*)\s*(?=\()"""), TokenType.FUNCTION, 1)
    private val CAP_TYPE = Rule(Regex("""\b[A-Z][A-Za-z0-9_]*\b"""), TokenType.TYPE)
    private val ANNOTATION = Rule(Regex("""@[A-Za-z_][\w.]*"""), TokenType.META)

    private fun cLike(keywords: Regex, extra: List<Rule> = emptyList()) =
        listOf(BLOCK_COMMENT, SLASH_COMMENT) + extra + listOf(
            DQ, SQ, Rule(keywords, TokenType.KEYWORD), ANNOTATION, NUM, FUNC_CALL, CAP_TYPE
        )

    private val rulesCache = HashMap<CodeLanguage, List<Rule>>()

    private fun rulesFor(lang: CodeLanguage): List<Rule> = rulesCache.getOrPut(lang) {
        when (lang) {
            CodeLanguage.KOTLIN -> cLike(
                kw("package", "import", "class", "interface", "object", "fun", "val", "var", "if", "else", "when", "for",
                    "while", "do", "return", "break", "continue", "try", "catch", "finally", "throw", "in", "is", "as",
                    "null", "true", "false", "this", "super", "private", "public", "protected", "internal", "open",
                    "override", "abstract", "data", "enum", "sealed", "companion", "suspend", "inline", "lateinit",
                    "const", "by", "lazy", "typealias", "init", "constructor", "annotation", "operator", "vararg", "reified"),
                listOf(Rule(Regex("\"\"\"[\\s\\S]*?(?:\"\"\"|$)"), TokenType.STRING))
            )
            CodeLanguage.JAVA, CodeLanguage.CSHARP -> cLike(
                kw("package", "import", "using", "namespace", "class", "interface", "enum", "struct", "record", "extends",
                    "implements", "new", "return", "if", "else", "switch", "case", "default", "for", "foreach", "while",
                    "do", "break", "continue", "try", "catch", "finally", "throw", "throws", "public", "private",
                    "protected", "internal", "static", "final", "readonly", "abstract", "virtual", "override", "void",
                    "int", "long", "short", "byte", "char", "float", "double", "boolean", "bool", "string", "var",
                    "null", "true", "false", "this", "base", "super", "async", "await", "synchronized", "volatile",
                    "const", "in", "out", "ref", "is", "as", "get", "set", "yield", "instanceof")
            )
            CodeLanguage.JAVASCRIPT, CodeLanguage.TYPESCRIPT -> cLike(
                kw("var", "let", "const", "function", "return", "if", "else", "for", "while", "do", "switch", "case",
                    "default", "break", "continue", "new", "delete", "typeof", "instanceof", "in", "of", "class",
                    "extends", "super", "this", "import", "export", "from", "as", "async", "await", "yield", "try",
                    "catch", "finally", "throw", "null", "undefined", "true", "false", "interface", "type", "enum",
                    "implements", "public", "private", "protected", "readonly", "static", "declare", "namespace",
                    "keyof", "any", "number", "string", "boolean", "void", "never", "unknown"),
                listOf(BT, Rule(Regex("""/(?![/*])(?:\\.|[^/\\\n])+/[gimsuy]*(?=\s*[,;).\]}]|$)"""), TokenType.STRING))
            )
            CodeLanguage.C_CPP -> listOf(Rule(Regex("""^\s*#\s*\w+[^\n]*""", ML), TokenType.META)) + cLike(
                kw("auto", "break", "case", "char", "const", "continue", "default", "do", "double", "else", "enum",
                    "extern", "float", "for", "goto", "if", "inline", "int", "long", "register", "return", "short",
                    "signed", "sizeof", "static", "struct", "switch", "typedef", "union", "unsigned", "void", "volatile",
                    "while", "class", "namespace", "template", "typename", "public", "private", "protected", "virtual",
                    "override", "new", "delete", "this", "nullptr", "true", "false", "bool", "using", "constexpr", "std")
            )
            CodeLanguage.GO -> cLike(
                kw("break", "case", "chan", "const", "continue", "default", "defer", "else", "fallthrough", "for",
                    "func", "go", "goto", "if", "import", "interface", "map", "package", "range", "return", "select",
                    "struct", "switch", "type", "var", "nil", "true", "false", "string", "int", "int64", "float64",
                    "bool", "byte", "error", "rune"), listOf(BT)
            )
            CodeLanguage.RUST -> cLike(
                kw("as", "async", "await", "break", "const", "continue", "crate", "else", "enum", "extern", "false",
                    "fn", "for", "if", "impl", "in", "let", "loop", "match", "mod", "move", "mut", "pub", "ref",
                    "return", "self", "Self", "static", "struct", "super", "trait", "true", "type", "unsafe", "use",
                    "where", "while", "dyn", "Some", "None", "Ok", "Err"),
                listOf(Rule(Regex("""\b\w+!"""), TokenType.META))
            )
            CodeLanguage.SWIFT -> cLike(
                kw("class", "struct", "enum", "protocol", "extension", "func", "var", "let", "if", "else", "guard",
                    "switch", "case", "default", "for", "in", "while", "repeat", "return", "break", "continue",
                    "import", "init", "self", "super", "nil", "true", "false", "public", "private", "internal",
                    "fileprivate", "open", "static", "override", "mutating", "throws", "throw", "try", "catch", "do",
                    "async", "await", "some", "any", "where", "as", "is")
            )
            CodeLanguage.PHP -> listOf(Rule(Regex("""<\?php|\?>"""), TokenType.META), HASH_COMMENT,
                Rule(Regex("""\$\w+"""), TokenType.VARIABLE)) + cLike(
                kw("function", "class", "interface", "trait", "extends", "implements", "public", "private",
                    "protected", "static", "abstract", "final", "return", "if", "else", "elseif", "foreach", "for",
                    "while", "do", "switch", "case", "default", "break", "continue", "new", "echo", "print", "require",
                    "require_once", "include", "include_once", "namespace", "use", "as", "try", "catch", "finally",
                    "throw", "null", "true", "false", "array", "fn", "match")
            )
            CodeLanguage.PYTHON -> listOf(
                Rule(Regex("(?:[rRbBuUfF]{0,2})\"\"\"[\\s\\S]*?(?:\"\"\"|$)"), TokenType.STRING),
                Rule(Regex("""(?:[rRbBuUfF]{0,2})'''[\s\S]*?(?:'''|$)"""), TokenType.STRING),
                HASH_COMMENT,
                Rule(Regex("""[rRbBuUfF]{0,2}"(?:\\.|[^"\\\n])*""""), TokenType.STRING),
                Rule(Regex("""[rRbBuUfF]{0,2}'(?:\\.|[^'\\\n])*'"""), TokenType.STRING),
                Rule(Regex("""^\s*@[\w.]+""", ML), TokenType.META),
                Rule(Regex("""\b(?:def|class)\s+([A-Za-z_]\w*)"""), TokenType.FUNCTION, 1),
                Rule(kw("False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "continue",
                    "def", "del", "elif", "else", "except", "finally", "for", "from", "global", "if", "import", "in",
                    "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with",
                    "yield", "match", "case"), TokenType.KEYWORD),
                Rule(kw("self", "cls", "print", "len", "range", "int", "str", "float", "list", "dict", "set",
                    "tuple", "bool", "open", "super", "isinstance", "enumerate", "zip", "map", "filter"), TokenType.TYPE),
                NUM, FUNC_CALL
            )
            CodeLanguage.RUBY -> listOf(HASH_COMMENT, DQ, SQ,
                Rule(Regex(""":\w+"""), TokenType.VARIABLE), Rule(Regex("""@{1,2}\w+"""), TokenType.VARIABLE),
                Rule(kw("def", "end", "class", "module", "if", "elsif", "else", "unless", "while", "until", "for",
                    "in", "do", "return", "yield", "begin", "rescue", "ensure", "raise", "require", "attr_accessor",
                    "self", "nil", "true", "false", "and", "or", "not", "then", "case", "when", "lambda", "puts"),
                    TokenType.KEYWORD), NUM, CAP_TYPE)
            CodeLanguage.HTML, CodeLanguage.XML -> listOf(
                Rule(Regex("""<!--[\s\S]*?(?:-->|$)"""), TokenType.COMMENT),
                Rule(Regex("""<!\[CDATA\[[\s\S]*?(?:]]>|$)"""), TokenType.STRING),
                Rule(Regex("""<[!?][^>]*>"""), TokenType.META),
                Rule(Regex("""(?<=<)/?[A-Za-z][\w:.-]*"""), TokenType.TAG),
                Rule(Regex("""</?|/?>"""), TokenType.PUNCTUATION),
                Rule(Regex("""(?<=[\s"'])[A-Za-z_:@][\w:.-]*(?=\s*=)"""), TokenType.ATTRIBUTE),
                Rule(Regex(""""[^"]*"|'[^']*'"""), TokenType.STRING),
                Rule(Regex("""&#?\w+;"""), TokenType.NUMBER)
            )
            CodeLanguage.CSS -> listOf(
                BLOCK_COMMENT, SLASH_COMMENT, DQ, SQ,
                Rule(Regex("""@[\w-]+"""), TokenType.KEYWORD),
                Rule(Regex("""#[0-9a-fA-F]{3,8}\b"""), TokenType.NUMBER),
                Rule(Regex("""(?<![\w-])-?\d*\.?\d+(?:px|em|rem|%|vh|vw|vmin|vmax|s|ms|deg|fr|pt|ch|ex)?\b"""), TokenType.NUMBER),
                Rule(Regex("""([\w-]+)\s*(?=:[^{}]*[;}])"""), TokenType.PROPERTY, 1),
                Rule(Regex("""!important"""), TokenType.KEYWORD),
                Rule(Regex("""\b([\w-]+)(?=\()"""), TokenType.FUNCTION, 1),
                Rule(Regex("""\$[\w-]+|--[\w-]+"""), TokenType.VARIABLE),
                Rule(Regex("""[.#][A-Za-z_-][\w-]*|::?[\w-]+|^[ \t]*[a-zA-Z][\w-]*(?=[^{};]*\{)""", ML), TokenType.TAG)
            )
            CodeLanguage.JSON -> listOf(
                SLASH_COMMENT, BLOCK_COMMENT,
                Rule(Regex(""""(?:\\.|[^"\\\n])*"(?=\s*:)"""), TokenType.PROPERTY),
                DQ, Rule(kw("true", "false", "null"), TokenType.KEYWORD),
                Rule(Regex("""-?\b\d+(?:\.\d+)?(?:[eE][+-]?\d+)?\b"""), TokenType.NUMBER),
                Rule(Regex("""[{}\[\]]"""), TokenType.PUNCTUATION)
            )
            CodeLanguage.YAML -> listOf(
                HASH_COMMENT, DQ, SQ,
                Rule(Regex("""^\s*-?\s*([\w.\-"' ]+?)(?=\s*:(?:\s|$))""", ML), TokenType.PROPERTY, 1),
                Rule(Regex("""^---$|^\.\.\.$""", ML), TokenType.META),
                Rule(Regex("""[&*][\w-]+"""), TokenType.VARIABLE),
                Rule(kw("true", "false", "null", "yes", "no", "on", "off"), TokenType.KEYWORD), NUM
            )
            CodeLanguage.SQL -> listOf(
                Rule(Regex("""--[^\n]*"""), TokenType.COMMENT), BLOCK_COMMENT, SQ, DQ,
                Rule(Regex("""(?i)\b(?:select|from|where|and|or|not|insert|into|values|update|set|delete|create|table|drop|alter|add|column|index|view|join|left|right|inner|outer|full|on|as|group|by|order|having|limit|offset|union|all|distinct|case|when|then|else|end|is|null|in|like|between|exists|primary|key|foreign|references|default|unique|constraint|begin|commit|rollback|transaction|with|returning|asc|desc|if|trigger|procedure|function|database|schema)\b"""), TokenType.KEYWORD),
                Rule(Regex("""(?i)\b(?:int|integer|bigint|smallint|varchar|char|text|date|datetime|timestamp|boolean|bool|float|real|double|decimal|numeric|blob|serial|uuid|json)\b"""), TokenType.TYPE),
                NUM, FUNC_CALL
            )
            CodeLanguage.SHELL -> listOf(
                Rule(Regex("""^#![^\n]*"""), TokenType.META), HASH_COMMENT, DQ, SQ, BT,
                Rule(Regex("""\$\{[^}]*}|\$\w+|\$[@#?$!*0-9]"""), TokenType.VARIABLE),
                Rule(kw("if", "then", "else", "elif", "fi", "for", "while", "until", "do", "done", "case", "esac",
                    "in", "function", "return", "exit", "export", "local", "readonly", "source", "echo", "cd",
                    "set", "unset", "shift", "true", "false", "sudo"), TokenType.KEYWORD), NUM
            )
            CodeLanguage.BATCH -> listOf(
                Rule(Regex("""(?im)^\s*(?:rem\b|::)[^\n]*"""), TokenType.COMMENT), DQ,
                Rule(Regex("""%~?\w+%?|!\w+!"""), TokenType.VARIABLE),
                Rule(Regex("""(?im)^\s*:\w+"""), TokenType.META),
                Rule(Regex("""(?i)\b(?:echo|set|if|else|for|in|do|goto|call|exit|not|exist|errorlevel|setlocal|endlocal|pause|cd|copy|del|move|mkdir|rmdir|start|shift)\b"""), TokenType.KEYWORD), NUM
            )
            CodeLanguage.POWERSHELL -> listOf(
                Rule(Regex("""<#[\s\S]*?(?:#>|$)"""), TokenType.COMMENT), HASH_COMMENT, DQ, SQ,
                Rule(Regex("""\$[\w:]+"""), TokenType.VARIABLE),
                Rule(Regex("""\b[A-Z][a-z]+-[A-Z]\w+\b"""), TokenType.FUNCTION),
                Rule(Regex("""(?i)\b(?:function|param|if|elseif|else|foreach|for|while|do|switch|return|break|continue|try|catch|finally|throw|in|begin|process|end)\b"""), TokenType.KEYWORD),
                Rule(Regex("""-[A-Za-z]+\b"""), TokenType.ATTRIBUTE), NUM
            )
            CodeLanguage.MARKDOWN -> listOf(
                Rule(Regex("""```[\s\S]*?(?:```|$)"""), TokenType.STRING),
                Rule(Regex("""`[^`\n]+`"""), TokenType.STRING),
                Rule(Regex("""^#{1,6} [^\n]*""", ML), TokenType.KEYWORD),
                Rule(Regex("""^\s*(?:[-*+]|\d+\.)\s""", ML), TokenType.PUNCTUATION),
                Rule(Regex("""^>[^\n]*""", ML), TokenType.COMMENT),
                Rule(Regex("""\*\*[^*\n]+\*\*|__[^_\n]+__"""), TokenType.TYPE),
                Rule(Regex("""!?\[[^\]\n]*]\([^)\n]*\)"""), TokenType.FUNCTION)
            )
            CodeLanguage.INI -> listOf(
                Rule(Regex("""^\s*[;#][^\n]*""", ML), TokenType.COMMENT),
                Rule(Regex("""^\s*\[[^\]\n]+]""", ML), TokenType.KEYWORD),
                Rule(Regex("""^\s*([\w.\-]+)\s*(?==|:)""", ML), TokenType.PROPERTY, 1),
                DQ, SQ, Rule(kw("true", "false", "yes", "no", "on", "off"), TokenType.KEYWORD), NUM
            )
            CodeLanguage.PLAIN -> emptyList()
        }
    }

    /** Límite para colorear: archivos más grandes se muestran sin resaltado para mantener fluidez. */
    const val MAX_HIGHLIGHT_CHARS = 200_000

    fun highlight(text: String, lang: CodeLanguage): List<TokenSpan> {
        if (lang == CodeLanguage.PLAIN || text.length > MAX_HIGHLIGHT_CHARS) return emptyList()
        val owner = BooleanArray(text.length)
        val spans = ArrayList<TokenSpan>()
        for (rule in rulesFor(lang)) {
            for (m in rule.regex.findAll(text)) {
                val g = if (rule.group == 0) m.range else (m.groups[rule.group]?.range ?: continue)
                val s = g.first
                val e = g.last + 1
                if (e <= s) continue
                var free = true
                for (k in s until e) if (owner[k]) { free = false; break }
                if (!free) continue
                for (k in s until e) owner[k] = true
                spans += TokenSpan(s, e, rule.type)
            }
        }
        return spans
    }

    // ── Esquema (funciones, clases, selectores, etiquetas…) ──────────────────────────────

    private data class OutlineRule(val regex: Regex, val kind: String, val group: Int = 1)

    private fun outlineRules(lang: CodeLanguage): List<OutlineRule> = when (lang) {
        CodeLanguage.KOTLIN -> listOf(
            OutlineRule(Regex("""^\s*(?:[\w@]+\s+)*(?:class|interface|object)\s+(\w+)""", ML), "clase"),
            OutlineRule(Regex("""^\s*(?:[\w@]+\s+)*fun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)\s*\(""", ML), "función")
        )
        CodeLanguage.JAVA, CodeLanguage.CSHARP -> listOf(
            OutlineRule(Regex("""^\s*(?:\w+\s+)*(?:class|interface|enum|record|struct)\s+(\w+)""", ML), "clase"),
            OutlineRule(Regex("""^\s*(?:(?:public|private|protected|internal|static|final|abstract|virtual|override|async|synchronized)\s+)+[\w<>\[\],? ]+\s+(\w+)\s*\([^;]*$""", ML), "método")
        )
        CodeLanguage.JAVASCRIPT, CodeLanguage.TYPESCRIPT -> listOf(
            OutlineRule(Regex("""^\s*(?:export\s+)?(?:default\s+)?class\s+(\w+)""", ML), "clase"),
            OutlineRule(Regex("""^\s*(?:export\s+)?(?:async\s+)?function\s*\*?\s*(\w+)""", ML), "función"),
            OutlineRule(Regex("""^\s*(?:export\s+)?(?:const|let|var)\s+(\w+)\s*=\s*(?:async\s*)?(?:\([^)]*\)|\w+)\s*=>""", ML), "función"),
            OutlineRule(Regex("""^\s*(?:interface|type)\s+(\w+)""", ML), "tipo")
        )
        CodeLanguage.C_CPP -> listOf(
            OutlineRule(Regex("""^\s*(?:class|struct)\s+(\w+)\s*[:{]""", ML), "clase"),
            OutlineRule(Regex("""^[\w:<>*&\s]+?\b(\w+)\s*\([^;{]*\)\s*(?:const\s*)?\{""", ML), "función")
        )
        CodeLanguage.GO -> listOf(
            OutlineRule(Regex("""^func\s+(?:\([^)]*\)\s*)?(\w+)""", ML), "función"),
            OutlineRule(Regex("""^type\s+(\w+)""", ML), "tipo")
        )
        CodeLanguage.RUST -> listOf(
            OutlineRule(Regex("""^\s*(?:pub(?:\([^)]*\))?\s+)?(?:async\s+)?fn\s+(\w+)""", ML), "función"),
            OutlineRule(Regex("""^\s*(?:pub\s+)?(?:struct|enum|trait|impl(?:<[^>]*>)?)\s+(\w+)""", ML), "tipo")
        )
        CodeLanguage.SWIFT -> listOf(
            OutlineRule(Regex("""^\s*(?:\w+\s+)*(?:class|struct|enum|protocol|extension)\s+(\w+)""", ML), "tipo"),
            OutlineRule(Regex("""^\s*(?:\w+\s+)*func\s+(\w+)""", ML), "función")
        )
        CodeLanguage.PHP -> listOf(
            OutlineRule(Regex("""^\s*(?:\w+\s+)*(?:class|interface|trait)\s+(\w+)""", ML), "clase"),
            OutlineRule(Regex("""^\s*(?:\w+\s+)*function\s+(\w+)""", ML), "función")
        )
        CodeLanguage.PYTHON -> listOf(
            OutlineRule(Regex("""^(\s*class\s+\w+)""", ML), "clase"),
            OutlineRule(Regex("""^(\s*(?:async\s+)?def\s+\w+)""", ML), "función")
        )
        CodeLanguage.RUBY -> listOf(
            OutlineRule(Regex("""^\s*(?:class|module)\s+([\w:]+)""", ML), "clase"),
            OutlineRule(Regex("""^\s*def\s+([\w.?!]+)""", ML), "método")
        )
        CodeLanguage.CSS -> listOf(
            OutlineRule(Regex("""^\s*(@(?:media|supports|font-face|keyframes|import|layer|container)[^{;\n]*)""", ML), "regla"),
            OutlineRule(Regex("""^\s*([^@\s{}/;][^{};/]*?)\s*\{""", ML), "selector")
        )
        CodeLanguage.HTML -> listOf(
            OutlineRule(Regex("""<(h[1-6])[^>]*>([^<]{1,80})""", setOf(RegexOption.IGNORE_CASE)), "título", 2),
            OutlineRule(Regex("""<((?:head|body|header|nav|main|section|article|aside|footer|form|table|script|style)\b[^>]*?(?:id="[^"]*")?)[^>]*>""", setOf(RegexOption.IGNORE_CASE)), "etiqueta"),
            OutlineRule(Regex("""<\w+[^>]*\bid="([^"]+)"""", setOf(RegexOption.IGNORE_CASE)), "#id")
        )
        CodeLanguage.XML -> listOf(
            OutlineRule(Regex("""^(?: {0,4}|\t?)<([A-Za-z][\w:.-]*)(?:\s[^>]*?(?:(?:name|id|android:id)="([^"]*)")?)?[^>]*>""", ML), "elemento")
        )
        CodeLanguage.JSON -> listOf(OutlineRule(Regex("""^ {0,4}\t?"([^"]+)"\s*:""", ML), "clave"))
        CodeLanguage.YAML -> listOf(OutlineRule(Regex("""^([\w.\-]+)\s*:""", ML), "clave"))
        CodeLanguage.SQL -> listOf(
            OutlineRule(Regex("""(?i)^\s*create\s+(?:or\s+replace\s+)?(?:table|view|index|function|procedure|trigger)\s+(?:if\s+not\s+exists\s+)?([\w."`\[\]]+)""", ML), "objeto")
        )
        CodeLanguage.SHELL -> listOf(OutlineRule(Regex("""^\s*(?:function\s+)?(\w+)\s*\(\)\s*\{?""", ML), "función"))
        CodeLanguage.BATCH -> listOf(OutlineRule(Regex("""^:(\w+)""", ML), "etiqueta"))
        CodeLanguage.POWERSHELL -> listOf(OutlineRule(Regex("""(?i)^\s*function\s+([\w-]+)""", ML), "función"))
        CodeLanguage.MARKDOWN -> listOf(OutlineRule(Regex("""^(#{1,6} .+)$""", ML), "título"))
        CodeLanguage.INI -> listOf(OutlineRule(Regex("""^\s*\[([^\]]+)]""", ML), "sección"))
        CodeLanguage.PLAIN -> emptyList()
    }

    fun outline(text: String, lang: CodeLanguage, max: Int = 400): List<OutlineItem> {
        if (text.length > 2_000_000) return emptyList()
        // Índice de inicio de cada línea para convertir offset → número de línea
        val lineStarts = ArrayList<Int>().apply {
            add(0)
            text.forEachIndexed { i, c -> if (c == '\n') add(i + 1) }
        }
        fun lineOf(offset: Int): Int {
            var lo = 0; var hi = lineStarts.size - 1
            while (lo < hi) {
                val mid = (lo + hi + 1) / 2
                if (lineStarts[mid] <= offset) lo = mid else hi = mid - 1
            }
            return lo
        }
        val items = ArrayList<OutlineItem>()
        for (rule in outlineRules(lang)) {
            for (m in rule.regex.findAll(text)) {
                var label = (m.groups[rule.group]?.value ?: m.value).trim()
                if (lang == CodeLanguage.XML && m.groups[2]?.value?.isNotBlank() == true) label += " · ${m.groups[2]!!.value}"
                if (lang == CodeLanguage.HTML && rule.kind == "etiqueta") label = "<" + label.substringBefore(' ').lowercase(Locale.ROOT) + ">"
                if (label.isBlank() || label.length > 90) continue
                if (rule.kind == "función" && label in setOf("if", "for", "while", "switch", "catch", "return")) continue
                items += OutlineItem(label, rule.kind, lineOf(m.range.first + (m.value.length - m.value.trimStart().length)))
                if (items.size >= max) break
            }
        }
        return items.sortedBy { it.line }.distinctBy { it.line to it.label }
    }
}
