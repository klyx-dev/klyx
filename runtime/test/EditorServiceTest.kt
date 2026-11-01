package com.klyx.runtime

import com.klyx.runtime.workspace.DocumentLocation
import com.klyx.runtime.workspace.ProjectId
import com.klyx.runtime.workspace.Workspace
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.io.files.Path
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorServiceTest {
    private fun withWorkspace(test: (Workspace) -> Unit) {
        test(Workspace())
    }

    @Test
    fun closingOneSplitKeepsTabOpenInOtherGroup() = withWorkspace { workspace ->
        val id = workspace.openDocument(DocumentLocation.Platform(PlatformFile("/missing-split-test")), "test")
        val first = workspace.activeEditorGroupId.value!!
        val second = workspace.createEditorGroup()
        workspace.openDocument(second, id)
        workspace.activateEditorGroup(second)
        workspace.closeActiveDocument()
        assertTrue(workspace.isDocumentOpen(id))
        assertEquals(second, workspace.activeEditorGroupId.value)
        workspace.activateEditorGroup(first)
        workspace.closeActiveDocument()
        assertFalse(workspace.isDocumentOpen(id))
    }

    @Test
    fun closingGroupKeepsTabOpenInRemainingGroup() = withWorkspace { workspace ->
        val id = workspace.openDocument(DocumentLocation.Platform(PlatformFile("/missing-group-test")), "test")
        val first = workspace.activeEditorGroupId.value!!
        val second = workspace.createEditorGroup()
        workspace.openDocument(second, id)
        workspace.closeEditorGroup(second)
        assertTrue(workspace.isDocumentOpen(id))
        assertEquals(first, workspace.activeEditorGroupId.value)
    }

    @Test
    fun replacingPreviewDropsPreviousPreview() = withWorkspace { workspace ->
        val first = workspace.openDocument(DocumentLocation.Platform(PlatformFile("/missing-preview-a")), "a", preview = true)
        val _ = workspace.openDocument(DocumentLocation.Platform(PlatformFile("/missing-preview-b")), "b", preview = true)
        assertFalse(workspace.isDocumentOpen(first))
    }

    @Test
    fun previewFromHiddenProjectSurvivesNewPreview() = withWorkspace { workspace ->
        val a = ProjectId("a")
        val b = ProjectId("b")
        workspace.addProject(a)
        workspace.addProject(b)
        workspace.activateProject(a)
        val first = workspace.openDocument(DocumentLocation.Project(a, Path("a.kt")), "a.kt", preview = true)
        workspace.activateProject(b)
        val second = workspace.openDocument(DocumentLocation.Project(b, Path("b.kt")), "b.kt", preview = true)
        assertTrue(workspace.isDocumentOpen(first))
        assertTrue(workspace.isDocumentOpen(second))
        workspace.activateProject(a)
        assertTrue(workspace.isDocumentOpen(first))
    }
}
