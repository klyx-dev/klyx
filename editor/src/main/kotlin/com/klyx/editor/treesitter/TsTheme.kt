package com.klyx.editor.treesitter

import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.treesitter.ktreesitter.Query

/**
 * Theme for tree-sitter. This is different from [EditorColorScheme].
 * It is only used for colorizing spans in tree-sitter module. The real colors are still stored in editor
 * color schemes.
 * As what tree-sitter do, we try to match the longest scope.
 * For example, if 'variable' and 'variable.builtin' rule are both defined, Query 'variable.builtin'
 * and 'variable.builtin.this' will get 'variable.builtin' rule.
 * The theme also provide a fallback. You may call [TsTheme.putStyleRule] with a rule of length 0 to
 *  set fallback color scheme.
 * Note that colors of 'locals.definition', 'locals.reference', etc. can not be set by this theme object.
 *
 * @author Vivek
 */
class TsTheme(private val query: Query) {
    private val styles = mutableMapOf<String, Long>()

    private val resolvedCache = HashMap<String, Long>()

    var normalTextStyle = TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL)

    fun putStyleRule(rule: String, style: Long) {
        styles[rule] = style
        resolvedCache.clear()
    }

    fun eraseStyleRule(rule: String) = putStyleRule(rule, 0L)

    /**
     * Resolve style by capture name, walking up the dot-separated hierarchy.
     * e.g. "keyword.control.import" → tries "keyword.control.import",
     *      then "keyword.control", then "keyword", then ""
     */
    fun resolveStyleForCaptureName(name: String): Long {
        resolvedCache[name]?.let { return it }

        var current = name
        var style = 0L
        while (style == 0L && current.isNotEmpty()) {
            style = styles[current] ?: 0L
            if (style == 0L) {
                current = current.substringBeforeLast('.', "")
            }
        }
        // Also check the empty-string fallback rule
        if (style == 0L) {
            style = styles[""] ?: 0L
        }

        resolvedCache[name] = style
        return style
    }

    /**
     * Kept for any callers that still have a pattern index.
     * Delegates to the name-based resolver.
     */
    fun resolveStyleForPattern(patternIndex: Int): Long {
        val name = query.captureNames.getOrNull(patternIndex) ?: return 0L
        return resolveStyleForCaptureName(name)
    }
}

/**
 * Builder class for tree-sitter themes
 */
class TsThemeBuilder(query: Query) {

    internal val theme = TsTheme(query)

    infix fun Long.applyTo(targetRule: String) {
        theme.putStyleRule(targetRule, this)
    }

    infix fun Long.applyTo(targetRules: Array<String>) {
        targetRules.forEach { applyTo(it) }
    }
}

/**
 * Build tree-sitter theme
 */
fun tsTheme(query: Query, description: TsThemeBuilder.() -> Unit) =
    TsThemeBuilder(query).also { it.description() }.theme
