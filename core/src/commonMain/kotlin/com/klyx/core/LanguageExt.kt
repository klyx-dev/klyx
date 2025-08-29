package com.klyx.core

import com.klyx.core.file.KxFile

fun KxFile.language() = when (extension.lowercase()) {
    "kt", "kts" -> "Kotlin"
    "java" -> "Java"
    "groovy", "gvy", "gy", "gsh" -> "Groovy"
    "scala" -> "Scala"
    "clj", "cljs", "cljc", "edn" -> "Clojure"

    "js", "mjs", "cjs" -> "JavaScript"
    "ts", "tsx" -> "TypeScript"
    "jsx" -> "JavaScript (JSX)"
    "html", "htm", "xhtml" -> "HTML"
    "css" -> "CSS"
    "scss", "sass" -> "Sass/SCSS"
    "less" -> "Less"

    "py" -> "Python"
    "rb" -> "Ruby"
    "php" -> "PHP"
    "pl", "pm" -> "Perl"
    "r" -> "R"
    "sh", "bash", "zsh" -> "Shell Script"
    "ps1", "psm1" -> "PowerShell"
    "lua" -> "Lua"
    "tcl" -> "Tcl"

    "c" -> "C"
    "h" -> "C/C++ Header"
    "cpp", "cxx", "cc", "c++" -> "C++"
    "hpp", "hh", "hxx" -> "C++ Header"
    "rs" -> "Rust"
    "go" -> "Go"
    "swift" -> "Swift"
    "m" -> "Objective-C"
    "mm" -> "Objective-C++"

    "hs" -> "Haskell"
    "ml" -> "OCaml"
    "fs", "fsi", "fsx", "fsscript" -> "F#"
    "erl", "hrl" -> "Erlang"
    "ex", "exs" -> "Elixir"

    "gradle", "gradlew" -> "Gradle"
    "xml" -> "XML"
    "properties" -> "Properties File"
    "yaml", "yml" -> "YAML"
    "toml" -> "TOML"
    "ini" -> "INI"

    "json" -> "JSON"
    "csv" -> "CSV"
    "tsv" -> "TSV"
    "md" -> "Markdown"
    "rst" -> "reStructuredText"
    "tex" -> "LaTeX"

    "vue" -> "Vue"
    "svelte" -> "Svelte"
    "astro" -> "Astro"

    "sql" -> "SQL"
    "psql" -> "PostgreSQL"
    "mysql" -> "MySQL"
    "db" -> "Database File"

    "bat" -> "Batch Script"
    "coffee" -> "CoffeeScript"
    "dart" -> "Dart"
    "ktm" -> "Kotlin Multiplatform Script"
    "asm" -> "Assembly"

    "txt", "" -> "Plain Text"
    else -> "Plain Text"
}
