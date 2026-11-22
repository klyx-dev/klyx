package com.klyx.ui.palette

import com.klyx.runtime.workspace.Document
import com.klyx.runtime.workspace.DocumentBuffer
import com.klyx.runtime.workspace.DocumentId
import com.klyx.runtime.workspace.DocumentLocation
import com.klyx.runtime.workspace.Project
import com.klyx.runtime.workspace.ProjectId
import com.klyx.ui.editor.mobileBinding
import io.github.vinceglb.filekit.PlatformFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.last
import kotlin.io.path.createTempDirectory

class PaletteTest : FunSpec({
    test("mobile modifiers and Gboard uppercase resolve the configured palette shortcut") {
        val palette = EditorCommand.Palette.binding(emptyMap())
        mobileBinding(listOf("ctrl", "shift"), "p", false) shouldBe palette
        mobileBinding(listOf("ctrl"), "p", true) shouldBe palette
        mobileBinding(listOf("shift", "ctrl"), "p", true) shouldBe palette
        mobileBinding(listOf("ctrl"), "p", false) shouldBe "ctrl+p"
    }

    test("shortcut overrides normalize and persist explicit unbinding") {
        normalizeBinding("shift + cmd + p") shouldBe "ctrl+shift+p"
        normalizeBinding("Alt+j") shouldBe "alt+j"
        EditorCommand.References.binding(emptyMap()) shouldBe "shift+f12"
        EditorCommand.References.binding(mapOf("References" to "Alt+R")) shouldBe "alt+r"
        normalizeBinding("F12") shouldBe "f12"
        normalizeBinding("Alt+Left") shouldBe "alt+left"
        normalizeBinding("left") shouldBe null
        normalizeBinding("Shift+P") shouldBe null
        normalizeBinding("Ctrl+Cmd+P") shouldBe null
        EditorCommand.Palette.binding(emptyMap()) shouldBe "ctrl+shift+p"
        EditorCommand.Palette.binding(mapOf("Palette" to "")) shouldBe ""
        EditorCommand.QuickOpen.binding(mapOf("QuickOpen" to "ctrl+o")) shouldBe "ctrl+o"
    }

    test("find in files searches unsaved buffers once and returns editor coordinates") {
        val root = createTempDirectory().toFile()
        try {
            val project = Project(ProjectId("test"), PlatformFile(root))
            val edited = root.resolve("edited.txt").apply { writeText("needle on disk") }
            root.resolve("other.txt").writeText("first\r\n  Needle here")
            root.resolve("binary").writeBytes(byteArrayOf(0) + "needle".encodeToByteArray())
            val files = projectFiles(project).last()
            val document = Document(
                id = DocumentId("edited"),
                name = edited.name,
                location = DocumentLocation.Platform(PlatformFile(edited))
            )
            val buffers = searchBuffers(
                project = project,
                documents = listOf(document),
                buffers = mapOf(document.id to DocumentBuffer("new\nabc NEEDLE", "needle on disk"))
            )
            val hits = searchLines(files, buffers, "needle").last()
            hits.map { Triple(it.file.path, it.line, it.column) } shouldBe listOf(
                Triple("edited.txt", 1, 4), Triple("other.txt", 1, 2)
            )
            hits.first().file.location shouldBe document.location
            searchLines(files, buffers, "on disk").last().isEmpty() shouldBe true
            EditorCommand.FindInFiles.binding(emptyMap()) shouldBe "ctrl+shift+f"
            EditorCommand.FindInFiles.binding(mapOf("FindInFiles" to "Alt+f")) shouldBe "alt+f"
        } finally {
            root.deleteRecursively()
        }
    }

    test("quick open searches nested paths and skips build output") {
        val root = createTempDirectory().toFile()
        try {
            for (path in listOf("src/MainActivity.kt", "test/MainActivity.kt", "build/MainActivity.kt")) {
                val file = root.resolve(path)
                file.parentFile.mkdirs()
                file.writeText("")
            }
            val files = projectFiles(Project(ProjectId("test"), PlatformFile(root))).last()
            files.map { it.path.replace('\\', '/') }.sorted() shouldBe listOf(
                "src/MainActivity.kt",
                "test/MainActivity.kt"
            )
            matchRank("src/MainActivity.kt", "MainActivity.kt", "mainact") shouldBe 1
            matchRank("src/MainActivity.kt", "MainActivity.kt", "mactkt") shouldBe 4
            matchRank("src/MainActivity.kt", "MainActivity.kt", "xyz") shouldBe null
        } finally {
            root.deleteRecursively()
        }
    }
})
