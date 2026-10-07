package com.klyx.runtime.workspace

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import okio.Path.Companion.toPath

class WorkspaceTest : FunSpec({
    fun workspace() = Workspace()

    val a = DocumentId("a")
    val b = DocumentId("b")

    test("isDocumentOpen reflects tabs in any group") {
        val w = workspace()
        val first = w.createEditorGroup()
        val second = w.createEditorGroup()

        w.isDocumentOpen(a).shouldBeFalse()
        w.isDocumentOpen(first, a).shouldBeFalse()

        w.openDocument(first, a)
        w.isDocumentOpen(a).shouldBeTrue()
        w.isDocumentOpen(first, a).shouldBeTrue()
        w.isDocumentOpen(second, a).shouldBeFalse()

        w.openDocument(second, b)
        w.isDocumentOpen(b).shouldBeTrue()
        w.isDocumentOpen(second, b).shouldBeTrue()
    }

    test("isDocumentOpen goes false once the tab is closed") {
        val w = workspace()
        val group = w.createEditorGroup()
        w.openDocument(group, a)
        w.isDocumentOpen(a).shouldBeTrue()
        w.closeDocument(group, a)
        w.isDocumentOpen(a).shouldBeFalse()
    }

    test("groupOf prefers the focused group") {
        val w = workspace()
        val first = w.createEditorGroup()
        val second = w.createEditorGroup()

        w.openDocument(first, a)
        w.openDocument(second, a)
        w.activateEditorGroup(second)

        w.groupOf(a)?.id shouldBe second

        w.activateEditorGroup(first)
        w.groupOf(a)?.id shouldBe first
    }

    test("groupOf is null for a document that is not open") {
        workspace().groupOf(a).shouldBeNull()
    }

    test("openDocumentIds deduplicates across groups") {
        val w = workspace()
        val first = w.createEditorGroup()
        val second = w.createEditorGroup()

        w.openDocument(first, a)
        w.openDocument(second, a)
        w.openDocument(second, b)

        w.openDocumentIds() shouldBe setOf(a, b)
    }

    test("preview tabs are replaced but pinned ones are kept") {
        val w = workspace()
        val group = w.createEditorGroup()

        w.openDocument(group, a)
        w.openDocument(group, b, preview = true)
        w.openDocument(group, DocumentId("c"), preview = true)

        w.openDocumentIds() shouldBe setOf(a, DocumentId("c"))

        w.pinDocument(group, DocumentId("c"))
        w.openDocument(group, DocumentId("d"), preview = true)
        w.openDocumentIds() shouldBe setOf(a, DocumentId("c"), DocumentId("d"))
    }

    test("reopening a permanent tab as preview does not demote it or replace another preview") {
        val w = workspace()
        val group = w.createEditorGroup()
        w.openDocument(group, a)
        w.openDocument(group, b, preview = true)

        w.openDocument(group, a, preview = true)

        w.editorGroups.value.single() shouldBe EditorGroup(
            group,
            listOf(WorkspaceDocument(a), WorkspaceDocument(b, preview = true)),
            a,
        )
    }

    test("reopening a pinned tab as preview keeps it permanent even after unpinning") {
        val w = workspace()
        val group = w.createEditorGroup()
        w.openDocument(group, a, preview = true)
        w.pinDocument(group, a)
        w.openDocument(group, b, preview = true)

        w.openDocument(group, a, preview = true)
        w.editorGroups.value.single().documents shouldBe listOf(
            WorkspaceDocument(a, pinned = true), WorkspaceDocument(b, preview = true),
        )
        w.unpinDocument(group, a)
        w.openDocument(group, DocumentId("c"), preview = true)
        w.openDocumentIds() shouldBe setOf(a, DocumentId("c"))
    }

    test("find does not register anything") {
        val w = workspace()
        val location = DocumentLocation.Local("/tmp/x.kt".toPath())

        w.find(location).shouldBeNull()
        w.documents.value.size shouldBe 0

        val document = w.open(location, "x.kt")
        w.find(location) shouldBe document
        w.open(location, "x.kt") shouldBe document
        w.documents.value.size shouldBe 1
    }
})
