package com.klyx.core.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThemeFile(
    @SerialName("${"$"}schema")
    val schema: String?,
    val name: String,
    val author: String,
    val themes: Array<Theme>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other != null && this::class != other::class) return false

        other as ThemeFile

        if (name != other.name) return false
        if (author != other.author) return false
        if (!themes.contentEquals(other.themes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + themes.contentHashCode()
        return result
    }
}

@Serializable
data class Theme(
    val name: String,
    val appearance: Appearance,
    val style: Style
) {
    @Serializable
    data class Style(
        val border: String? = null,
        @SerialName("border.variant")
        val borderVariant: String? = null,
        @SerialName("border.focused")
        val borderFocused: String? = null,
        @SerialName("border.selected")
        val borderSelected: String? = null,
        @SerialName("border.transparent")
        val borderTransparent: String? = null,
        @SerialName("border.disabled")
        val borderDisabled: String? = null,
        @SerialName("elevated_surface.background")
        val elevatedSurfaceBackground: String? = null,
        @SerialName("surface.background")
        val surfaceBackground: String? = null,
        val background: String? = null,
        @SerialName("element.background")
        val elementBackground: String? = null,
        @SerialName("element.hover")
        val elementHover: String? = null,
        @SerialName("element.active")
        val elementActive: String? = null,
        @SerialName("element.selected")
        val elementSelected: String? = null,
        @SerialName("element.disabled")
        val elementDisabled: String? = null,
        @SerialName("drop_target.background")
        val dropTargetBackground: String? = null,
        @SerialName("ghost_element.background")
        val ghostElementBackground: String? = null,
        @SerialName("ghost_element.hover")
        val ghostElementHover: String? = null,
        @SerialName("ghost_element.active")
        val ghostElementActive: String? = null,
        @SerialName("ghost_element.selected")
        val ghostElementSelected: String? = null,
        @SerialName("ghost_element.disabled")
        val ghostElementDisabled: String? = null,
        val text: String? = null,
        @SerialName("text.muted")
        val textMuted: String? = null,
        @SerialName("text.placeholder")
        val textPlaceholder: String? = null,
        @SerialName("text.disabled")
        val textDisabled: String? = null,
        @SerialName("text.accent")
        val textAccent: String? = null,
        val icon: String? = null,
        @SerialName("icon.muted")
        val iconMuted: String? = null,
        @SerialName("icon.disabled")
        val iconDisabled: String? = null,
        @SerialName("icon.placeholder")
        val iconPlaceholder: String? = null,
        @SerialName("icon.accent")
        val iconAccent: String? = null,
        @SerialName("status_bar.background")
        val statusBarBackground: String? = null,
        @SerialName("title_bar.background")
        val titleBarBackground: String? = null,
        @SerialName("title_bar.inactive_background")
        val titleBarInactiveBackground: String? = null,
        @SerialName("toolbar.background")
        val toolbarBackground: String? = null,
        @SerialName("tab_bar.background")
        val tabBarBackground: String? = null,
        @SerialName("tab.inactive_background")
        val tabInactiveBackground: String? = null,
        @SerialName("tab.active_background")
        val tabActiveBackground: String? = null,
        @SerialName("search.match_background")
        val searchMatchBackground: String? = null,
        @SerialName("panel.background")
        val panelBackground: String? = null,
        @SerialName("panel.focused_border")
        val panelFocusedBorder: String? = null,
        @SerialName("pane.focused_border")
        val paneFocusedBorder: String? = null,
        @SerialName("scrollbar.thumb.background")
        val scrollbarThumbBackground: String? = null,
        @SerialName("scrollbar.thumb.hover_background")
        val scrollbarThumbHoverBackground: String? = null,
        @SerialName("scrollbar.thumb.border")
        val scrollbarThumbBorder: String? = null,
        @SerialName("scrollbar.track.background")
        val scrollbarTrackBackground: String? = null,
        @SerialName("scrollbar.track.border")
        val scrollbarTrackBorder: String? = null,
        @SerialName("editor.foreground")
        val editorForeground: String? = null,
        @SerialName("editor.background")
        val editorBackground: String? = null,
        @SerialName("editor.gutter.background")
        val editorGutterBackground: String? = null,
        @SerialName("editor.subheader.background")
        val editorSubheaderBackground: String? = null,
        @SerialName("editor.active_line.background")
        val editorActiveLineBackground: String? = null,
        @SerialName("editor.highlighted_line.background")
        val editorHighlightedLineBackground: String? = null,
        @SerialName("editor.line_number")
        val editorLineNumber: String? = null,
        @SerialName("editor.active_line_number")
        val editorActiveLineNumber: String? = null,
        @SerialName("editor.hover_line_number")
        val editorHoverLineNumber: String? = null,
        @SerialName("editor.invisible")
        val editorInvisible: String? = null,
        @SerialName("editor.wrap_guide")
        val editorWrapGuide: String? = null,
        @SerialName("editor.active_wrap_guide")
        val editorActiveWrapGuide: String? = null,
        @SerialName("editor.document_highlight.read_background")
        val editorDocumentHighlightReadBackground: String? = null,
        @SerialName("editor.document_highlight.write_background")
        val editorDocumentHighlightWriteBackground: String? = null,
        @SerialName("terminal.background")
        val terminalBackground: String? = null,
        @SerialName("terminal.foreground")
        val terminalForeground: String? = null,
        @SerialName("terminal.bright_foreground")
        val terminalBrightForeground: String? = null,
        @SerialName("terminal.dim_foreground")
        val terminalDimForeground: String? = null,
        @SerialName("terminal.ansi.black")
        val terminalAnsiBlack: String? = null,
        @SerialName("terminal.ansi.bright_black")
        val terminalAnsiBrightBlack: String? = null,
        @SerialName("terminal.ansi.dim_black")
        val terminalAnsiDimBlack: String? = null,
        @SerialName("terminal.ansi.red")
        val terminalAnsiRed: String? = null,
        @SerialName("terminal.ansi.bright_red")
        val terminalAnsiBrightRed: String? = null,
        @SerialName("terminal.ansi.dim_red")
        val terminalAnsiDimRed: String? = null,
        @SerialName("terminal.ansi.green")
        val terminalAnsiGreen: String? = null,
        @SerialName("terminal.ansi.bright_green")
        val terminalAnsiBrightGreen: String? = null,
        @SerialName("terminal.ansi.dim_green")
        val terminalAnsiDimGreen: String? = null,
        @SerialName("terminal.ansi.yellow")
        val terminalAnsiYellow: String? = null,
        @SerialName("terminal.ansi.bright_yellow")
        val terminalAnsiBrightYellow: String? = null,
        @SerialName("terminal.ansi.dim_yellow")
        val terminalAnsiDimYellow: String? = null,
        @SerialName("terminal.ansi.blue")
        val terminalAnsiBlue: String? = null,
        @SerialName("terminal.ansi.bright_blue")
        val terminalAnsiBrightBlue: String? = null,
        @SerialName("terminal.ansi.dim_blue")
        val terminalAnsiDimBlue: String? = null,
        @SerialName("terminal.ansi.magenta")
        val terminalAnsiMagenta: String? = null,
        @SerialName("terminal.ansi.bright_magenta")
        val terminalAnsiBrightMagenta: String? = null,
        @SerialName("terminal.ansi.dim_magenta")
        val terminalAnsiDimMagenta: String? = null,
        @SerialName("terminal.ansi.cyan")
        val terminalAnsiCyan: String? = null,
        @SerialName("terminal.ansi.bright_cyan")
        val terminalAnsiBrightCyan: String? = null,
        @SerialName("terminal.ansi.dim_cyan")
        val terminalAnsiDimCyan: String? = null,
        @SerialName("terminal.ansi.white")
        val terminalAnsiWhite: String? = null,
        @SerialName("terminal.ansi.bright_white")
        val terminalAnsiBrightWhite: String? = null,
        @SerialName("terminal.ansi.dim_white")
        val terminalAnsiDimWhite: String? = null,
        @SerialName("link_text.hover")
        val linkTextHover: String? = null,
        @SerialName("version_control.added")
        val versionControlAdded: String? = null,
        @SerialName("version_control.modified")
        val versionControlModified: String? = null,
        @SerialName("version_control.deleted")
        val versionControlDeleted: String? = null,
        @SerialName("version_control.conflict_marker.ours")
        val versionControlConflictMarkerOurs: String? = null,
        @SerialName("version_control.conflict_marker.theirs")
        val versionControlConflictMarkerTheirs: String? = null,
        val conflict: String? = null,
        @SerialName("conflict.background")
        val conflictBackground: String? = null,
        @SerialName("conflict.border")
        val conflictBorder: String? = null,
        val created: String? = null,
        @SerialName("created.background")
        val createdBackground: String? = null,
        @SerialName("created.border")
        val createdBorder: String? = null,
        val deleted: String? = null,
        @SerialName("deleted.background")
        val deletedBackground: String? = null,
        @SerialName("deleted.border")
        val deletedBorder: String? = null,
        val error: String? = null,
        @SerialName("error.background")
        val errorBackground: String? = null,
        @SerialName("error.border")
        val errorBorder: String? = null,
        val hidden: String? = null,
        @SerialName("hidden.background")
        val hiddenBackground: String? = null,
        @SerialName("hidden.border")
        val hiddenBorder: String? = null,
        val hint: String? = null,
        @SerialName("hint.background")
        val hintBackground: String? = null,
        @SerialName("hint.border")
        val hintBorder: String? = null,
        val ignored: String? = null,
        @SerialName("ignored.background")
        val ignoredBackground: String? = null,
        @SerialName("ignored.border")
        val ignoredBorder: String? = null,
        val info: String? = null,
        @SerialName("info.background")
        val infoBackground: String? = null,
        @SerialName("info.border")
        val infoBorder: String? = null,
        val modified: String? = null,
        @SerialName("modified.background")
        val modifiedBackground: String? = null,
        @SerialName("modified.border")
        val modifiedBorder: String? = null,
        val predictive: String? = null,
        @SerialName("predictive.background")
        val predictiveBackground: String? = null,
        @SerialName("predictive.border")
        val predictiveBorder: String? = null,
        val renamed: String? = null,
        @SerialName("renamed.background")
        val renamedBackground: String? = null,
        @SerialName("renamed.border")
        val renamedBorder: String? = null,
        val success: String? = null,
        @SerialName("success.background")
        val successBackground: String? = null,
        @SerialName("success.border")
        val successBorder: String? = null,
        val unreachable: String? = null,
        @SerialName("unreachable.background")
        val unreachableBackground: String? = null,
        @SerialName("unreachable.border")
        val unreachableBorder: String? = null,
        val warning: String? = null,
        @SerialName("warning.background")
        val warningBackground: String? = null,
        @SerialName("warning.border")
        val warningBorder: String? = null,
        val players: Array<Player>? = null,
        val syntax: Map<String, SyntaxStyle> = emptyMap()
    ) {
        @Suppress("LongMethod", "CyclomaticComplexMethod")
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other != null && this::class != other::class) return false

            other as Style

            if (border != other.border) return false
            if (borderVariant != other.borderVariant) return false
            if (borderFocused != other.borderFocused) return false
            if (borderSelected != other.borderSelected) return false
            if (borderTransparent != other.borderTransparent) return false
            if (borderDisabled != other.borderDisabled) return false
            if (elevatedSurfaceBackground != other.elevatedSurfaceBackground) return false
            if (surfaceBackground != other.surfaceBackground) return false
            if (background != other.background) return false
            if (elementBackground != other.elementBackground) return false
            if (elementHover != other.elementHover) return false
            if (elementActive != other.elementActive) return false
            if (elementSelected != other.elementSelected) return false
            if (elementDisabled != other.elementDisabled) return false
            if (dropTargetBackground != other.dropTargetBackground) return false
            if (ghostElementBackground != other.ghostElementBackground) return false
            if (ghostElementHover != other.ghostElementHover) return false
            if (ghostElementActive != other.ghostElementActive) return false
            if (ghostElementSelected != other.ghostElementSelected) return false
            if (ghostElementDisabled != other.ghostElementDisabled) return false
            if (text != other.text) return false
            if (textMuted != other.textMuted) return false
            if (textPlaceholder != other.textPlaceholder) return false
            if (textDisabled != other.textDisabled) return false
            if (textAccent != other.textAccent) return false
            if (icon != other.icon) return false
            if (iconMuted != other.iconMuted) return false
            if (iconDisabled != other.iconDisabled) return false
            if (iconPlaceholder != other.iconPlaceholder) return false
            if (iconAccent != other.iconAccent) return false
            if (statusBarBackground != other.statusBarBackground) return false
            if (titleBarBackground != other.titleBarBackground) return false
            if (titleBarInactiveBackground != other.titleBarInactiveBackground) return false
            if (toolbarBackground != other.toolbarBackground) return false
            if (tabBarBackground != other.tabBarBackground) return false
            if (tabInactiveBackground != other.tabInactiveBackground) return false
            if (tabActiveBackground != other.tabActiveBackground) return false
            if (searchMatchBackground != other.searchMatchBackground) return false
            if (panelBackground != other.panelBackground) return false
            if (panelFocusedBorder != other.panelFocusedBorder) return false
            if (paneFocusedBorder != other.paneFocusedBorder) return false
            if (scrollbarThumbBackground != other.scrollbarThumbBackground) return false
            if (scrollbarThumbHoverBackground != other.scrollbarThumbHoverBackground) return false
            if (scrollbarThumbBorder != other.scrollbarThumbBorder) return false
            if (scrollbarTrackBackground != other.scrollbarTrackBackground) return false
            if (scrollbarTrackBorder != other.scrollbarTrackBorder) return false
            if (editorForeground != other.editorForeground) return false
            if (editorBackground != other.editorBackground) return false
            if (editorGutterBackground != other.editorGutterBackground) return false
            if (editorSubheaderBackground != other.editorSubheaderBackground) return false
            if (editorActiveLineBackground != other.editorActiveLineBackground) return false
            if (editorHighlightedLineBackground != other.editorHighlightedLineBackground) return false
            if (editorLineNumber != other.editorLineNumber) return false
            if (editorActiveLineNumber != other.editorActiveLineNumber) return false
            if (editorHoverLineNumber != other.editorHoverLineNumber) return false
            if (editorInvisible != other.editorInvisible) return false
            if (editorWrapGuide != other.editorWrapGuide) return false
            if (editorActiveWrapGuide != other.editorActiveWrapGuide) return false
            if (editorDocumentHighlightReadBackground != other.editorDocumentHighlightReadBackground) return false
            if (editorDocumentHighlightWriteBackground != other.editorDocumentHighlightWriteBackground) return false
            if (terminalBackground != other.terminalBackground) return false
            if (terminalForeground != other.terminalForeground) return false
            if (terminalBrightForeground != other.terminalBrightForeground) return false
            if (terminalDimForeground != other.terminalDimForeground) return false
            if (terminalAnsiBlack != other.terminalAnsiBlack) return false
            if (terminalAnsiBrightBlack != other.terminalAnsiBrightBlack) return false
            if (terminalAnsiDimBlack != other.terminalAnsiDimBlack) return false
            if (terminalAnsiRed != other.terminalAnsiRed) return false
            if (terminalAnsiBrightRed != other.terminalAnsiBrightRed) return false
            if (terminalAnsiDimRed != other.terminalAnsiDimRed) return false
            if (terminalAnsiGreen != other.terminalAnsiGreen) return false
            if (terminalAnsiBrightGreen != other.terminalAnsiBrightGreen) return false
            if (terminalAnsiDimGreen != other.terminalAnsiDimGreen) return false
            if (terminalAnsiYellow != other.terminalAnsiYellow) return false
            if (terminalAnsiBrightYellow != other.terminalAnsiBrightYellow) return false
            if (terminalAnsiDimYellow != other.terminalAnsiDimYellow) return false
            if (terminalAnsiBlue != other.terminalAnsiBlue) return false
            if (terminalAnsiBrightBlue != other.terminalAnsiBrightBlue) return false
            if (terminalAnsiDimBlue != other.terminalAnsiDimBlue) return false
            if (terminalAnsiMagenta != other.terminalAnsiMagenta) return false
            if (terminalAnsiBrightMagenta != other.terminalAnsiBrightMagenta) return false
            if (terminalAnsiDimMagenta != other.terminalAnsiDimMagenta) return false
            if (terminalAnsiCyan != other.terminalAnsiCyan) return false
            if (terminalAnsiBrightCyan != other.terminalAnsiBrightCyan) return false
            if (terminalAnsiDimCyan != other.terminalAnsiDimCyan) return false
            if (terminalAnsiWhite != other.terminalAnsiWhite) return false
            if (terminalAnsiBrightWhite != other.terminalAnsiBrightWhite) return false
            if (terminalAnsiDimWhite != other.terminalAnsiDimWhite) return false
            if (linkTextHover != other.linkTextHover) return false
            if (versionControlAdded != other.versionControlAdded) return false
            if (versionControlModified != other.versionControlModified) return false
            if (versionControlDeleted != other.versionControlDeleted) return false
            if (versionControlConflictMarkerOurs != other.versionControlConflictMarkerOurs) return false
            if (versionControlConflictMarkerTheirs != other.versionControlConflictMarkerTheirs) return false
            if (conflict != other.conflict) return false
            if (conflictBackground != other.conflictBackground) return false
            if (conflictBorder != other.conflictBorder) return false
            if (created != other.created) return false
            if (createdBackground != other.createdBackground) return false
            if (createdBorder != other.createdBorder) return false
            if (deleted != other.deleted) return false
            if (deletedBackground != other.deletedBackground) return false
            if (deletedBorder != other.deletedBorder) return false
            if (error != other.error) return false
            if (errorBackground != other.errorBackground) return false
            if (errorBorder != other.errorBorder) return false
            if (hidden != other.hidden) return false
            if (hiddenBackground != other.hiddenBackground) return false
            if (hiddenBorder != other.hiddenBorder) return false
            if (hint != other.hint) return false
            if (hintBackground != other.hintBackground) return false
            if (hintBorder != other.hintBorder) return false
            if (ignored != other.ignored) return false
            if (ignoredBackground != other.ignoredBackground) return false
            if (ignoredBorder != other.ignoredBorder) return false
            if (info != other.info) return false
            if (infoBackground != other.infoBackground) return false
            if (infoBorder != other.infoBorder) return false
            if (modified != other.modified) return false
            if (modifiedBackground != other.modifiedBackground) return false
            if (modifiedBorder != other.modifiedBorder) return false
            if (predictive != other.predictive) return false
            if (predictiveBackground != other.predictiveBackground) return false
            if (predictiveBorder != other.predictiveBorder) return false
            if (renamed != other.renamed) return false
            if (renamedBackground != other.renamedBackground) return false
            if (renamedBorder != other.renamedBorder) return false
            if (success != other.success) return false
            if (successBackground != other.successBackground) return false
            if (successBorder != other.successBorder) return false
            if (unreachable != other.unreachable) return false
            if (unreachableBackground != other.unreachableBackground) return false
            if (unreachableBorder != other.unreachableBorder) return false
            if (warning != other.warning) return false
            if (warningBackground != other.warningBackground) return false
            if (warningBorder != other.warningBorder) return false
            if (!players.contentEquals(other.players)) return false
            if (syntax != other.syntax) return false

            return true
        }

        @Suppress("LongMethod", "CyclomaticComplexMethod")
        override fun hashCode(): Int {
            var result = border?.hashCode() ?: 0
            result = 31 * result + (borderVariant?.hashCode() ?: 0)
            result = 31 * result + (borderFocused?.hashCode() ?: 0)
            result = 31 * result + (borderSelected?.hashCode() ?: 0)
            result = 31 * result + (borderTransparent?.hashCode() ?: 0)
            result = 31 * result + (borderDisabled?.hashCode() ?: 0)
            result = 31 * result + (elevatedSurfaceBackground?.hashCode() ?: 0)
            result = 31 * result + (surfaceBackground?.hashCode() ?: 0)
            result = 31 * result + (background?.hashCode() ?: 0)
            result = 31 * result + (elementBackground?.hashCode() ?: 0)
            result = 31 * result + (elementHover?.hashCode() ?: 0)
            result = 31 * result + (elementActive?.hashCode() ?: 0)
            result = 31 * result + (elementSelected?.hashCode() ?: 0)
            result = 31 * result + (elementDisabled?.hashCode() ?: 0)
            result = 31 * result + (dropTargetBackground?.hashCode() ?: 0)
            result = 31 * result + (ghostElementBackground?.hashCode() ?: 0)
            result = 31 * result + (ghostElementHover?.hashCode() ?: 0)
            result = 31 * result + (ghostElementActive?.hashCode() ?: 0)
            result = 31 * result + (ghostElementSelected?.hashCode() ?: 0)
            result = 31 * result + (ghostElementDisabled?.hashCode() ?: 0)
            result = 31 * result + (text?.hashCode() ?: 0)
            result = 31 * result + (textMuted?.hashCode() ?: 0)
            result = 31 * result + (textPlaceholder?.hashCode() ?: 0)
            result = 31 * result + (textDisabled?.hashCode() ?: 0)
            result = 31 * result + (textAccent?.hashCode() ?: 0)
            result = 31 * result + (icon?.hashCode() ?: 0)
            result = 31 * result + (iconMuted?.hashCode() ?: 0)
            result = 31 * result + (iconDisabled?.hashCode() ?: 0)
            result = 31 * result + (iconPlaceholder?.hashCode() ?: 0)
            result = 31 * result + (iconAccent?.hashCode() ?: 0)
            result = 31 * result + (statusBarBackground?.hashCode() ?: 0)
            result = 31 * result + (titleBarBackground?.hashCode() ?: 0)
            result = 31 * result + (titleBarInactiveBackground?.hashCode() ?: 0)
            result = 31 * result + (toolbarBackground?.hashCode() ?: 0)
            result = 31 * result + (tabBarBackground?.hashCode() ?: 0)
            result = 31 * result + (tabInactiveBackground?.hashCode() ?: 0)
            result = 31 * result + (tabActiveBackground?.hashCode() ?: 0)
            result = 31 * result + (searchMatchBackground?.hashCode() ?: 0)
            result = 31 * result + (panelBackground?.hashCode() ?: 0)
            result = 31 * result + (panelFocusedBorder?.hashCode() ?: 0)
            result = 31 * result + (paneFocusedBorder?.hashCode() ?: 0)
            result = 31 * result + (scrollbarThumbBackground?.hashCode() ?: 0)
            result = 31 * result + (scrollbarThumbHoverBackground?.hashCode() ?: 0)
            result = 31 * result + (scrollbarThumbBorder?.hashCode() ?: 0)
            result = 31 * result + (scrollbarTrackBackground?.hashCode() ?: 0)
            result = 31 * result + (scrollbarTrackBorder?.hashCode() ?: 0)
            result = 31 * result + (editorForeground?.hashCode() ?: 0)
            result = 31 * result + (editorBackground?.hashCode() ?: 0)
            result = 31 * result + (editorGutterBackground?.hashCode() ?: 0)
            result = 31 * result + (editorSubheaderBackground?.hashCode() ?: 0)
            result = 31 * result + (editorActiveLineBackground?.hashCode() ?: 0)
            result = 31 * result + (editorHighlightedLineBackground?.hashCode() ?: 0)
            result = 31 * result + (editorLineNumber?.hashCode() ?: 0)
            result = 31 * result + (editorActiveLineNumber?.hashCode() ?: 0)
            result = 31 * result + (editorHoverLineNumber?.hashCode() ?: 0)
            result = 31 * result + (editorInvisible?.hashCode() ?: 0)
            result = 31 * result + (editorWrapGuide?.hashCode() ?: 0)
            result = 31 * result + (editorActiveWrapGuide?.hashCode() ?: 0)
            result = 31 * result + (editorDocumentHighlightReadBackground?.hashCode() ?: 0)
            result = 31 * result + (editorDocumentHighlightWriteBackground?.hashCode() ?: 0)
            result = 31 * result + (terminalBackground?.hashCode() ?: 0)
            result = 31 * result + (terminalForeground?.hashCode() ?: 0)
            result = 31 * result + (terminalBrightForeground?.hashCode() ?: 0)
            result = 31 * result + (terminalDimForeground?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiBlack?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiBrightBlack?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiDimBlack?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiRed?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiBrightRed?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiDimRed?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiGreen?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiBrightGreen?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiDimGreen?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiYellow?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiBrightYellow?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiDimYellow?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiBlue?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiBrightBlue?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiDimBlue?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiMagenta?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiBrightMagenta?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiDimMagenta?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiCyan?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiBrightCyan?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiDimCyan?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiWhite?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiBrightWhite?.hashCode() ?: 0)
            result = 31 * result + (terminalAnsiDimWhite?.hashCode() ?: 0)
            result = 31 * result + (linkTextHover?.hashCode() ?: 0)
            result = 31 * result + (versionControlAdded?.hashCode() ?: 0)
            result = 31 * result + (versionControlModified?.hashCode() ?: 0)
            result = 31 * result + (versionControlDeleted?.hashCode() ?: 0)
            result = 31 * result + (versionControlConflictMarkerOurs?.hashCode() ?: 0)
            result = 31 * result + (versionControlConflictMarkerTheirs?.hashCode() ?: 0)
            result = 31 * result + (conflict?.hashCode() ?: 0)
            result = 31 * result + (conflictBackground?.hashCode() ?: 0)
            result = 31 * result + (conflictBorder?.hashCode() ?: 0)
            result = 31 * result + (created?.hashCode() ?: 0)
            result = 31 * result + (createdBackground?.hashCode() ?: 0)
            result = 31 * result + (createdBorder?.hashCode() ?: 0)
            result = 31 * result + (deleted?.hashCode() ?: 0)
            result = 31 * result + (deletedBackground?.hashCode() ?: 0)
            result = 31 * result + (deletedBorder?.hashCode() ?: 0)
            result = 31 * result + (error?.hashCode() ?: 0)
            result = 31 * result + (errorBackground?.hashCode() ?: 0)
            result = 31 * result + (errorBorder?.hashCode() ?: 0)
            result = 31 * result + (hidden?.hashCode() ?: 0)
            result = 31 * result + (hiddenBackground?.hashCode() ?: 0)
            result = 31 * result + (hiddenBorder?.hashCode() ?: 0)
            result = 31 * result + (hint?.hashCode() ?: 0)
            result = 31 * result + (hintBackground?.hashCode() ?: 0)
            result = 31 * result + (hintBorder?.hashCode() ?: 0)
            result = 31 * result + (ignored?.hashCode() ?: 0)
            result = 31 * result + (ignoredBackground?.hashCode() ?: 0)
            result = 31 * result + (ignoredBorder?.hashCode() ?: 0)
            result = 31 * result + (info?.hashCode() ?: 0)
            result = 31 * result + (infoBackground?.hashCode() ?: 0)
            result = 31 * result + (infoBorder?.hashCode() ?: 0)
            result = 31 * result + (modified?.hashCode() ?: 0)
            result = 31 * result + (modifiedBackground?.hashCode() ?: 0)
            result = 31 * result + (modifiedBorder?.hashCode() ?: 0)
            result = 31 * result + (predictive?.hashCode() ?: 0)
            result = 31 * result + (predictiveBackground?.hashCode() ?: 0)
            result = 31 * result + (predictiveBorder?.hashCode() ?: 0)
            result = 31 * result + (renamed?.hashCode() ?: 0)
            result = 31 * result + (renamedBackground?.hashCode() ?: 0)
            result = 31 * result + (renamedBorder?.hashCode() ?: 0)
            result = 31 * result + (success?.hashCode() ?: 0)
            result = 31 * result + (successBackground?.hashCode() ?: 0)
            result = 31 * result + (successBorder?.hashCode() ?: 0)
            result = 31 * result + (unreachable?.hashCode() ?: 0)
            result = 31 * result + (unreachableBackground?.hashCode() ?: 0)
            result = 31 * result + (unreachableBorder?.hashCode() ?: 0)
            result = 31 * result + (warning?.hashCode() ?: 0)
            result = 31 * result + (warningBackground?.hashCode() ?: 0)
            result = 31 * result + (warningBorder?.hashCode() ?: 0)
            result = 31 * result + (players?.contentHashCode() ?: 0)
            result = 31 * result + syntax.hashCode()
            return result
        }
    }

    @Serializable
    data class SyntaxStyle(
        val color: String,
        @SerialName("font_style")
        val fontStyle: String? = null,
        @SerialName("font_weight")
        val fontWeight: Int? = null
    )

    @Serializable
    data class Player(
        val cursor: String,
        val background: String,
        val selection: String
    )
}

fun ThemeFile.firstDarkTheme() = themes.first { it.appearance == Appearance.Dark }
fun ThemeFile.firstLightTheme() = themes.first { it.appearance == Appearance.Light }

val LocalIsDarkMode = compositionLocalOf { true }
