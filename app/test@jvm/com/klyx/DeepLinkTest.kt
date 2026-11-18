package com.klyx

import com.klyx.runtime.action.OpenDocument
import com.klyx.runtime.action.OpenProject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.net.URLEncoder
import kotlin.io.path.createTempDirectory

class DeepLinkTest : FunSpec({
    test("new creates once and open routes the decoded file or directory") {
        val directory = createTempDirectory().toFile()
        try {
            val file = File(directory, "notes + # & %.txt")
            fun link(action: String, target: File) =
                "klyx://$action?path=" + URLEncoder.encode(target.absolutePath, "UTF-8")

            fun resolve(url: String) = resolveDeepLink(url) { File(it).createNewFile() }

            (resolve(link("open", directory)) is OpenProject) shouldBe true
            (resolve(link("new", file)) as OpenDocument).name shouldBe file.name
            file.readText() shouldBe ""
            file.writeText("keep")
            resolve(link("new", file)) shouldBe null
            file.readText() shouldBe "keep"
            (resolve(link("open", file)) as OpenDocument).name shouldBe file.name
            resolve("klyx://new?path=relative") shouldBe null
            resolve("klyx://new?path=%00") shouldBe null
            resolve(link("file", file)) shouldBe null
        } finally {
            directory.deleteRecursively()
        }
    }
})
