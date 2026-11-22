package com.klyx.lsp

import com.klyx.lsp.types.DocumentUri
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.isLeft
import com.klyx.lsp.types.isRight
import kotlinx.serialization.Serializable
import kotlin.contracts.contract

/**
 * A full document diagnostic report for a workspace diagnostic result.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceFullDocumentDiagnosticReport)
 *
 * @since 3.17.0
 */
@Serializable
data class WorkspaceFullDocumentDiagnosticReport(
    override val items: List<Diagnostic>,
    /**
     * The URI for which diagnostic information is reported.
     */
    val uri: DocumentUri,

    /**
     * The version number for which the diagnostics are reported.
     * If the document is not marked as open, `null` can be provided.
     */
    val version: Int?,
    override var resultId: String?
) : FullDocumentDiagnosticReport

/**
 * An unchanged document diagnostic report for a workspace diagnostic result.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceUnchangedDocumentDiagnosticReport)
 *
 * @since 3.17.0
 */
@Serializable
data class WorkspaceUnchangedDocumentDiagnosticReport(
    /**
     * The URI for which diagnostic information is reported.
     */
    val uri: DocumentUri,

    /**
     * The version number for which the diagnostics are reported.
     * If the document is not marked as open, `null` can be provided.
     */
    val version: Int?,
    override var resultId: String
) : UnchangedDocumentDiagnosticReport

/**
 * A workspace diagnostic document report.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceDocumentDiagnosticReport)
 *
 * @since 3.17.0
 */
typealias WorkspaceDocumentDiagnosticReport = OneOf<WorkspaceFullDocumentDiagnosticReport, WorkspaceUnchangedDocumentDiagnosticReport>

val WorkspaceDocumentDiagnosticReport.isFull: Boolean
    get() {
        contract {
            returns(true) implies (this@isFull is OneOf.Left)
        }
        return this is OneOf.Left
    }

val WorkspaceDocumentDiagnosticReport.isUnchanged: Boolean
    get() {
        contract {
            returns(true) implies (this@isUnchanged is OneOf.Right)
        }
        return this is OneOf.Right
    }

val WorkspaceDocumentDiagnosticReport.full: WorkspaceFullDocumentDiagnosticReport?
    get() {
        contract {
            returnsNotNull() implies (this@full is OneOf.Left)
        }
        return if (isLeft()) value else null
    }

val WorkspaceDocumentDiagnosticReport.unchanged: WorkspaceUnchangedDocumentDiagnosticReport?
    get() {
        contract {
            returnsNotNull() implies (this@unchanged is OneOf.Right)
        }
        return if (isRight()) value else null
    }

/**
 * A workspace diagnostic report.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceDiagnosticReport)
 *
 * @since 3.17.0
 */
@Serializable
data class WorkspaceDiagnosticReport(val items: List<WorkspaceDocumentDiagnosticReport>)

/**
 * A partial result for a workspace diagnostic report.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#workspaceDiagnosticReportPartialResult)
 *
 * @since 3.17.0
 */
@Serializable
data class WorkspaceDiagnosticReportPartialResult(val items: List<WorkspaceDocumentDiagnosticReport>)
