package com.klyx.i18n.processor

private fun formatCamelCase(input: String, ignore: CharArray, upperCase: Boolean) =
    if (input.isEmpty()) input else StringBuilder(input.length).also {
        var seenSeparator = upperCase
        var seenUpperCase = !upperCase

        input.forEach { c ->
            when (c) {
                in ignore -> {
                    it.append(c)
                    seenSeparator = upperCase
                    seenUpperCase = !upperCase
                }

                in '0'..'9' -> {
                    it.append(c)
                    seenSeparator = false
                    seenUpperCase = false
                }

                in 'a'..'z' -> {
                    it.append(if (seenSeparator) c.uppercaseChar() else c)
                    seenSeparator = false
                    seenUpperCase = false
                }

                in 'A'..'Z' -> {
                    it.append(if (seenUpperCase) c.lowercaseChar() else c)
                    seenSeparator = false
                    seenUpperCase = true
                }

                else -> if (it.isNotEmpty()) {
                    seenSeparator = true
                    seenUpperCase = false
                }
            }
        }
    }.toString()

/**
 * Format this [String] in **lowerCamelCase** (aka. _mixedCase_,
 * _Smalltalk case_, …).
 *
 * @param ignore can be used to specify characters that should be included
 *   verbatim in the result, note that they are still considered separators
 */
public fun String.toLowerCamelCase(vararg ignore: Char): String =
    formatCamelCase(this, ignore, false)

/**
 * Format this [String] in **UpperCamelCase** (aka. _PascalCase_, _WikiCase_,
 * …).
 *
 * @param ignore can be used to specify characters that should be included
 *   verbatim in the result, note that they are still considered separators
 */
public fun String.toUpperCamelCase(vararg ignore: Char): String =
    formatCamelCase(this, ignore, true)

private fun formatCase(input: String, separator: Char, ignore: CharArray, upperCase: Boolean) =
    if (input.isEmpty()) input else StringBuilder(input.length).also {
        var seenSeparator = true
        var seenUpperCase = false

        input.forEach { c ->
            when (c) {
                in ignore -> {
                    it.append(c)
                    seenSeparator = true
                    seenUpperCase = false
                }

                in '0'..'9' -> {
                    it.append(c)
                    seenSeparator = false
                    seenUpperCase = false
                }

                in 'a'..'z' -> {
                    it.append(if (upperCase) c.uppercaseChar() else c)
                    seenSeparator = false
                    seenUpperCase = false
                }

                in 'A'..'Z' -> {
                    if (!seenSeparator && !seenUpperCase) it.append(separator)
                    it.append(if (upperCase) c else c.lowercaseChar())
                    seenSeparator = false
                    seenUpperCase = true
                }

                else -> {
                    if (!seenSeparator || !seenUpperCase) it.append(separator)
                    seenSeparator = true
                    seenUpperCase = false
                }
            }
        }
    }.toString()

private fun formatLowerCase(input: String, separator: Char, ignore: CharArray) =
    formatCase(input, separator, ignore, false)

/**
 * Format this [String] in another **lower case** format where words are
 * separated by the given [separator].
 *
 * @param separator to separate words with
 * @param ignore can be used to specify characters that should be included
 *   verbatim in the result, note that they are still considered separators
 */
public fun String.toLowerCaseFormat(separator: Char, vararg ignore: Char): String =
    formatLowerCase(this, separator, ignore)

/**
 * Format this [String] in **lower-dash-case** (aka. _lower hyphen case_,
 * _lower kebab case_, …).
 *
 * @param ignore can be used to specify characters that should be included
 *   verbatim in the result, note that they are still considered separators
 */
public fun String.toLowerDashCase(vararg ignore: Char): String =
    formatLowerCase(this, '-', ignore)

/**
 * Format this [String] in **lower_snake_case**.
 *
 * @param ignore can be used to specify characters that should be included
 *   verbatim in the result, note that they are still considered separators
 */
public fun String.toLowerSnakeCase(vararg ignore: Char): String =
    formatLowerCase(this, '_', ignore)

private fun formatUpperCase(input: String, separator: Char, ignore: CharArray) =
    formatCase(input, separator, ignore, true)

/**
 * Format this [String] in another **UPPER CASE** format where words are
 * separated by the given [separator].
 *
 * @param separator to separate words with
 * @param ignore can be used to specify characters that should be included
 *   verbatim in the result, note that they are still considered separators
 */
public fun String.toUpperCaseFormat(separator: Char, vararg ignore: Char): String =
    formatUpperCase(this, separator, ignore)

/**
 * Format this [String] in **UPPER-DASH-CASE** (aka. _upper hyphen case_,
 * _upper kebab case_, …).
 *
 * @param ignore can be used to specify characters that should be included
 *   verbatim in the result, note that they are still considered separators
 */
public fun String.toUpperDashCase(vararg ignore: Char): String =
    formatUpperCase(this, '-', ignore)

/**
 * Format this [String] in **UPPER_SNAKE_CASE** (aka. _screaming snake case_).
 *
 * @param ignore can be used to specify characters that should be included
 *   verbatim in the result, note that they are still considered separators
 */
public fun String.toUpperSnakeCase(vararg ignore: Char): String =
    formatUpperCase(this, '_', ignore)
