package com.klyx.lsp

import com.klyx.lsp.internal.verify
import com.klyx.lsp.types.DocumentUri
import com.klyx.lsp.types.OneOf
import com.klyx.lsp.types.isLeft
import com.klyx.lsp.types.isRight
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.contracts.contract
import kotlin.jvm.JvmInline

/**
 * The document diagnostic report kinds.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentDiagnosticReportKind)
 *
 * @since 3.17.0
 */
@Serializable
@JvmInline
value class DocumentDiagnosticReportKind private constructor(private val value: String) {
    override fun toString() = value

    companion object {
        /**
         * A diagnostic report with a full
         * set of problems.
         */
        val Full = DocumentDiagnosticReportKind("full")

        /**
         * A report indicating that the last
         * returned report is still accurate.
         */
        val Unchanged = DocumentDiagnosticReportKind("unchanged")
    }
}

/**
 * A diagnostic report with a full set of problems.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#fullDocumentDiagnosticReport)
 *
 * @since 3.17.0
 */
@Serializable(FullDocumentDiagnosticReportSerializer::class)
sealed interface FullDocumentDiagnosticReport {
    /**
     * A full document diagnostic report.
     */
    val kind get() = DocumentDiagnosticReportKind.Full

    /**
     * An optional result ID. If provided, it will
     * be sent on the next diagnostic request for the
     * same document.
     */
    var resultId: String?

    /**
     * The actual items.
     */
    val items: List<Diagnostic>
}

/**
 * A diagnostic report with a full set of problems.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#fullDocumentDiagnosticReport)
 *
 * @param items The actual items.
 * @param resultId An optional result ID. If provided, it will be sent on the next diagnostic request for the same document.
 * @since 3.17.0
 */
fun FullDocumentDiagnosticReport(
    items: List<Diagnostic>,
    resultId: String? = null
): FullDocumentDiagnosticReport = FullDocumentDiagnosticReportImpl(resultId, items)

private class FullDocumentDiagnosticReportImpl(
    override var resultId: String?,
    override val items: List<Diagnostic>
) : FullDocumentDiagnosticReport

/**
 * A diagnostic report indicating that the last returned
 * report is still accurate.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#unchangedDocumentDiagnosticReport)
 *
 * @since 3.17.0
 */
@Serializable(UnchangedDocumentDiagnosticReportSerializer::class)
sealed interface UnchangedDocumentDiagnosticReport {
    /**
     * A document diagnostic report indicating
     * no changes to the last result. A server can
     * only return `unchanged` if result IDs are
     * provided.
     */
    val kind get() = DocumentDiagnosticReportKind.Unchanged

    /**
     * A result ID which will be sent on the next
     * diagnostic request for the same document.
     */
    val resultId: String
}

/**
 * A diagnostic report indicating that the last returned
 * report is still accurate.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#unchangedDocumentDiagnosticReport)
 *
 * @param resultId A result ID which will be sent on the next diagnostic request for the same document.
 * @since 3.17.0
 */
fun UnchangedDocumentDiagnosticReport(
    resultId: String
): UnchangedDocumentDiagnosticReport = UnchangedDocumentDiagnosticReportImpl(resultId)

private class UnchangedDocumentDiagnosticReportImpl(
    override val resultId: String
) : UnchangedDocumentDiagnosticReport

/**
 * A full diagnostic report with a set of related documents.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#relatedFullDocumentDiagnosticReport)
 *
 * @since 3.17.0
 */
@Serializable
data class RelatedFullDocumentDiagnosticReport(
    override val items: List<Diagnostic>,
    override var resultId: String?,
    /**
     * Diagnostics of related documents. This information is useful
     * in programming languages where code in a file A can generate
     * diagnostics in a file B which A depends on. An example of
     * such a language is C/C++, where macro definitions in a file
     * a.cpp can result in errors in a header file b.hpp.
     *
     * @since 3.17.0
     */
    var relatedDocuments: Map<DocumentUri, OneOf<FullDocumentDiagnosticReport, UnchangedDocumentDiagnosticReport>>?
) : FullDocumentDiagnosticReport

/**
 * An unchanged diagnostic report with a set of related documents.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#relatedUnchangedDocumentDiagnosticReport)
 *
 * @since 3.17.0
 */
