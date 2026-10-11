package com.klyx.lsp.capabilities

import com.klyx.lsp.CodeActionKind
import com.klyx.lsp.CompletionItemTag
import com.klyx.lsp.MarkupKind

fun defaultClientCapabilities(): ClientCapabilities = ClientCapabilities(
    workspace = WorkspaceClientCapabilities(
        workspaceFolders = true,
        applyEdit = true,
        configuration = true,
        inlayHint = InlayHintWorkspaceClientCapabilities(refreshSupport = true),
        codeLens = CodeLensWorkspaceClientCapabilities(refreshSupport = true),
        executeCommand = ExecuteCommandCapabilities(dynamicRegistration = false),
    ),
    textDocument = TextDocumentClientCapabilities(
        synchronization = SynchronizationCapabilities(
            dynamicRegistration = false,
            willSave = false,
            willSaveWaitUntil = false,
            didSave = true,
        ),
        completion = CompletionCapabilities(
            dynamicRegistration = false,
            contextSupport = true,
            completionItem = CompletionItemCapabilities(
                snippetSupport = true,
                commitCharactersSupport = true,
                documentationFormat = listOf(MarkupKind.Markdown, MarkupKind.PlainText),
                deprecatedSupport = true,
                preselectSupport = true,
                labelDetailsSupport = true,
                insertReplaceSupport = true,
                tagSupport = CompletionItemTagSupportClientCapabilities(
                    valueSet = listOf(CompletionItemTag.Deprecated),
                ),
                resolveSupport = CompletionItemResolveSupportClientCapabilities(
                    properties = listOf("documentation", "detail"),
                ),
            ),
        ),
        hover = HoverCapabilities(
            contentFormat = listOf(MarkupKind.Markdown, MarkupKind.PlainText),
        ),
        publishDiagnostics = PublishDiagnosticsCapabilities(
            relatedInformation = true,
            versionSupport = true,
        ),
        diagnostic = DiagnosticClientCapabilities(
            relatedDocumentSupport = false,
            relatedInformation = true,
        ),
        foldingRange = FoldingRangeClientCapabilities(
            dynamicRegistration = false,
            lineFoldingOnly = false,
        ),
        codeLens = CodeLensClientCapabilities(
            dynamicRegistration = false,
            resolveSupport = ClientCodeLensResolveOptions(
                properties = listOf("command"),
            ),
        ),
        documentHighlight = DocumentHighlightClientCapabilities(
            dynamicRegistration = false,
        ),
        codeAction = CodeActionCapabilities(
            codeActionLiteralSupport = CodeActionLiteralSupportCapabilities(
                codeActionKind = CodeActionKindCapabilities(
                    valueSet = listOf(
                        CodeActionKind.QuickFix,
                        CodeActionKind.Refactor,
                        CodeActionKind.RefactorExtract,
                        CodeActionKind.RefactorInline,
                        CodeActionKind.RefactorRewrite,
                        CodeActionKind.Source,
                        CodeActionKind.SourceOrganizeImports,
                    )
                )
            )
        ),
        inlayHint = InlayHintClientCapabilities(
            dynamicRegistration = false,
            resolveSupport = ResolveSupportCapabilities(
                properties = listOf("tooltip", "textEdits"),
            ),
        ),
    ),
    window = WindowClientCapabilities(
        workDoneProgress = true,
    ),
)
