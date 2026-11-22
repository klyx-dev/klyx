package com.klyx.runtime.workspace

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class EditorGroupTransitionsTest : FunSpec({
    val a = DocumentId("a")
    val b = DocumentId("b")
    val c = DocumentId("c")

    test("opening previews replaces only the preview") {
        val w = Workspace()
        val group = w.createEditorGroup()
        w.openDocument(group, a, preview = false)
        w.openDocument(group, b, preview = true)
        w.openDocument(group, c, preview = true)

        w.editorGroups.value.single().documents shouldBe listOf(
            WorkspaceDocument(a),
            WorkspaceDocument(c, preview = true),
        )
    }

    test("opening a preview permanently promotes it without duplicating") {
        val w = Workspace()
        val group = w.createEditorGroup()
        w.openDocument(group, a, preview = true)
        w.openDocument(group, b, preview = false)
        w.openDocument(group, a, preview = false)

        val current = w.editorGroups.value.single()
        current.documents shouldBe listOf(WorkspaceDocument(a), WorkspaceDocument(b))
        current.activeDocumentId shouldBe a
    }

    test("pinning promotes a preview and unpinning keeps it permanent") {
        val w = Workspace()
        val group = w.createEditorGroup()
        w.openDocument(group, a, preview = true)
        w.pinDocument(group, a)
        w.editorGroups.value.single().documents shouldBe listOf(WorkspaceDocument(a, pinned = true))
        w.unpinDocument(group, a)
        w.editorGroups.value.single().documents shouldBe listOf(WorkspaceDocument(a))
    }

    test("explicit promotion preserves selection") {
        val w = Workspace()
        val group = w.createEditorGroup()
        w.openDocument(group, a, preview = true)
        w.openDocument(group, b, preview = false)
        w.promotePreviewDocument(group, a)
        val current = w.editorGroups.value.single()
        current.documents shouldBe listOf(WorkspaceDocument(a), WorkspaceDocument(b))
        current.activeDocumentId shouldBe b
    }

    test("promoting a permanent tab changes nothing") {
        val w = Workspace()
        val group = w.createEditorGroup()
        w.openDocument(group, a, preview = false)
        w.openDocument(group, b, preview = false)
        val before = w.editorGroups.value.single()
        w.promotePreviewDocument(group, a)
        w.editorGroups.value.single() shouldBe before
    }

    test("closing selects the right neighbor then the left neighbor then nothing") {
        val w = Workspace()
        val group = w.createEditorGroup()
        w.openDocument(group, a, preview = false)
        w.openDocument(group, b, preview = false)
        w.openDocument(group, c, preview = false)
        w.activateDocument(group, b)
        w.closeDocument(group, b)
        var current = w.editorGroups.value.single()
        current.documents.map { tab -> tab.documentId } shouldBe listOf(a, c)
        current.activeDocumentId shouldBe c
        w.closeDocument(group, c)
        current = w.editorGroups.value.single()
        current.activeDocumentId shouldBe a
        w.closeDocument(group, a)
        w.editorGroups.value.single().documents shouldBe emptyList<WorkspaceDocument>()
    }

    test("operations requiring an open tab reject missing documents") {
        val w = Workspace()
        val group = w.createEditorGroup()
        shouldThrow<IllegalStateException> { w.activateDocument(group, a) }
        shouldThrow<IllegalStateException> { w.pinDocument(group, a) }
        shouldThrow<IllegalStateException> { w.unpinDocument(group, a) }
        w.promotePreviewDocument(group, a)
        w.editorGroups.value.single() shouldBe EditorGroup(group)
    }
})