@Serializable
data class RelatedUnchangedDocumentDiagnosticReport(
    override val resultId: String,
    /**
     * Diagnostics of related documents. This information is useful
     * in programming languages where code in a file A can generate
     * diagnostics in a file B which A depends on. An example of
     * such a language is C/C++, where macro definitions in a file
     * a.cpp can result in errors in a header file b.hpp.
     *
     * @since 3.17.0
     */
    var relatedDocuments: Map<DocumentUri, OneOf<FullDocumentDiagnosticReport, UnchangedDocumentDiagnosticReport>>?
) : UnchangedDocumentDiagnosticReport

/**
 * The result of a document diagnostic pull request. A report can
 * either be a full report, containing all diagnostics for the
 * requested document, or an unchanged report, indicating that nothing
 * has changed in terms of diagnostics in comparison to the last
 * pull request.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentDiagnosticReport)
 *
 * @since 3.17.0
 */
typealias DocumentDiagnosticReport = OneOf<RelatedFullDocumentDiagnosticReport, RelatedUnchangedDocumentDiagnosticReport>

val DocumentDiagnosticReport.isFull: Boolean
    get() {
        contract {
            returns(true) implies (this@isFull is OneOf.Left)
        }
        return this is OneOf.Left
    }

val DocumentDiagnosticReport.isUnchanged: Boolean
    get() {
        contract {
            returns(true) implies (this@isUnchanged is OneOf.Right)
        }
        return this is OneOf.Right
    }

val DocumentDiagnosticReport.full: RelatedFullDocumentDiagnosticReport?
    get() {
        contract {
            returnsNotNull() implies (this@full is OneOf.Left)
        }
        return if (isLeft()) value else null
    }

val DocumentDiagnosticReport.unchanged: RelatedUnchangedDocumentDiagnosticReport?
    get() {
        contract {
            returnsNotNull() implies (this@unchanged is OneOf.Right)
        }
        return if (isRight()) value else null
    }

/**
 * A partial result for a document diagnostic report.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#documentDiagnosticReportPartialResult)
 *
 * @since 3.17.0
 */
@Serializable
data class DocumentDiagnosticReportPartialResult(
    val relatedDocuments: Map<DocumentUri, OneOf<FullDocumentDiagnosticReport, UnchangedDocumentDiagnosticReport>>
)

/**
 * Cancellation data returned from a diagnostic request.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#diagnosticServerCancellationData)
 *
 * @since 3.17.0
 */
@Serializable
data class DiagnosticServerCancellationData(val retriggerRequest: Boolean)

internal object FullDocumentDiagnosticReportSerializer : KSerializer<FullDocumentDiagnosticReport> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.klyx.lsp.FullDocumentDiagnosticReport") {
            element("resultId", String.serializer().descriptor, isOptional = true)
            element("items", ListSerializer(Diagnostic.serializer()).descriptor)
        }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: FullDocumentDiagnosticReport) {
        verify(encoder)

        val resultId = value.resultId
        if (resultId == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeNotNullMark()
            encoder.encodeString(resultId)
        }
        encoder.encodeSerializableValue(ListSerializer(Diagnostic.serializer()), value.items)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): FullDocumentDiagnosticReport {
        verify(decoder)
        val resultId = if (decoder.decodeNotNullMark()) {
            decoder.decodeString()
        } else {
            decoder.decodeNull()
        }
        val items = decoder.decodeSerializableValue(ListSerializer(Diagnostic.serializer()))
        return FullDocumentDiagnosticReportImpl(resultId, items)
    }
}

internal object UnchangedDocumentDiagnosticReportSerializer : KSerializer<UnchangedDocumentDiagnosticReport> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.klyx.lsp.UnchangedDocumentDiagnosticReport")

    override fun serialize(encoder: Encoder, value: UnchangedDocumentDiagnosticReport) {
        verify(encoder)
        encoder.encodeString(value.resultId)
    }

    override fun deserialize(decoder: Decoder): UnchangedDocumentDiagnosticReport {
        verify(decoder)
        return UnchangedDocumentDiagnosticReportImpl(decoder.decodeString())
    }
}
