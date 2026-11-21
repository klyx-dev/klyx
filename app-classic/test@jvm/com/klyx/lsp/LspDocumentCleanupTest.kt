package com.klyx.lsp

import com.klyx.runtime.workspace.DocumentLocation
import com.klyx.runtime.workspace.Workspace
import io.github.vinceglb.filekit.PlatformFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path

class LspDocumentCleanupTest : FunSpec({
    test("definition descriptors select the explicit grammar and preserve supported filenames") {
        val workspace = Workspace()
        for ([name, expected, language] in listOf(
            Triple("iostream", "iostream.cpp", "cpp"),
            Triple("builtins.pyi", "builtins.pyi", "python"),
            Triple("README", "README", "plaintext"),
        )) {
            val doc = workspace.open(DocumentLocation.Platform(PlatformFile("/project/$name")), name)
            val descriptor = LspDocumentUtils.highlightDescriptor(doc)
            descriptor.fileName shouldBe expected
            descriptor.syntaxName shouldBe language.takeUnless { it == "plaintext" }
            LspDocumentUtils.languageIdOf(doc) shouldBe language
            doc.name shouldBe name
        }
    }

    test("project document URI lookup is safe after closing its project") {
        val directory = withContext(Dispatchers.IO) {
            Files.createTempDirectory("klyx-lsp-close-project")
        }
        try {
            val workspace = Workspace()
            val project = workspace.openProject(PlatformFile(directory.toString()))
            val id = workspace.openDocument(
                location = DocumentLocation.Project(project.id, Path("settings.json")),
                name = "settings.json"
            )
            val document = workspace[id]
            val uri = LspDocumentUtils.uriOf(document, workspace)
            LspDocumentUtils.uriOfOrNull(document, workspace) shouldBe uri

            workspace.closeProject(project.id)

            workspace.projects.value[project.id].shouldBeNull()
            LspDocumentUtils.uriOfOrNull(document, workspace).shouldBeNull()
        } finally {
            withContext(Dispatchers.IO) {
                Files.delete(directory)
            }
        }
    }

    test("platform document URI remains available after its project closes") {
        val directory = withContext(Dispatchers.IO) {
            Files.createTempDirectory("klyx-lsp-platform-document")
        }
        try {
            val workspace = Workspace()
            val project = workspace.openProject(PlatformFile(directory.toString()))
            val file = PlatformFile(directory.resolve("settings.json").toString())
            val id = workspace.openDocument(DocumentLocation.Platform(file), "settings.json")
            val document = workspace[id]
            val uri = LspDocumentUtils.uriOf(document, workspace)

            workspace.closeProject(project.id)

            LspDocumentUtils.uriOfOrNull(document, workspace) shouldBe uri
        } finally {
            withContext(Dispatchers.IO) {
                Files.delete(directory)
            }
        }
    }
})
