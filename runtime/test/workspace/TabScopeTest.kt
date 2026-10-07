package com.klyx.runtime.workspace

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import okio.Path.Companion.toPath

class TabScopeTest : FunSpec({
    val projectA = ProjectId("a")
    val projectB = ProjectId("b")

    fun projectDoc(project: ProjectId, name: String) = Document(
        id = DocumentId("${project.value}:$name"),
        name = name,
        location = DocumentLocation.Project(project, name.toPath()),
    )

    test("workspace defaults to the current project only") {
        Workspace().tabScope.value shouldBe EditorTabScope.CurrentProject
    }

    test("workspace remembers the chosen scope") {
        val workspace = Workspace()
        workspace.setTabScope(EditorTabScope.AllProjects)
        workspace.tabScope.value shouldBe EditorTabScope.AllProjects
        workspace.setTabScope(EditorTabScope.CurrentProject)
        workspace.tabScope.value shouldBe EditorTabScope.CurrentProject
    }

    test("project documents are visible only in their own project") {
        val doc = projectDoc(projectA, "a.kt")

        doc.isVisibleIn(projectA) shouldBe true
        doc.isVisibleIn(projectB) shouldBe false
        doc.isVisibleIn(null) shouldBe false
    }

    test("local and uri documents are visible everywhere") {
        val local = Document(
            id = DocumentId("local:/tmp/x.kt"),
            name = "x.kt",
            location = DocumentLocation.Local("/tmp/x.kt".toPath()),
        )
        val uri = Document(
            id = DocumentId("uri:content"),
            name = "shared",
            location = DocumentLocation.Uri("content://shared"),
        )

        local.isVisibleIn(projectA) shouldBe true
        local.isVisibleIn(null) shouldBe true
        uri.isVisibleIn(projectB) shouldBe true
        uri.isVisibleIn(null) shouldBe true
    }
})
