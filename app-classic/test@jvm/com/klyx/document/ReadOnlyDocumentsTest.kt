package com.klyx.document

import io.github.vinceglb.filekit.PlatformFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ReadOnlyDocumentsTest : FunSpec({
    val session = PlatformFile("/app/files/session.json")

    test("the app session file is protected through equivalent paths") {
        isWorkspaceSessionFile(session, session) shouldBe true
        isWorkspaceSessionFile(PlatformFile("/app/files/../files/session.json"), session) shouldBe true
        isWorkspaceSessionFile(PlatformFile("/app/files/./session.json"), session) shouldBe true
    }

    test("other session files and app settings stay editable") {
        isWorkspaceSessionFile(PlatformFile("/projects/example/session.json"), session) shouldBe false
        isWorkspaceSessionFile(PlatformFile("/app/files/settings.json"), session) shouldBe false
        isWorkspaceSessionFile(PlatformFile("/app/files/session.json.backup"), session) shouldBe false
    }
})
