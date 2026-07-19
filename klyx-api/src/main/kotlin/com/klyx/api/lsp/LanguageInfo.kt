package com.klyx.api.lsp

import com.klyx.api.data.file.KxFile
import com.klyx.api.language.LanguageRegistry

data class LanguageInfo(val id: String, val displayName: String)

private val extensionLanguageMap: Map<String, LanguageInfo> = mapOf(
    // ABAP
    "abap" to LanguageInfo("abap", "ABAP"),

    // Windows Bat
    "bat" to LanguageInfo("bat", "Windows Bat"),
    "cmd" to LanguageInfo("bat", "Windows Bat"),

    // BibTeX
    "bib" to LanguageInfo("bibtex", "BibTeX"),

    // Clojure
    "clj" to LanguageInfo("clojure", "Clojure"),
    "cljs" to LanguageInfo("clojure", "Clojure"),
    "cljc" to LanguageInfo("clojure", "Clojure"),
    "edn" to LanguageInfo("clojure", "Clojure"),

    // CoffeeScript
    "coffee" to LanguageInfo("coffeescript", "Coffeescript"),

    // C
    "c" to LanguageInfo("c", "C"),
    "h" to LanguageInfo("c", "C"),

    // C++
    "cpp" to LanguageInfo("cpp", "C++"),
    "cc" to LanguageInfo("cpp", "C++"),
    "cxx" to LanguageInfo("cpp", "C++"),
    "c++" to LanguageInfo("cpp", "C++"),
    "hpp" to LanguageInfo("cpp", "C++"),
    "hh" to LanguageInfo("cpp", "C++"),
    "hxx" to LanguageInfo("cpp", "C++"),
    "h++" to LanguageInfo("cpp", "C++"),
    "inl" to LanguageInfo("cpp", "C++"),
    "ipp" to LanguageInfo("cpp", "C++"),

    // C#
    "cs" to LanguageInfo("csharp", "C#"),
    "csx" to LanguageInfo("csharp", "C#"),

    // CSS
    "css" to LanguageInfo("css", "CSS"),

    // D
    "d" to LanguageInfo("d", "D"),
    "di" to LanguageInfo("d", "D"),

    // Delphi / Pascal
    "pas" to LanguageInfo("pascal", "Delphi"),
    "pp" to LanguageInfo("pascal", "Delphi"),
    "dpr" to LanguageInfo("pascal", "Delphi"),
    "dpk" to LanguageInfo("pascal", "Delphi"),

    // Diff
    "diff" to LanguageInfo("diff", "Diff"),
    "patch" to LanguageInfo("diff", "Diff"),

    // Dart
    "dart" to LanguageInfo("dart", "Dart"),

    // Elixir
    "ex" to LanguageInfo("elixir", "Elixir"),
    "exs" to LanguageInfo("elixir", "Elixir"),

    // Erlang
    "erl" to LanguageInfo("erlang", "Erlang"),
    "hrl" to LanguageInfo("erlang", "Erlang"),

    // F#
    "fs" to LanguageInfo("fsharp", "F#"),
    "fsi" to LanguageInfo("fsharp", "F#"),
    "fsx" to LanguageInfo("fsharp", "F#"),
    "fsscript" to LanguageInfo("fsharp", "F#"),

    // Go
    "go" to LanguageInfo("go", "Go"),

    // Groovy
    "groovy" to LanguageInfo("groovy", "Groovy"),
    "gvy" to LanguageInfo("groovy", "Groovy"),
    "gradle" to LanguageInfo("groovy", "Groovy"),

    // Handlebars
    "hbs" to LanguageInfo("handlebars", "Handlebars"),
    "handlebars" to LanguageInfo("handlebars", "Handlebars"),

    // Haskell
    "hs" to LanguageInfo("haskell", "Haskell"),
    "lhs" to LanguageInfo("haskell", "Haskell"),

    // HTML
    "html" to LanguageInfo("html", "HTML"),
    "htm" to LanguageInfo("html", "HTML"),

    // Ini
    "ini" to LanguageInfo("ini", "Ini"),
    "cfg" to LanguageInfo("ini", "Ini"),

    // Java
    "java" to LanguageInfo("java", "Java"),

    // JavaScript
    "js" to LanguageInfo("javascript", "JavaScript"),
    "mjs" to LanguageInfo("javascript", "JavaScript"),
    "cjs" to LanguageInfo("javascript", "JavaScript"),

    // JavaScript React
    "jsx" to LanguageInfo("javascriptreact", "JavaScript React"),

    // JSON
    "json" to LanguageInfo("json", "JSON"),
    "jsonc" to LanguageInfo("json", "JSON"),

    // LaTeX
    "latex" to LanguageInfo("latex", "LaTeX"),

    // Less
    "less" to LanguageInfo("less", "Less"),

    // Lua
    "lua" to LanguageInfo("lua", "Lua"),

    // Markdown
    "md" to LanguageInfo("markdown", "Markdown"),
    "markdown" to LanguageInfo("markdown", "Markdown"),

    // Objective-C
    "m" to LanguageInfo("objective-c", "Objective-C"),

    // Objective-C++
    "mm" to LanguageInfo("objective-cpp", "Objective-C++"),

    // Perl
    "pl" to LanguageInfo("perl", "Perl"),
    "pm" to LanguageInfo("perl", "Perl"),

    // Perl 6
    "p6" to LanguageInfo("perl6", "Perl 6"),
    "raku" to LanguageInfo("perl6", "Perl 6"),

    // PHP
    "php" to LanguageInfo("php", "PHP"),
    "php4" to LanguageInfo("php", "PHP"),
    "php5" to LanguageInfo("php", "PHP"),
    "phtml" to LanguageInfo("php", "PHP"),

    // Text
    "txt" to LanguageInfo("plaintext", "Plaintext"),

    // Powershell
    "ps1" to LanguageInfo("powershell", "Powershell"),
    "psm1" to LanguageInfo("powershell", "Powershell"),
    "psd1" to LanguageInfo("powershell", "Powershell"),

    // Pug
    "jade" to LanguageInfo("jade", "Pug"),
    "pug" to LanguageInfo("jade", "Pug"),

    // Python
    "py" to LanguageInfo("python", "Python"),
    "pyw" to LanguageInfo("python", "Python"),
    "pyi" to LanguageInfo("python", "Python"),

    // R
    "r" to LanguageInfo("r", "R"),

    // Razor
    "cshtml" to LanguageInfo("razor", "Razor (cshtml)"),
    "razor" to LanguageInfo("razor", "Razor (cshtml)"),

    // Ruby
    "rb" to LanguageInfo("ruby", "Ruby"),
    "rbw" to LanguageInfo("ruby", "Ruby"),

    // Rust
    "rs" to LanguageInfo("rust", "Rust"),

    // SCSS / Sass
    "scss" to LanguageInfo("scss", "SCSS"),
    "sass" to LanguageInfo("sass", "Sass"),

    // Scala
    "scala" to LanguageInfo("scala", "Scala"),
    "sc" to LanguageInfo("scala", "Scala"),

    // ShaderLab
    "shader" to LanguageInfo("shaderlab", "ShaderLab"),

    // Shell
    "sh" to LanguageInfo("shellscript", "Shell Script"),
    "bash" to LanguageInfo("shellscript", "Shell Script (Bash)"),
    "zsh" to LanguageInfo("shellscript", "Shell Script (Zsh)"),
    "ksh" to LanguageInfo("shellscript", "Shell Script (Ksh)"),

    // SQL
    "sql" to LanguageInfo("sql", "SQL"),

    // Swift
    "swift" to LanguageInfo("swift", "Swift"),

    // TypeScript
    "ts" to LanguageInfo("typescript", "TypeScript"),
    "mts" to LanguageInfo("typescript", "TypeScript"),
    "cts" to LanguageInfo("typescript", "TypeScript"),

    // TypeScript React
    "tsx" to LanguageInfo("typescriptreact", "TypeScript React"),

    // TeX
    "tex" to LanguageInfo("tex", "TeX"),

    // Visual Basic
    "vb" to LanguageInfo("vb", "Visual Basic"),

    // XML
    "xml" to LanguageInfo("xml", "XML"),

    // XSL
    "xsl" to LanguageInfo("xsl", "XSL"),
    "xslt" to LanguageInfo("xsl", "XSL"),

    // YAML
    "yaml" to LanguageInfo("yaml", "YAML"),
    "yml" to LanguageInfo("yaml", "YAML"),
)

