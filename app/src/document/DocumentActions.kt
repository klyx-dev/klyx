package com.klyx.document

import com.klyx.core.Disposable
import com.klyx.core.action.ActionRegistrar
import com.klyx.core.action.action
import com.klyx.core.asDisposable
import com.klyx.runtime.action.SaveActiveDocument
import com.klyx.runtime.action.SaveAllDocuments
import com.klyx.runtime.action.SaveDocument
import org.koin.core.annotation.Single

@Single
class DocumentActions(private val documents: DocumentPersistence) : ActionRegistrar {
    override fun registerActions(): Disposable = listOf(
        action<SaveActiveDocument> { documents.saveActiveDocument() },
        action<SaveDocument> { documents.saveDocument(it.documentId) },
        action<SaveAllDocuments> { documents.saveAllDocuments() },
    ).asDisposable()
}
