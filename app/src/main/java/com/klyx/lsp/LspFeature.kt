package com.klyx.lsp

import com.klyx.lsp.capabilities.ServerCapabilities
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.OneOfThree
import com.klyx.lsp.types.fold

/**
 * The LSP features that have a presentation in the sora-editor.
 */
internal enum class LspFeature {
    Completion,
    Formatting,
    RangeFormatting,
    InlayHints,
    PullDiagnostics;

    fun isSupportedBy(capabilities: ServerCapabilities): Boolean = when (this) {
        Completion -> capabilities.completionProvider != null
        Formatting -> capabilities.documentFormattingProvider.isEnabled()
        RangeFormatting -> capabilities.documentRangeFormattingProvider.isEnabled()
        InlayHints -> capabilities.inlayHintProvider.isEnabled()
        PullDiagnostics -> capabilities.diagnosticProvider != null
    }
}

private fun <T> OneOf<Boolean, T>?.isEnabled(): Boolean =
    this?.fold({ it }, { true }) == true

private fun <A, B, C> OneOfThree<A, B, C>?.isEnabled(): Boolean =
    this?.fold({ it }, { true }, { true }) == true

/** Text sync is a capability too; notifications must follow the negotiated mode. */
internal fun ServerCapabilities.supportsDidOpenClose(): Boolean = textDocumentSync?.fold(
    { it.openClose == true },
    // The legacy numeric form does not carry openClose; LSP clients conventionally open documents.
    { it != TextDocumentSyncKind.None }
) == true

internal fun ServerCapabilities.supportsDidChange(): Boolean = textDocumentSync?.fold(
    { it.change != null && it.change != TextDocumentSyncKind.None },
    { it != TextDocumentSyncKind.None }
) == true

internal fun ServerCapabilities.supportsDidSave(): Boolean = textDocumentSync?.fold(
    { options -> options.save?.fold({ it }, { true }) == true },
    { it != TextDocumentSyncKind.None }
) == true
