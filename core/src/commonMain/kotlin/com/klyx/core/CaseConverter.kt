package com.klyx.core

enum class StringCase {
    Camel,
    Pascal,
    Snake,
    ScreamingSnake,
    Kebab,
    Lower,
    Upper
}

private val WORD_REGEX = Regex(
    """
    (?<=[a-z0-9])(?=[A-Z]) |
    (?<=[A-Z])(?=[A-Z][a-z]) |
    [_\-\s]+
    """.trimIndent(),
    RegexOption.MULTILINE
)

fun String.toWords(): List<String> =
    trim()
        .split(WORD_REGEX)
        .filter { it.isNotBlank() }
        .map { it.lowercase() }

fun interface CaseConverter {
    fun convert(words: List<String>): String
}

object CaseConverters {

    val camel = CaseConverter { words ->
        words.first() +
                words.drop(1).joinToString("") { it.replaceFirstChar(Char::uppercase) }
    }

    val pascal = CaseConverter { words ->
        words.joinToString("") { it.replaceFirstChar(Char::uppercase) }
    }

    val snake = CaseConverter { words ->
        words.joinToString("_")
    }

    val screamingSnake = CaseConverter { words ->
        words.joinToString("_").uppercase()
    }

    val kebab = CaseConverter { words ->
        words.joinToString("-")
    }

    val lower = CaseConverter { words ->
        words.joinToString("")
    }

    val upper = CaseConverter { words ->
        words.joinToString("").uppercase()
    }

    fun from(case: StringCase): CaseConverter =
        when (case) {
            StringCase.Camel -> camel
            StringCase.Pascal -> pascal
            StringCase.Snake -> snake
            StringCase.ScreamingSnake -> screamingSnake
            StringCase.Kebab -> kebab
            StringCase.Lower -> lower
            StringCase.Upper -> upper
        }
}

fun String.convertTo(case: StringCase): String {
    val words = this.toWords()
    if (words.isEmpty()) return this
    return CaseConverters.from(case).convert(words)
}

fun String.toSnakeCase() = convertTo(StringCase.Snake)
