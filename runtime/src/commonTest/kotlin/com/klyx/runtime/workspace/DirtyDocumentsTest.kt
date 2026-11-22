package com.klyx.runtime.workspace

import io.github.vinceglb.filekit.PlatformFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class DirtyDocumentsTest : FunSpec({
    fun open(workspace: Workspace, name: String = "example.txt", preview: Boolean = false): DocumentId =
        workspace.openDocument(DocumentLocation.Platform(PlatformFile("/tmp/$name")), name, preview)

    test("undo to saved text clears dirty state, including empty buffers") {
        val workspace = Workspace()
        val id = open(workspace)
        workspace.initializeBuffer(id, "original")
        workspace.updateBuffer(id, "")
        workspace.isDirty(id) shouldBe true
        workspace.buffers.value[id]?.text shouldBe ""
        workspace.updateBuffer(id, "original")
        workspace.isDirty(id) shouldBe false
    }

    test("saving an earlier revision does not clear newer edits") {
        val workspace = Workspace()
        val id = open(workspace)
        workspace.initializeBuffer(id, "original")
        workspace.updateBuffer(id, "first edit")
        workspace.updateBuffer(id, "second edit")
        workspace.markSaved(id, "first edit")
        workspace.isDirty(id) shouldBe true
        workspace.buffers.value[id]?.text shouldBe "second edit"
    }

    test("close is blocked until save or discard, and cancel keeps the buffer") {
        val workspace = Workspace()
        val id = open(workspace)
        workspace.initializeBuffer(id, "original")
        workspace.updateBuffer(id, "edited")
        workspace.closeDocument(id)
        workspace.isDocumentOpen(id) shouldBe true
        workspace.pendingClose.value?.documentIds shouldBe listOf(id)
        workspace.cancelClose()
        workspace.buffers.value[id]?.text shouldBe "edited"
        workspace.closeDocument(id)
        workspace.finishClose(discard = false)
        workspace.isDocumentOpen(id) shouldBe true
        workspace.markSaved(id, "edited")
        workspace.finishClose(discard = false)
        workspace.isDocumentOpen(id) shouldBe false
    }

    test("dirty previews survive opening another preview") {
        val workspace = Workspace()
        val id = open(workspace, preview = true)
        workspace.initializeBuffer(id, "original")
        workspace.updateBuffer(id, "edited")
        open(workspace, "other.txt", preview = true)
        workspace.isDocumentOpen(id) shouldBe true
        workspace.groupOf(id)?.documents?.first { it.documentId == id }?.preview shouldBe false
    }

    test("closing a split asks before losing dirty documents") {
        val workspace = Workspace()
        val id = open(workspace)
        val group = workspace.activeGroup().id
        workspace.initializeBuffer(id, "original")
        workspace.updateBuffer(id, "edited")
        workspace.createEditorGroup()
        workspace.closeEditorGroup(group)
        workspace.editorGroups.value.size shouldBe 2
        workspace.finishClose(discard = true)
        workspace.editorGroups.value.size shouldBe 1
    }

    test("recovery restores an empty dirty buffer even when its file is missing") {
        val workspace = Workspace()
        val id = open(workspace, "klyx-missing-recovery-file.txt")
        workspace.initializeBuffer(id, "original")
        workspace.updateBuffer(id, "")
        val json = workspaceSessionJson.encodeToString(workspace.createSessionSnapshot())
        val snapshot = workspaceSessionJson.decodeFromString<WorkspaceSessionSnapshot>(json)
        val restored = Workspace()
        restored.restoreSession(snapshot)
        restored.buffers.value[id] shouldBe DocumentBuffer("", "original")
        restored.isDirty(id) shouldBe true
        restored.isDocumentOpen(id) shouldBe true
    }

    test("exit cancellation does not run the exit callback") {
        val workspace = Workspace()
        val id = open(workspace)
        workspace.initializeBuffer(id, "original")
        workspace.updateBuffer(id, "edited")
        var exited = false
        workspace.requestClose(workspace.documents.value.keys) { exited = true }
        workspace.cancelClose()
        exited shouldBe false
        workspace.isDirty(id) shouldBe true
    }
})
