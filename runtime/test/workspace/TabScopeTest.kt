package com.klyx.runtime.workspace

import io.github.vinceglb.filekit.PlatformFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.io.files.Path

class TabScopeTest : FunSpec({
    val projectA = ProjectId("a")
    val projectB = ProjectId("b")

    fun projectDoc(project: ProjectId, name: String) = Document(
        id = DocumentId("${project.value}:$name"),
        name = name,
        location = DocumentLocation.Project(project, Path(name)),
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

    test("platform documents are visible everywhere") {
        val local = Document(
            id = DocumentId("local:/tmp/x.kt"),
            name = "x.kt",
            location = DocumentLocation.Platform(PlatformFile("/tmp/x.kt")),
        )

        val shared = Document(
            id = DocumentId("shared:document"),
            name = "shared",
            location = DocumentLocation.Platform(PlatformFile("/tmp/shared")),
        )

        local.isVisibleIn(projectA) shouldBe true
        local.isVisibleIn(null) shouldBe true

        shared.isVisibleIn(projectB) shouldBe true
        shared.isVisibleIn(null) shouldBe true
    }
})