private val fileNameLanguageMap: Map<String, LanguageInfo> = mapOf(
    "dockerfile" to LanguageInfo("dockerfile", "Dockerfile"),
    "makefile" to LanguageInfo("makefile", "Makefile"),
    "gnumakefile" to LanguageInfo("makefile", "Makefile"),
    ".gitignore" to LanguageInfo("plaintext", "Plaintext"),
    "commit_editmsg" to LanguageInfo("git-commit", "Git"),
    "merge_msg" to LanguageInfo("git-commit", "Git"),
    "git-rebase-todo" to LanguageInfo("git-rebase", "Git"),
)

fun languageInfoForFile(file: KxFile): LanguageInfo {
    val lowerName = file.name.lowercase()
    fileNameLanguageMap[lowerName]?.let { return it }

    val ext = file.extension.lowercase()
    return extensionLanguageMap[ext] ?: LanguageInfo("plaintext", "Plaintext")
}

fun languageInfoForFile(file: KxFile, registry: LanguageRegistry): LanguageInfo {
    val lowerName = file.name.lowercase()
    fileNameLanguageMap[lowerName]?.let { return it }
    registry.getFileNames()[lowerName]?.let { name ->
        registry.getDescriptor(name)?.let { desc ->
            return LanguageInfo(desc.languageId, desc.displayName)
        }
    }

    val ext = file.extension.lowercase()
    extensionLanguageMap[ext]?.let { return it }
    registry.getExtensions()[ext]?.let { name ->
        registry.getDescriptor(name)?.let { desc ->
            return LanguageInfo(desc.languageId, desc.displayName)
        }
    }
    return LanguageInfo("plaintext", "Plaintext")
}

fun languageIdForFile(file: KxFile): String = languageInfoForFile(file).id
fun languageIdForFile(file: KxFile, registry: LanguageRegistry): String = languageInfoForFile(file, registry).id

fun languageNameForFile(file: KxFile): String = languageInfoForFile(file).displayName
fun languageNameForFile(file: KxFile, registry: LanguageRegistry): String = languageInfoForFile(file, registry).displayName

val KxFile.languageId: String get() = languageInfoForFile(this).id
val KxFile.languageName: String get() = languageInfoForFile(this).displayName
