package com.klyx.lsp

import com.klyx.runtime.OperatingSystem
import com.klyx.runtime.settings.CustomLspServer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class LspFileAccessTest : FunSpec({
    val android = OperatingSystem.Android
    val workspaceServer = LspServerConfig("test", setOf("test"), "test-server")

    test("Android remote transports block private documents by default") {
        for (transport in listOf(LspTransportType.Auto, LspTransportType.Termux, LspTransportType.TcpSocket)) {
            workspaceServer.copy(transportType = transport).canOpenPrivateFile(android) shouldBe false
        }
    }

    test("content-only servers can open private text but never use private workspace folders") {
        val config = workspaceServer.copy(supportsContentOnly = true)
        config.canOpenPrivateFile(android) shouldBe true
        config.canUseWorkspaceFolder(isPrivate = true, android) shouldBe false
        config.canUseWorkspaceFolder(isPrivate = false, android) shouldBe true
    }

    test("local Android stdio and desktop servers retain private file access") {
        workspaceServer.copy(transportType = LspTransportType.Stdio).canOpenPrivateFile(android) shouldBe true
        workspaceServer.canOpenPrivateFile(OperatingSystem.Linux) shouldBe true
        workspaceServer.canUseWorkspaceFolder(isPrivate = true, OperatingSystem.Linux) shouldBe true
    }

    test("private directory matching includes descendants without matching sibling names") {
        val roots = listOf("/data/user/0/com.klyx", "/data/user_de/0/com.klyx/")
        isPathInDirectories("/data/user/0/com.klyx", roots) shouldBe true
        isPathInDirectories("/data/user/0/com.klyx/files/settings.json", roots) shouldBe true
        isPathInDirectories("/data/user_de/0/com.klyx/files/test.kt", roots) shouldBe true
        isPathInDirectories("/data/user/0/com.klyx.other/files/test.kt", roots) shouldBe false
        isPathInDirectories("/storage/emulated/0/project/test.kt", roots) shouldBe false
        isPathInDirectories("/data/data/com.termux/files/home/test.kt", roots) shouldBe false
    }

    test("built-in JSON server supports private settings while workspace-dependent servers do not") {
        LspServerRegistry.findByLanguageId("json")!!.canOpenPrivateFile(android) shouldBe true
        for (language in listOf("rust", "typescript", "go", "python")) {
            LspServerRegistry.findByLanguageId(language)!!.canOpenPrivateFile(android) shouldBe false
        }
    }

    test("custom servers must explicitly opt in to content-only operation") {
        val custom = CustomLspServer(languageId = "json", command = "custom-json-server")
        custom.toConfig().canOpenPrivateFile(android) shouldBe false
        val optedIn = custom.copy(supportsContentOnly = true)
        val restored = Json.decodeFromString<CustomLspServer>(Json.encodeToString(optedIn))
        restored.toConfig().canOpenPrivateFile(android) shouldBe true
        restored.copy(manualSocket = true).toConfig().canOpenPrivateFile(android) shouldBe true
    }
})
